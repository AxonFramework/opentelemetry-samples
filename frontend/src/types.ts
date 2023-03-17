export interface Taints {
    errorRate?: ErrorRateTaint,
    fixedDelay?: FixedDelayTaint,
    randomDelay?: RandomDelayTaint,
}

export interface ErrorRateTaint {
    rate: number
    runtimeException: boolean
}

export interface FixedDelayTaint {
    delay: number
}

export interface RandomDelayTaint {
    higherBound: number
    lowerBound: number

}
