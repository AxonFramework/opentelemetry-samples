package io.axoniq.demo.tracing

import com.google.api.gax.core.CredentialsProvider
import com.google.cloud.opentelemetry.trace.TraceConfiguration
import com.google.cloud.opentelemetry.trace.TraceExporter
import com.google.cloud.spring.core.CredentialsSupplier
import com.google.cloud.spring.core.DefaultCredentialsProvider
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.SpanProcessor
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import io.opentelemetry.sdk.trace.export.SpanExporter
import io.opentelemetry.sdk.trace.samplers.Sampler
import org.axonframework.common.transaction.TransactionManager
import org.axonframework.config.Configuration
import org.axonframework.config.ConfigurationScopeAwareProvider
import org.axonframework.deadline.SimpleDeadlineManager
import org.axonframework.tracing.LoggingSpanFactory
import org.axonframework.tracing.MultiSpanFactory
import org.axonframework.tracing.NestingSpanFactory
import org.axonframework.tracing.SpanFactory
import org.axonframework.tracing.attributes.MetadataSpanAttributesProvider
import org.axonframework.tracing.opentelemetry.OpenTelemetrySpanFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.EnableScheduling
import java.time.Duration

@SpringBootApplication
@EnableScheduling
class GoogleTraceDemoApplication {
    @Bean
    fun gcpSpanExporter(credentialsProvider: CredentialsProvider): SpanExporter {
        return TraceExporter
            .createWithConfiguration(
                TraceConfiguration
                    .builder()
                    .setCredentials(credentialsProvider.credentials)
                    .build()
            )
    }

    @Bean
    fun spanProcessor(spanExporter: SpanExporter): SpanProcessor? {
        return SpanProcessor.composite(BatchSpanProcessor.builder(spanExporter).build())
    }

    @Bean
    fun sdkTracerProvider(spanProcessor: SpanProcessor): SdkTracerProvider {
        return SdkTracerProvider.builder()
            .addSpanProcessor(spanProcessor)
            .setSampler(Sampler.alwaysOn())
            .build()
    }

    @Bean
    fun propagators(): ContextPropagators = ContextPropagators { W3CTraceContextPropagator.getInstance() }

    @Bean
    fun openTelemetry(sdkTracerProvider: SdkTracerProvider, propagators: ContextPropagators): OpenTelemetry {
        return OpenTelemetrySdk.builder()
            .setTracerProvider(sdkTracerProvider)
            .setPropagators(propagators)
            .buildAndRegisterGlobal()
    }

    @Bean
    @Profile("!nested")
    fun spanFactory(openTelemetry: OpenTelemetry): SpanFactory {
        val factory = MultiSpanFactory(
            listOf(
                OpenTelemetrySpanFactory
                    .builder()
                    .tracer(openTelemetry.getTracer("AxonFramework"))
                    .build(),
                LoggingSpanFactory.INSTANCE
            )
        )
        factory.registerSpanAttributeProvider(MetadataSpanAttributesProvider())
        return factory;
    }

    @Bean
    @Profile("nested")
    fun spanFactoryNested(openTelemetry: OpenTelemetry): SpanFactory {
        val factory = MultiSpanFactory(
            listOf(
                OpenTelemetrySpanFactory
                    .builder()
                    .tracer(openTelemetry.getTracer("AxonFramework"))
                    .build(),
                LoggingSpanFactory.INSTANCE
            )
        )
        factory.registerSpanAttributeProvider(MetadataSpanAttributesProvider())
        return NestingSpanFactory
            .builder()
            .delegate(factory)
            .timeLimit(Duration.ofSeconds(60))
            .build()
    }
}

fun main(args: Array<String>) {
    runApplication<GoogleTraceDemoApplication>(*args)
}
