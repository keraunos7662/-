package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import com.example.ui.theme.*
import com.example.viewmodel.GameUiState

@Composable
fun JailScreen(
    uiState: GameUiState,
    onAnswerRiddle: (Int) -> Unit,
    onBribeWarden: () -> Unit,
    onHireLawyer: () -> Unit,
    onPassDay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CrimeBgDark)
            .padding(16.dp)
            .testTag("jail_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Prison Header Card with Iron Bars Motif
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CrimeSurfaceDark),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CrimeSteel, CrimeBorder)))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = CrimeCrimsonLight,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "СИЗО №1 • НА ТЮРЕМНЫХ НАРАХ",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CrimeTextPrimary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Осталось отсидеть: ${uiState.jailDaysLeft} дн.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CrimeGold
                )
                Text(
                    text = "Держись достойно, соблюдай воровские понятия и не поддавайся на провокации вертухаев.",
                    fontSize = 12.sp,
                    color = CrimeTextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Active Prison Riddle Card
        val riddle = uiState.currentRiddle
        if (riddle != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CrimeSurfaceCard),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CrimeGoldDark, CrimeBorder)))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.HelpOutline, contentDescription = null, tint = CrimeGold, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Проверка по понятиям",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrimeGold
                        )
                    }

                    Text(
                        text = riddle.question,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CrimeTextPrimary,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Riddle Options
                    riddle.options.forEachIndexed { index, optionText ->
                        OutlinedButton(
                            onClick = { onAnswerRiddle(index) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("riddle_option_$index"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = CrimeSurfaceHighlight,
                                contentColor = CrimeTextPrimary
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.horizontalGradient(listOf(CrimeBorder, CrimeSurfaceHighlight))
                            ),
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Text(
                                text = "${index + 1}. $optionText",
                                fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        } else if (uiState.riddleFeedback != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CrimeSurfaceCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = CrimeGold, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Сходняк решил:",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = CrimeGoldLight
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.riddleFeedback,
                        fontSize = 13.sp,
                        color = CrimeTextPrimary,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }

        // Ways Out of Jail
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onBribeWarden,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("bribe_warden_button"),
                colors = ButtonDefaults.buttonColors(containerColor = CrimeGoldDark, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.MonetizationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Подкупить начальника СИЗО (35 000 ₽)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            FilledTonalButton(
                onClick = onHireLawyer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("hire_lawyer_button"),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = CrimeSurfaceHighlight, contentColor = CrimePoliceBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Задействовать адвоката (развалить дело)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = onPassDay,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .testTag("jail_pass_day_button"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimeTextSecondary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Hotel, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Переночевать на нарах (Пропустить день)", fontSize = 12.sp)
            }
        }
    }
}
