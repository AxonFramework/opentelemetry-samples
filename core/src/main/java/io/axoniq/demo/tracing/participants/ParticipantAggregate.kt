package io.axoniq.demo.tracing.participants

import org.axonframework.commandhandling.CommandHandler
import org.axonframework.common.IdentifierFactory
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle.apply
import org.axonframework.modelling.command.TargetAggregateIdentifier
import org.axonframework.spring.stereotype.Aggregate

@Aggregate
class ParticipantAggregate {
    @AggregateIdentifier
    private lateinit var id: String
    private var email: String? = null
    private var changingEmail: Boolean = false
    private var balance = 0.0

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
            apply(CreditsAddedToParticipant(id, 20.0, 20.0, "New account creation"))
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

    @EventSourcingHandler
    fun on(event: ParticipantEmailConfirmed) {
        this.email = event.email
    }

    private constructor()
}

data class RegisterParticipant(
    val requestedEmail: String,
)

data class ChangeParticipantEmail(
    @TargetAggregateIdentifier
    val id: String,
    val requestedEmail: String,
)

data class ConfirmParticipantEmail(
    @TargetAggregateIdentifier
    val id: String,
    val email: String,
)

data class DenyParticipantEmail(
    @TargetAggregateIdentifier
    val id: String,
    val email: String,
)


data class ParticipantRegisteredAwaitingEmailConfirmation(
    val id: String,
)

data class ParticipantEmailRequested(
    val id: String,
    val email: String,
)

data class ParticipantEmailChangeRequested(
    val id: String,
    val oldEmail: String,
    val email: String,
)

data class ParticipantEmailConfirmed(
    val id: String,
    val email: String,
)

data class ParticipantEmailRequestRejected(
    val id: String,
    val email: String,
)

data class ParticipantRemoved(
    val id: String,
    val reason: String,
)

data class CreditsAddedToParticipant(
    val id: String,
    val amount: Double,
    val newAmount: Double,
    val reason: String,
)
