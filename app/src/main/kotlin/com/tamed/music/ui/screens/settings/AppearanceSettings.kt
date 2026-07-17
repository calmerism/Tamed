/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.tamed.music.ui.screens.settings

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.tamed.music.BuildConfig
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tamed.music.LocalPlayerAwareWindowInsets
import com.tamed.music.R
import com.tamed.music.constants.ChipSortTypeKey
import com.tamed.music.constants.DarkModeKey
import com.tamed.music.constants.DefaultOpenTabKey
import com.tamed.music.constants.DynamicThemeKey
import com.tamed.music.constants.GridItemSize
import com.tamed.music.constants.GridItemsSizeKey
import com.tamed.music.constants.LibraryFilter
import com.tamed.music.constants.LyricsClickKey
import com.tamed.music.constants.LyricsScrollKey
import com.tamed.music.constants.LyricsTextPositionKey
import com.tamed.music.constants.PlayerDesignStyle
import com.tamed.music.constants.PlayerDesignStyleKey
import com.tamed.music.constants.PlayerBackgroundStyle
import com.tamed.music.constants.PlayerBackgroundStyleKey
import com.tamed.music.constants.HomeBackgroundStyle
import com.tamed.music.constants.HomeBackgroundStyleKey
import com.tamed.music.constants.ArtistBackgroundStyleKey
import com.tamed.music.ui.component.ListPreference
import com.tamed.music.constants.PureBlackKey
import com.tamed.music.constants.RandomThemeOnStartupKey
import com.tamed.music.constants.UseSystemFontKey
import com.tamed.music.constants.PlayerButtonsStyle
import com.tamed.music.constants.PlayerButtonsStyleKey
import com.tamed.music.constants.LyricsAnimationStyleKey
import com.tamed.music.constants.LyricsAnimationStyle
import com.tamed.music.constants.LyricsTextSizeKey
import com.tamed.music.constants.LyricsLineSpacingKey
import com.tamed.music.constants.SliderStyle
import com.tamed.music.constants.SliderStyleKey
import com.tamed.music.constants.ShowLikedPlaylistKey
import com.tamed.music.constants.ShowDownloadedPlaylistKey
import com.tamed.music.constants.ShowHomeCategoryChipsKey
import com.tamed.music.constants.ShowTopPlaylistKey
import com.tamed.music.constants.ShowCachedPlaylistKey
import com.tamed.music.constants.ShowTagsInLibraryKey
import com.tamed.music.constants.SwipeThumbnailKey
import com.tamed.music.constants.SwipeSensitivityKey
import com.tamed.music.constants.SwipeToSongKey
import com.tamed.music.constants.HidePlayerThumbnailKey
import com.tamed.music.constants.TamedCanvasKey
import com.tamed.music.constants.ThumbnailCornerRadiusKey
import com.tamed.music.constants.CropThumbnailToSquareKey
import com.tamed.music.constants.DisableBlurKey
import com.tamed.music.constants.BlurRadiusKey
import com.tamed.music.constants.UseLyricsV2Key
import com.tamed.music.constants.asSupportedPlayerDesignStyle
import com.tamed.music.ui.component.DefaultDialog
import com.tamed.music.ui.component.EnumListPreference
import com.tamed.music.ui.component.IconButton
import com.tamed.music.ui.component.ListPreference
import com.tamed.music.ui.component.PreferenceEntry
import com.tamed.music.ui.component.PreferenceGroupTitle
import com.tamed.music.ui.component.SwitchPreference
import com.tamed.music.ui.component.ThumbnailCornerRadiusSelectorButton
import com.tamed.music.ui.player.StyledPlaybackSlider
import com.tamed.music.ui.utils.backToMain
import com.tamed.music.utils.rememberEnumPreference
import com.tamed.music.utils.rememberPreference
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (dynamicTheme, onDynamicThemeChange) = rememberPreference(
        DynamicThemeKey,
        defaultValue = true
    )
    val (randomThemeOnStartup, onRandomThemeOnStartupChange) = rememberPreference(
        RandomThemeOnStartupKey,
        defaultValue = false
    )
    val (darkMode, onDarkModeChange) = rememberEnumPreference(
        DarkModeKey,
        defaultValue = DarkMode.AUTO
    )
    val (playerDesignStyle, onPlayerDesignStyleChange) = rememberEnumPreference(
        PlayerDesignStyleKey,
        defaultValue = PlayerDesignStyle.V1
    )
    val effectivePlayerDesignStyle = playerDesignStyle.asSupportedPlayerDesignStyle()
    val (useNewLibraryDesign, onUseNewLibraryDesignChange) = rememberPreference(
        key = com.tamed.music.constants.UseNewLibraryDesignKey,
        defaultValue = false
    )
    val (hidePlayerThumbnail, onHidePlayerThumbnailChange) = rememberPreference(
        HidePlayerThumbnailKey,
        defaultValue = false
    )
    val (tamedCanvasEnabled, onTamedCanvasEnabledChange) = rememberPreference(
        TamedCanvasKey,
        defaultValue = false
    )
    val (thumbnailCornerRadius, onThumbnailCornerRadiusChange) = rememberPreference(
        key = ThumbnailCornerRadiusKey,
        defaultValue = 8f // default dp
    )
    val (cropThumbnailToSquare, onCropThumbnailToSquareChange) = rememberPreference(
        CropThumbnailToSquareKey,
        defaultValue = false
    )
    val (playerBackground, onPlayerBackgroundChange) =
        rememberEnumPreference(
            PlayerBackgroundStyleKey,
            defaultValue = PlayerBackgroundStyle.BACKDROP,
        )
    val (homeBackground, onHomeBackgroundChange) =
        rememberEnumPreference(
            HomeBackgroundStyleKey,
            defaultValue = HomeBackgroundStyle.BACKDROP,
        )
    val (artistBackground, onArtistBackgroundChange) =
        rememberEnumPreference(
            ArtistBackgroundStyleKey,
            defaultValue = HomeBackgroundStyle.BACKDROP,
        )
    val (pureBlack, onPureBlackChange) = rememberPreference(PureBlackKey, defaultValue = false)
    val (disableBlur, onDisableBlurChange) = rememberPreference(DisableBlurKey, defaultValue = false)
    val (blurRadius, onBlurRadiusChange) = rememberPreference(BlurRadiusKey, defaultValue = 36f)
    val (useSystemFont, onUseSystemFontChange) = rememberPreference(UseSystemFontKey, defaultValue = false)
    val (defaultOpenTab, onDefaultOpenTabChange) = rememberEnumPreference(
        DefaultOpenTabKey,
        defaultValue = NavigationTab.HOME
    )
    val (playerButtonsStyle, onPlayerButtonsStyleChange) = rememberEnumPreference(
        PlayerButtonsStyleKey,
        defaultValue = PlayerButtonsStyle.DEFAULT
    )
    val (lyricsPosition, onLyricsPositionChange) = rememberEnumPreference(
        LyricsTextPositionKey,
        defaultValue = LyricsPosition.LEFT
    )
    val (lyricsAnimation, onLyricsAnimationChange) = rememberEnumPreference<LyricsAnimationStyle>(
    key = LyricsAnimationStyleKey,
    defaultValue = LyricsAnimationStyle.APPLE
    )
    val (lyricsClick, onLyricsClickChange) = rememberPreference(LyricsClickKey, defaultValue = true)
    val (lyricsScroll, onLyricsScrollChange) = rememberPreference(LyricsScrollKey, defaultValue = true)
    val (lyricsTextSize, onLyricsTextSizeChange) = rememberPreference(LyricsTextSizeKey, defaultValue = 26f)
    val (lyricsLineSpacing, onLyricsLineSpacingChange) = rememberPreference(LyricsLineSpacingKey, defaultValue = 1.3f)
    val (useLyricsV2, onUseLyricsV2Change) = rememberPreference(UseLyricsV2Key, defaultValue = false)

    val (sliderStyle, onSliderStyleChange) = rememberEnumPreference(
        SliderStyleKey,
        defaultValue = SliderStyle.Standard
    )
    val (swipeThumbnail, onSwipeThumbnailChange) = rememberPreference(
        SwipeThumbnailKey,
        defaultValue = true
    )
    val (swipeSensitivity, onSwipeSensitivityChange) = rememberPreference(
        SwipeSensitivityKey,
        defaultValue = 0.73f
    )
    val (gridItemSize, onGridItemSizeChange) = rememberEnumPreference(
        GridItemsSizeKey,
        defaultValue = GridItemSize.SMALL
    )

    val (swipeToSong, onSwipeToSongChange) = rememberPreference(
        SwipeToSongKey,
        defaultValue = false
    )

    val (showLikedPlaylist, onShowLikedPlaylistChange) = rememberPreference(
        ShowLikedPlaylistKey,
        defaultValue = true
    )
    val (showDownloadedPlaylist, onShowDownloadedPlaylistChange) = rememberPreference(
        ShowDownloadedPlaylistKey,
        defaultValue = true
    )
    val (showTopPlaylist, onShowTopPlaylistChange) = rememberPreference(
        ShowTopPlaylistKey,
        defaultValue = true
    )
    val (showCachedPlaylist, onShowCachedPlaylistChange) = rememberPreference(
        ShowCachedPlaylistKey,
        defaultValue = true
    )
    val (showTagsInLibrary, onShowTagsInLibraryChange) = rememberPreference(
        ShowTagsInLibraryKey,
        defaultValue = true
    )
    val (showHomeCategoryChips, onShowHomeCategoryChipsChange) = rememberPreference(
        ShowHomeCategoryChipsKey,
        defaultValue = true
    )

    val availableBackgroundStyles = PlayerBackgroundStyle.entries.filter {
        it != PlayerBackgroundStyle.BLUR || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }
    val isTamedCanvasAvailable = true
    val availablePlayerDesignStyles = remember {
        listOf(
            PlayerDesignStyle.V1,
            PlayerDesignStyle.V2,
            PlayerDesignStyle.V3,
            PlayerDesignStyle.V6,
        )
    }

    LaunchedEffect(playerDesignStyle) {
        if (playerDesignStyle != effectivePlayerDesignStyle) {
            onPlayerDesignStyleChange(effectivePlayerDesignStyle)
        }
    }

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme =
        remember(darkMode, isSystemInDarkTheme) {
            if (darkMode == DarkMode.AUTO) isSystemInDarkTheme else darkMode == DarkMode.ON
        }

    val (defaultChip, onDefaultChipChange) = rememberEnumPreference(
        key = ChipSortTypeKey,
        defaultValue = LibraryFilter.LIBRARY
    )

    var showSliderOptionDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showSliderOptionDialog) {
        val sliderStyles = remember {
            listOf(
                SliderStyle.Standard,
                SliderStyle.Wavy,
                SliderStyle.Thick,
                SliderStyle.Circular,
                SliderStyle.Simple
            )
        }
        DefaultDialog(
            buttons = {
                TextButton(
                    onClick = { showSliderOptionDialog = false },
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            },
            onDismiss = {
                showSliderOptionDialog = false
            }
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sliderStyles.chunked(3).forEach { styleRow ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        styleRow.forEach { style ->
                            SliderStyleOptionCard(
                                sliderStyle = style,
                                selected = sliderStyle == style,
                                onClick = {
                                    onSliderStyleChange(style)
                                    showSliderOptionDialog = false
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(3 - styleRow.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState()),
    ) {
        SwitchPreference(
            title = { Text(stringResource(R.string.enable_dynamic_theme)) },
            icon = { Icon(painterResource(R.drawable.palette), null) },
            checked = dynamicTheme,
            onCheckedChange = onDynamicThemeChange,
        )

        EnumListPreference(
            title = { Text(stringResource(R.string.dark_theme)) },
            icon = { Icon(painterResource(R.drawable.dark_mode), null) },
            selectedValue = darkMode,
            onValueSelected = onDarkModeChange,
            valueText = {
                when (it) {
                    DarkMode.ON -> stringResource(R.string.dark_theme_on)
                    DarkMode.OFF -> stringResource(R.string.dark_theme_off)
                    DarkMode.AUTO -> stringResource(R.string.dark_theme_follow_system)
                }
            },
        )

        PreferenceEntry(
            title = { Text(stringResource(R.string.blur_intensity)) },
            description = stringResource(R.string.blur_intensity_value, blurRadius.roundToInt()),
            icon = { Icon(painterResource(R.drawable.blur_on), null) },
            isEnabled = !disableBlur,
            content = {
                Spacer(modifier = Modifier.height(10.dp))
                Slider(
                    value = blurRadius,
                    onValueChange = onBlurRadiusChange,
                    valueRange = 0f..48f,
                    steps = 47,
                    enabled = !disableBlur,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )

        val (selectedFont, onSelectedFontChange) = rememberPreference(
            com.tamed.music.constants.SelectedFontKey,
            defaultValue = com.tamed.music.constants.AppFont.SYSTEM.value
        )
        val appFont = remember(selectedFont) { com.tamed.music.constants.AppFont.fromValue(selectedFont) }

        ListPreference(
            title = { Text("Font") },
            icon = { Icon(painterResource(R.drawable.text_fields), null) },
            selectedValue = appFont,
            values = com.tamed.music.constants.AppFont.entries,
            onValueSelected = { onSelectedFontChange(it.value) },
            valueText = {
                when (it) {
                    com.tamed.music.constants.AppFont.SYSTEM -> "System Font"
                    com.tamed.music.constants.AppFont.GOOGLE_SANS -> "Google Sans Flex"
                    com.tamed.music.constants.AppFont.SANS_FLEX -> "Sans Flex"
                    com.tamed.music.constants.AppFont.OUTFIT -> "Outfit"
                    com.tamed.music.constants.AppFont.PLUS_JAKARTA_SANS -> "Plus Jakarta Sans"
                }
            }
        )

        ListPreference(
            title = { Text(stringResource(R.string.player_design_style)) },
            icon = { Icon(painterResource(R.drawable.palette), null) },
            selectedValue = effectivePlayerDesignStyle,
            values = availablePlayerDesignStyles,
            onValueSelected = onPlayerDesignStyleChange,
            valueText = {
                when (it) {
                    PlayerDesignStyle.V1 -> stringResource(R.string.player_design_v1)
                    PlayerDesignStyle.V2 -> stringResource(R.string.player_design_v2)
                    PlayerDesignStyle.V3 -> stringResource(R.string.player_design_v3)
                    PlayerDesignStyle.V6 -> stringResource(R.string.player_design_v6)
                    else -> stringResource(R.string.player_design_v3)
                }
            },
        )

        val playerBackgroundStyles = remember {
            listOf(
                PlayerBackgroundStyle.DEFAULT,
                PlayerBackgroundStyle.COLORING,
                PlayerBackgroundStyle.GLOW,
                PlayerBackgroundStyle.GLOW_ANIMATED,
                PlayerBackgroundStyle.APPLE_MUSIC,
                PlayerBackgroundStyle.LIVE_MESH,
                PlayerBackgroundStyle.BACKDROP,
            )
        }
        val artistBackgroundStyles = remember {
            listOf(
                HomeBackgroundStyle.DEFAULT,
                HomeBackgroundStyle.BACKDROP,
                HomeBackgroundStyle.LIVE_MESH,
            )
        }

        val playerText = when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
            PlayerBackgroundStyle.COLORING -> stringResource(R.string.coloring)
            PlayerBackgroundStyle.GLOW -> stringResource(R.string.glow)
            PlayerBackgroundStyle.GLOW_ANIMATED -> "Glow (Animated)"
            PlayerBackgroundStyle.LIVE_MESH -> "Mesh"
            PlayerBackgroundStyle.APPLE_MUSIC -> "Cinematic"
            PlayerBackgroundStyle.BACKDROP -> "Backdrop"
            else -> ""
        }
        val artistText = when (artistBackground) {
            HomeBackgroundStyle.DEFAULT -> "Default"
            HomeBackgroundStyle.BACKDROP -> "Backdrop"
            HomeBackgroundStyle.LIVE_MESH -> "Mesh"
        }
        val backgroundStyleSummary = "Player: $playerText, Artist: $artistText"

        var showBackgroundStyleDialog by remember { mutableStateOf(false) }

        if (showBackgroundStyleDialog) {
            DefaultDialog(
                onDismiss = { showBackgroundStyleDialog = false },
                title = { Text("Background style") },
                buttons = {
                    TextButton(onClick = { showBackgroundStyleDialog = false }) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Player background selection row
                    var playerDropdownExpanded by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Player background", style = MaterialTheme.typography.bodyLarge)
                        Box {
                            TextButton(onClick = { playerDropdownExpanded = true }) {
                                Text(playerText)
                            }
                            DropdownMenu(
                                expanded = playerDropdownExpanded,
                                onDismissRequest = { playerDropdownExpanded = false }
                            ) {
                                playerBackgroundStyles.forEach { style ->
                                    val styleText = when (style) {
                                        PlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
                                        PlayerBackgroundStyle.COLORING -> stringResource(R.string.coloring)
                                        PlayerBackgroundStyle.GLOW -> stringResource(R.string.glow)
                                        PlayerBackgroundStyle.GLOW_ANIMATED -> "Glow (Animated)"
                                        PlayerBackgroundStyle.LIVE_MESH -> "Mesh"
                                        PlayerBackgroundStyle.APPLE_MUSIC -> "Cinematic"
                                        PlayerBackgroundStyle.BACKDROP -> "Backdrop"
                                        else -> ""
                                    }
                                    DropdownMenuItem(
                                        text = { Text(styleText) },
                                        onClick = {
                                            onPlayerBackgroundChange(style)
                                            playerDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }



                    // Artist background selection row
                    var artistDropdownExpanded by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Artist background", style = MaterialTheme.typography.bodyLarge)
                        Box {
                            TextButton(onClick = { artistDropdownExpanded = true }) {
                                Text(artistText)
                            }
                            DropdownMenu(
                                expanded = artistDropdownExpanded,
                                onDismissRequest = { artistDropdownExpanded = false }
                            ) {
                                artistBackgroundStyles.forEach { style ->
                                    val styleText = when (style) {
                                        HomeBackgroundStyle.DEFAULT -> "Default"
                                        HomeBackgroundStyle.BACKDROP -> "Backdrop"
                                        HomeBackgroundStyle.LIVE_MESH -> "Mesh"
                                    }
                                    DropdownMenuItem(
                                        text = { Text(styleText) },
                                        onClick = {
                                            onArtistBackgroundChange(style)
                                            artistDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        PreferenceEntry(
            title = { Text("Background style") },
            description = backgroundStyleSummary,
            icon = { Icon(painterResource(R.drawable.gradient), null) },
            onClick = { showBackgroundStyleDialog = true }
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.tamed_canvas)) },
            description = if (isTamedCanvasAvailable) {
                stringResource(R.string.tamed_canvas_desc)
            } else {
                stringResource(R.string.tamed_canvas_legacy_desc)
            },
            icon = { Icon(painterResource(R.drawable.motion_photos_on), null) },
            checked = tamedCanvasEnabled && isTamedCanvasAvailable,
            onCheckedChange = onTamedCanvasEnabledChange,
            isEnabled = isTamedCanvasAvailable,
        )

        ThumbnailCornerRadiusSelectorButton(
            onRadiusSelected = {}
        )

        PreferenceEntry(
            title = { Text(stringResource(R.string.player_slider_style)) },
            description = sliderStyleLabel(sliderStyle),
            icon = { Icon(painterResource(R.drawable.sliders), null) },
            onClick = {
                showSliderOptionDialog = true
            },
        )

        EnumListPreference(
            title = { Text(stringResource(R.string.player_buttons_style)) },
            icon = { Icon(painterResource(R.drawable.palette), null) },
            selectedValue = playerButtonsStyle,
            onValueSelected = onPlayerButtonsStyleChange,
            valueText = {
                when (it) {
                    PlayerButtonsStyle.DEFAULT -> stringResource(R.string.default_style)
                    PlayerButtonsStyle.SECONDARY -> stringResource(R.string.secondary_color_style)
                }
            },
        )

        SwitchPreference(
            title = { Text("Enable swipe to change song") },
            icon = { Icon(painterResource(R.drawable.swipe), null) },
            checked = swipeToSong,
            onCheckedChange = onSwipeToSongChange,
        )

        SwitchPreference(
            title = { Text("Enable swipe thumbnail") },
            icon = { Icon(painterResource(R.drawable.swipe), null) },
            checked = swipeThumbnail,
            onCheckedChange = onSwipeThumbnailChange,
        )

        AnimatedVisibility(swipeThumbnail || swipeToSong) {
            var showSensitivityDialog by rememberSaveable { mutableStateOf(false) }
            
            if (showSensitivityDialog) {
                var tempSensitivity by remember { mutableFloatStateOf(swipeSensitivity) }
                
                DefaultDialog(
                    onDismiss = { 
                        tempSensitivity = swipeSensitivity
                        showSensitivityDialog = false 
                    },
                    buttons = {
                        TextButton(
                            onClick = { 
                                tempSensitivity = 0.73f
                            },
                            shapes = ButtonDefaults.shapes(),
                        ) {
                            Text(stringResource(R.string.reset))
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        TextButton(
                            onClick = { 
                                tempSensitivity = swipeSensitivity
                                showSensitivityDialog = false 
                            },
                            shapes = ButtonDefaults.shapes(),
                        ) {
                            Text(stringResource(android.R.string.cancel))
                        }
                        TextButton(
                            onClick = { 
                                onSwipeSensitivityChange(tempSensitivity)
                                showSensitivityDialog = false 
                            },
                            shapes = ButtonDefaults.shapes(),
                        ) {
                            Text(stringResource(android.R.string.ok))
                        }
                    }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Mini Player Swipe Sensitivity",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
    
                        Text(
                            text = stringResource(R.string.sensitivity_percentage, (tempSensitivity * 100).roundToInt()),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
    
                        Slider(
                            value = tempSensitivity,
                            onValueChange = { tempSensitivity = it },
                            valueRange = 0f..1f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            
            PreferenceEntry(
                title = { Text("Mini Player Swipe Sensitivity") },
                description = stringResource(R.string.sensitivity_percentage, (swipeSensitivity * 100).roundToInt()),
                icon = { Icon(painterResource(R.drawable.tune), null) },
                onClick = { showSensitivityDialog = true }
            )
        }
    }


    TopAppBar(
        title = { Text(stringResource(R.string.appearance)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        }
    )
}

@Composable
private fun SliderStyleOptionCard(
    sliderStyle: SliderStyle,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sliderValue by remember {
        mutableFloatStateOf(0.5f)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        StyledPlaybackSlider(
            sliderStyle = sliderStyle,
            value = sliderValue,
            valueRange = 0f..1f,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = {},
            activeColor = MaterialTheme.colorScheme.primary,
            isPlaying = true,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        Text(
            text = sliderStyleLabel(sliderStyle),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun sliderStyleLabel(sliderStyle: SliderStyle): String {
    return when (sliderStyle) {
        SliderStyle.Standard -> stringResource(R.string.slider_style_standard)
        SliderStyle.Wavy -> stringResource(R.string.slider_style_wavy)
        SliderStyle.Thick -> stringResource(R.string.slider_style_thick)
        SliderStyle.Circular -> stringResource(R.string.slider_style_circular)
        SliderStyle.Simple -> stringResource(R.string.slider_style_simple)
    }
}

enum class DarkMode {
    ON,
    OFF,
    AUTO,
}

enum class NavigationTab {
    HOME,
    SEARCH,
    LIBRARY,
}

enum class LyricsPosition {
    LEFT,
    CENTER,
    RIGHT,
}

enum class PlayerTextAlignment {
    SIDED,
    CENTER,
}
