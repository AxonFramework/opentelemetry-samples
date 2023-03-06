package io.axoniq.demo.tracing.demoengine

import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


private val scheduler = Executors.newScheduledThreadPool(5)
private val logger = LoggerFactory.getLogger("DemoTaskScheduler")

fun runTask(delayInMs: Long, block: () -> Unit) {
    scheduler.schedule({
        try {
            block.invoke()
        } catch (e: Exception) {
            logger.error("Error", e)
        }
    }, delayInMs, TimeUnit.MILLISECONDS)
}
