/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.tamed.music.spotiflac

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.tamed.music.R

enum class SpotiFlacProvider(val requestValue: String, val label: String) {
    TIDAL("tidal", "Tidal"),
    QOBUZ("qobuz", "Qobuz"),
}

data class SpotiFlacQuality(
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    @DrawableRes val iconRes: Int,
    val requestValue: String,
) {
    companion object {
        val all =
            listOf(
                SpotiFlacQuality(
                    titleRes = R.string.flac_lossless,
                    subtitleRes = R.string.flac_lossless_desc,
                    iconRes = R.drawable.music_note,
                    requestValue = "LOSSLESS",
                ),
                SpotiFlacQuality(
                    titleRes = R.string.hi_res_flac,
                    subtitleRes = R.string.hi_res_flac_desc,
                    iconRes = R.drawable.graphic_eq,
                    requestValue = "HI_RES",
                ),
                SpotiFlacQuality(
                    titleRes = R.string.hi_res_flac_max,
                    subtitleRes = R.string.hi_res_flac_max_desc,
                    iconRes = R.drawable.graphic_eq,
                    requestValue = "HI_RES_MAX",
                ),
            )
    }
}
