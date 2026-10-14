package com.example.ui.dialogs

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.DailySummary
import com.example.ui.theme.*

@Composable
fun MorningNewsDialog(
    summary: DailySummary,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("morning_news_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C23)),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.verticalGradient(listOf(CrimeGold, CrimeBorder))
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Newspaper Header
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "КРИМИНАЛЬНЫЙ ВЕСТНИК",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CrimeGoldLight,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "Выпуск №${summary.day} • 1994 год • Утренний экстренный спецвыпуск",
                        fontSize = 10.sp,
                        color = CrimeTextMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = CrimeGoldDark.copy(alpha = 0.5f), thickness = 1.5.dp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Headline
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CrimeSurfaceHighlight
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "ГЛАВНАЯ НОВОСТЬ ГОРОДА:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrimeCrimsonLight
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = summary.headline,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = CrimeTextPrimary,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Financial Balance Card
                Text(
                    text = "Суточный отчёт общака",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CrimeTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CrimeSurfaceCard
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        BalanceRow(
                            label = "Доход с бизнесов и точек:",
                            value = "+${formatNum(summary.businessIncome)} ₽",
                            color = CrimeCashGreen
                        )
                        BalanceRow(
                            label = "Дань с контролируемых районов:",
                            value = "+${formatNum(summary.districtTribute)} ₽",
                            color = CrimeCashGreen
                        )
                        BalanceRow(
                            label = "Зарплаты нанятой братве:",
                            value = "-${formatNum(summary.crewSalaries)} ₽",
                            color = CrimeCrimsonLight
                        )
                        BalanceRow(
                            label = "Аренда хаты и расходы на жизнь:",
                            value = "-${formatNum(summary.livingExpenses)} ₽",
                            color = CrimeCrimsonLight
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(color = CrimeBorder)
                        Spacer(modifier = Modifier.height(6.dp))

                        BalanceRow(
                            label = "Итог за сутки:",
                            value = if (summary.netChange >= 0) "+${formatNum(summary.netChange)} ₽" else "${formatNum(summary.netChange)} ₽",
                            color = if (summary.netChange >= 0) CrimeCashGreen else CrimeHeatRed,
                            isBold = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Health & Energy Restored Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatusBadge(
                        text = "⚡ Энергия 100%",
                        color = CrimeGold
                    )
                    StatusBadge(
                        text = "❤️ Здоровье +${summary.healthRegen}%",
                        color = CrimeCashGreen
                    )
                    StatusBadge(
                        text = "🚨 Розыск -${summary.heatDecay}%",
                        color = CrimePoliceBlue
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("dismiss_morning_news"),
                    colors = ButtonDefaults.buttonColors(containerColor = CrimeGold),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Начать новый день",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun BalanceRow(
    label: String,
    value: String,
    color: Color,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (isBold) CrimeTextPrimary else CrimeTextMuted,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun StatusBadge(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

private fun formatNum(value: Long): String {
    return "%,d".format(value).replace(',', ' ')
}
