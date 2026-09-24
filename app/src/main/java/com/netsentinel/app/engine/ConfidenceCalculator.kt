package com.netsentinel.app.engine

/**
 * Calculates statistical threat confidence rating based on multi-vector signals.
 */
class ConfidenceCalculator {

    fun calculateConfidenceScore(reasons: List<ThreatReason>): Int {
        if (reasons.isEmpty()) return 100 // 100% confidence clean
        val maxDeduction = reasons.sumOf { it.deductionPoints }
        return (100 - maxDeduction).coerceIn(0, 100)
    }
}
