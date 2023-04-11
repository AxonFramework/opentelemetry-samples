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
import type {ApplicationChaosSettings, ServiceChaosSettings} from "@/stores/chaos";

interface Props extends ServiceChaosSettings{
    title: string,
}

const props = defineProps<Props>()

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
        const result = map[baseUrl]() || {};
        console.log(result)
        fetch(baseUrl + "/fire-starter/settings", {
            method: 'POST',
            body: JSON.stringify(result),
            headers: {'Content-Type': 'application/json'}
        }).then(() => {
            loading.value = false
        })
    })
}
</script>

<template>
    <div class="btn btn-outline-secondary btn-sm d-inline-block mx-2 mb-3" @click="setChaos()">{{ title }}</div>
</template>

<style scoped>
</style>
