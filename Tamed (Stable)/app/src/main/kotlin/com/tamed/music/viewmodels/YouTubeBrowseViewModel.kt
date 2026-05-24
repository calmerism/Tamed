/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.tamed.music.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tamed.music.innertube.YouTube
import com.tamed.music.innertube.pages.BrowseResult
import com.tamed.music.constants.HideExplicitKey
import com.tamed.music.constants.HideVideoKey
import com.tamed.music.utils.dataStore
import com.tamed.music.utils.get
import com.tamed.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class YouTubeBrowseViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val browseId = savedStateHandle.get<String>("browseId")!!
    private val params = savedStateHandle.get<String>("params")

    val result = MutableStateFlow<BrowseResult?>(null)

    init {
        viewModelScope.launch {
            YouTube
                .browse(browseId, params)
                .onSuccess {
                    val hideVideo = context.dataStore.get(HideVideoKey, false)
                    result.value = it.filterExplicit(context.dataStore.get(HideExplicitKey, false)).filterVideo(hideVideo)
                }.onFailure {
                    reportException(it)
                }
        }
    }
}
