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

import com.github.javafaker.Faker
import io.axoniq.demo.tracing.objectsregistry.SubmitAuctionObject
import io.axoniq.demo.tracing.participants.RegisterParticipant
import io.micrometer.core.instrument.MeterRegistry
import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.common.IdentifierFactory
import org.axonframework.queryhandling.QueryGateway
import org.axonframework.queryhandling.QueryHandler
import org.axonframework.queryhandling.QueryUpdateEmitter
import org.axonframework.tracing.SpanFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class SimulatedParticipantOrchestrator(
    private val queryGateway: QueryGateway,
    private val commandGateway: CommandGateway,
    private val spanFactory: SpanFactory,
    private val queryUpdateEmitter: QueryUpdateEmitter,
    private val meterRegistry: MeterRegistry,
) {
    val auctionHouseId = IdentifierFactory.getInstance().generateIdentifier()
    private val faker = Faker()

    private val simulatedParticipants = mutableListOf<SimulatedParticipant>()
    var desiredCount = 20

    @QueryHandler
    fun handle(query: GetSimulatedParticipants): SimulatedParticipantDtoResponse {
        return SimulatedParticipantDtoResponse(simulatedParticipants.map { it.toDto() })
    }

    @Scheduled(fixedRate = 200)
    fun sendUpdate() {
        queryUpdateEmitter.emit(
            GetSimulatedParticipants::class.java,
            { true },
            handle(GetSimulatedParticipants())
        )
    }

    @Scheduled(fixedDelay = 1000, initialDelay = 10000)
    fun setup() {
        while (simulatedParticipants.size < desiredCount) {
            val email = "${faker.name().firstName()}@${auctionHouseId}.io"
            val id = commandGateway.sendAndWait<String>(RegisterParticipant(email))
            1.rangeTo(3).forEach { _ ->
                val name = getNewName()
                commandGateway.sendAndWait<String>(SubmitAuctionObject(name, id, auctionHouseId))
            }
            simulatedParticipants += SimulatedParticipant(
                id = id,
                email = email,
                queryGateway = queryGateway,
                commandGateway = commandGateway,
                spanFactory = spanFactory,
                auctionHouseId = auctionHouseId,
                meterRegistry = meterRegistry
            )
        }


        while (simulatedParticipants.size > desiredCount) {
            val participantToTerminate = simulatedParticipants.random()
            simulatedParticipants.remove(participantToTerminate)
            participantToTerminate.terminate()

            queryUpdateEmitter.emit(
                GetSimulatedParticipants::class.java,
                { true },
                SimulatedParticipantDtoResponse(participants = listOf(participantToTerminate.toDto(true)))
            )
        }
    }

    private val generatedNames = mutableListOf<String>()
    fun getNewName(): String {
        val name = faker.commerce().productName()
        if (generatedNames.contains(name)) {
            return getNewName()
        }
        generatedNames.add(name)
        return name
    }
}
