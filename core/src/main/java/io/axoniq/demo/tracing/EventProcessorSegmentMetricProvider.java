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
import jakarta.annotation.PostConstruct;
import org.axonframework.common.AxonConfigurationException;
import org.axonframework.config.EventProcessingConfiguration;
import org.axonframework.eventhandling.StreamingEventProcessor;
import org.springframework.stereotype.Service;

@Service
public class EventProcessorSegmentMetricProvider {

    private final EventProcessingConfiguration configuration;
    private final MeterRegistry meterRegistry;

    public EventProcessorSegmentMetricProvider(EventProcessingConfiguration configuration,
                                               MeterRegistry meterRegistry) {
        this.configuration = configuration;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void setup() {
        try {
            configuration.eventProcessors().forEach((s, eventProcessor) -> {
                if (eventProcessor instanceof StreamingEventProcessor streamingEventProcessor) {
                    meterRegistry.gauge("eventProcessor_segments_claimed",
                                        Tags.of("eventProcessor", s),
                                        streamingEventProcessor,
                                        value -> value.processingStatus().values().stream()
                                                      .filter(segment -> !segment.isErrorState())
                                                      .mapToDouble(segment -> (double) 1 / (segment.getSegment().getMask() + 1))
                                                      .sum());
                }
            });
        } catch (AxonConfigurationException e) {
            // Ignore, means there are no processors
        }
    }
}
