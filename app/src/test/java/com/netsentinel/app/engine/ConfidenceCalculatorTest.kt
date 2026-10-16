package com.netsentinel.app.engine

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ConfidenceCalculatorTest {

    private lateinit var calculator: ConfidenceCalculator

    @Before
    fun setup() {
        calculator = ConfidenceCalculator()
    }

    @Test
    fun calculateConfidenceScore_noReasons_returns100() {
        val score = calculator.calculateConfidenceScore(emptyList())
        assertEquals(100, score)
    }

    @Test
    fun calculateConfidenceScore_singleDeduction_subtractsPoints() {
        val reasons = listOf(
            ThreatReason(ThreatType.LATENCY_HIJACK, ThreatSeverity.MEDIUM, "Latency spike", 15)
        )
        val score = calculator.calculateConfidenceScore(reasons)
        assertEquals(85, score)
    }

    @Test
    fun calculateConfidenceScore_multipleDeductions_sumsDeductions() {
        val reasons = listOf(
            ThreatReason(ThreatType.ARP_SPOOFING, ThreatSeverity.HIGH, "Gateway change", 30),
            ThreatReason(ThreatType.ROGUE_DNS, ThreatSeverity.CRITICAL, "Rogue DNS", 40)
        )
        val score = calculator.calculateConfidenceScore(reasons)
        assertEquals(30, score)
    }

    @Test
    fun calculateConfidenceScore_extremeDeductions_clampsToZero() {
        val reasons = listOf(
            ThreatReason(ThreatType.EVIL_TWIN, ThreatSeverity.CRITICAL, "Evil twin clone", 50),
            ThreatReason(ThreatType.SECURITY_DOWNGRADE, ThreatSeverity.CRITICAL, "Open broadcast", 45),
            ThreatReason(ThreatType.ROGUE_DNS, ThreatSeverity.CRITICAL, "Rogue DNS", 40)
        )
        val score = calculator.calculateConfidenceScore(reasons)
        assertEquals(0, score)
    }
}
