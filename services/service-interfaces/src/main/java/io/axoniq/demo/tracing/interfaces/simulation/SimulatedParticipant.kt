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
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.messaging.responsetypes.ResponseTypes
import org.axonframework.queryhandling.QueryGateway
import org.axonframework.tracing.SpanFactory
import org.slf4j.LoggerFactory
import reactor.core.Disposable
import reactor.core.scheduler.Schedulers
import java.time.Instant
import java.util.concurrent.ConcurrentSkipListMap

class SimulatedParticipant(
    private val id: String,
    private val email: String,
    private val auctionHouseId: String,
    private val queryGateway: QueryGateway,
    private val commandGateway: CommandGateway,
    private val spanFactory: SpanFactory,
    private val meterRegistry: MeterRegistry,
) {
    private val biddingFactor = Math.random()
    private lateinit var activeAuctionSubscription: Disposable
    private lateinit var balanceSubscription: Disposable
    private lateinit var ownershipSubscription: Disposable
    private lateinit var auctionLoopSubscription: Disposable

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val scheduler = Schedulers.boundedElastic()

    private var balance: Long = 0

    private val itemNames = ConcurrentSkipListMap<String, String>()
    private val items: MutableMap<String, ItemInPossession> = ConcurrentSkipListMap()
    private val auctions: MutableMap<String, ActiveAuction> = ConcurrentSkipListMap()
    private val bids: MutableMap<String, ActiveBid> = ConcurrentSkipListMap()

    inner class ItemInPossession(
        val identifier: String,
        val name: String,
        val boughtFor: Long,
    )

    /**
     * Represents an auction that I am participating in
     */
    inner class ActiveAuction(
        val objectId: String,
        val objectName: String,
        var auctionId: String? = null,
        var createdAt: Instant? = null,
        var receivedBalanceUpdate: Boolean = false,
        var receivedItemUpdate: Boolean = false,
    ) {
        fun cleanIfComplete() {
            if (!receivedBalanceUpdate || !receivedItemUpdate) {
                return
            }
            auctions.remove(this.objectId)
        }

        fun markItemUpdated() {
            this.receivedItemUpdate = true
            cleanIfComplete()
        }

        fun markBalanceUpdated() {
            this.receivedBalanceUpdate = true
            cleanIfComplete()
        }
    }

    inner class ActiveBid(
        val objectId: String,
        val auctionId: String,
        val interest: Double,
        var currentBid: Long,
        var receivedWinUpdate: Boolean = false,
        var receivedBalanceUpdate: Boolean = false,
        var receivedItemUpdate: Boolean = false,
    ) {
        fun markItemUpdated() {
            this.receivedItemUpdate = true
            cleanIfComplete()
        }

        fun markBalanceUpdated() {
            this.receivedBalanceUpdate = true
            cleanIfComplete()
        }

        fun markWinUpdated() {
            this.receivedWinUpdate = true
            cleanIfComplete()
        }

        fun cleanIfComplete() {
            if (!receivedWinUpdate || !receivedBalanceUpdate || !receivedItemUpdate) {
                return
            }
            bids.remove(this.auctionId)
        }
    }

    init {
        initializeOwnershipQuery()
        initializeBalanceQuery()
        initializeActiveAuctionQuery()
        startAuctionLoop()

        val name = email.split("@")[0]
        meterRegistry.gauge("participant_money", Tags.of("name", name), balance) {
            balance.toDouble()
        }
        meterRegistry.gauge("participant_worth", Tags.of("name", name), items) {
            balance.toDouble() + items.values.sumOf { it.boughtFor }
        }
        meterRegistry.gauge("participant_items", Tags.of("name", name), items) {
            items.size.toDouble()
        }
    }

    private fun startAuctionLoop() {
        this.auctionLoopSubscription = runTask(scheduler, (1000 + (Math.random() * 2000)).toLong()) {
            auctionLoop()
        }
    }

    private fun auctionLoop() {
        spanFactory.createRootTrace { "[$email] auctionLoop" }.run {
            val feelingFactor = Math.random()
            if (feelingFactor <= 0.6) {
                return@run
            }
            // I want to auction something!
            pickItemToAuction()?.let { itemToAuction ->
                logger.debug("[$email] Want to auction item $itemToAuction!")
                val minimumBid = (200 + 200 * Math.random()).toLong()

                try {
                    val auctionId = commandGateway.sendAndWait<String>(CreateAuction(
                        objectId = itemToAuction.identifier,
                        owner = id,
                        minimumPrice = minimumBid
                    ))

                    auctions[itemToAuction.identifier] = ActiveAuction(
                        objectId = itemToAuction.identifier,
                        objectName = itemToAuction.name,
                        createdAt = Instant.now(),
                        auctionId = auctionId
                    )
                    logger.info("[$email] Started auction for ${itemToAuction.name}! AuctionId: $auctionId")
                } catch (e: Exception) {
                    logger.error("[$email] Exception while sending bid!", e)
                    auctions.remove(itemToAuction.identifier)
                }
            }
        }

        this.auctionLoopSubscription = runTask(scheduler, (3000 + (Math.random() * 7000)).toLong()) {
            auctionLoop()
        }
    }

    private fun pickItemToAuction(): ItemInPossession? {
        return items
            .filterKeys { objectId -> !auctions.containsKey(objectId) }
            .values.randomOrNull()
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

    private fun handleAuctionUpdate(update: ActiveAuctionItem) {
        if (update.state == ActiveAuctionState.INACTIVE) {
            return
        }
        if (update.state == ActiveAuctionState.REVERTED) {
            if (auctions.remove(update.identifier) != null) {
                logger.info("[$email] Received auction revert for auction ${update.identifier} of object ${update.objectId}")
            }
            return
        }
        if (update.state == ActiveAuctionState.ENDED) {
            if (update.currentBidder == null) {
                // We didn't sell the item successfully. Remove from locks
                auctions.remove(update.objectId)
            }
            if (update.currentBidder != id) {
                // We didn't win the item. Remove any bids
                bids.remove(update.identifier)
            }
            return
        }
        if (update.currentBidder == id) {
            // Woohoo, my bid got finalized. Don't need to do anything
            bids[update.identifier]?.markWinUpdated()
            logger.debug(
                "[$email] My bid of {} on auction {} got confirmed for item {}!",
                update.currentBid,
                update.identifier,
                update.objectId
            )
            return
        }
        if (auctions.containsKey(update.objectId)) {
            // My own auction, I won't bid
            return
        }

        val currentBidInfo = bids.computeIfAbsent(update.identifier) {
            ActiveBid(
                objectId = update.objectId,
                auctionId = update.identifier,
                interest = Math.random(),
                currentBid = 0
            )
        }
        if (currentBidInfo.interest < 0.3) {
            return
        }

        val price: Long = when {
            (update.currentBid ?: 0) > 0 -> update.currentBid ?: 0
            else -> (update.minimumBid ?: 0)
        }
        if (currentBidInfo.currentBid > price) {
            // This is an older update. There will come one a bit later with our bid!
            return
        }

        val bid = calculateBid(update, currentBidInfo, price)
        logger.debug("[$email] Want to bid $bid for current price $price on auction $update")
        if (bid > price) {
            currentBidInfo.currentBid = bid
            runTask(scheduler, (20 * 30 * Math.random()).toLong()) {
                spanFactory.createRootTrace { "[$email] sendBid" }.run {
                    logger.debug("[$email] Bidding $bid on auction $update")
                    commandGateway.send<Boolean>(PlaceBidOnAuction(update.identifier, id, bid))
                }
            }
        } else {
            currentBidInfo.currentBid = 0
        }
    }

    private fun calculateBid(
        it: ActiveAuctionItem,
        currentBid: ActiveBid,
        price: Long
    ): Long {
        val interest = currentBid.interest
        val currentBidMoney = bids.filterValues { bid -> bid.auctionId != it.identifier }.values.sumOf { it.currentBid }
        val availableMoney = balance - currentBidMoney
        if (availableMoney < 0) {
            return -1
        }


        val maxToSpend = interest * availableMoney
        val availableForBid = maxToSpend - price
        val actualValueForBid = availableForBid * interest * biddingFactor * 0.5
        return price + actualValueForBid.toLong()
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
                    it.items
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
        itemNames[item.identifier] = item.name
        if (item.owner == id && !items.containsKey(item.identifier)) {
            logger.info("[$email] Received item ${item.identifier}")
            val bid = bids.values.firstOrNull { it.objectId == item.identifier }
            items[item.identifier] = ItemInPossession(item.identifier, item.name, bid?.currentBid ?: 0)
            bid?.markItemUpdated()
        }
        if (items.containsKey(item.identifier) && item.owner != id) {
            logger.info("[$email] Gave item ${item.identifier} to ${item.owner}")
            items.remove(item.identifier)
            auctions[item.identifier]?.markItemUpdated()
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
                    if (it.update > 0) {
                        auctions.values.firstOrNull { a -> a.auctionId == it.reference }?.markBalanceUpdated()

                    } else {
                        bids[it.reference]?.markBalanceUpdated()
                    }
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
            id = id,
            terminated = terminated,
            email = email,
            balance = balance,
            activeBids = bids.filterValues { it.currentBid > 0 }.map { (_, value) ->
                ActiveBidDto(
                    auctionId = value.auctionId,
                    objectId = value.objectId,
                    objectName = itemNames[value.objectId] ?: "Unknown",
                    bid = value.currentBid,
                    interest = value.interest,
                    receivedWinUpdate = value.receivedWinUpdate,
                    receivedBalanceUpdate = value.receivedBalanceUpdate,
                    receivedItemUpdate = value.receivedItemUpdate
                )
            },
            items = items.map {
                val auction = auctions[it.key]
                ParticipantItem(
                    id = it.value.identifier,
                    name = it.value.name,
                    boughtFor = it.value.boughtFor,
                    auctioning = auction != null,
                    auctionStarted = auction?.createdAt,
                    receivedBalanceUpdate = auction?.receivedBalanceUpdate,
                    receivedItemUpdate = auction?.receivedItemUpdate,
                )
            }
        )
    }
}
