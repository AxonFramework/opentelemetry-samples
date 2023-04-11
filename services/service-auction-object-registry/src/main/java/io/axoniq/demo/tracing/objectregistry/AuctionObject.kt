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

package io.axoniq.demo.tracing.objectregistry

import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.axoniq.demo.tracing.objectsregistry.AuctionObjectOwnerTransferred
import io.axoniq.demo.tracing.objectsregistry.AuctionObjectSubmitted
import io.axoniq.demo.tracing.objectsregistry.SubmitAuctionObject
import io.axoniq.demo.tracing.objectsregistry.TransferAuctionObject
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.common.IdentifierFactory
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.spring.stereotype.Aggregate

@Aggregate(snapshotTriggerDefinition = "snapshotTriggerDefinition")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class AuctionObject {
    @AggregateIdentifier
    private lateinit var id: String
    private lateinit var owner: String

    @CommandHandler
    constructor(command: SubmitAuctionObject) {
        AggregateLifecycle.apply(
            AuctionObjectSubmitted(
                IdentifierFactory.getInstance().generateIdentifier(),
                command.name,
                command.initialOwner,
                command.auctionHouseId,
            )
        )
    }

    @CommandHandler
    fun on(command: TransferAuctionObject) {
        AggregateLifecycle.apply(AuctionObjectOwnerTransferred(id, owner, command.owner))
    }

    @EventSourcingHandler
    fun on(event: AuctionObjectSubmitted) {
        this.id = event.id
        this.owner = event.owner
    }

    @EventSourcingHandler
    fun on(event: AuctionObjectOwnerTransferred) {
        this.owner = event.newOwner
    }

    private constructor()
}

