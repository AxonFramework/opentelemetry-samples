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

package io.axoniq.demo.tracing.interfaces

import io.axoniq.demo.tracing.auction.*
import io.axoniq.demo.tracing.objectsregistry.AuctionOwnershipInfoItem
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
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.ScheduledFuture

class SimulatedParticipant(
    private val id: String,
    private val email: String,
    private val queryGateway: QueryGateway,
    private val commandGateway: CommandGateway,
    private val spanFactory: SpanFactory
) {
    private lateinit var activeAuctionSubscription: Disposable
    private lateinit var balanceSubscription: Disposable
    private lateinit var ownershipSubscription: Disposable
    private lateinit var auctionLoopSubscription: Disposable
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val items = mutableListOf<String>()
    private var balance: Long = 0
    private var lastPlacedAuction: Instant = Instant.now()

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
        spanFactory.createRootTrace { "auctionLoop" }.run {
            val timeFactor = ChronoUnit.SECONDS.between(lastPlacedAuction, Instant.now()) / 480
            val feelingFactor = Math.random()

            val totalFactor = timeFactor + feelingFactor
            if (totalFactor > 0.5) {
                // We need to auction something!
                if (items.isNotEmpty()) {
                    val item = items.random()
                    logger.info("[$email] Wanted to auction item $item!")
                    commandGateway.send<String>(CreateAuction(item, id, (200 + 200 * Math.random()).toLong()))
                }
                lastPlacedAuction = Instant.now()
            }


            this.auctionLoopSubscription = runTask((3000 + (Math.random() * 7000)).toLong()) {
                auctionLoop()
            }
        }
    }

    private val interestMap = ConcurrentSkipListMap<String, Double>()
    private val committedAuctionMoney = ConcurrentSkipListMap<String, Long>()

    private fun initializeActiveAuctionQuery() {
        val query = queryGateway.subscriptionQuery(
            GetActiveAuctions(),
            ResponseTypes.multipleInstancesOf(ActiveAuctionItem::class.java),
            ResponseTypes.instanceOf(ActiveAuctionItem::class.java)
        )

        this.activeAuctionSubscription = query.updates()
            .subscribeOn(Schedulers.boundedElastic())
            .doOnError {
                logger.error("Error in GetActiveAuctions updates", it)
                runTask(500) { initializeActiveAuctionQuery() }
            }
            .subscribe {
                if (it.currentBidder != id || it.state == ActiveAuctionState.ENDED) {
                    committedAuctionMoney.remove(it.identifier)
                }
                if (it.state == ActiveAuctionState.ENDED) {
                    interestMap.remove(it.identifier)
                }

                if (it.currentBidder == id) {
                    committedAuctionMoney[it.identifier] = it.currentBid ?: 0
                }

                if (it.currentBidder != id) {
                    val interest = interestMap.computeIfAbsent(it.identifier) { Math.random() }
                    if (interest > 0.6) {
                        val price = it.currentBid ?: it.minimumBid ?: 0
                        val maxToSpend = interest * balance
                        if (maxToSpend > price) {
                            val bid = (price + (maxToSpend - price) * 0.3 * Math.random()).toLong()
                            spanFactory.createRootTrace { "sendBid" }.run {
                                commandGateway.send<Void>(PlaceBidOnAuction(it.identifier, id, bid))
                            }
                        }
                    }
                }

            }

    }

    private fun initializeOwnershipQuery() {
        spanFactory.createRootTrace { "ownerShipQuery" }.run {
            val query = queryGateway.subscriptionQuery(
                GetAuctionObjects(),
                ResponseTypes.multipleInstancesOf(AuctionOwnershipInfoItem::class.java),
                ResponseTypes.instanceOf(AuctionOwnershipInfoItem::class.java)
            )
            query.initialResult()
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError {
                    logger.error("Error in GetAuctionObjects", it)
                    runTask(500) { initializeOwnershipQuery() }
                }.subscribe {
                    val relevantItems = it.filter { l -> l.owner == id }
                    items.addAll(relevantItems.map { l -> l.identifier })
                }
            this.ownershipSubscription = query.updates()
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError {
                    logger.error("Error in GetAuctionObjects updates", it)
                    runTask(500) { initializeOwnershipQuery() }
                }.subscribe {
                    if (it.owner == id && !items.contains(it.identifier)) {
                        logger.info("[$email] Received item ${it.identifier}")
                        items.add(it.identifier)
                    }
                    if (items.contains(it.identifier) && it.owner != id) {
                        logger.info("[$email] Sold item ${it.identifier}")
                        items.remove(it.identifier)
                    }
                }
        }
    }

    private fun initializeBalanceQuery() {
        spanFactory.createRootTrace { "balanceQuery" }.run {
            val query = queryGateway.subscriptionQuery(
                GetBalanceForParticipant(email),
                ResponseTypes.instanceOf(Long::class.java),
                ResponseTypes.instanceOf(Long::class.java)
            )
            query.initialResult()
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError {
                    logger.error("Error in GetBalanceForParticipant", it)
                    runTask(500) { initializeBalanceQuery() }
                }
                .subscribe { balance = it }
            this.balanceSubscription = query.updates()
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError {
                    logger.error("Error in GetBalanceForParticipant updates", it)
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
}
