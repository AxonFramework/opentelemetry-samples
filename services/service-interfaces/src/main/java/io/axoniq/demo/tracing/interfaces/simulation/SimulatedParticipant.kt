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
import io.axoniq.demo.tracing.participants.ParticipantBalanceUpdate
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
    private val currentAuctionsTime: MutableMap<String, Instant> = mutableMapOf()
    private val scheduler = Schedulers.newSingle(email)

    init {
        initializeOwnershipQuery()
        initializeBalanceQuery()
        initializeActiveAuctionQuery()
        startAuctionLoop()
    }

    private fun startAuctionLoop() {
        this.auctionLoopSubscription = runTask(scheduler, (1000 + (Math.random() * 2000)).toLong()) {
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
                    logger.debug("[$email] Want to auction item $itemToAuction!")
                    val minimumBid = if (balance < 0 && currentItems.count { !currentAuctions.contains(it.key) } == 1) {
                        abs(balance)
                    } else (200 + 200 * Math.random()).toLong()
                    currentAuctions += itemToAuction.identifier

                    commandGateway.send<String>(CreateAuction(itemToAuction.identifier, id, minimumBid))
                        .thenAccept { auctionId ->
                            logger.debug("[$email] Started auction for ${itemToAuction.identifier}! ID: $auctionId")
                        }
                        .exceptionally {
                            logger.error("[$email] Exception while sending bid!", it)
                            currentAuctions.remove(itemToAuction.identifier)
                            return@exceptionally null
                        }
                }
                lastPlacedAuction = Instant.now()
            }


            this.auctionLoopSubscription = runTask(scheduler, (3000 + (Math.random() * 7000)).toLong()) {
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
            .publishOn(scheduler)
            .doOnError {
                logger.error("[$email] Error in GetActiveAuctions updates", it)
                runTask(scheduler, 500) { initializeActiveAuctionQuery() }
            }
            .subscribe {
                handleAuctionUpdate(it)
            }

    }

    private fun handleAuctionUpdate(it: ActiveAuctionItem) {
        if (it.state == ActiveAuctionState.INACTIVE) {
            return
        }
        if (it.state == ActiveAuctionState.REVERTED && currentAuctions.contains(it.identifier)) {
            currentAuctions.remove(it.objectId)
            return
        }
        if (it.state == ActiveAuctionState.ENDED) {
            if (it.currentBidder == id) {
                boughtPendingItems[it.identifier] = it.currentBid ?: 0
            }
            committedAuctionMoney.remove(it.identifier)
            interestMap.remove(it.identifier)
            if (it.currentBidder == null && currentAuctions.contains(it.objectId)) {
                currentAuctions.remove(it.objectId)
            }
            return
        }
        if (it.currentBidder == id) {
            // Woohoo, my bid got finalized. Don't need to do anything
            logger.debug(
                "[$email] My bid of {} on auction {} got confirmed for item {}!",
                it.currentBid,
                it.identifier,
                it.objectId
            )
            return
        }

        val interest = interestMap.computeIfAbsent(it.identifier) { Math.random() }

        val currentPrice = (if (it.currentBid != null && ((it.currentBid ?: 0) > (it.minimumBid
                ?: 0))
        ) it.currentBid else it.minimumBid) ?: 0
        val ownBid = committedAuctionMoney[it.identifier] ?: -1
        if (ownBid > currentPrice) {
            return
        }

        val bid = calculateBid(it, interest, currentPrice)
        logger.debug("[$email] Want to bid $bid for current price $currentPrice on auction $it")
        if (bid > currentPrice) {
            committedAuctionMoney[it.identifier] = bid
            runTask(scheduler, (20 * 30 * Math.random()).toLong()) {
                spanFactory.createRootTrace { "[$email] sendBid" }.run {
                    logger.debug("[$email] Bidding $bid on auction $it")
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
        val currentBidMoney = committedAuctionMoney.filterKeys { id -> id != it.objectId }.values.sum()
        val currentPendingItemMoney = boughtPendingItems.values.sum()
        val availableMoney = balance - currentBidMoney - currentPendingItemMoney
        if(availableMoney < 0) {
            return -1
        }


        val maxToSpend = interest * availableMoney
        val availableForBid = maxToSpend - currentPrice
        val actualValueForBid = availableForBid * interest * Math.random()
        return currentPrice + actualValueForBid.toLong()
    }

    private fun initializeOwnershipQuery() {
        spanFactory.createRootTrace { "[$email] ownerShipQuery" }.run {
            val query = queryGateway.subscriptionQuery(
                GetAuctionObjects(auctionHouseId = auctionHouseId),
                ResponseTypes.instanceOf(AuctionOwnershipResponse::class.java),
                ResponseTypes.instanceOf(AuctionOwnershipInfoItem::class.java)
            )
            query.initialResult()
                .publishOn(scheduler)
                .doOnError {
                    logger.error("[$email] Error in GetAuctionObjects", it)
                    runTask(scheduler, 500) { initializeOwnershipQuery() }
                }.subscribe {
                    it.items.filter { l -> l.owner == id }
                        .forEach { l -> handleOwnershipItem(l) }
                }
            this.ownershipSubscription = query.updates()
                .publishOn(scheduler)
                .doOnError {
                    logger.error("[$email] Error in GetAuctionObjects updates", it)
                    runTask(scheduler, 500) { initializeOwnershipQuery() }
                }.subscribe {
                    handleOwnershipItem(it)
                }
        }
    }

    private fun handleOwnershipItem(item: AuctionOwnershipInfoItem) {
        if (item.owner == id && !currentItems.containsKey(item.identifier)) {
            logger.debug("[$email] Received item ${item.identifier}")
            currentItems[item.identifier] = item
        }
        if (currentItems.containsKey(item.identifier) && item.owner != id) {
            logger.debug("[$email] Sold item ${item.identifier}")
            currentItems.remove(item.identifier)
            currentAuctions.remove(item.identifier)
        }
    }

    private fun initializeBalanceQuery() {
        spanFactory.createRootTrace { "[$email] balanceQuery" }.run {
            val query = queryGateway.subscriptionQuery(
                GetBalanceForParticipant(email),
                ResponseTypes.instanceOf(Long::class.java),
                ResponseTypes.instanceOf(ParticipantBalanceUpdate::class.java)
            )
            query.initialResult()
                .publishOn(scheduler)
                .doOnError {
                    logger.error("[$email] Error in GetBalanceForParticipant", it)
                    runTask(scheduler, 500) { initializeBalanceQuery() }
                }
                .subscribe { balance = it }
            this.balanceSubscription = query.updates()
                .publishOn(scheduler)
                .doOnError {
                    logger.error("[$email] Error in GetBalanceForParticipant updates", it)
                    runTask(scheduler, 500) { initializeBalanceQuery() }
                }
                .subscribe {
                    balance = it.newBalance
                    logger.debug("[$email] balance was updated for ${it.reference}")
                    boughtPendingItems.remove(it.reference)
                }
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
