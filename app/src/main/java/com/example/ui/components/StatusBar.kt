package com.example.ui.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ReputationRank
import com.example.ui.theme.*
import com.example.viewmodel.GameUiState

@Composable
fun GameStatusBar(
    uiState: GameUiState,
    onNextDayClick: () -> Unit,
    onHealClick: () -> Unit,
    onBribePoliceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Siren animation if police heat is high
    val infiniteTransition = rememberInfiniteTransition(label = "police_siren")
    val sirenColor by infiniteTransition.animateColor(
        initialValue = CrimePoliceBlue,
        targetValue = CrimeHeatRed,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "siren_pulse"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("status_bar_card"),
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CrimeSurfaceDark
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = if (uiState.policeHeat >= 75) Brush.horizontalGradient(
                listOf(sirenColor, CrimeGold, sirenColor)
            ) else Brush.horizontalGradient(
                listOf(CrimeBorder, CrimeSurfaceHighlight, CrimeBorder)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Row 1: Nickname, Rank, Day and Next Day Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = CrimeGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = uiState.nickname,
                            color = CrimeTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = CrimeCrimsonDark.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = uiState.rank.title,
                                color = CrimeGoldLight,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "День ${uiState.day} • Энергия: ⚡ ${uiState.energy}%",
                        color = CrimeTextMuted,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Next Day Button
                Button(
                    onClick = onNextDayClick,
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("next_day_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CrimeGoldDark,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Следующий день",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "След. день",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Money and Quick Treatment Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = "Деньги",
                        tint = CrimeCashGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${formatNum(uiState.money)} ₽",
                        color = CrimeCashGreen,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Hospital button
                    FilledTonalButton(
                        onClick = onHealClick,
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("heal_button"),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = CrimeSurfaceHighlight,
                            contentColor = CrimeCrimsonLight
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.LocalHospital, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Больничка", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    // Bribe police button
                    FilledTonalButton(
                        onClick = onBribePoliceClick,
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("bribe_button"),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (uiState.policeHeat >= 70) CrimeHeatRed.copy(alpha = 0.2f) else CrimeSurfaceHighlight,
                            contentColor = if (uiState.policeHeat >= 70) CrimeHeatRed else CrimeTextSecondary
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.LocalPolice, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Отмазаться", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 3: Progress Bars for Health, Energy, Heat, and Respect
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Health bar
                StatBar(
                    label = "Здоровье",
                    current = uiState.health,
                    max = 100,
                    color = CrimeCrimsonLight,
                    icon = Icons.Default.Favorite,
                    modifier = Modifier.weight(1f)
                )

                // Energy bar
                StatBar(
                    label = "Энергия",
                    current = uiState.energy,
                    max = 100,
                    color = CrimeGold,
                    icon = Icons.Default.ElectricBolt,
                    modifier = Modifier.weight(1f)
                )

                // Heat bar
                StatBar(
                    label = "Розыск",
                    current = uiState.policeHeat,
                    max = 100,
                    color = if (uiState.policeHeat >= 70) sirenColor else CrimePoliceBlue,
                    icon = Icons.Default.Warning,
                    modifier = Modifier.weight(1f)
                )

                // Respect
                StatBar(
                    label = "Авторитет",
                    current = uiState.respect % 500,
                    max = 500,
                    color = CrimeGoldLight,
                    icon = Icons.Default.MilitaryTech,
                    displayValue = "${uiState.respect}",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatBar(
    label: String,
    current: Int,
    max: Int,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    displayValue: String? = null,
    modifier: Modifier = Modifier
) {
    val progress = (current.toFloat() / max.toFloat()).coerceIn(0f, 1f)
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = label,
                    color = CrimeTextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = displayValue ?: "$current%",
                color = color,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(CrimeSurfaceCard)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}

private fun formatNum(value: Long): String {
    return "%,d".format(value).replace(',', ' ')
}
