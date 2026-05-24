/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.tamed.music.ui.screens.settings

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.animation.animateColorAsState
import coil3.compose.AsyncImage
import com.tamed.music.R

@Composable
fun SettingsProfileHeader(
    state: SettingsProfileState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.995f else 1f,
        animationSpec = SettingsAnimations.pressSpring(),
        label = "profileHeaderScale",
    )
    val title = if (state.isLoading) {
        stringResource(R.string.loading)
    } else if (state.isLoggedIn) {
        state.accountName.ifBlank { stringResource(R.string.account) }
    } else {
        stringResource(R.string.login)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                )
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(SettingsDimensions.RowIconSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                if (state.isLoading) {
                    CircularWavyProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else if (state.isLoggedIn && !state.accountImageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = state.accountImageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                    )
                } else {
                    Icon(
                        painter = painterResource(if (state.isLoggedIn) R.drawable.account else R.drawable.login),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(SettingsDimensions.RowIconInnerSize),
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = state.accountEmail.ifBlank { stringResource(R.string.account) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Icon(
                painter = painterResource(R.drawable.navigate_next),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                modifier = Modifier.size(SettingsDimensions.ChevronSize),
            )
        }

    }
}

@Composable
fun SettingsPermissionBanner(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(SettingsDimensions.RowIconSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.security),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(SettingsDimensions.RowIconInnerSize),
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.permissions_title),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.permissions_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.allow),
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }

    }
}

@Composable
fun SettingsUpdateBanner(
    latestVersion: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) SettingsAnimations.PressScale else 1f,
        animationSpec = SettingsAnimations.pressSpring(),
        label = "updateScale",
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(SettingsDimensions.BannerCardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            Color.Transparent,
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                    ),
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(SettingsDimensions.BannerIconSize)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.update),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(SettingsDimensions.BannerIconInnerSize),
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.new_version_available),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "v$latestVersion",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Medium,
                )
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_forward),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
fun SettingsSearchEmpty(
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(SettingsDimensions.GroupCardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.search),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }

            Text(
                text = stringResource(R.string.no_results_found),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = stringResource(R.string.search_try_different),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun SettingsGroupCard(
    group: SettingsGroup,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (group.title.isNotEmpty()) {
            Text(
                text = group.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    horizontal = 2.dp,
                    vertical = 10.dp,
                ),
            )
        }

        Column {
            group.items.forEachIndexed { index, item ->
                SettingsRow(
                    item = item,
                    showDivider = index < group.items.size - 1,
                )
            }
        }
    }
}

@Composable
fun SettingsRow(
    item: SettingsItem,
    showDivider: Boolean,
    modifier: Modifier = Modifier,
) {
    val effectiveAccent = if (item.accentColor.isSpecified) {
        item.accentColor
    } else {
        MaterialTheme.colorScheme.primary
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.99f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "rowScale",
    )

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = item.onClick,
                )
                .padding(
                    vertical = 14.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(SettingsDimensions.RowIconSize)
                    .clip(CircleShape)
                    .background(effectiveAccent.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                if (item.showUpdateIndicator) {
                    BadgedBox(
                        badge = {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(8.dp),
                            )
                        },
                    ) {
                        Icon(
                            painter = item.icon,
                            contentDescription = null,
                            tint = effectiveAccent,
                            modifier = Modifier.size(SettingsDimensions.RowIconInnerSize),
                        )
                    }
                } else {
                    Icon(
                        painter = item.icon,
                        contentDescription = null,
                        tint = effectiveAccent,
                        modifier = Modifier.size(SettingsDimensions.RowIconInnerSize),
                    )
                }
            }

            Spacer(modifier = Modifier.width(SettingsDimensions.RowIconTextSpacing))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                item.subtitle?.let { subtitle ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.showUpdateIndicator) effectiveAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            item.badge?.let { badge ->
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                painter = painterResource(R.drawable.navigate_next),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.66f),
                modifier = Modifier.size(SettingsDimensions.ChevronSize),
            )
        }
    }
}

@Composable
fun ExpressiveSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(SettingsDimensions.GroupCardCornerRadius),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier.padding(vertical = 4.dp),
                content = content,
            )
        }
    }
}

@Composable
fun ExpressiveActionRow(
    icon: Painter,
    title: String,
    subtitle: String? = null,
    accent: Color? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "rowScale",
    )
    val tint = accent ?: MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = SettingsDimensions.RowHorizontalPadding,
                vertical = SettingsDimensions.RowVerticalPadding
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SettingsDimensions.RowIconTextSpacing),
        ) {
            ExpressiveRowIcon(icon = icon, tint = tint)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Icon(
                painter = painterResource(R.drawable.arrow_forward),
                contentDescription = null,
                modifier = Modifier.size(SettingsDimensions.ChevronSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            )
        }
    }
}

@Composable
fun ExpressiveSwitchRow(
    icon: Painter,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val containerColor by animateColorAsState(
        targetValue = if (checked) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            Color.Transparent
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "switchRowBg",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(containerColor)
            .clickable { onCheckedChange(!checked) },
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = SettingsDimensions.RowHorizontalPadding,
                vertical = SettingsDimensions.RowVerticalPadding
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SettingsDimensions.RowIconTextSpacing),
        ) {
            ExpressiveRowIcon(
                icon = icon,
                tint = MaterialTheme.colorScheme.primary,
                emphasized = checked,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.40f),
                ),
            )
        }
    }
}

@Composable
fun ExpressiveRowIcon(
    icon: Painter,
    tint: Color,
    emphasized: Boolean = false,
) {
    val bgAlpha by animateFloatAsState(
        targetValue = if (emphasized) 0.20f else 0.10f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "iconBgAlpha",
    )

    Surface(
        modifier = Modifier.size(SettingsDimensions.RowIconSize),
        shape = RoundedCornerShape(14.dp),
        color = tint.copy(alpha = bgAlpha),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(SettingsDimensions.RowIconInnerSize),
                tint = tint,
            )
        }
    }
}

@Composable
fun ExpressiveDivider() {
}
