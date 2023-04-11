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

import com.fasterxml.jackson.annotation.JsonAutoDetect
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.*
import org.axonframework.modelling.command.AggregateLifecycle.apply
import org.axonframework.spring.stereotype.Aggregate

@Aggregate(snapshotTriggerDefinition = "snapshotTriggerDefinition")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class EmailUniquenessAggregate private constructor() {
    @AggregateIdentifier
    private lateinit var email: String
    private var claimedAccountId: String? = null

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    fun on(command: ClaimEmail) {
        if (claimedAccountId == null) {
            apply(EmailClaimed(command.email, command.accountId))
        } else {
            apply(EmailClaimRejected(command.email, command.accountId))
        }
    }

    @CommandHandler
    fun on(command: UnclaimEmail) {
        if(claimedAccountId != command.accountId) {
            throw IllegalStateException("Only accountId $claimedAccountId can unclaim the email!")
        }
        apply(EmailUnclaimed(email, claimedAccountId!!))
    }

    @EventSourcingHandler
    fun on(event: EmailClaimed) {
        this.email = event.email
        this.claimedAccountId = event.accountId
    }

    @EventSourcingHandler
    fun on(event: EmailUnclaimed) {
        claimedAccountId = null
    }

}


data class ClaimEmail(
    @TargetAggregateIdentifier
    val email: String,
    val accountId: String
)

data class UnclaimEmail(
    @TargetAggregateIdentifier
    val email: String,
    val accountId: String,
)

data class EmailClaimed(
    val email: String,
    val accountId: String
)

data class EmailUnclaimed(
    val email: String,
    val accountId: String
)

data class EmailClaimRejected(
    val email: String,
    val accountId: String
)
