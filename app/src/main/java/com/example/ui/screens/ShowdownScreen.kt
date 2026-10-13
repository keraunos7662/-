package com.example.ui.screens

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
            .padding(16.dp)
            .testTag("showdown_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Banner: Standoff Title
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CrimeSurfaceDark),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CrimeCrimson, CrimeGold, CrimeCrimson)))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Dangerous, contentDescription = null, tint = CrimeCrimsonLight, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "БАНДИТСКАЯ СТРЕЛКА",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CrimeTextPrimary,
                    letterSpacing = 1.sp
                )
            }
        }

        // Duelists Health Panel: Player VS Rival
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CrimeSurfaceCard)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Rival Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
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
                        fontWeight = FontWeight.Bold,
                        color = CrimeCrimsonLight
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                val rivalProgress = (showdown.rivalCurrentHp.toFloat() / showdown.rivalMaxHp.toFloat()).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
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

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = CrimeBorder)
                Spacer(modifier = Modifier.height(12.dp))

                // Player Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ты (Твоё здоровье)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CrimeCashGreen
                    )
                    Text(
                        text = "${showdown.playerCurrentHp}/${showdown.playerMaxHp} HP",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CrimeCashGreen
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                val playerProgress = (showdown.playerCurrentHp.toFloat() / showdown.playerMaxHp.toFloat()).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
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
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(showdown.battleLog) { message ->
                    val isWin = message.contains("ПОБЕДА")
                    val isLoss = message.contains("Поражение")
                    val isBazar = message.contains("понятиям")
                    val isShot = message.contains("Выстрел")

                    val textColor = when {
                        isWin -> CrimeGoldLight
                        isLoss -> CrimeHeatRed
                        isBazar -> CrimePoliceBlue
                        isShot -> CrimeGold
                        else -> CrimeTextPrimary
                    }

                    Text(
                        text = "• $message",
                        fontSize = 13.sp,
                        color = textColor,
                        lineHeight = 17.sp,
                        fontWeight = if (isWin || isLoss) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // Action Panel or Result Panel
        if (!showdown.isFinished) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ShowdownActionButton(
                        text = "Побазарить",
                        subtext = "Сбить спесь",
                        icon = Icons.Default.Chat,
                        color = CrimePoliceBlue,
                        onClick = { onAction(ShowdownAction.BAZAR) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("action_bazar")
                    )
                    ShowdownActionButton(
                        text = "В челюсть!",
                        subtext = "Удар в упор",
                        icon = Icons.Default.SportsMartialArts,
                        color = CrimeCrimsonLight,
                        onClick = { onAction(ShowdownAction.PUNCH) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("action_punch")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ShowdownActionButton(
                        text = "Пальнуть!",
                        subtext = "Из волыны",
                        icon = Icons.Default.FlashOn,
                        color = CrimeGold,
                        onClick = { onAction(ShowdownAction.SHOOT) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("action_shoot")
                    )
                    ShowdownActionButton(
                        text = "Братва, в бой!",
                        subtext = "Штурм толпой",
                        icon = Icons.Default.Groups,
                        color = CrimeSteel,
                        onClick = { onAction(ShowdownAction.CALL_CREW) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("action_crew")
                    )
                }
                OutlinedButton(
                    onClick = { onAction(ShowdownAction.FLEE) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("action_flee"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimeTextMuted)
                ) {
                    Icon(Icons.Default.DirectionsRun, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Дать по газам (Свалить на машине)", fontSize = 12.sp)
                }
            }
        } else {
            // Battle finished: Show outcome and exit button
            Button(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("close_showdown_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showdown.isPlayerWon) CrimeGold else CrimeCrimson
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = if (showdown.isPlayerWon) "Забрать трофеи и уехать" else "Вернуться в строй",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun ShowdownActionButton(
    text: String,
    subtext: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = CrimeSurfaceHighlight,
            contentColor = CrimeTextPrimary
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(text = subtext, fontSize = 10.sp, color = CrimeTextMuted)
            }
        }
    }
}
