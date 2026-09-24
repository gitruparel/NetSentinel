package com.netsentinel.app.repository

import com.netsentinel.app.data.model.NetworkSnapshot
import com.netsentinel.app.network.NetworkScanner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

interface NetworkRepository {
    fun getNetworkSnapshot(): NetworkSnapshot
    fun observeNetworkState(): Flow<NetworkSnapshot>
}

class FakeNetworkRepository(
    private val scanner: NetworkScanner = NetworkScanner()
) : NetworkRepository {

    private val snapshotState = MutableStateFlow(scanner.captureSnapshot())

    override fun getNetworkSnapshot(): NetworkSnapshot = snapshotState.value

    override fun observeNetworkState(): Flow<NetworkSnapshot> = snapshotState
}
