package com.home.servicelinks.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.home.servicelinks.ui.theme.Slate50
import com.home.servicelinks.ui.theme.Slate800

@Composable
fun FloatingBottomBar(
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    labelLinks: String = "Links",
    labelApps: String = "Apps",
    labelSettings: String = "Settings"
) {
    val items = listOf(
        Triple(0, Icons.Default.Dns, labelLinks),
        Triple(1, Icons.Default.Apps, labelApps),
        Triple(2, Icons.Default.Settings, labelSettings)
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        // Generous dock width (20% enlarged floating dock)
        val dockMaxWidth = minOf(maxWidth - 12.dp, 400.dp)
        val horizontalPadding = 6.dp
        val verticalPadding = 6.dp
        val innerWidth = dockMaxWidth - (horizontalPadding * 2)
        val tabWidth = innerWidth / items.size

        // Physics-based spring slide animation between tabs
        val animatedIndicatorOffset by animateDpAsState(
            targetValue = tabWidth * currentTab,
            animationSpec = spring(
                dampingRatio = 0.82f,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "floatingBarTabSlide"
        )

        Surface(
            modifier = Modifier.width(dockMaxWidth),
            shape = CircleShape,
            color = if (isDark) Slate800.copy(alpha = 0.96f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
            shadowElevation = 14.dp,
            tonalElevation = 6.dp
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding)
                    .height(54.dp)
            ) {
                // Sliding Active Tab Capsule
                Box(
                    modifier = Modifier
                        .offset(x = animatedIndicatorOffset)
                        .width(tabWidth)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                )

                // Tab Buttons Row
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEach { (index, icon, label) ->
                        val selected = currentTab == index
                        val contentColor = if (selected) MaterialTheme.colorScheme.primary
                                           else if (isDark) Slate50.copy(alpha = 0.70f)
                                           else MaterialTheme.colorScheme.onSurfaceVariant

                        Box(
                            modifier = Modifier
                                .width(tabWidth)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onTabSelected(index) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = contentColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                AnimatedVisibility(
                                    visible = selected,
                                    enter = fadeIn(animationSpec = tween(180)) + expandHorizontally(
                                        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                                    ),
                                    exit = fadeOut(animationSpec = tween(100)) + shrinkHorizontally(
                                        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                                    )
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = label,
                                            color = contentColor,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
