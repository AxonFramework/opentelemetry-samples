<!--
  - Copyright (c) 2023-2023. AxonIQ
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -    http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -->

<script setup lang="ts">
import dashboard from '@/assets/dashboard_exercise_1.json'
import DownloadJsonButton from "@/components/DownloadJsonButton.vue";
import ExerciseStep from "@/components/ExerciseStep.vue";
import Hint from "@/components/Hint.vue";
import ChaosButton from "@/components/ChaosButton.vue";
import {delay, errorRate} from "@/utils"

</script>

<template>
    <p>
        This second exercise will focus on setting up alerts around some of the situations that can occur, and then
        triggering that situation!
    </p>
    <p>If you didn't finish exercise one in time, or want a fresh start, you can use this dashboard as a base:</p>
    <DownloadJsonButton filename="dashboard_exercise_one" :json="dashboard"/>

    <ExerciseStep title="Slow event processing">
        <p>
            As we talked about during the session, it can take a long time for events to be processed once they are
            published. This can impact the user experience and indicates a problem.
        </p>
        <p>
            Please create an alert that triggers when event processing, for any processor, becomes higher than 2000ms.
            You can do this straight from the panel. Evaluate it every '20s' for '1m' as Evaluation behavior. This will
            speed up the exercise.
        </p>
        <Hint>Select a classic condition, when max() of A is above 2000 in the last minute.</Hint>


        <div class="px-3 row">
            <div class="col-6">
                <h4>a) Tracing</h4>
                <p class="mt-3">
                    Event processors emit traces for each event they handle. They can be used to debug your system.
                    Before we start introducing chaos, let us check out a normal trace.
                </p>
                <p>
                    Find a trace of the "AuctionQueries" service and the "AuctionProjection.on(AuctionCreated)"
                    operation in Jaeger. The operation here is actually the span name, and Axon Framework exposes the
                    method's name. You will see that the handler was quite fast.
                </p>
            </div>
            <div class="col-6">
                <h4>b) Errors</h4>
                <p class="mt-3">Let's take a look at one of the situations at which the latency of a processor can rise:
                    when an event processor is in error. </p>
                <ChaosButton title="Event processor errors"
                             :auction-query="{events: {handlers: {...errorRate(1)}}}"></ChaosButton>
            </div>
            <div class="col-6">
                <h4>c) Observe alert</h4>
                <p class="mt-3">Do you see the alert triggering in the Dashboard? Good! We proved it works. Let's fix
                    the issue with this button:</p>
                <ChaosButton title="Reset" :auction-query="{}"></ChaosButton>
            </div>
            <div class="col-6">
                <h4>d) Slow, so slow</h4>
                <p class="mt-3">Now we need to wait for a minute for the alert to disappear. When the alert is resolved,
                    we can take a look at the second situation: a slow event processor. </p>
                <ChaosButton title="Event processor delay"
                             :auction-query="{events: {handlers: {...delay(2000)}}}"></ChaosButton>
            </div>
            <div class="col-6">
                <h4>e) Get alerted again</h4>
                <p class="mt-3">Do you see the alert triggering in the Dashboard again? Good! Let's revert it to
                    normal.</p>
                <ChaosButton title="Reset"></ChaosButton>
            </div>
            <div class="col-6">
                <h4>f) Recheck traces</h4>
                <p class="mt-3">
                    Re-search for the traces we did in part A. Can you see what took so long?
                </p>
            </div>

            <p>As you can see, we can use metrics to alert ourselves, and tracing to find out what's going on.</p>
        </div>
    </ExerciseStep>
    <ExerciseStep title="Advanced tracing">
        <p>As setting up alerts it quite tedious and easy, we will continue with just tracing for now.
            There are many situations where it can be useful. </p>

        <p>For example, commands might be slow! But why? Tracing can give us the answer. Click the following chaos
            button:</p>
        <ChaosButton title="Set chaos" :auctions="{events: {readAggregateStream: {...delay(100)}}}"></ChaosButton>
        <p>
            Now, look up a trace of the "Auctions" service with operation "Auction.on(PlaceBidOnAuction)". Can you see
            where the 100ms delay is happening? Click the hint once you think you know the answer.
        </p>
        <Hint>
            Fetching the events for the aggregate now takes a 100ms extra, and makes the application a lot slower. You
            can see this because the "EventSourcingRepository.load" trace is much longer than other traces.
        </Hint>
    </ExerciseStep>

    <ExerciseStep title="Wrapping up">
        <p>As you can see, observability is very powerful. It will take the guessing out of your debugging cycle and
            give
            you proper information.
        </p>

        <p>Feel free to ask questions now, or play around further to learn more. You can introduce chaos yourself by
            using the "Firestarter" dropdown on the top menu. This is what we've been using for the chaos buttons.</p>
    </ExerciseStep>

</template>

<style scoped>
</style>
