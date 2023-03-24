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

import {ref} from "vue";

interface ChaosSettings {
  errorRate?: number,
  delay?: number,
}

interface ApplicationChaosSettings {
  queries?: ChaosSettings,
  events?: ChaosSettings,
  commands?: ChaosSettings,
}

const props = defineProps<{
  title: string,
  auctions?: ApplicationChaosSettings,
  objectRegistry?: ApplicationChaosSettings,
  auctionQuery?: ApplicationChaosSettings,
  participants?: ApplicationChaosSettings,
}>()

const map: { [key: string]: () => ApplicationChaosSettings | undefined } = {
  "/service-participants": () => props.participants,
  "/service-auction-object-registry": () => props.objectRegistry,
  "/service-auction-query": () => props.auctionQuery,
  "/service-auctions": () => props.auctions,
}

const loading = ref(false)

const setChaos = () => {
  loading.value = true
  Object.keys(map).forEach((baseUrl: string) => {
    const result = map[baseUrl]();
    const call: any = {
      command: {
        handlers: {
          errorRate: null,
          fixedDelay: null,
        }
      },
      events: {
        handlers: {
          errorRate: null,
          fixedDelay: null,
        }
      },
      query: {
        handlers: {
          errorRate: null,
          fixedDelay: null,
        }
      }
    };
    if (result?.commands?.delay) {
      call.command.handlers.fixedDelay = {delay: result.commands.delay}
    }
    if (result?.commands?.errorRate) {
      call.command.handlers.errorRate = {rate: result.commands.errorRate, runtimeException: true}
    }
    if (result?.queries?.delay) {
      call.query.handlers.fixedDelay = {delay: result.queries.delay}
    }
    if (result?.queries?.errorRate) {
      call.query.handlers.errorRate = {rate: result.queries.errorRate, runtimeException: true}
    }
    if (result?.events?.delay) {
      call.events.handlers.fixedDelay = {delay: result.events.delay}
    }
    if (result?.events?.errorRate) {
      call.events.handlers.errorRate = {rate: result.events.errorRate, runtimeException: true}
    }

    fetch(baseUrl + "/fire-starter/settings", {
      method: 'POST',
      body: JSON.stringify(call),
      headers: {'Content-Type': 'application/json'}
    }).then(() => {
      loading.value = false
    })
  })
}
</script>

<template>
  <div class="btn btn-outline-secondary d-inline-block mx-2" @click="setChaos()">{{ title }}</div>
</template>

<style scoped>
</style>
