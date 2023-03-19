package io.axoniq.demo.tracing.objectsregistry


data class AuctionOwnershipInfoItem(
    val identifier: String,
    val name: String,
    val owner: String,
)

data class AuctionOwnershipResponse(
    val items: List<AuctionOwnershipInfoItem>,
)
