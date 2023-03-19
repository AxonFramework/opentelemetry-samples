/*
 * Copyright (c) 2023-2023. AxonIQ
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.axoniq.demo.tracing.interfaces.simulation

import io.axoniq.demo.tracing.auction.*
import io.axoniq.demo.tracing.interfaces.runTask
import io.axoniq.demo.tracing.objectsregistry.AuctionOwnershipInfoItem
import io.axoniq.demo.tracing.objectsregistry.AuctionOwnershipResponse
import io.axoniq.demo.tracing.objectsregistry.GetAuctionObjects
import io.axoniq.demo.tracing.participants.GetBalanceForParticipant
import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.messaging.responsetypes.ResponseTypes
import org.axonframework.queryhandling.QueryGateway
import org.axonframework.tracing.SpanFactory
import org.slf4j.LoggerFactory
import reactor.core.Disposable
import reactor.core.scheduler.Schedulers
import java.time.Instant
import java.util.concurrent.ConcurrentSkipListMap
import kotlin.math.abs

class SimulatedParticipant(
    private val id: String,
    private val email: String,
    private val auctionHouseId: String,
    private val queryGateway: QueryGateway,
    private val commandGateway: CommandGateway,
    private val spanFactory: SpanFactory
) {
    private lateinit var activeAuctionSubscription: Disposable
    private lateinit var balanceSubscription: Disposable
    private lateinit var ownershipSubscription: Disposable
    private lateinit var auctionLoopSubscription: Disposable
    private val logger = LoggerFactory.getLogger(this::class.java)
    private var balance: Long = 0
    private var lastPlacedAuction: Instant = Instant.now()

    private val interestMap = ConcurrentSkipListMap<String, Double>()
    private val committedAuctionMoney = ConcurrentSkipListMap<String, Long>()
    private val boughtPendingItems = ConcurrentSkipListMap<String, Long>()
    private val currentItems: MutableMap<String, AuctionOwnershipInfoItem> = ConcurrentSkipListMap()
    private val currentAuctions: MutableList<String> = mutableListOf()
    private val scheduler = Schedulers.newParallel(email, 1)

    init {
        initializeOwnershipQuery()
        initializeBalanceQuery()
        initializeActiveAuctionQuery()
        startAuctionLoop()
    }

    private fun startAuctionLoop() {
        this.auctionLoopSubscription = runTask((1000 + (Math.random() * 2000)).toLong()) {
            auctionLoop()
        }
    }

    private fun auctionLoop() {
        spanFactory.createRootTrace { "[$email] auctionLoop" }.run {
            val feelingFactor = Math.random()

            if (feelingFactor > 0.6) {
                // We need to auction something!
                val itemToAuction = currentItems.filterKeys { !currentAuctions.contains(it) }.values.randomOrNull()
                if (itemToAuction != null) {
                    logger.info("[$email] Wanted to auction item $itemToAuction!")
                    currentAuctions += itemToAuction.identifier
                    val minimumBid = if(balance < 0 && currentItems.count { !currentAuctions.contains(it.key) } == 1) {
                        abs(balance)
                    } else (200 + 200 * Math.random()).toLong()
                    commandGateway
                        .send<Any>(
                            CreateAuction(itemToAuction.identifier, id,minimumBid)
                        )
                        .exceptionallyAsync {
                            currentAuctions.remove(itemToAuction.identifier)
                            return@exceptionallyAsync null
                        }
                }
                lastPlacedAuction = Instant.now()
            }


            this.auctionLoopSubscription = runTask((3000 + (Math.random() * 7000)).toLong()) {
                auctionLoop()
            }
        }
    }

    private fun initializeActiveAuctionQuery() {
        val query = queryGateway.subscriptionQuery(
            GetActiveAuctions(),
            ResponseTypes.instanceOf(ActiveAuctionsResponse::class.java),
            ResponseTypes.instanceOf(ActiveAuctionItem::class.java)
        )

        this.activeAuctionSubscription = query.updates()
            .subscribeOn(scheduler)
            .doOnError {
                logger.error("[$email] Error in GetActiveAuctions updates", it)
                runTask(500) { initializeActiveAuctionQuery() }
            }
            .subscribe {
                handleAuctionUpdate(it)
            }

    }

    private fun handleAuctionUpdate(it: ActiveAuctionItem) {
        if (it.state == ActiveAuctionState.REVERTED && currentAuctions.contains(it.identifier)) {
            currentAuctions.remove(it.identifier)
            return
        }
        if (it.state == ActiveAuctionState.ENDED) {
            if (it.currentBidder == id) {
                boughtPendingItems[it.objectId] = it.currentBid ?: 0
            }
            committedAuctionMoney.remove(it.identifier)
            interestMap.remove(it.identifier)
            return
        }
        if (it.currentBidder == id) {
            // Woohoo, my bid got finalized. Don't need to do anything
            logger.info("[$email] My bid of {} on auction {} got confirmed!", it.currentBid, it.identifier)
            return
        }

        val interest = interestMap.computeIfAbsent(it.identifier) { Math.random() }
        if (interest < 0.7) {
            return
        }

        val currentPrice = it.currentBid ?: it.minimumBid ?: 0
        val ownBid = committedAuctionMoney[it.identifier] ?: -1

        if (ownBid > currentPrice) {
            return
        }

        val bid = calculateBid(it, interest, currentPrice)

        if (bid > currentPrice) {
            runTask((20 * 30 * Math.random()).toLong()) {
                spanFactory.createRootTrace { "[$email] sendBid" }.run {
                    commandGateway.send<Boolean>(PlaceBidOnAuction(it.identifier, id, bid)).thenAccept { success ->
                        if (!success) {
                            committedAuctionMoney.remove(it.identifier)
                        }
                    }
                }
            }
        }
    }

    private fun calculateBid(
        it: ActiveAuctionItem,
        interest: Double,
        currentPrice: Long
    ): Long {
        val currentBidMoney = committedAuctionMoney.filterKeys { id -> id != it.identifier }.values.sum()
        val currentPendingItemMoney = boughtPendingItems.values.sum()
        val availableMoney = balance - currentBidMoney - currentPendingItemMoney


        val maxToSpend = interest * availableMoney
        val availableForBid = maxToSpend - currentPrice
        val actualValueForBid = availableForBid * interest * Math.random()
        val bid = currentPrice + actualValueForBid.toLong()
        if (bid > currentPrice) {
            committedAuctionMoney[it.identifier] = bid
        }
        return bid
    }

    private fun initializeOwnershipQuery() {
        spanFactory.createRootTrace { "[$email] ownerShipQuery" }.run {
            val query = queryGateway.subscriptionQuery(
                GetAuctionObjects(auctionHouseId = auctionHouseId),
                ResponseTypes.instanceOf(AuctionOwnershipResponse::class.java),
                ResponseTypes.instanceOf(AuctionOwnershipInfoItem::class.java)
            )
            query.initialResult()
                .subscribeOn(scheduler)
                .doOnError {
                    logger.error("[$email] Error in GetAuctionObjects", it)
                    runTask(500) { initializeOwnershipQuery() }
                }.subscribe {
                    it.items.filter { l -> l.owner == id }
                        .forEach { l -> handleOwnershipItem(l) }
                }
            this.ownershipSubscription = query.updates()
                .subscribeOn(scheduler)
                .doOnError {
                    logger.error("[$email] Error in GetAuctionObjects updates", it)
                    runTask(500) { initializeOwnershipQuery() }
                }.subscribe {
                    handleOwnershipItem(it)
                }
        }
    }

    private fun handleOwnershipItem(item: AuctionOwnershipInfoItem) {
        if (item.owner == id && !currentItems.containsKey(item.identifier)) {
            logger.info("[$email] Received item ${item.identifier}")
            currentItems[item.identifier] = item
            boughtPendingItems.remove(item.identifier)
        }
        if (currentItems.containsKey(item.identifier) && item.owner != id) {
            logger.info("[$email] Sold item ${item.identifier}")
            currentItems.remove(item.identifier)
            currentAuctions.remove(item.identifier)
        }
    }

    private fun initializeBalanceQuery() {
        spanFactory.createRootTrace { "[$email] balanceQuery" }.run {
            val query = queryGateway.subscriptionQuery(
                GetBalanceForParticipant(email),
                ResponseTypes.instanceOf(Long::class.java),
                ResponseTypes.instanceOf(Long::class.java)
            )
            query.initialResult()
                .subscribeOn(scheduler)
                .doOnError {
                    logger.error("[$email] Error in GetBalanceForParticipant", it)
                    runTask(500) { initializeBalanceQuery() }
                }
                .subscribe { balance = it }
            this.balanceSubscription = query.updates()
                .subscribeOn(scheduler)
                .doOnError {
                    logger.error("[$email] Error in GetBalanceForParticipant updates", it)
                    runTask(500) { initializeBalanceQuery() }
                }
                .subscribe { balance = it }
        }
    }

    fun terminate() {
        this.balanceSubscription.dispose()
        this.activeAuctionSubscription.dispose()
        this.ownershipSubscription.dispose()
        this.auctionLoopSubscription.dispose()
    }

    fun toDto(terminated: Boolean = false): SimulatedParticipantDto {
        return SimulatedParticipantDto(
            id,
            terminated,
            email,
            balance,
            committedAuctionMoney,
            currentItems.map {
                ParticipantItem(
                    it.value.identifier,
                    it.value.name,
                    currentAuctions.contains(it.key)
                )
            }
        )
    }
}
