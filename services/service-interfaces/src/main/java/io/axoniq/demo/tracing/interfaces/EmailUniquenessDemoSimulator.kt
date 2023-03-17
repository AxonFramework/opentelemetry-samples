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

import io.axoniq.demo.tracing.participants.ChangeParticipantEmail
import io.axoniq.demo.tracing.participants.RegisterParticipant
import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.common.IdentifierFactory
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Registers participants with different emails, sometimes clashing the email and closing the account.
 */
@Component
@Profile("uniquedemo")
class EmailUniquenessDemoSimulator(
    private val commandGateway: CommandGateway
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val possibleNames = listOf(
        "steven.uniquedemo",
        "mitchell.uniquedemo",
        "gerard.uniquedemo",
    )

    @Scheduled(fixedRate = 10000, initialDelay = 2000)
    fun handle() {
        val name = possibleNames.random()
        val addition = IdentifierFactory.getInstance().generateIdentifier().split("-")[0]
        val completeName = "$name.$addition"
        logger.info("Starting EmailUniquenessDemoSimulator for name {}", completeName)
        commandGateway.send<String>(RegisterParticipant("$completeName@gmail.com")).whenComplete { id, _ ->
            logger.info("{} registered @gmail.com", completeName)

            commandGateway.send<String>(RegisterParticipant("$completeName@gmail.com")).whenComplete { _, _ ->
                logger.info("{} registering @gmail.com a second time, which should fail", completeName)
            }

            runTask(200) {
                logger.info("{} changing mail to @axoniq.io", completeName)
                commandGateway.send<Void>(ChangeParticipantEmail(id, "$completeName@axoniq.io")).whenComplete { _, _ ->
                    runTask(200) {
                        logger.info("{} changed mail to @axoniq.io", completeName)
                        commandGateway.send<String>(RegisterParticipant("$completeName@gmail.com"))
                            .whenComplete { id, t ->
                                if (t == null) {
                                    logger.info("Was now able to register $completeName@axoniq.io, as expected!")
                                } else {
                                    logger.error("Was not able to register $completeName@axoniq.io. Was the mail not unclaimed somehow?")
                                }
                            }
                    }
                }
            }
        }
    }
}
