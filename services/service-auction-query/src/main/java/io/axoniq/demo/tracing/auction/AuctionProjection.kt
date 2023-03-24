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

import io.axoniq.demo.tracing.auction.*
import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.EventHandler
import org.axonframework.queryhandling.QueryHandler
import org.axonframework.queryhandling.QueryUpdateEmitter
import org.springframework.stereotype.Service

@Service
@ProcessingGroup("auction")
class AuctionProjection(
    private val queryUpdateEmitter: QueryUpdateEmitter,
    private val repository: AuctionInfoRepository
) {
    @EventHandler
    fun on(event: AuctionCreated) {
        val auction = repository.save(
            AuctionInformation(
                event.auctionId,
                event.auctionHouseId,
                event.objectId,
                ActiveAuctionState.STARTED,
                event.minimumPrice,
                0,
                null,
                event.endTime
            )
        )
        auction.emit()
    }

    @EventHandler
    fun on(event: AuctionEnded) {
        val auction = repository.findById(event.auctionId).orElseThrow()
        auction.state = ActiveAuctionState.ENDED
        auction.emit()
    }

    @EventHandler
    fun on(event: AuctionReverted) {
        val auction = repository.findById(event.auctionId).orElseThrow()
        auction.state = ActiveAuctionState.REVERTED
        auction.emit()
    }

    @EventHandler
    fun on(event: BidPlacedOnAuction) {
        val auction = repository.findById(event.auctionId).orElseThrow()
        auction.currentBid = event.price
        auction.currentBidder = event.participant
        auction.emit()
    }

    private fun AuctionInformation.emit() {
        queryUpdateEmitter.emit(GetActiveAuctions::class.java, { it.auctionHouseId == this.auctionHouseId }, this.toDto())

    }

    @QueryHandler
    fun on(query: GetActiveAuctions): ActiveAuctionsResponse {
        return ActiveAuctionsResponse(repository.findAllByStateAndAuctionHouseId(ActiveAuctionState.STARTED, query.auctionHouseId).map { it.toDto() })
    }

    fun AuctionInformation.toDto() =
        ActiveAuctionItem(identifier, objectId, state, minimumBid, currentBid, currentBidder, endTime)
}
