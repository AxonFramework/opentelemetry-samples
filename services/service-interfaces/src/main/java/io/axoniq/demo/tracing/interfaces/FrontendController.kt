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

import io.axoniq.demo.tracing.auction.ActiveAuctionItem
import io.axoniq.demo.tracing.auction.ActiveAuctionsResponse
import io.axoniq.demo.tracing.auction.GetActiveAuctions
import io.axoniq.demo.tracing.interfaces.simulation.GetSimulatedParticipants
import io.axoniq.demo.tracing.interfaces.simulation.SimulatedParticipantDto
import io.axoniq.demo.tracing.interfaces.simulation.SimulatedParticipantDtoResponse
import io.axoniq.demo.tracing.interfaces.simulation.SimulatedParticipantOrchestrator
import io.axoniq.demo.tracing.objectsregistry.AuctionOwnershipInfoItem
import io.axoniq.demo.tracing.objectsregistry.AuctionOwnershipResponse
import io.axoniq.demo.tracing.objectsregistry.GetAuctionObjects
import org.axonframework.messaging.responsetypes.ResponseTypes
import org.axonframework.queryhandling.QueryGateway
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.util.concurrent.TimeUnit

@RestController
@RequestMapping("api")
class FrontendController(
    private val orchestrator: SimulatedParticipantOrchestrator,
    private val queryGateway: QueryGateway,
) {
    @GetMapping("participants/count")
    fun getPartcipantCount() = orchestrator.desiredCount


    @PutMapping("participants/count/{amount}")
    fun setPartcipantCount(@PathVariable amount: Int) {
        orchestrator.desiredCount = amount
    }

    @GetMapping("auctions")
    fun auctions(): Flux<ServerSentEvent<*>> {
        val subscriptionQuery = queryGateway.subscriptionQuery(
            GetActiveAuctions(),
            ResponseTypes.instanceOf(ActiveAuctionsResponse::class.java),
            ResponseTypes.instanceOf(ActiveAuctionItem::class.java)
        )

        return subscriptionQuery
            .initialResult().flatMapIterable { it.auctions }
            .concatWith(subscriptionQuery.updates())
            .doOnTerminate {
                subscriptionQuery.close()
            }
            .buffer(Duration.ofMillis(500))
            .asServerSentEvents()
            .withHeartbeat()

    }


    @GetMapping("participants")
    fun participants(): Flux<ServerSentEvent<*>> {
        val subscriptionQuery = queryGateway.subscriptionQuery(
            GetSimulatedParticipants(),
            ResponseTypes.instanceOf(SimulatedParticipantDtoResponse::class.java),
            ResponseTypes.instanceOf(SimulatedParticipantDtoResponse::class.java)
        )

        return subscriptionQuery
            .initialResult()
            .publishOn(Schedulers.boundedElastic())
            .map { it.participants }
            .concatWith(subscriptionQuery.updates().map { it.participants })
            .doOnTerminate {
                subscriptionQuery.close()
            }
            .asServerSentEvents()
            .withHeartbeat()

    }


    @GetMapping("ownership")
    fun ownership(): Flux<ServerSentEvent<*>> {
        val query = queryGateway.subscriptionQuery(
            GetAuctionObjects(orchestrator.auctionHouseId),
            ResponseTypes.instanceOf(AuctionOwnershipResponse::class.java),
            ResponseTypes.instanceOf(AuctionOwnershipInfoItem::class.java)
        )

        return query.initialResult()
            .publishOn(Schedulers.boundedElastic())
            .flatMapIterable { it.items }
            .concatWith(query.updates())
            .doOnTerminate {
                query.close()
            }
            .buffer(Duration.ofMillis(500))
            .asServerSentEvents()
            .withHeartbeat()

    }
}


fun <T> Flux<T>.asServerSentEvents() = this.map { update ->
    ServerSentEvent.builder<T>()
        .event("message")
        .data(update)
        .build()
}

fun <T> Flux<ServerSentEvent<T>>.withHeartbeat(): Flux<ServerSentEvent<*>> {
    return Flux.merge(this, Flux.interval(Duration.ofSeconds(2))
        .publishOn(Schedulers.boundedElastic())
        .map { _ ->
            ServerSentEvent.builder<String>()
                .event("ping")
                .build()
        })
}
