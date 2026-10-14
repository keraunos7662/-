package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
fun SafeDialog(
    cashMoney: Long,
    safeMoney: Long,
    onDeposit: (Long) -> Unit,
    onWithdraw: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("safe_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CrimeSurfaceDark),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.verticalGradient(listOf(CrimeGold, CrimeBorder))
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
                        Text("💼", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "БАНДИТСКИЙ ОБЩАК & СЕЙФ",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CrimeGoldLight
                            )
                            Text(
                                text = "Неприкосновенный запас братвы",
                                fontSize = 11.sp,
                                color = CrimeTextMuted
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = CrimeTextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Safe Balance Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CrimeSurfaceHighlight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Деньги на руках (кэш):", fontSize = 11.sp, color = CrimeTextMuted)
                            Text("${formatNum(cashMoney)} ₽", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CrimeCashGreen)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("В сейфе общака:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CrimeTextPrimary)
                            Text("${formatNum(safeMoney)} ₽", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = CrimeGoldLight)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "ℹ️ Деньги в сейфе защищены от конфискации ментами и больничных счетов при поражениях, а также приносят +2% дивидендов каждые сутки.",
                            fontSize = 10.sp,
                            color = CrimeTextSecondary,
                            lineHeight = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Операции с общаком:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CrimeTextPrimary)
                Spacer(modifier = Modifier.height(8.dp))

                // Deposit Quick Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { onDeposit(cashMoney / 2) },
                        enabled = cashMoney >= 1000,
                        modifier = Modifier.weight(1f).height(38.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CrimeCashGreen),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text("Положить 50%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                    Button(
                        onClick = { onDeposit(cashMoney) },
                        enabled = cashMoney > 0,
                        modifier = Modifier.weight(1f).height(38.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CrimeGold),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text("Положить всё", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Withdraw Quick Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { onWithdraw(safeMoney / 2) },
                        enabled = safeMoney >= 1000,
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text("Забрать 50%", fontSize = 11.sp, color = CrimeTextPrimary)
                    }
                    OutlinedButton(
                        onClick = { onWithdraw(safeMoney) },
                        enabled = safeMoney > 0,
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text("Забрать всё", fontSize = 11.sp, color = CrimeTextPrimary)
                    }
                }
            }
        }
    }
}

private fun formatNum(value: Long): String {
    return "%,d".format(value).replace(',', ' ')
}
