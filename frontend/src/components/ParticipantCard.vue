<script setup lang="ts">

import type {Participant} from "@/dtos.types";
import {toName} from "../utils";

defineProps<{ participant: Participant }>()

const print = (item: any) => {
  console.log(item)
}
</script>

<template>
  <div class="card mb-2" style="height: 300px; overflow-y: scroll">
    <div class="card-body">
      <h5 class="card-title">{{ toName(participant.email) }}</h5>
      <h6 class="card-subtitle mb-2 text-muted d-flex justify-content-between">
        <span>
          Total worth: ${{ participant.balance + participant.items.map(i => i.boughtFor).reduce((a, b) => a + b, 0) }}
        </span>
        <span>
         Cash: ${{ participant.balance }}
        </span>
      </h6>
      <div class="card-text">
        <table class="table table-responsive">
          <tbody>

          <tr v-for="item of participant.items" @click="() => print(item)" :key="item.id"
              :title="`Bought for ${item.boughtFor}`">
            <td>{{ item.name }}</td>
            <td v-if="!item.auctioning"><i class="bi bi-bag-check"/> Bought</td>
            <td v-else><i class="bi bi-shop"/> Selling</td>
          </tr>
          <tr v-for="item in participant.activeBids" @click="() => print(item)" :key="item.auctionId" class="text-muted"
              :title="`Interest: ${item.interest}`">
            <td>{{ item.objectName }}</td>
            <td><i class="bi bi-cart"/> ${{ item.bid }}</td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<style>
.selling {
  color: #FE5E00;
}
</style>
