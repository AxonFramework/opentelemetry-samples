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

package io.axoniq.demo.tracing.auction

import org.axonframework.commandhandling.CommandHandler
import org.axonframework.common.IdentifierFactory
import org.axonframework.deadline.DeadlineManager
import org.axonframework.deadline.annotation.DeadlineHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.spring.stereotype.Aggregate
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import io.axoniq.demo.tracing.auction.*

@Aggregate
class Auction {
    @AggregateIdentifier
    private lateinit var id: String
    private lateinit var objectId: String
    private lateinit var owner: String
    private var minimalBid: Long = 0
    private var state = State.CREATED
    private val bids = mutableListOf<Bid>()
    private val winningBid: Bid?
        get() = bids.maxByOrNull { it.amount }

    enum class State {
        CREATED, STARTED, ENDED
    }

    @CommandHandler
    constructor(command: CreateAuction, @Autowired deadlineManager: DeadlineManager) {
        val endTime = Instant.now().plusSeconds(30)
        AggregateLifecycle.apply(
            AuctionCreated(
                IdentifierFactory.getInstance().generateIdentifier(),
                command.objectId,
                command.owner,
                command.minimumPrice,
                endTime
            )
        )
        deadlineManager.schedule(endTime, "closeAuction")
    }

    @CommandHandler
    fun on(command: PlaceBidOnAuction) {
        val highest = winningBid?.amount ?: minimalBid
        if (highest >= command.price) {
//            AggregateLifecycle.apply(BidRejectedOnAuction(id, command.participantId, command.price))
            return
        }
        AggregateLifecycle.apply(BidPlacedOnAuction(id, command.participantId, command.price))
    }

    @DeadlineHandler(deadlineName = "closeAuction")
    fun on() {
        val winningBid = bids.maxByOrNull { it.amount }
        AggregateLifecycle.apply(
            AuctionEnded(
                auctionId = id,
                winningParticipant = winningBid?.participantId,
                winningPrice = winningBid?.amount,
                endTime = Instant.now(),
                seller = owner,
                objectId = objectId
            )
        )
    }

    @EventSourcingHandler
    fun on(event: AuctionCreated) {
        this.id = event.auctionId
        this.objectId = event.objectId
        this.state = State.STARTED
        this.owner = event.owner
        this.minimalBid = event.minimumPrice
    }

    @EventSourcingHandler
    fun on(event: AuctionEnded) {
        this.state = State.ENDED
    }

    @EventSourcingHandler
    fun on(event: BidPlacedOnAuction) {
        bids.add(Bid(event.participant, event.price))
    }

    private constructor()
}

data class Bid(
    val participantId: String, val amount: Long
)



