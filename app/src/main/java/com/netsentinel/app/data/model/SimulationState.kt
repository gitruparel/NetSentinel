package com.netsentinel.app.data.model

/**
 * Interactive developer simulation attack flags.
 */
data class SimulationState(
    val isDuplicateSsidActive: Boolean = false,
    val isGatewayChangeActive: Boolean = false,
    val isDnsChangeActive: Boolean = false,
    val isLatencySpikeActive: Boolean = false,
    val isPacketLossActive: Boolean = false,
    val isSecurityDowngradeActive: Boolean = false
)
