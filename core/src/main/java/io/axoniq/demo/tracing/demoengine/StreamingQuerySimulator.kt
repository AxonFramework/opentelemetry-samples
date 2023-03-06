package io.axoniq.demo.tracing.demoengine

import io.axoniq.demo.tracing.participants.GetAllParticipants
import io.axoniq.demo.tracing.participants.GetAllParticipantsForStreamingQuery
import io.axoniq.demo.tracing.participants.ParticipantDto
import org.axonframework.messaging.responsetypes.ResponseTypes
import org.axonframework.queryhandling.QueryGateway
import org.reactivestreams.Subscriber
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux

@Component
class StreamingQuerySimulator(
    private val queryGateway: QueryGateway
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Scheduled(fixedRate = 5000, initialDelay = 2000)
    fun sendQuery() {
        var count = 0;
        Flux.from(queryGateway.streamingQuery(GetAllParticipantsForStreamingQuery(), ParticipantDto::class.java))
            .doOnComplete {
                logger.info("Received {} items as streaming result", count)
                count = 0
            }
            .subscribe { _ ->
                count ++
            }

    }
}
