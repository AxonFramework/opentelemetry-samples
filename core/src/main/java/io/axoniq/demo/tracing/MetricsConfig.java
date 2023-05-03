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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.axonframework.commandhandling.CommandBus;
import org.axonframework.config.Configurer;
import org.axonframework.config.ConfigurerModule;
import org.axonframework.config.MessageMonitorFactory;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.StreamingEventProcessor;
import org.axonframework.eventhandling.TrackingEventProcessor;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.messaging.Message;
import org.axonframework.micrometer.CapacityMonitor;
import org.axonframework.micrometer.EventProcessorLatencyMonitor;
import org.axonframework.micrometer.MessageCountingMonitor;
import org.axonframework.micrometer.MessageTimerMonitor;
import org.axonframework.micrometer.TagsUtil;
import org.axonframework.monitoring.MessageMonitor;
import org.axonframework.monitoring.MultiMessageMonitor;
import org.axonframework.queryhandling.QueryBus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
public class MetricsConfig {

    @Bean
    public ConfigurerModule metricConfigurer(MeterRegistry meterRegistry) {
        return configurer -> {
            instrumentEventStore(meterRegistry, configurer);
            instrumentEventProcessors(meterRegistry, configurer);
            instrumentCommandBus(meterRegistry, configurer);
            instrumentQueryBus(meterRegistry, configurer);
        };
    }

    @Bean
    public CapacityMetricProvider capacityMetricProvider(MeterRegistry meterRegistry,
                                                         @Value("${axon.axonserver.query-threads:10}") Long queryThreads,
                                                         @Value("${axon.axonserver.command-threads:10}") Long commandThreads) {
        return new CapacityMetricProvider(meterRegistry, queryThreads, commandThreads);
    }

    private void instrumentEventStore(MeterRegistry meterRegistry, Configurer configurer) {
        MessageMonitorFactory messageMonitorFactory = (configuration, componentType, componentName) -> {
            return MessageCountingMonitor.buildMonitor(
                    componentName, meterRegistry,
                    message -> Tags.of(TagsUtil.PAYLOAD_TYPE_TAG, message.getPayloadType().getSimpleName())
            );
        };
        configurer.configureMessageMonitor(EventStore.class, messageMonitorFactory);
    }

    private void instrumentEventProcessors(MeterRegistry meterRegistry, Configurer configurer) {
        MessageMonitorFactory messageMonitorFactory = (configuration, componentType, componentName) -> {
            List<MessageMonitor<? super EventMessage<?>>> monitors = new ArrayList();
            monitors.add(MessageCountingMonitor.buildMonitor(
                    "eventProcessor", meterRegistry,
                    message -> Tags.of(
                            TagsUtil.PAYLOAD_TYPE_TAG, message.getPayloadType().getSimpleName(),
                            TagsUtil.PROCESSOR_NAME_TAG, componentName
                    )
            ));
            monitors.add(MessageTimerMonitor
                                 .builder()
                                 .timerCustomization(timer -> timer.distributionStatisticExpiry(Duration.ofMinutes(1)))
                                 .meterRegistry(meterRegistry)
                                 .meterNamePrefix("eventProcessor")
                                 .tagsBuilder(message -> Tags.of(
                                         TagsUtil.PAYLOAD_TYPE_TAG, message.getPayloadType().getSimpleName(),
                                         TagsUtil.PROCESSOR_NAME_TAG, componentName
                                 ))
                                 .build());

            monitors.add(CapacityMonitor.buildMonitor(
                    "eventProcessor", meterRegistry,
                    1, TimeUnit.MINUTES,
                    message -> Tags.of(
                            TagsUtil.PROCESSOR_NAME_TAG, componentName
                    )
            ));


            monitors.add(EventProcessorLatencyMonitor
                                 .builder()
                                 .meterRegistry(meterRegistry)
                                 .meterNamePrefix("eventProcessor")
                                 .tagsBuilder(
                                         message -> Tags.of(
                                                 TagsUtil.PROCESSOR_NAME_TAG, componentName
                                         ))
                                 .build());

            return new MultiMessageMonitor(monitors);
        };

        configurer.configureMessageMonitor(StreamingEventProcessor.class, messageMonitorFactory);
    }

    private void instrumentCommandBus(MeterRegistry meterRegistry, Configurer configurer) {
        MessageMonitorFactory messageMonitorFactory = (configuration, componentType, componentName) -> {
            MessageCountingMonitor messageCounter = MessageCountingMonitor.buildMonitor(
                    componentName, meterRegistry,
                    message -> Tags.of(TagsUtil.PAYLOAD_TYPE_TAG, message.getPayloadType().getSimpleName())
            );
            MessageTimerMonitor messageTimer = MessageTimerMonitor
                    .builder()
                    .timerCustomization(timer -> timer.distributionStatisticExpiry(
                            Duration.ofMinutes(1)))
                    .meterRegistry(meterRegistry)
                    .meterNamePrefix(componentName)
                    .tagsBuilder(message -> Tags.of(TagsUtil.PAYLOAD_TYPE_TAG,
                                                    message.getPayloadType()
                                                           .getSimpleName()))
                    .build();

            CapacityMonitor capacityMonitor1Minute = CapacityMonitor.buildMonitor(
                    componentName, meterRegistry,
                    1, TimeUnit.MINUTES,
                    message -> Tags.of(TagsUtil.PAYLOAD_TYPE_TAG, message.getPayloadType().getSimpleName())
            );

            return new MultiMessageMonitor<>(messageCounter, messageTimer, capacityMonitor1Minute);
        };
        configurer.configureMessageMonitor(CommandBus.class, messageMonitorFactory);
    }

    private void instrumentQueryBus(MeterRegistry meterRegistry, Configurer configurer) {
        MessageMonitorFactory messageMonitorFactory = (configuration, componentType, componentName) -> {
            MessageCountingMonitor messageCounter = MessageCountingMonitor.buildMonitor(
                    componentName, meterRegistry,
                    message -> Tags.of(TagsUtil.PAYLOAD_TYPE_TAG, message.getPayloadType().getSimpleName())
            );

            MessageTimerMonitor messageTimer = MessageTimerMonitor
                    .builder()
                    .timerCustomization(timer -> timer.distributionStatisticExpiry(Duration.ofMinutes(1)))
                    .meterRegistry(meterRegistry)
                    .meterNamePrefix(componentName)
                    .tagsBuilder(message -> Tags.of(TagsUtil.PAYLOAD_TYPE_TAG,
                                                    message.getPayloadType().getSimpleName()))
                    .build();
            CapacityMonitor capacityMonitor1Minute = CapacityMonitor.buildMonitor(
                    componentName, meterRegistry,
                    message -> Tags.of(TagsUtil.PAYLOAD_TYPE_TAG, message.getPayloadType().getSimpleName())
            );

            return new MultiMessageMonitor<>(messageCounter, messageTimer, capacityMonitor1Minute);
        };
        configurer.configureMessageMonitor(QueryBus.class, messageMonitorFactory);
    }
}
