package io.axoniq.demo.tracing.participants

import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.deadline.DeadlineManager
import org.axonframework.deadline.annotation.DeadlineHandler
import org.axonframework.modelling.saga.EndSaga
import org.axonframework.modelling.saga.SagaEventHandler
import org.axonframework.modelling.saga.SagaLifecycle
import org.axonframework.modelling.saga.StartSaga
import org.axonframework.spring.stereotype.Saga
import java.time.Instant

@Saga
class ParticipantEmailUniquenessProcessManager {
    private var deadlineId: String? = null
    private var oldEmail: String? = null
    private lateinit var accountId: String
    private lateinit var email: String


    @SagaEventHandler(associationProperty = "id", keyName = "accountId")
    @StartSaga
    fun on(event: ParticipantEmailRequested, commandGateway: CommandGateway, deadlineManager: DeadlineManager) {
        email = event.email
        accountId = event.id
        commandGateway.send<Void>(ClaimEmail(event.email, event.id))
        deadlineId = deadlineManager.schedule(Instant.now().plusSeconds(30), "expireClaim")
    }

    @SagaEventHandler(associationProperty = "id", keyName = "accountId")
    @StartSaga
    fun on(event: ParticipantEmailChangeRequested, commandGateway: CommandGateway, deadlineManager: DeadlineManager) {
        email = event.email
        accountId = event.id
        oldEmail = event.oldEmail
        commandGateway.send<Void>(ClaimEmail(event.email, event.id))
        deadlineId = deadlineManager.schedule(Instant.now().plusSeconds(30), "expireClaim")
    }

    @SagaEventHandler(associationProperty = "accountId", keyName = "accountId")
    @EndSaga
    fun on(event: EmailClaimed, commandGateway: CommandGateway) {
        commandGateway.send<Void>(ConfirmParticipantEmail(accountId, email))
        if (oldEmail == null) {
            SagaLifecycle.end()
            return
        }
        // We have the new email, unclaim the old one
        commandGateway.send<Void>(UnclaimEmail(oldEmail!!, accountId))
    }

    @SagaEventHandler(associationProperty = "accountId", keyName = "accountId")
    @EndSaga
    fun on(event: EmailClaimRejected, commandGateway: CommandGateway) {
        commandGateway.send<Void>(DenyParticipantEmail(accountId, email))
    }

    @DeadlineHandler(deadlineName = "expireClaim")
    @EndSaga
    fun expire(commandGateway: CommandGateway) {
        // Request took too long. Cancel email claim request
        commandGateway.send<Void>(DenyParticipantEmail(accountId, email)).get()
    }
}
