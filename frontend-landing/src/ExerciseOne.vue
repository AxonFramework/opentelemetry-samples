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


import Hint from "@/components/Hint.vue";
import ChaosButton from "@/components/ChaosButton.vue";
import ExerciseStep from "@/components/ExerciseStep.vue";
import dashboard from "@/assets/base_dashboard.json";
import DownloadJsonButton from "@/components/DownloadJsonButton.vue";
import {delay, errorRate} from "@/utils";
</script>

<template>
    <p>
        The first exercise will focus on creating a Dashboard for Axon Framework and Axon Server. You will use this
        dashboard in later exercises to create alerts and debug the Auction House application!
    </p>

    <p>During most exercises, hints will be available if you are stuck. They look like this:</p>

    <Hint>You can also hide the hint again by clicking on it.</Hint>

    <p>Let's get to it!</p>
    <ExerciseStep title="Creating a Grafana folder">
        <p>
            Grafana dashboards are organized in folders, as are Alerts, which you will use later. We will create all
            dashboards
            in a folder called "Axon Observability". Go to Grafana via the top menu, login in with admin/admin and
            create a
            folder for the dashboard you will create.
        </p>
    </ExerciseStep>

    <ExerciseStep title="Import the base dashboard">
        <p>To get started, we have prepared a base dashboard with the current amount of free heap memory. Import the
            following
            Dashboard JSON into your Grafana folder. You can do this in the left of Grafana (Dashboards > Import).</p>

        <div class="row">
            <div class="col">
                <DownloadJsonButton filename="dashboard_base" :json="dashboard"/>
            </div>
            <div class="col">
                <Hint>Go to <a href="/grafana/dashboard/import">this Grafana page</a></Hint>
            </div>
        </div>

        <p>Let's expand this dashboard! Based on the information of the presentation, we can create an effective
            dashboard.
            This dashboard could look something like
            <a target="_blank" href="https://snapshots.raintank.io/dashboard/snapshot/iP4Mamt07RHbo1yzLcnIXPIIwRT0e1G7">the
                one in this link</a>. So let's create this dashboard, by making a few panels.</p>


    </ExerciseStep>
    <ExerciseStep title="Awareness">
        <p>Without any observability, we never know when our applications are in trouble. Click the try me button and
            see what happens.</p>
        <ChaosButton title="Try me"
                     :auction-query="{query: {handlers: {...errorRate(1)}}, command: {handlers: {...errorRate(1)}}, events: {handlers: {...errorRate(1)}}}"
                     :object-registry="{query: {handlers: {...errorRate(1)}}, command: {handlers: {...errorRate(1)}}, events: {handlers: {...errorRate(1)}}}"
                     :participants="{query: {handlers: {...errorRate(1)}}, command: {handlers: {...errorRate(1)}}, events: {handlers: {...errorRate(1)}}}"
                     :auctions="{query: {handlers: {...errorRate(1)}}, command: {handlers: {...errorRate(1)}}, events: {handlers: {...errorRate(1)}}}"
        ></ChaosButton>
        <p>Can you figure out what is going wrong? Click the hint if you know, or are stuck.</p>
        <Hint>
            This button set the error rate on all event- query- and command handlers to 100%. All 5 applications have
            come to a grinding halt, and the auction house no longer works. <br/>
            You can press the reset button now.
        </Hint>
        <ChaosButton title="Reset"></ChaosButton>

        <p>You see we definitely need awareness of our application! Let's build a dashboard for that. </p>
    </ExerciseStep>


    <ExerciseStep title="Add traffic to the dashboard">
        <p>
            We should measure how much traffic our services get.
        </p>

        <div class="px-3 row">
            <div class="col-6">
                <h4>a) CommandBus message rate</h4>
                <p>Create a panel that shows the CommandBus ingestion rate per minute, summing it for all instances of
                    the
                    application.</p>
                <Hint>
                    <div>sum by(app) (rate(commandBus_ingestedCounter_total[1m]))</div>
                </Hint>
            </div>

            <div class="col-6">
                <h4>b) QueryBus message rate</h4>
                <p>Create a panel that shows the QueryBus ingestion rate per minute, summing it for all instances of the
                    application.</p>
                <Hint>
                    <div>sum by(app) (rate(queryBus_ingestedCounter_total[1m]))</div>
                </Hint>
            </div>
        </div>

        <p>
            Increase the number of participants on the Configuration tab of the demo, and see the numbers go up!
        </p>
    </ExerciseStep>


    <ExerciseStep title="Add errors to the dashboard">
        <p>We should also monitor any errors that are occurring, right? Let's add those. </p>
        <div class="px-3 row">
            <div class="col-6">

                <h4>a) CommandBus error rate</h4>
                <p>Create a panel that shows the CommandBus error rate per minute, summing it per payload type.</p>
                <Hint>
                    <div>sum by(payloadType) (rate(commandBus_failureCounter_total[1m]))</div>
                </Hint>
            </div>

            <div class="col-6">

                <h4>b) QueryBus error rate</h4>
                <p>Create a panel that shows the QueryBus error rate per minute, summing it per payload type.</p>
                <Hint>
                    <div>sum by(payloadType) (rate(queryBus_failureCounter_total[1m]))</div>
                </Hint>
            </div>
        </div>
        <p>
            Use the following chaos buttons to see if your panels work!
        </p>
        <ChaosButton title="Errors in commands"
                     :auctions="{command: {handlers: {...errorRate(.25)}}}"></ChaosButton>
        <ChaosButton title="Errors in queries"
                     :auction-query="{query: {handlers: {...errorRate(.25)}}}"></ChaosButton>
        <ChaosButton title="Reset"></ChaosButton>
    </ExerciseStep>

    <ExerciseStep title="Add capacity">
        <p>It's important whether we need to scale, or have capacity problems. Besides the memory usage that's already
            there, add the following panels to the dashboard</p>
        <div class="px-3 row">

            <div class="col-6">
                <h4>a) Event processor</h4>
                <p>Create a panel that shows the maximum latency per processor and application</p>
                <Hint>
                    <div>max by(processorName, app) (eventProcessor_latency)</div>
                </Hint>
            </div>

            <div class="col-6">


                <h4>b) Command Bus capacity</h4>
                <p>Create a panel that shows the Command bus capacity, summed per application</p>
                <Hint>
                    <div>sum by(app) (commandBus_capacity)</div>
                </Hint>
            </div>

            <div class="col-6">

                <h4>c) Query Bus capacity</h4>
                <p>Create a panel that shows the Query bus capacity, summed per application</p>
                <Hint>
                    <div>sum by(app) (queryBus_capacity)</div>
                </Hint>
            </div>
        </div>

        <p>
            When the amount of commands increase, the capacity increases as well. Check the graph and play with the
            number of participants. It should reflect how busy the system is.
        </p>
    </ExerciseStep>


    <ExerciseStep title="Create the latency">
        <p>One more left! Latency.</p>

        <div class="px-3 row">

            <div class="col-6">

                <h4>a) Event processor handler duration</h4>
                <p>Create a panel that shows the Event processor handler duration as maximum 0.95 quantile per
                    application
                    and
                    processorName</p>
                <Hint>
                    <div>max by(processorName) (eventProcessor_successTimer_seconds{quantile="0.95"})</div>
                </Hint>
            </div>

            <div class="col-6">


                <h4>b) Command handler duration</h4>
                <p>Create a panel that shows the Command handler duration for the biggest 0.95 quantile per payload type
                    and
                    application</p>
                <Hint>
                    <div>max by(payloadType) (commandBus_allTimer_seconds{quantile="0.95"})</div>
                </Hint>
            </div>

            <div class="col-6">


                <h4>c) Query handler duration</h4>
                <p>Create a panel that shows the Command handler duration for the biggest 0.95 quantile per payload type
                    and
                    application</p>
                <Hint>
                    <div>max by(payloadType) (queryBus_allTimer_seconds{quantile="0.95"})</div>
                </Hint>
            </div>

            <div class="col-6">


                <h4>d) Command Bus latency</h4>
                <p>Create a panel that shows the Command bus latency from the Axon Server perspective, by
                    request/payloadType</p>
                <Hint>
                    <div>sum by(request) (axon_commands_seconds_sum) / sum by(request) (axon_commands_seconds_count)
                    </div>
                </Hint>
            </div>

            <div class="col-6">

                <h4>e) Query Bus latency</h4>
                <p>Create a panel that shows the Query bus latency, summed per application</p>
                <Hint>
                    <div>sum by(request) (axon_queries_seconds_sum) / sum by(request) (axon_queries_seconds_count)</div>
                </Hint>
            </div>
        </div>
    </ExerciseStep>

    <ExerciseStep title="Play with the board">
        <p>
            Now, we can observe the demo as we introduce chaos! If you have time left, feel free to play with any of
            these
            chaos configurations:
        </p>
        <ChaosButton title="Chaos configuration 1" :auction-query="{events: {handlers: {...delay(2000)}}}"/>
        <ChaosButton title="Chaos configuration 2" :auctions="{command: {handlers: {...errorRate(.25)}}}"/>
        <ChaosButton title="Chaos configuration 3" :participants="{command: {handlers: {...delay(5000)}}}"/>
        <ChaosButton title="Chaos configuration 4" :participants="{command: {handlers: {...errorRate(.25)}}}"/>
        <ChaosButton title="Reset"></ChaosButton>
    </ExerciseStep>

</template>

<style scoped>
</style>
