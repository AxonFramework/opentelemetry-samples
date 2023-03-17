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
import org.axonframework.queryhandling.QueryHandler
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("participant", "all")
@ProcessingGroup("participants")
class ParticipantProjectionAdditionalForScatterGather(
    private val repository: ParticipantInfoRepository
) {
    @QueryHandler
    fun directQuery(query: GetAllParticipantsForScatterGather): List<ParticipantDto> {
        return repository.findAll().map { ParticipantDto(it.identifier, it.email, it.balance) }
    }
}
