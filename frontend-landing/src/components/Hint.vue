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

const revealed = ref(false)
</script>

<template>
  <div class="alert alert-link p-1 px-2 fw-light border-1 border-light" style="cursor:pointer" @click="revealed = !revealed">
    <div v-if="revealed">
      <slot></slot>
    </div>
    <div v-else>
      <i class="bi bi-info-circle mx-2"></i>Hint
    </div>
  </div>
</template>

<style scoped>
</style>
