package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ShowdownState
import com.example.ui.theme.*
import com.example.viewmodel.ShowdownAction

@Composable
fun ShowdownScreen(
    showdown: ShowdownState,
    onAction: (ShowdownAction) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Auto-scroll log to bottom
    LaunchedEffect(showdown.battleLog.size) {
        if (showdown.battleLog.isNotEmpty()) {
            listState.animateScrollToItem(showdown.battleLog.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CrimeBgDark)
            .padding(14.dp)
            .testTag("showdown_screen"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Banner: Standoff Title & Round Counter
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CrimeSurfaceDark),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.horizontalGradient(listOf(CrimeCrimson, CrimeGold, CrimeCrimson))
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Dangerous,
                        contentDescription = null,
                        tint = CrimeCrimsonLight,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "БАНДИТСКАЯ СТРЕЛКА",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CrimeTextPrimary,
                        letterSpacing = 1.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CrimeSurfaceHighlight
                ) {
                    Text(
                        text = "РАУНД ${showdown.round}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CrimeGoldLight,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Rival Card (Boss Info + Intent)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CrimeSurfaceCard),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.horizontalGradient(listOf(CrimeCrimsonDark, CrimeBorder))
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Boss Avatar
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(CrimeCrimsonDark.copy(alpha = 0.5f))
                            .border(1.5.dp, CrimeCrimsonLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = showdown.rivalAvatarEmoji.ifEmpty { "👊" },
                            fontSize = 22.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = showdown.rivalName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrimeCrimsonLight
                        )
                        Text(
                            text = showdown.rivalTitle,
                            fontSize = 11.sp,
                            color = CrimeTextMuted
                        )
                    }

                    Text(
                        text = "${showdown.rivalCurrentHp}/${showdown.rivalMaxHp} HP",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CrimeCrimsonLight
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                val rivalProgress = (showdown.rivalCurrentHp.toFloat() / showdown.rivalMaxHp.toFloat()).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(9.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(CrimeSurfaceDark)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(rivalProgress)
                            .background(Brush.horizontalGradient(listOf(CrimeCrimsonDark, CrimeCrimsonLight)))
                    )
                }

                if (!showdown.isFinished && showdown.rivalIntent.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = CrimeSurfaceHighlight
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = CrimeGold, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Намерение врага: ${showdown.rivalIntent}",
                                fontSize = 11.sp,
                                color = CrimeGoldLight,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Player Status Card (HP + Shield + Resources)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CrimeSurfaceCard),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.horizontalGradient(listOf(CrimeCashGreen.copy(alpha = 0.5f), CrimeBorder))
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Твоё состояние",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrimeCashGreen
                        )
                        if (showdown.playerShield > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = CrimePoliceBlue.copy(alpha = 0.3f)
                            ) {
                                Text(
                                    text = "Блок: ${showdown.playerShield} HP",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CrimePoliceBlue,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${showdown.playerCurrentHp}/${showdown.playerMaxHp} HP",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CrimeCashGreen
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                val playerProgress = (showdown.playerCurrentHp.toFloat() / showdown.playerMaxHp.toFloat()).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(9.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(CrimeSurfaceDark)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(playerProgress)
                            .background(Brush.horizontalGradient(listOf(CrimeCashGreen.copy(alpha = 0.6f), CrimeCashGreen)))
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Ammo, First Aid, Adrenaline Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ResourcePill(
                        label = "Патроны",
                        value = "${showdown.ammoCount}",
                        icon = Icons.Default.FlashOn,
                        color = CrimeGold
                    )
                    ResourcePill(
                        label = "Аптечки",
                        value = "${showdown.firstAidKits}",
                        icon = Icons.Default.MedicalServices,
                        color = CrimeCrimsonLight
                    )
                    ResourcePill(
                        label = "Ярость",
                        value = "${showdown.rageMeter}%",
                        icon = Icons.Default.ElectricBolt,
                        color = if (showdown.rageMeter >= 100) CrimeGoldLight else CrimeSteel
                    )
                }
            }
        }

        // Live Battle Log
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CrimeSurfaceDark),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CrimeBorder, CrimeBorder)))
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(showdown.battleLog) { message ->
                    val isWin = message.contains("ПОБЕДА")
                    val isLoss = message.contains("Поражение")
                    val isCrit = message.contains("КРИТИЧЕСКИЙ") || message.contains("ЯРОСТНЫЙ")
                    val isShot = message.contains("выстрел") || message.contains("волыны")
                    val isBazar = message.contains("понятиям")

                    val textColor = when {
                        isWin -> CrimeGoldLight
                        isLoss -> CrimeHeatRed
                        isCrit -> CrimeGold
                        isShot -> CrimeGoldLight
                        isBazar -> CrimePoliceBlue
                        else -> CrimeTextPrimary
                    }

                    Text(
                        text = "• $message",
                        fontSize = 12.sp,
                        color = textColor,
                        lineHeight = 16.sp,
                        fontWeight = if (isWin || isLoss || isCrit) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // Actions Grid or Battle Outcome
        if (!showdown.isFinished) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Row 1: Punch & Shoot
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TacticalCombatButton(
                        text = "В челюсть!",
                        subtext = "Удар в упор (+25% ярости)",
                        icon = Icons.Default.SportsMartialArts,
                        color = CrimeCrimsonLight,
                        onClick = { onAction(ShowdownAction.PUNCH) },
                        modifier = Modifier.weight(1f).testTag("action_punch")
                    )
                    TacticalCombatButton(
                        text = "Пальнуть!",
                        subtext = "Волына (${showdown.ammoCount} патр.)",
                        icon = Icons.Default.FlashOn,
                        color = CrimeGold,
                        onClick = { onAction(ShowdownAction.SHOOT) },
                        modifier = Modifier.weight(1f).testTag("action_shoot")
                    )
                }

                // Row 2: Guard & Bazar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TacticalCombatButton(
                        text = "В блок!",
                        subtext = "Занять укрытие (+щит)",
                        icon = Icons.Default.Shield,
                        color = CrimePoliceBlue,
                        onClick = { onAction(ShowdownAction.GUARD) },
                        modifier = Modifier.weight(1f).testTag("action_guard")
                    )
                    TacticalCombatButton(
                        text = "Побазарить",
                        subtext = "Сбить спесь врага",
                        icon = Icons.Default.Chat,
                        color = CrimeSteel,
                        onClick = { onAction(ShowdownAction.BAZAR) },
                        modifier = Modifier.weight(1f).testTag("action_bazar")
                    )
                }

                // Row 3: First Aid & Call Crew
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TacticalCombatButton(
                        text = "Аптечка",
                        subtext = "+45 HP (${showdown.firstAidKits} ост.)",
                        icon = Icons.Default.MedicalServices,
                        color = CrimeCrimsonLight,
                        onClick = { onAction(ShowdownAction.FIRST_AID) },
                        modifier = Modifier.weight(1f).testTag("action_heal")
                    )
                    TacticalCombatButton(
                        text = "Братва, в бой!",
                        subtext = "Штурм бойцами",
                        icon = Icons.Default.Groups,
                        color = CrimeGold,
                        onClick = { onAction(ShowdownAction.CALL_CREW) },
                        modifier = Modifier.weight(1f).testTag("action_crew")
                    )
                }

                // Row 4: Super Rage Finish or Flee
                if (showdown.rageMeter >= 100) {
                    Button(
                        onClick = { onAction(ShowdownAction.RAGE_KO) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("action_rage_ko"),
                        colors = ButtonDefaults.buttonColors(containerColor = CrimeGold),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "💥 ЯРОСТНЫЙ НОКАУТ! (100% АДРЕНАЛИН)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = { onAction(ShowdownAction.FLEE) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .testTag("action_flee"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimeTextMuted)
                    ) {
                        Icon(Icons.Default.DirectionsRun, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Дать по газам (Свалить на машине)", fontSize = 11.sp)
                    }
                }
            }
        } else {
            // Battle finished: Outcome card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (showdown.isPlayerWon) CrimeGoldDark else CrimeCrimsonDark
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (showdown.isPlayerWon) "🏆 СТРЕЛКА ВЫИГРАНА!" else "💀 ТЕБЯ ВЫРУБИЛИ...",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (showdown.isPlayerWon) Color.Black else Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (showdown.isPlayerWon)
                            "Куш: +${showdown.rewardMoney} ₽ | Авторитет: +${showdown.rewardRespect}"
                        else
                            "Ты доставлен в больницу. Счёт за лечение оплачен.",
                        fontSize = 12.sp,
                        color = if (showdown.isPlayerWon) Color.Black.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onClose,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("close_showdown_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showdown.isPlayerWon) Color.Black else CrimeSurfaceCard
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (showdown.isPlayerWon) "Забрать трофеи и уехать" else "Вернуться в строй",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResourcePill(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = CrimeSurfaceHighlight
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "$label: ", fontSize = 10.sp, color = CrimeTextMuted)
            Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun TacticalCombatButton(
    text: String,
    subtext: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = CrimeSurfaceHighlight,
            contentColor = CrimeTextPrimary
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1
                )
                Text(
                    text = subtext,
                    fontSize = 9.sp,
                    color = CrimeTextMuted,
                    maxLines = 1
                )
            }
        }
    }
}
