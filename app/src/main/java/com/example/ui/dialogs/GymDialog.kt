package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

@Composable
fun GymDialog(
    currentStrength: Int,
    boxingSkill: Int,
    shootingSkill: Int,
    energy: Int,
    onTrain: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("gym_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CrimeSurfaceDark),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.verticalGradient(listOf(CrimeCrimson, CrimeBorder))
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🏋️", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "ПОДПОЛЬНАЯ КАЧАЛКА",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CrimeTextPrimary
                            )
                            Text(
                                text = "Тягай железо и качай боевые навыки",
                                fontSize = 11.sp,
                                color = CrimeTextSecondary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = CrimeTextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Energy Status Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CrimeSurfaceHighlight
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Запас сил на сегодня:", fontSize = 11.sp, color = CrimeTextMuted)
                        Text(text = "⚡ $energy% энергии", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CrimeGoldLight)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Workout Option 1: Heavy Iron
                WorkoutOptionCard(
                    title = "Жим штанги и чугунные блины",
                    description = "Тягать советские блины до седьмого пота. Увеличивает Силу на +3..5.",
                    statsInfo = "Текущая Сила: $currentStrength",
                    energyCost = 25,
                    canAfford = energy >= 25,
                    icon = Icons.Default.FitnessCenter,
                    color = CrimeCrimsonLight,
                    onStart = { onTrain("strength") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Workout Option 2: Punching Bag Sparring
                WorkoutOptionCard(
                    title = "Боксёрский мешок и спарринг",
                    description = "Отработка хуков и апперкотов. Повышает навык «Боксёрская школа» на +1.",
                    statsInfo = "Уровень бокса: $boxingSkill",
                    energyCost = 25,
                    canAfford = energy >= 25,
                    icon = Icons.Default.SportsMartialArts,
                    color = CrimeGold,
                    onStart = { onTrain("boxing") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Workout Option 3: Shooting Range
                WorkoutOptionCard(
                    title = "Стрельбище на заброшенном заводе",
                    description = "Стрельба по банкам и мишеням. Повышает навык «Хладнокровный стрелок» на +1.",
                    statsInfo = "Уровень стрельбы: $shootingSkill",
                    energyCost = 25,
                    canAfford = energy >= 25,
                    icon = Icons.Default.FlashOn,
                    color = CrimePoliceBlue,
                    onStart = { onTrain("shooting") }
                )
            }
        }
    }
}

@Composable
private fun WorkoutOptionCard(
    title: String,
    description: String,
    statsInfo: String,
    energyCost: Int,
    canAfford: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onStart: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CrimeSurfaceCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(color.copy(alpha = 0.4f), CrimeBorder)))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CrimeTextPrimary)
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(text = description, fontSize = 11.sp, color = CrimeTextSecondary, lineHeight = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = statsInfo, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
                Button(
                    onClick = onStart,
                    enabled = canAfford,
                    colors = ButtonDefaults.buttonColors(containerColor = color),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(
                        text = if (canAfford) "Потренироваться (-$energyCost%)" else "Нет сил",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
    }
}
