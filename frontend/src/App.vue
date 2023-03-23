<script setup lang="ts">


import {computed, onUnmounted, reactive, ref} from "vue";
import type {ActiveAuction, ObjectItem, Participant} from "@/dtos.types";
import Configuration from "@/components/Configuration.vue";
import ParticipantCard from "@/components/ParticipantCard.vue";
import {toName} from "./utils";

let auctions = reactive<{ [id: string]: ActiveAuction }>({})
let participants = reactive<{ [id: string]: Participant }>({})
let ownerships = reactive<{ [id: string]: ObjectItem }>({})
const events = ref<Event[]>([])
const auctionError = ref<boolean>(false)
const participantError = ref<boolean>(false)
const ownershipError = ref<boolean>(false)


interface Event {
  index: number
  objectName: string
  auctionId: string
  type: string
  value: string
}

let index = 0;

function addEvent(content: Omit<Event, 'index'>) {
  index++
  events.value = [{...content, index}, ...events.value.splice(0, 20)]
}

let auctionSource: EventSource | null = null;

function openAuctionSource() {
  auctionSource = new EventSource(window.location.pathname + `api/auctions`)
  auctionSource.onopen = () => {
    Object.keys(auctions).forEach(key => {
      delete auctions [key]
    })
    auctionError.value = false
  }
  auctionSource.onerror = () => {
    auctionError.value = true
    auctionSource?.close()
    setTimeout(openAuctionSource, 500)
  }

  auctionSource.onmessage = (ev) => {
    const datas = JSON.parse(ev.data) as ActiveAuction[];
    datas.forEach(data => {
      const objectName = ownerships[data.objectId]?.name || data.objectId;
      const bidder = data.currentBidder ? toName(participants[data.currentBidder]?.email || data.currentBidder) : null;
      if (data.state === "ENDED") {
        addEvent({
          auctionId: data.identifier,
          objectName: objectName,
          type: 'Auction won',
          value: bidder || 'No one'
        })
        delete auctions[data.identifier]
      } else {
        if (data.currentBidder) {
          addEvent({
            auctionId: data.identifier,
            objectName: objectName,
            type: 'New bid',
            value: `${data.currentBid} by ${bidder || 'Unknown'}`
          })
        } else {
          addEvent({
            auctionId: data.identifier,
            objectName: objectName,
            type: 'New Auction',
            value: ''
          })
        }
        auctions[data.identifier] = data
      }
    })
  }
}

openAuctionSource()


let participantSource: EventSource | null = null;

function openParticipantSource() {
  participantSource = new EventSource(window.location.pathname + `api/participants`)
  participantSource.onopen = () => {
    Object.keys(participants).forEach(key => {
      delete participants [key]
    })
    participantError.value = false
  }
  participantSource.onerror = () => {
    participantError.value = true
    participantSource?.close()
    setTimeout(openParticipantSource, 5000)
  }

  participantSource.onmessage = (ev) => {
    const datas = JSON.parse(ev.data) as Participant[];
    datas.forEach(data => {
      if (data.terminated) {
        delete participants[data.id]
      } else {
        participants[data.id] = data
      }
    })
  }
}

openParticipantSource()


let ownershipSource: EventSource | null = null;

function openOwnershipSource() {
  ownershipSource = new EventSource(window.location.pathname + `api/ownership`)
  ownershipSource.onopen = () => {
    Object.keys(ownerships).forEach(key => {
      delete ownerships [key]
    })
    ownershipError.value = false
  }
  ownershipSource.onerror = (e) => {
    ownershipError.value = true
    ownershipSource?.close()
    setTimeout(openOwnershipSource, 5000)
  }

  ownershipSource.onmessage = (ev) => {
    const datas = JSON.parse(ev.data) as ObjectItem[];
    datas.forEach(data => {
      ownerships[data.identifier] = data
    })
  }
}

openOwnershipSource()


const auctionsSortedById = computed(() => {
  return Object.values(auctions).sort((a, b) => new Date(a.endTime).getTime() - new Date(b.endTime).getTime())
})

const formatTime = (date: string) => {
  let time = new Date(date);
  return time.getHours().toString().padStart(2, "0") + ":" + time.getMinutes().toString().padStart(2, "0") + ":" + time.getSeconds().toString().padStart(2, "0")
}

const tab = ref('auctions')
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
          <ul class="nav nav-pills">
            <li class="nav-item">
              <a :class="`nav-link ${tab === 'auctions' ? 'active' : ''}`" aria-current="page" href="#"
                 @click.prevent.stop="() => tab = 'auctions'">Auctions ({{ Object.keys(auctions).length }})</a>
            </li>
            <li class="nav-item">
              <a :class="`nav-link ${tab === 'participants' ? 'active' : ''}`" aria-current="page" href="#"
                 @click.prevent.stop="() => tab = 'participants'">Participants ({{
                  Object.keys(participants).length
                }})</a>
            </li>
            <li class="nav-item">
              <a :class="`nav-link ${tab === 'config' ? 'active' : ''}`" aria-current="page" href="#"
                 @click.prevent.stop="() => tab = 'config'">Configuration</a>
            </li>
          </ul>
        </div>
        <div class="col-md-12 mb-4" v-if="tab === 'config'">
          <Configuration/>
        </div>
        <div class="col-md-6" v-if="tab === 'auctions'">
          <h1>Auctions ({{ Object.keys(auctions).length }})</h1>
          <table class="table table-responsive">
            <thead>
            <tr>
              <th>Item</th>
              <th>Minimal bid</th>
              <th>Current bid</th>
              <th>By</th>
              <th>End Time</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="row in auctionsSortedById" :key="row.identifier">
              <td>{{ ownerships[row.objectId]?.name }}</td>
              <td>{{ row.minimumBid }}</td>
              <td>{{ row.currentBid || '-' }}</td>
              <td>{{ toName(participants[row.currentBidder]?.email || row.currentBidder) }}</td>
              <td>{{ formatTime(row.endTime) }}</td>
            </tr>
            </tbody>
          </table>

        </div>
        <div class="col-md-6" v-if="tab === 'auctions'">
          <h1>Last 20 events</h1>
          <table class="table table-responsive">
            <thead>
            <tr>
              <th>Index</th>
              <th>Object</th>
              <th>Event</th>
              <th>Details</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="event in events" :key="event.index">
              <td>{{ event.index }}</td>
              <td>{{ event.objectName }}</td>
              <td>{{ event.type }}</td>
              <td>{{ event.value }}</td>
            </tr>
            </tbody>
          </table>
        </div>


        <div class="col-md-12" v-if="tab === 'participants'">
          <div class="row">
            <p>Total participant auctions:
              {{ Object.values(participants).flatMap(p => p.items.filter(i => i.auctioning)).length }} / Total
              participant bidding: {{ Object.values(participants).flatMap(p => p.activeBids).length }}</p>

            <div class="col-md-3" v-for="participant in participants">
              <ParticipantCard :participant="participant"/>
            </div>
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
