package com.netsentinel.app.ui.hunt

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HuntViewModel : ViewModel() {

    private val _isHunting = MutableStateFlow(false)
    val isHunting: StateFlow<Boolean> = _isHunting.asStateFlow()

    private val _rssiDbm = MutableStateFlow(-38)
    val rssiDbm: StateFlow<Int> = _rssiDbm.asStateFlow()

    fun toggleHunt() {
        _isHunting.value = !_isHunting.value
    }
}
