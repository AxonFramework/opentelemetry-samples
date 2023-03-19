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

package io.axoniq.demo.tracing.interfaces.simulation

import java.time.Instant

data class SimulatedParticipantDto(
    val id: String,
    val terminated: Boolean,
    val email: String,
    val balance: Long,
    val activeBids: List<ActiveBidDto>,
    val items: List<ParticipantItem>,
)

data class ActiveBidDto(
    val auctionId: String,
    val objectId: String,
    val objectName: String,
    val bid: Long,
    val interest: Double,
    val receivedWinUpdate: Boolean,
    val receivedBalanceUpdate: Boolean,
    val receivedItemUpdate: Boolean,
)

data class ParticipantItem(
    val id: String,
    val name: String,
    val boughtFor: Long,
    val auctioning: Boolean,
    val auctionStarted: Instant?,
    val receivedBalanceUpdate: Boolean?,
    val receivedItemUpdate: Boolean?,
)

data class SimulatedParticipantDtoResponse(
    val participants: List<SimulatedParticipantDto>
)
