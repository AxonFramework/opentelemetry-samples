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

package io.axoniq.demo.tracing.objectregistry

import io.axoniq.demo.tracing.objectsregistry.*
import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.EventHandler
import org.axonframework.queryhandling.QueryHandler
import org.axonframework.queryhandling.QueryUpdateEmitter
import org.springframework.stereotype.Service

@Service
@ProcessingGroup("auction-objects")
class AuctionObjectProjection(
    private val queryUpdateEmitter: QueryUpdateEmitter,
    private val repository: AuctionObjectInfoRepository
) {
    @EventHandler
    fun on(event: AuctionObjectSubmitted) {
        val obj = repository.save(
            AuctionObjectInformation(event.id, event.name, event.owner, event.auctionHouseId)
        )
        queryUpdateEmitter.emit(GetAuctionObjects::class.java, { true }, obj.toDto())
    }

    @EventHandler
    fun on(event: AuctionObjectOwnerTransferred) {
        val obj = repository.findById(event.id).orElseThrow()
        obj.owner = event.newOwner
        queryUpdateEmitter.emit(GetAuctionObjects::class.java, { it.auctionHouseId == obj.auctionHouseId }, obj.toDto())
    }

    @QueryHandler
    fun on(query: GetAuctionObjects): AuctionOwnershipResponse {
        return AuctionOwnershipResponse(repository.findAllByAuctionHouseId(query.auctionHouseId).map { it.toDto() })
    }

    fun AuctionObjectInformation.toDto() = AuctionOwnershipInfoItem(identifier, name, owner)
}
