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

import io.axoniq.demo.tracing.objectsregistry.SubmitAuctionObject
import io.axoniq.demo.tracing.participants.RegisterParticipant
import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.common.IdentifierFactory
import org.axonframework.queryhandling.QueryGateway
import org.axonframework.tracing.SpanFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicInteger

@Service
class SimulatedParticipantOrchestrator(
    private val queryGateway: QueryGateway,
    private val commandGateway: CommandGateway,
    private val spanFactory: SpanFactory
) {
    private val emailSuffix = IdentifierFactory.getInstance().generateIdentifier().split("-")[0]
    private var desiredCount = 20
    private val indexInteger = AtomicInteger()
    private val objectInteger = AtomicInteger()

    private val simulatedParticipants = mutableListOf<SimulatedParticipant>()

    fun setDesiredCount(count: Int) {
        desiredCount = count
    }

    fun getDesiredCount() = desiredCount

    @Scheduled(fixedDelay = 1000, initialDelay = 10000)
    fun setup() {
        while (simulatedParticipants.size < desiredCount) {
            val index = indexInteger.incrementAndGet()
            val id = commandGateway.sendAndWait<String>(RegisterParticipant("$emailSuffix-$index@axoniq-auction.io"))
            1.rangeTo(5).forEach {_ ->
                val itemId = objectInteger.incrementAndGet()
                commandGateway.sendAndWait<String>(SubmitAuctionObject("Auction Object $itemId", id))
            }
            simulatedParticipants += SimulatedParticipant(
                id = id,
                email = "$emailSuffix-$index@axoniq-auction.io",
                queryGateway = queryGateway,
                commandGateway = commandGateway,
                spanFactory = spanFactory
            )
        }


        while (simulatedParticipants.size > desiredCount) {
            val participantToTerminate = simulatedParticipants.random()
            simulatedParticipants.remove(participantToTerminate)
            participantToTerminate.terminate()
        }
    }
}
