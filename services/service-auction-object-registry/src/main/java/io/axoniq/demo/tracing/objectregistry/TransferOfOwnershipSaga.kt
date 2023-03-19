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
import io.axoniq.demo.tracing.auction.AuctionEnded
import io.axoniq.demo.tracing.auction.RevertAuction
import io.axoniq.demo.tracing.objectsregistry.TransferAuctionObject
import io.axoniq.demo.tracing.participants.BalanceAddedToParticipant
import io.axoniq.demo.tracing.participants.BalanceDeducted
import io.axoniq.demo.tracing.participants.DeductBalance
import io.axoniq.demo.tracing.participants.IncreaseBalance
import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.modelling.saga.EndSaga
import org.axonframework.modelling.saga.SagaEventHandler
import org.axonframework.modelling.saga.SagaLifecycle
import org.axonframework.modelling.saga.StartSaga
import org.axonframework.spring.stereotype.Saga
import org.springframework.context.annotation.Profile

@Saga
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class TransferOfOwnershipSaga {
    private lateinit var seller: String
    private lateinit var buyer: String
    private lateinit var objectId: String
    private var amount: Long = 0

    @StartSaga
    @SagaEventHandler(associationProperty = "auctionId")
    fun on(event: AuctionEnded, commandGateway: CommandGateway) {
        if (event.winningParticipant == null || event.winningPrice == null) {
            SagaLifecycle.end()
            return
        }


        seller = event.seller
        buyer = event.winningParticipant!!
        amount = event.winningPrice!!
        objectId = event.objectId
        // We need to transfer from original owner to new one
        commandGateway.send<Void>(DeductBalance(buyer, event.auctionId, amount))
            .exceptionally {
                commandGateway.send<Void>(RevertAuction(auctionId = event.auctionId, reason = "Buyer didn't have the money!"))
                    .get()
            }
    }

    @SagaEventHandler(associationProperty = "reference", keyName = "auctionId")
    fun on(event: BalanceDeducted, commandGateway: CommandGateway) {
        commandGateway.send<Void>(IncreaseBalance(seller, event.reference, amount))
    }

    @SagaEventHandler(associationProperty = "reference", keyName = "auctionId")
    @EndSaga
    fun on(event: BalanceAddedToParticipant, commandGateway: CommandGateway) {
        commandGateway.send<Void>(TransferAuctionObject(objectId, buyer))
    }
}
