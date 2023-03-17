package io.axoniq.demo.tracing

import org.axonframework.common.IdentifierFactory
import java.util.*

class ShortIdentifierFactory : IdentifierFactory(){
    override fun generateIdentifier(): String {
        val base = UUID.randomUUID().toString()
        return base.split("-")[0]
    }
}
