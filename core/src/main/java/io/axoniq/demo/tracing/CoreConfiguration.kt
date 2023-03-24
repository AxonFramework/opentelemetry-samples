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

package io.axoniq.demo.tracing

import com.fasterxml.jackson.databind.ObjectMapper
import org.axonframework.common.transaction.TransactionManager
import org.axonframework.config.ConfigurationScopeAwareProvider
import org.axonframework.config.ConfigurerModule
import org.axonframework.deadline.SimpleDeadlineManager
import org.axonframework.eventhandling.PropagatingErrorHandler
import org.axonframework.serialization.Serializer
import org.axonframework.serialization.json.JacksonSerializer
import org.axonframework.tracing.SpanFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableScheduling
@Import(MetricsConfig::class)
class CoreConfiguration {

    @Bean
    fun deadlineManager(configuration: org.axonframework.config.Configuration, spanFactory: SpanFactory) =
        SimpleDeadlineManager
            .builder()
            .scopeAwareProvider(ConfigurationScopeAwareProvider(configuration))
            .transactionManager(configuration.getComponent(TransactionManager::class.java))
            .spanFactory(spanFactory)
            .build()

    @Bean
    @Primary
    fun serializer(): Serializer = JacksonSerializer.builder()
        .lenientDeserialization()
        .objectMapper(ObjectMapper().findAndRegisterModules())
        .build()

    @Bean
    fun configurerModule() = ConfigurerModule {
        it.eventProcessing().registerDefaultListenerInvocationErrorHandler { PropagatingErrorHandler.INSTANCE }
    }
}
