package com.example.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
fun NewGameDialog(
    onConfirm: (nickname: String, perk: String) -> Unit,
    onDismiss: () -> Unit
) {
    var nickname by remember { mutableStateOf("Седой") }
    var selectedPerk by remember { mutableStateOf("Деньги от цеховика") }

    val presetNames = listOf("Седой", "Кабан", "Бритва", "Фома", "Крест", "Белый")
    val perks = listOf(
        "Деньги от цеховика" to "+15 000 ₽ начального капитала",
        "Боксёр" to "+20 силы, сокрушительный удар в драках",
        "Связи в органах" to "+25 связей, легче договариваться",
        "Братва с района" to "+30 авторитета на улицах со старта"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("new_game_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CrimeSurfaceDark),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CrimeGold, CrimeBorder)))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = CrimeGold,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Новая криминальная жизнь",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CrimeTextPrimary
                    )
                }

                Text(
                    text = "Выбери воровское погоняло и прошлое, с которого начнёшь путь авторитета.",
                    fontSize = 12.sp,
                    color = CrimeTextSecondary
                )

                // Nickname Input
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("Погоняло / Кличка") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CrimeTextPrimary,
                        unfocusedTextColor = CrimeTextPrimary,
                        focusedBorderColor = CrimeGold,
                        unfocusedBorderColor = CrimeBorder,
                        focusedLabelColor = CrimeGold,
                        unfocusedLabelColor = CrimeTextMuted
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("nickname_input")
                )

                // Presets
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(presetNames) { preset ->
                        SuggestionChip(
                            onClick = { nickname = preset },
                            label = { Text(preset, fontSize = 11.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (nickname == preset) CrimeGoldDark else CrimeSurfaceHighlight,
                                labelColor = if (nickname == preset) Color.Black else CrimeTextSecondary
                            )
                        )
                    }
                }

                Text(
                    text = "Твоё прошлое (стартовый бонус):",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = CrimeTextPrimary
                )

                // Perks selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    perks.forEach { (perkName, perkDesc) ->
                        val isSelected = selectedPerk == perkName
                        Surface(
                            onClick = { selectedPerk = perkName },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) CrimeSurfaceHighlight else CrimeSurfaceCard,
                            border = if (isSelected) ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.horizontalGradient(listOf(CrimeGold, CrimeGoldDark))
                            ) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = perkName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) CrimeGoldLight else CrimeTextPrimary
                                )
                                Text(
                                    text = perkDesc,
                                    fontSize = 11.sp,
                                    color = CrimeTextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimeTextSecondary)
                    ) {
                        Text("Отмена", fontSize = 12.sp)
                    }
                    Button(
                        onClick = { onConfirm(nickname, selectedPerk) },
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("start_game_confirm_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CrimeGold)
                    ) {
                        Text("Начать игру", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
