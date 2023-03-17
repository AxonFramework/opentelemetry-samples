<script setup lang="ts">


import {computed, onUnmounted, ref} from "vue";

interface ActiveAuction {
  identifier: string;
  objectId: string;
  state: string;
  minimumBid: number;
  currentBid: number;
  currentBidder: string;
  endTime: string;
}

interface Participant {
  id: string
  email: string
  state: string
  balance: number
}

interface ObjectItem {
  identifier: string
  name: string
  owner: string,
}

const auctions = ref<{ [id: string]: ActiveAuction }>({})
const participants = ref<{ [id: string]: Participant }>({})
const ownerships = ref<{ [id: string]: ObjectItem }>({})
const events = ref<string[]>([])
const auctionError = ref<boolean>(false)
const participantError = ref<boolean>(false)
const ownershipError = ref<boolean>(false)

const participantAmount = ref(0)

let participantAmountInFlight = false;
function fetchAmountOfparticipants() {
  if(participantAmountInFlight) {
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

const inerval = setInterval(() => {
  fetchAmountOfparticipants();
}, 2000)
onUnmounted(() => clearInterval(inerval))
fetchAmountOfparticipants();

function addEvent(content: string) {
  events.value = [content, ...events.value.splice(0, 30)]
}

let auctionSource: EventSource | null = null;
function openAuctionSource() {
  auctionSource = new EventSource(`/api/auctions`)
  auctionSource.onopen = () => {
    auctions.value = {}
    auctionError.value = false
  }
  auctionSource.onerror = () => {
    auctionError.value = true
    auctionSource?.close()
    setTimeout(openAuctionSource, 500)
  }

  auctionSource.onmessage = (ev) => {
    const data = JSON.parse(ev.data) as ActiveAuction;
    if (data.state === "ENDED") {
      addEvent(`Auction with id ${data.identifier} ended. Item goes to ${participants.value[data.currentBidder]?.email || data.currentBidder}`)
      const copy = {...auctions.value}
      delete copy[data.identifier]
      auctions.value = copy
    } else {
      if (data.currentBidder) {
        addEvent(`Auction with id ${data.identifier} got new bid of ${data.currentBid}.`)
      } else {
        addEvent(`Auction with id ${data.identifier} created with minimum bid of ${data.minimumBid}.`)
      }
      auctions.value = {...auctions.value, [data.identifier]: data}
    }
  }
}

openAuctionSource()


let participantSource: EventSource | null = null;

function openParticipantSource() {
  participantSource = new EventSource(`/api/participants`)
  participantSource.onopen = () => {
    participants.value = {}
    participantError.value = false
  }
  participantSource.onerror = () => {
    participantError.value = true
    participantSource?.close()
    setTimeout(openParticipantSource, 5000)
  }

  participantSource.onmessage = (ev) => {
    const data = JSON.parse(ev.data) as Participant;
    participants.value = {...participants.value, [data.id]: data}
  }
}

openParticipantSource()


let ownershipSource: EventSource | null = null;
function openOwnershipSource() {
  ownershipSource = new EventSource(`/api/ownership`)
  ownershipSource.onopen = () => {
    ownerships.value = {}
    ownershipError.value = false
  }
  ownershipSource.onerror = (e) => {
    ownershipError.value = true
    ownershipSource?.close()
    setTimeout(openOwnershipSource, 5000)
  }

  ownershipSource.onmessage = (ev) => {
    const data = JSON.parse(ev.data) as ObjectItem;
    ownerships.value = {...ownerships.value, [data.identifier]: data}
  }
}

openOwnershipSource()


const auctionsSortedById = computed(() => {
  return Object.values(auctions.value).sort((a: any, b: any) => a.identifier.localeCompare(b.identifier))
})
</script>

<template>
  <div class="container-fluid">
    <nav class="navbar navbar-expand-lg navbar-light bg-white">
      <div class="container-fluid px-3">
        <a class="navbar-brand" href="/"><img src="./assets/logo.svg" alt="" width="30" height="24"> AxonFramework
          Auction Demo</a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarSupportedContent"
                aria-controls="navbarSupportedContent" aria-expanded="false" aria-label="Toggle navigation">
          <span class="navbar-toggler-icon"></span>
        </button>
      </div>
    </nav>

    <div class="bg-white mt-4 rounded m-4 p-4">
      <div class="alert-warning alert" v-if="auctionError || participantError || ownershipError">
        Was unable to connect to backed. Data might be incomplete. Will reconnect once available.
      </div>
      <div class="row">
        <div class="col-md-12 mb-4">
          <h1>Configuration</h1>
          <p>Number of partcipants: {{ participantAmount }}</p>
          <div>
            <button type="button" class="btn btn-secondary mx-2" @click.prevent.stop="() => updateParticipantAmount(participantAmount + 1)">Add participant</button>
            <button type="button" class="btn btn-secondary mx-2" @click.prevent.stop="() => updateParticipantAmount(participantAmount - 1)">Remove participant</button>
          </div>
        </div>
        <div class="col-md-8">
          <h1>Auctions</h1>
          <table class="table table-responsive">
            <thead>
            <tr>
              <th>Auction id</th>
              <th>Object</th>
              <th>Minimal bid</th>
              <th>Current bid</th>
              <th>By</th>
              <th>End Time</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="row in auctionsSortedById">
              <td>{{ row.identifier }}</td>
              <td>{{ownerships[row.objectId].name }}</td>
              <td>{{ row.minimumBid }}</td>
              <td>{{ row.currentBid }}</td>
              <td>{{ participants[row.currentBidder]?.email || row.currentBidder }}</td>
              <td>{{ row.endTime }}</td>
            </tr>
            </tbody>
          </table>

          <h1>Participants</h1>
          <table class="table table-responsive">
            <thead>
            <tr>
              <th>ID</th>
              <th>Email</th>
              <th>Balance</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="row in participants">
              <td>{{ row.id }}</td>
              <td>{{ row.email }}</td>
              <td>{{ row.balance }}</td>
            </tr>
            </tbody>
          </table>

          <h1>Ownerships</h1>
          <table class="table table-responsive">
            <thead>
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>Owner</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="row in ownerships">
              <td>{{ row.identifier }}</td>
              <td>{{ row.name }}</td>
              <td>{{ participants[row.owner]?.email || row.owner }}</td>
            </tr>
            </tbody>
          </table>
        </div>
        <div class="col-md-4">
          <h1>Last 100 events</h1>
          <div v-for="event in events">
            {{ event }}
          </div>
        </div>

      </div>

    </div>
  </div>
</template>

<style>
.selling {
  color: orange;
}
</style>
