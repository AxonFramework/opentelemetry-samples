/*
 * Copyright (c) 2023-2023. AxonIQ
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

export interface ActiveAuction {
    identifier: string;
    objectId: string;
    state: string;
    minimumBid: number;
    currentBid: number;
    currentBidder: string;
    endTime: string;
}

export interface Participant {
    id: string
    terminated: boolean
    email: string
    balance: number
    activeBids: ParticipantBid[],
    items: ParticipantItem[]
}

export interface ParticipantBid {
    auctionId: string
    objectId: string
    objectName: string
    bid: number
    interest: number
    receivedWinUpdate: boolean
    receivedBalanceUpdate: boolean
    receivedItemUpdate: boolean
}

export interface ObjectItem {
    identifier: string
    name: string
    owner: string,
}


export interface ParticipantItem {
    id: string
    name: string
    boughtFor: number
    auctionStarted: string
    auctioning: boolean,
    receivedBalanceUpdate: boolean
    receivedItemUpdate: boolean
}
