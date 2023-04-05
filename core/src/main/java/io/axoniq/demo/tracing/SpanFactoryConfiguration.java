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

package io.axoniq.demo.tracing;

import org.axonframework.messaging.Message;
import org.axonframework.queryhandling.SubscriptionQueryUpdateMessage;
import org.axonframework.tracing.NoOpSpanFactory;
import org.axonframework.tracing.Span;
import org.axonframework.tracing.SpanAttributesProvider;
import org.axonframework.tracing.SpanFactory;
import org.axonframework.tracing.opentelemetry.OpenTelemetrySpanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
public class SpanFactoryConfiguration {

    final static Pattern sagaNameSelectorPattern = Pattern.compile("SagaManager<(.*?)>.invokeSaga");

    @Bean
    public SpanFactory spanFactory() {
        OpenTelemetrySpanFactory original = OpenTelemetrySpanFactory.builder().build();
        return new SpanFactory() {
            @Override
            public Span createRootTrace(Supplier<String> operationNameSupplier) {
                return original.createRootTrace(operationNameSupplier);
            }

            @Override
            public Span createHandlerSpan(Supplier<String> operationNameSupplier, Message<?> parentMessage,
                                          boolean isChildTrace, Message<?>... linkedParents) {
                if (parentMessage instanceof SubscriptionQueryUpdateMessage<?>) {
                    return NoOpSpanFactory.INSTANCE.createHandlerSpan(operationNameSupplier,
                                                                      parentMessage,
                                                                      isChildTrace,
                                                                      linkedParents);
                }
                return original.createHandlerSpan(operationNameSupplier, parentMessage, isChildTrace, linkedParents);
            }

            @Override
            public Span createDispatchSpan(Supplier<String> operationNameSupplier, Message<?> parentMessage,
                                           Message<?>... linkedSiblings) {
                return original.createDispatchSpan(operationNameSupplier, parentMessage, linkedSiblings);
            }


            @Override
            public Span createInternalSpan(Supplier<String> operationNameSupplier) {
                String name = operationNameSupplier.get();
                if (name.startsWith("EventSourcingRepository.load")) {
                    name = "EventSourcingRepository.load";
                } else if (name.startsWith("AxonFramework-Events.event")) {
                    name = "AxonFramework-Events.event";
                } else {
                    Matcher matcher = sagaNameSelectorPattern.matcher(name);
                    if (matcher.find()) {
                        name = "SagaManager<" + matcher.group(1) + ">.invokeSaga";
                    }
                }
                String finalName = name;
                return original.createInternalSpan(() -> finalName);
            }

            @Override
            public Span createInternalSpan(Supplier<String> operationNameSupplier, Message<?> message) {
                return original.createInternalSpan(operationNameSupplier, message);
            }

            @Override
            public void registerSpanAttributeProvider(SpanAttributesProvider provider) {
                original.registerSpanAttributeProvider(provider);
            }

            @Override
            public <M extends Message<?>> M propagateContext(M message) {
                return original.propagateContext(message);
            }
        };
    }
}
