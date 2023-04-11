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

export interface ChaosTaints {
    errorRate?: { rate: number, runtimeException?: boolean },
    fixedDelay?: { delay: number },
}

export interface ChaosSettings {
    handlers?: ChaosTaints
}

export interface EventChaosSettings extends ChaosSettings {
    readAggregateStream?: ChaosTaints,
    publishEvent?: ChaosTaints,
    commitEvents?: ChaosTaints,
}

export interface CommandChaosSettings extends ChaosSettings {
    lockTime?: ChaosTaints,
    dispatch?: ChaosTaints,
    repositoryLoad?: ChaosTaints,
}

export interface ApplicationChaosSettings {
    query?: ChaosSettings,
    events?: EventChaosSettings,
    command?: CommandChaosSettings,
}

export interface ServiceChaosSettings {
    auctions?: ApplicationChaosSettings,
    objectRegistry?: ApplicationChaosSettings,
    auctionQuery?: ApplicationChaosSettings,
    participants?: ApplicationChaosSettings,
}
