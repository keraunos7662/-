package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.ReputationRank
import com.example.model.RivalBoss
import com.example.ui.theme.*

@Composable
fun ShowdownSelectDialog(
    bosses: List<RivalBoss>,
    playerRank: ReputationRank,
    playerEnergy: Int,
    onSelectBoss: (RivalBoss) -> Unit,
    onTalkWithBoss: (RivalBoss) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .testTag("showdown_select_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CrimeSurfaceDark),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.verticalGradient(listOf(CrimeCrimson, CrimeBorder))
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "АРЕНА СТРЕЛОК",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CrimeTextPrimary
                        )
                        Text(
                            text = "Выбери криминального авторитета для разборки",
                            fontSize = 11.sp,
                            color = CrimeTextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = CrimeTextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(bosses) { boss ->
                        val isRankUnlocked = playerRank.minRespect >= boss.minRank.minRespect
                        val hasEnergy = playerEnergy >= 25

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("boss_card_${boss.id}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isRankUnlocked) CrimeSurfaceCard else CrimeSurfaceDark.copy(alpha = 0.5f)
                            ),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = if (isRankUnlocked) Brush.horizontalGradient(listOf(CrimeCrimsonDark, CrimeBorder))
                                else Brush.horizontalGradient(listOf(CrimeBorder.copy(alpha = 0.3f), CrimeBorder.copy(alpha = 0.1f)))
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(CrimeSurfaceHighlight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = boss.avatarEmoji, fontSize = 20.sp)
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = boss.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isRankUnlocked) CrimeCrimsonLight else CrimeTextMuted
                                        )
                                        Text(
                                            text = boss.title,
                                            fontSize = 11.sp,
                                            color = CrimeTextMuted
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = CrimeCrimsonDark.copy(alpha = 0.4f)
                                    ) {
                                        Text(
                                            text = boss.difficultyLabel,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CrimeGoldLight,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = boss.description,
                                    fontSize = 11.sp,
                                    color = CrimeTextSecondary,
                                    lineHeight = 15.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = "Здоровье: ${boss.maxHp} HP | Сила: ${boss.attack}", fontSize = 10.sp, color = CrimeTextMuted)
                                        Text(
                                            text = "Куш: +${formatNum(boss.rewardMoney)} ₽, +${boss.rewardRespect} авторитета",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CrimeCashGreen
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(
                                            onClick = { onTalkWithBoss(boss) },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.height(34.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimeGoldLight)
                                        ) {
                                            Text(text = "🗣️ Базар", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = { onSelectBoss(boss) },
                                            enabled = isRankUnlocked && hasEnergy,
                                            colors = ButtonDefaults.buttonColors(containerColor = CrimeCrimson),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Text(
                                                text = if (!isRankUnlocked) "Масть мала" else if (!hasEnergy) "Нет сил" else "⚔️ Стрелка",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
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
}

private fun formatNum(value: Long): String {
    return "%,d".format(value).replace(',', ' ')
}
