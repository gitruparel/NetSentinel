package com.netsentinel.app.domain.usecases

import com.netsentinel.app.data.model.Fingerprint
import com.netsentinel.app.data.model.NetworkSnapshot
import com.netsentinel.app.engine.FingerprintEngine

class GenerateFingerprintUseCase(
    private val fingerprintEngine: FingerprintEngine = FingerprintEngine()
) {
    operator fun invoke(snapshot: NetworkSnapshot): Fingerprint {
        return fingerprintEngine.generateFingerprint(snapshot)
    }
}
