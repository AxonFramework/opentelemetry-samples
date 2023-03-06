package io.axoniq.demo.tracing.participants

import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.*
import org.axonframework.modelling.command.AggregateLifecycle.apply
import org.axonframework.spring.stereotype.Aggregate

@Aggregate
class EmailUniquenessAggregate private constructor() {
    @AggregateIdentifier
    private lateinit var email: String
    private var claimedAccountId: String? = null

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    fun on(command: ClaimEmail) {
        if (claimedAccountId == null) {
            apply(EmailClaimed(command.email, command.accountId))
        } else {
            apply(EmailClaimRejected(command.email, command.accountId))
        }
    }

    @CommandHandler
    fun on(command: UnclaimEmail) {
        if(claimedAccountId != command.accountId) {
            throw IllegalStateException("Only accountId $claimedAccountId can unclaim the email!")
        }
        apply(EmailUnclaimed(email, claimedAccountId!!))
    }

    @EventSourcingHandler
    fun on(event: EmailClaimed) {
        this.email = event.email
        this.claimedAccountId = event.accountId
    }

    @EventSourcingHandler
    fun on(event: EmailUnclaimed) {
        claimedAccountId = null
    }

}


data class ClaimEmail(
    @TargetAggregateIdentifier
    val email: String,
    val accountId: String
)

data class UnclaimEmail(
    @TargetAggregateIdentifier
    val email: String,
    val accountId: String,
)

data class EmailClaimed(
    val email: String,
    val accountId: String
)

data class EmailUnclaimed(
    val email: String,
    val accountId: String
)

data class EmailClaimRejected(
    val email: String,
    val accountId: String
)
