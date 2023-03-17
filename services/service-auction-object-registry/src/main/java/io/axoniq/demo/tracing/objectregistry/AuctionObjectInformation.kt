package io.axoniq.demo.tracing.objectregistry

import jakarta.persistence.Entity
import jakarta.persistence.Id


@Entity
data class AuctionObjectInformation(
    @Id
    val identifier: String,
    var name: String,
    var owner: String,
)
