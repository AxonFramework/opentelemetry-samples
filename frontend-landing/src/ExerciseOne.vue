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
import DownloadBaseDashboard from "@/components/DownloadBaseDashboard.vue";
import ChaosButton from "@/components/ChaosButton.vue";
</script>

<template>
  <p>
    The first exercise will focus on creating a Dashboard for Axon Framework and Axon Server. You will use this
    dashboard in later exercises to create alerts and debug the Auction House application!
  </p>

  <p>During most exercises, hints will be available if you are stuck. They look like this:</p>

  <Hint>You can also hide the hint again by clicking on it.</Hint>

  <p>Let's get to it!</p>

  <h3 class="mt-4">Step 1: Creating a Grafana folder</h3>
  <p>
    Grafana dashboards are organized in folders, as are Alerts, which you will use later. We will create all dashboards
    in a folder called "Axon Observability". Go to Grafana via the top menu, login in with admin/admin and create a
    folder for the dashboard you will create.
  </p>

  <h3 class="mt-4">Step 2: Import the base dashboard</h3>
  <p>To get started, we have prepared a base dashboard with the current amount of free heap memory. Import the following
    Dashboard JSON into your Grafana folder. You can do this in the left of Grafana (Dashboards > Import).</p>
  <DownloadBaseDashboard/>
  <Hint>Go to <a href="/grafana/dashboard/import">this Grafana page</a></Hint>

  <h3 class="mt-4">Step 3: Create the golden signals</h3>
  <p>Let's expand this dashboard! Based on the information of the presentation, we can create an effective dashboard.
    This dashboard could look something like
    <a target="_blank" href="https://snapshots.raintank.io/dashboard/snapshot/iP4Mamt07RHbo1yzLcnIXPIIwRT0e1G7">the
      one in this link</a>. So let's create this dashboard, by making a few panels.</p>

  <h4>CommandBus message rate</h4>
  <p>Create a panel that shows the CommandBus ingestion rate per minute, summing it for all instances of the
    application.</p>
  <Hint>
    <div>sum by(app) (rate(commandBus_ingestedCounter_total[1m]))</div>
  </Hint>

  <h4>QueryBus message rate</h4>
  <p>Create a panel that shows the QueryBus ingestion rate per minute, summing it for all instances of the
    application.</p>
  <Hint>
    <div>sum by(app) (rate(queryBus_ingestedCounter_total[1m]))</div>
  </Hint>

  <h4>CommandBus error rate</h4>
  <p>Create a panel that shows the CommandBus error rate per minute, summing it per payload type.</p>
  <Hint>
    <div>sum by(payloadType) (rate(commandBus_failureCounter_total[1m]))</div>
  </Hint>

  <h4>QueryBus error rate</h4>
  <p>Create a panel that shows the QueryBus error rate per minute, summing it per payload type.</p>
  <Hint>
    <div>sum by(payloadType) (rate(queryBus_failureCounter_total[1m]))</div>
  </Hint>

  <h4>Event processor handler duration</h4>
  <p>Create a panel that shows the Event processor handler duration as maximum 0.95 quantile per application and processorName</p>
  <Hint>
    <div>max by(processorName) (eventProcessor_successTimer_seconds{quantile="0.95"})</div>
  </Hint>


  <h4>Command handler duration</h4>
  <p>Create a panel that shows the Command handler duration for the biggest 0.95 quantile per payload type and
    application</p>
  <Hint>
    <div>max by(payloadType) (commandBus_allTimer_seconds{quantile="0.95"})</div>
  </Hint>


  <h4>Query handler duration</h4>
  <p>Create a panel that shows the Command handler duration for the biggest 0.95 quantile per payload type and
    application</p>
  <Hint>
    <div>max by(payloadType) (queryBus_allTimer_seconds{quantile="0.95"})</div>
  </Hint>


  <h4>Event processor</h4>
  <p>Create a panel that shows the maximum latency per processor and application</p>
  <Hint>
    <div>max by(processorName, app) (eventProcessor_latency)</div>
  </Hint>


  <h4>Command Bus capacity</h4>
  <p>Create a panel that shows the Command bus capacity, summed per application</p>
  <Hint>
    <div>sum by(app) (commandBus_capacity)</div>
  </Hint>

  <h4>Query Bus capacity</h4>
  <p>Create a panel that shows the Query bus capacity, summed per application</p>
  <Hint>
    <div>sum by(app) (queryBus_capacity)</div>
  </Hint>


  <h4>Command Bus latency</h4>
  <p>Create a panel that shows the Command bus latency from the Axon Server perspective, by request/payloadType</p>
  <Hint>
    <div>sum by(request) (axon_commands_seconds_sum) / sum by(request) (axon_commands_seconds_count)</div>
  </Hint>

  <h4>Query Bus latency</h4>
  <p>Create a panel that shows the Query bus latency, summed per application</p>
  <Hint>
    <div>sum by(request) (axon_queries_seconds_sum) / sum by(request) (axon_queries_seconds_count)</div>
  </Hint>


  <h4>Command Bus latency two</h4>
  <p>Create a panel that shows the Command bus latency from the Axon Server perspective, but this time by target
    client</p>
  <Hint>
    <div>sum by(target) (axon_commands_seconds_sum) / sum by(target) (axon_commands_seconds_count)</div>
  </Hint>

  <h4>Query Bus latency two</h4>
  <p>Create a panel that shows the Query bus latency, but this time by target client</p>
  <Hint>
    <div>sum by(target) (axon_queries_seconds_sum) / sum by(target) (axon_queries_seconds_count)</div>
  </Hint>

  <h4>Finished</h4>

  <h3>Step 4: Play with the board</h3>
  <p>Now, we can observe the demo as we introduce chaos! If you have time left, feel free to play with any of these
    chaos buttons:</p>
  <ChaosButton title="Chaos configuration 1" :auction-query="{events: {delay: 2000}}"></ChaosButton>
  <ChaosButton title="Chaos configuration 2" :auctions="{commands: {errorRate: .25}}"></ChaosButton>
  <ChaosButton title="Reset"></ChaosButton>

</template>

<style scoped>
</style>
