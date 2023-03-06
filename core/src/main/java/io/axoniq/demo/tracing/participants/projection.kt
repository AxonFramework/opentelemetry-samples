package io.axoniq.demo.tracing.participants

import jakarta.persistence.Entity
import jakarta.persistence.Id
import org.axonframework.config.Configurer
import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.EventHandler
import org.axonframework.queryhandling.QueryHandler
import org.axonframework.queryhandling.QueryUpdateEmitter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

@Configuration
class ParticipantProjectionConfiguration {
    @Autowired
    fun configure(configurer: Configurer) {
        configurer.eventProcessing().usingPooledStreamingEventProcessors { t, u ->
            u.initialToken { it.createHeadToken() }.initialSegmentCount(2).batchSize(5)
        }
    }
}

@Service
@ProcessingGroup("participants")
class ParticipantProjection(
    private val queryUpdateEmitter: QueryUpdateEmitter,
    private val repository: ParticipantInfoRepository
) {
    @EventHandler
    fun on(event: ParticipantEmailConfirmed) {
        val existing = repository.findByIdOrNull(event.id)
        if (existing != null) {
            existing.email = event.email
        } else {
            repository.save(ParticipantInformation(event.id, event.email))
        }
        queryUpdateEmitter.emit(
            GetAllParticipantsForSubscriptionQuery::class.java,
            { true },
            ParticipantDto(event.id, event.email)
        )
    }

    @QueryHandler
    fun directSubQuery(query: GetAllParticipantsForSubscriptionQuery): List<ParticipantDto> {
        return repository.findAll().map { ParticipantDto(it.identifier, it.email) }
    }

    @QueryHandler
    fun directQuery(query: GetAllParticipants): List<ParticipantDto> {
        return repository.findAll().map { ParticipantDto(it.identifier, it.email) }
    }

    @QueryHandler
    fun directQuery(query: GetAllParticipantsForScatterGather): List<ParticipantDto> {
        return repository.findAll().map { ParticipantDto(it.identifier, it.email) }
    }

    @QueryHandler
    fun streamingQuery(query: GetAllParticipantsForStreamingQuery): Flux<ParticipantDto> {
        return Flux.fromIterable(repository.findAll().map { ParticipantDto(it.identifier, it.email) })
    }
}


@Service
@ProcessingGroup("participants")
class ParticipantProjectionAdditionalForScatterGather(
    private val repository: ParticipantInfoRepository
) {
    @QueryHandler
    fun directQuery(query: GetAllParticipantsForScatterGather): List<ParticipantDto> {
        return repository.findAll().map { ParticipantDto(it.identifier, it.email) }
    }
}

class GetAllParticipants
class GetAllParticipantsForStreamingQuery
class GetAllParticipantsForSubscriptionQuery
class GetAllParticipantsForScatterGather

data class ParticipantDto(val id: String, val email: String)

@Repository
interface ParticipantInfoRepository : JpaRepository<ParticipantInformation, String> {

}

@Entity
data class ParticipantInformation(
    @Id
    val identifier: String,
    var email: String,
)
