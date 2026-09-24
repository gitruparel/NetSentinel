package com.netsentinel.app.domain.usecases

import com.netsentinel.app.data.model.NetworkSnapshot
import com.netsentinel.app.engine.BaselineAnalyzer
import com.netsentinel.app.engine.ThreatReason

class AnalyzeBaselineUseCase(
    private val baselineAnalyzer: BaselineAnalyzer = BaselineAnalyzer()
) {
    operator fun invoke(snapshot: NetworkSnapshot): List<ThreatReason> {
        return baselineAnalyzer.analyzeBaselineDeviation(snapshot)
    }
}
