/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.tamed.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tamed.music.innertube.YouTube
import com.tamed.music.innertube.pages.MoodAndGenres
import com.tamed.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MoodAndGenresViewModel
@Inject
constructor() : ViewModel() {
    val moodAndGenres = MutableStateFlow<List<MoodAndGenres.Item>?>(null)

    init {
        viewModelScope.launch {
            YouTube
                .explore()
                .onSuccess {
                    moodAndGenres.value = it.moodAndGenres
                }.onFailure {
                    reportException(it)
                }
        }
    }
}
