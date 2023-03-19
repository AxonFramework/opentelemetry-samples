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

import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.EventHandler
import org.axonframework.queryhandling.QueryHandler
import org.axonframework.queryhandling.QueryUpdateEmitter
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

@Service
@ProcessingGroup("participants")
class ParticipantProjection(
    private val queryUpdateEmitter: QueryUpdateEmitter,
    private val repository: ParticipantInfoRepository
) {
    @EventHandler
    fun on(event: ParticipantEmailConfirmed) {
        var existing = repository.findByIdOrNull(event.id)
        if (existing != null) {
            existing.email = event.email
        } else {
            existing = repository.save(ParticipantInformation(event.id, event.email, 0))
        }
        queryUpdateEmitter.emit(
            GetAllParticipantsForSubscriptionQuery::class.java,
            { true },
            participantDto(existing)
        )
    }

    @EventHandler
    fun on(event: BalanceAddedToParticipant) {
        val existing = repository.findByIdOrNull(event.id) ?: return
        existing.balance = event.newBalance

        queryUpdateEmitter.emit(GetBalanceForParticipant::class.java, { it.email == existing.email }, event.newBalance)
        queryUpdateEmitter.emit(GetAllParticipantsForSubscriptionQuery::class.java, { true }, participantDto(existing))
    }

    @EventHandler
    fun on(event: BalanceDeducted) {
        val existing = repository.findByIdOrNull(event.id) ?: return
        existing.balance = event.newBalance

        queryUpdateEmitter.emit(GetBalanceForParticipant::class.java, { it.email == existing.email }, event.newBalance)
        queryUpdateEmitter.emit(GetAllParticipantsForSubscriptionQuery::class.java, { true }, participantDto(existing))
    }


    @QueryHandler
    fun directSubQuery(query: GetAllParticipantsForSubscriptionQuery): List<ParticipantDto> {
        return repository.findAll().map { participantDto(it) }
    }

    @QueryHandler
    fun directQuery(query: GetAllParticipants): List<ParticipantDto> {
        return repository.findAll().map { participantDto(it) }
    }

    @QueryHandler
    fun directQuery(query: GetAllParticipantsForScatterGather): List<ParticipantDto> {
        return repository.findAll().map { participantDto(it) }
    }

    private fun participantDto(it: ParticipantInformation) = ParticipantDto(it.identifier, it.email, it.balance)

    @QueryHandler
    fun streamingQuery(query: GetAllParticipantsForStreamingQuery): Flux<ParticipantDto> {
        return Flux.fromIterable(repository.findAll().map { participantDto(it) })
    }

    @QueryHandler
    fun on(query: GetBalanceForParticipant): Long {
        return repository.findByEmail(query.email).map { it.balance }.orElse(0)
    }
}
