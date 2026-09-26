/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.tamed.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import com.tamed.music.network.NetworkBannerUiState
import com.tamed.music.network.ObserveNetworkBannerStateUseCase

@HiltViewModel
class NetworkBannerViewModel
@Inject
constructor(
    observeNetworkBannerStateUseCase: ObserveNetworkBannerStateUseCase,
) : ViewModel() {
    val bannerState: StateFlow<NetworkBannerUiState> =
        observeNetworkBannerStateUseCase()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = NetworkBannerUiState.Hidden,
            )
}
