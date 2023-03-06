package io.axoniq.demo.tracing.demoengine

import io.axoniq.demo.tracing.participants.GetAllParticipantsForScatterGather
import io.axoniq.demo.tracing.participants.ParticipantDto
import org.axonframework.messaging.responsetypes.ResponseTypes
import org.axonframework.queryhandling.QueryGateway
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class ScatterGatherQuerySimulator(
    private val queryGateway: QueryGateway
) {
    @Scheduled(fixedRate = 5000, initialDelay = 2000)
    fun sendQuery() {
        queryGateway.scatterGather(
            GetAllParticipantsForScatterGather(),
            ResponseTypes.multipleInstancesOf(ParticipantDto::class.java),
            1,
            TimeUnit.SECONDS
        )
            .toList()
    }
}
