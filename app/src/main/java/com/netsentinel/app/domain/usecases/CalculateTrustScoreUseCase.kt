package com.netsentinel.app.domain.usecases

import com.netsentinel.app.data.model.NetworkSnapshot
import com.netsentinel.app.data.model.SimulationState
import com.netsentinel.app.engine.ThreatEngine

class CalculateTrustScoreUseCase(
    private val threatEngine: ThreatEngine = ThreatEngine()
) {
    operator fun invoke(
        snapshot: NetworkSnapshot,
        simulationState: SimulationState = SimulationState(),
        expectedGateway: String = "",
        expectedDns: String = ""
    ): ThreatEngine.EvaluationResult {
        return threatEngine.evaluateNetwork(snapshot, simulationState, expectedGateway, expectedDns)
    }
}
