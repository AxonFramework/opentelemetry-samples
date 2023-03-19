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

package io.axoniq.demo.tracing.participants

import org.axonframework.commandhandling.CommandHandler
import org.axonframework.common.IdentifierFactory
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle.apply
import org.axonframework.spring.stereotype.Aggregate

@Aggregate
class ParticipantAggregate {
    @AggregateIdentifier
    private lateinit var id: String
    private var email: String? = null
    private var changingEmail: Boolean = false
    private var balance = 0L

    @CommandHandler
    constructor(command: RegisterParticipant) {
        val id = IdentifierFactory.getInstance().generateIdentifier()
        apply(ParticipantRegisteredAwaitingEmailConfirmation(id))
        apply(ParticipantEmailRequested(id, command.requestedEmail))
    }

    @CommandHandler
    fun on(command: ChangeParticipantEmail) {
        assert(email != null) { "Cannot change email when initial email has not been confirmed yet!" }
        assert(!changingEmail) { "Cannot change email when already changing it!" }
        apply(ParticipantEmailChangeRequested(command.id, email!!, command.requestedEmail))
    }

    @EventSourcingHandler
    fun on(event: ParticipantRegisteredAwaitingEmailConfirmation) {
        id = event.id
    }

    @CommandHandler
    fun on(command: ConfirmParticipantEmail) {
        val creation = email == null
        apply(ParticipantEmailConfirmed(id, command.email))

        if (creation) {
            // New customers get 20 euros for free
            apply(BalanceAddedToParticipant(id, "0", 5000, 5000, "New account creation"))
        }
    }

    @CommandHandler
    fun on(command: DenyParticipantEmail) {
        apply(ParticipantEmailRequestRejected(command.id, command.email))
        if (email != null) {
            // Already had an email address, ignore the rejection and leave account on old email
            return
        }
        // No email address. Was a creation, not a change. Close account.
        apply(ParticipantRemoved(command.id, "Email was already in use."))
    }

    @CommandHandler
    fun on(command: DeductBalance) {
//        if (command.amount > balance) {
//            throw IllegalStateException("Not a high enough balance! Current: $balance, needed: ${command.amount}")
//        }
        apply(BalanceDeducted(id, command.reference, command.amount, balance - command.amount))
    }

    @CommandHandler
    fun on(command: IncreaseBalance) {
        apply(BalanceAddedToParticipant(id, command.reference, command.amount, balance + command.amount, ""))
    }

    @EventSourcingHandler
    fun on(event: ParticipantEmailConfirmed) {
        this.email = event.email
    }

    @EventSourcingHandler
    fun on(event: BalanceDeducted) {
        this.balance = event.newBalance
    }

    @EventSourcingHandler
    fun on(event: BalanceAddedToParticipant) {
        this.balance = event.newBalance
    }

    private constructor()
}


