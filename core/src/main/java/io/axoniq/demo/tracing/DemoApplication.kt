package io.axoniq.demo.tracing

import org.axonframework.common.transaction.TransactionManager
import org.axonframework.config.Configuration
import org.axonframework.config.ConfigurationScopeAwareProvider
import org.axonframework.deadline.SimpleDeadlineManager
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class DemoApplication {
    @Bean
    fun deadlineManager(configuration: Configuration) = SimpleDeadlineManager
        .builder()
        .scopeAwareProvider(ConfigurationScopeAwareProvider(configuration))
        .transactionManager(configuration.getComponent(TransactionManager::class.java))
        .spanFactory(configuration.spanFactory())
        .build()
}

fun main(args: Array<String>) {
    runApplication<DemoApplication>(*args)
}
