package io.axoniq.demo.tracing.demoengine

import io.axoniq.demo.tracing.participants.GetAllParticipants
import io.axoniq.demo.tracing.participants.ParticipantDto
import org.axonframework.messaging.responsetypes.ResponseTypes
import org.axonframework.queryhandling.QueryGateway
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class DirectQuerySimulator(
    private val queryGateway: QueryGateway
) {
    @Scheduled(fixedRate = 5000, initialDelay = 2000)
    fun sendQuery() {
        queryGateway.query(GetAllParticipants(), ResponseTypes.multipleInstancesOf(ParticipantDto::class.java))
    }
}
