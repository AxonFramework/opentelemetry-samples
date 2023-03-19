<script setup lang="ts">
import {onMounted, onUnmounted, ref} from "vue";

const participantAmount = ref(0)

let participantAmountInFlight = false;

function fetchAmountOfparticipants() {
  if (participantAmountInFlight) {
    return
  }
  participantAmountInFlight = true;
  fetch('/api/participants/count', {method: 'GET'}).then((r: Response) => {
    participantAmountInFlight = false;
    r.text().then(t => participantAmount.value = parseInt(t))
  }).catch(e => {
    participantAmountInFlight = false;
  })
}

function updateParticipantAmount(amount: number) {
  fetch(`/api/participants/count/${amount}`, {method: 'PUT'}).then((r: Response) => {
    fetchAmountOfparticipants()
  })
}

let interval: any;
onMounted(() => {
  interval = setInterval(() => {
    fetchAmountOfparticipants();
  }, 2000)
})
onUnmounted(() => clearInterval(interval))
fetchAmountOfparticipants();
</script>

<template>
  <h1>Configuration</h1>
  <p>Number of partcipants: {{ participantAmount }}</p>
  <div>
    <button type="button" class="btn btn-secondary mx-2"
            @click.prevent.stop="() => updateParticipantAmount(participantAmount + 1)">Add participant
    </button>
    <button type="button" class="btn btn-secondary mx-2"
            @click.prevent.stop="() => updateParticipantAmount(participantAmount - 1)">Remove participant
    </button>
  </div>
</template>

