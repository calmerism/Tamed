/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.tamed.music.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tamed.music.ui.theme.SfProFontFamily

import androidx.annotation.DrawableRes
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.width

// Clean Apple Music Style Menu Item
@Composable
fun AppleMenuItem(
    title: String,
    @DrawableRes icon: Int? = null,
    iconContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val isDark = isSystemInDarkTheme()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clickable(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier.size(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
        } else if (iconContent != null) {
            Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                iconContent()
            }
            Spacer(modifier = Modifier.width(16.dp))
        }

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = SfProFontFamily,
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = (-0.2).sp,
            ),
            color = if (enabled) (if (isDark) Color.White else Color(0xFF1C1C1E)) else Color.Gray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        if (trailingContent != null) {
            trailingContent()
        }
    }
}

// Clean Action Button - Tamed UI Shell Architecture
@Composable
fun NewActionButton(
    icon: @Composable () -> Unit,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(18.dp)

    val defaultBg = if (isDark) {
        Color(0xFF2C2C2E).copy(alpha = 0.6f)
    } else {
        Color(0xFFF2F2F7).copy(alpha = 0.8f)
    }
    val defaultContent = if (isDark) Color.White else Color(0xFF1C1C1E)

    val effectiveBg = if (backgroundColor != Color.Unspecified) backgroundColor else defaultBg
    val effectiveContent = if (contentColor != Color.Unspecified) contentColor else defaultContent

    val animatedBackground by animateColorAsState(
        targetValue = if (enabled) effectiveBg else effectiveBg.copy(alpha = 0.3f),
        animationSpec = tween(200),
        label = "background"
    )
    
    val animatedContent by animateColorAsState(
        targetValue = if (enabled) effectiveContent else effectiveContent.copy(alpha = 0.4f),
        animationSpec = tween(200),
        label = "content"
    )

    Surface(
        modifier = modifier
            .clip(shape)
            .clickable(enabled = enabled) { onClick() },
        color = animatedBackground,
        shape = shape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = animatedContent,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// Enhanced Menu Item - Material 3 Expressive Design
@Composable
fun NewMenuItem(
    headlineContent: @Composable () -> Unit,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.ListItem(
        headlineContent = headlineContent,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        supportingContent = supportingContent,
        modifier = modifier
            .clickable(enabled = enabled) { onClick?.invoke() }
            .padding(horizontal = 4.dp),
        tonalElevation = 0.dp
    )
}

// Enhanced Menu Section Header - Material 3 Expressive Design
@Composable
fun NewMenuSectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

// Enhanced Action Grid - Material 3 Expressive Design
@Composable
fun NewActionGrid(
    actions: List<NewAction>,
    modifier: Modifier = Modifier,
    columns: Int = 3
) {
    val rows = actions.chunked(columns)
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { action ->
                    NewActionButton(
                        icon = action.icon,
                        text = action.text,
                        onClick = action.onClick,
                        modifier = Modifier.weight(1f),
                        enabled = action.enabled,
                        backgroundColor = if (action.backgroundColor != Color.Unspecified) action.backgroundColor else Color.Unspecified,
                        contentColor = if (action.contentColor != Color.Unspecified) action.contentColor else Color.Unspecified
                    )
                }
                
                // Fill remaining space if row is not full
                repeat(columns - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// Enhanced Action Data Class
data class NewAction(
    val icon: @Composable () -> Unit,
    val text: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val backgroundColor: Color = Color.Unspecified,
    val contentColor: Color = Color.Unspecified
)

// Enhanced Menu Content - Material 3 Expressive Design
@Composable
fun NewMenuContent(
    headerContent: @Composable (() -> Unit)? = null,
    actionGrid: @Composable (() -> Unit)? = null,
    menuItems: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        headerContent?.invoke()
        
        // Action Grid
        actionGrid?.invoke()
        
        // Divider if both header and actions exist
        if (headerContent != null && actionGrid != null) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
            )
        }
        
        // Menu Items
        menuItems?.invoke()
    }
}

// Enhanced Icon Button - Material 3 Expressive Design
@Composable
fun NewIconButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val animatedBackground by animateColorAsState(
        targetValue = if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.5f),
        animationSpec = tween(200),
        label = "background"
    )
    
    val animatedContent by animateColorAsState(
        targetValue = if (enabled) contentColor else contentColor.copy(alpha = 0.5f),
        animationSpec = tween(200),
        label = "content"
    )

    Card(
        modifier = modifier
            .clickable(enabled = enabled) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = animatedBackground
        ),
        shape = CircleShape,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
    }
}

// Enhanced Menu Container - Material 3 Expressive Design
@Composable
fun NewMenuContainer(
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        content()
    }
}

@Composable
fun MenuSurfaceSection(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(22.dp)

    val surfaceColor = if (isDark) {
        Color(0xFF1C1C1E)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

    Surface(
        shape = shape,
        color = surfaceColor,
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 0.5.dp,
                color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f),
                shape = shape
            ),
    ) {
        Column(content = content)
    }
}

