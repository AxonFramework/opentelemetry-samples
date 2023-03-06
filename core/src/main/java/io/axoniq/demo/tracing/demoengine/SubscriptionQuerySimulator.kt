package io.axoniq.demo.tracing.demoengine

import io.axoniq.demo.tracing.participants.GetAllParticipants
import io.axoniq.demo.tracing.participants.GetAllParticipantsForStreamingQuery
import io.axoniq.demo.tracing.participants.GetAllParticipantsForSubscriptionQuery
import io.axoniq.demo.tracing.participants.ParticipantDto
import jakarta.annotation.PostConstruct
import org.axonframework.messaging.responsetypes.ResponseTypes
import org.axonframework.queryhandling.QueryGateway
import org.axonframework.queryhandling.SubscriptionQueryResult
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.security.cert.PKIXParameters

@Component
class SubscriptionQuerySimulator(
    private val queryGateway: QueryGateway
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private var query: SubscriptionQueryResult<List<ParticipantDto>, ParticipantDto>? = null

    @Scheduled(fixedRate = 5000, initialDelay = 2000)
    fun sendQuery() {
        if (query != null) {
            query?.close()
        }
        query = queryGateway.subscriptionQuery(
            GetAllParticipantsForSubscriptionQuery(),
            ResponseTypes.multipleInstancesOf(ParticipantDto::class.java),
            ResponseTypes.instanceOf(ParticipantDto::class.java)
        )
        query!!.initialResult().subscribe {
            it.forEach { item ->
                logger.info("Received {}: {} as initial result", item.id, item.email)
            }
        }
        query!!.updates()
            .subscribe { item ->
                logger.info("Received {}: {} as update result", item.id, item.email)
            }

    }
}
