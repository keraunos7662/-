package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.window.Dialog
import com.example.model.CardItem
import com.example.model.OchkoGameState
import com.example.ui.theme.*

@Composable
fun CasinoDialog(
    ochkoState: OchkoGameState,
    playerMoney: Long,
    onStartGame: (Long) -> Unit,
    onHitCard: () -> Unit,
    onStand: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedBet by remember { mutableStateOf(1000L) }
    val bets = listOf(500L, 1000L, 2500L, 5000L, 10000L)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("casino_dialog"),
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
                        Text(text = "🃏", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "ПОДПОЛЬНЫЙ КАТРАН",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CrimeGoldLight
                            )
                            Text(
                                text = "Игра в Очко (Русское 21)",
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

                // Green Felt Table
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1B382B), // Classic gambling green felt
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(CrimeGoldDark, CrimeBorder))
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        // Dealer Row
                        Text(
                            text = "Рука каталщика ${if (ochkoState.isFinished) "(${ochkoState.dealerScore} очков)" else ""}:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrimeGoldLight
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (ochkoState.dealerCards.isEmpty()) {
                                CardPlaceholder("🂠")
                                CardPlaceholder("🂠")
                            } else {
                                ochkoState.dealerCards.forEach { card ->
                                    PlayingCardTile(card = card)
                                }
                                if (!ochkoState.isFinished) {
                                    CardPlaceholder("🂠")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0xFF2E5E47))
                        Spacer(modifier = Modifier.height(14.dp))

                        // Player Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Твои карты (${ochkoState.playerScore} очков):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CrimeTextPrimary
                            )
                            Text(
                                text = "Ставка: ${formatNum(ochkoState.betAmount)} ₽",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CrimeCashGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (ochkoState.playerCards.isEmpty()) {
                                CardPlaceholder("🂠")
                                CardPlaceholder("🂠")
                            } else {
                                ochkoState.playerCards.forEach { card ->
                                    PlayingCardTile(card = card)
                                }
                            }
                        }

                        // Result Message if finished
                        if (ochkoState.resultMessage != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (ochkoState.isWin) CrimeCashGreen.copy(alpha = 0.2f) else CrimeHeatRed.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = ochkoState.resultMessage,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (ochkoState.isWin) CrimeCashGreen else CrimeCrimsonLight,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bet selector or in-game buttons
                if (!ochkoState.isPlaying || ochkoState.isFinished) {
                    Text(text = "Выбери ставку:", fontSize = 11.sp, color = CrimeTextMuted)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(bets) { bet ->
                            FilterChip(
                                selected = selectedBet == bet,
                                onClick = { selectedBet = bet },
                                label = { Text("${formatNum(bet)} ₽", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CrimeGoldDark,
                                    selectedLabelColor = Color.Black,
                                    containerColor = CrimeSurfaceHighlight,
                                    labelColor = CrimeTextSecondary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { onStartGame(selectedBet) },
                        enabled = playerMoney >= selectedBet,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("start_ochko_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CrimeGold),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (playerMoney >= selectedBet) "Раздать карты (${formatNum(selectedBet)} ₽)" else "Не хватает денег",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    // In-game controls: Hit or Stand
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onHitCard,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("ochko_hit_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = CrimeGold),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Ещё карту", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onStand,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("ochko_stand_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = CrimeSurfaceHighlight),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Себе! (Хватит)", color = CrimeTextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayingCardTile(card: CardItem) {
    val isRed = card.suit == "♥" || card.suit == "♦"
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFFF6F6F6),
        modifier = Modifier
            .width(42.dp)
            .height(58.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = card.name,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isRed) Color(0xFFC62828) else Color(0xFF212121)
            )
            Text(
                text = "${card.value} очк",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
            )
        }
    }
}

@Composable
private fun CardPlaceholder(symbol: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFF0F261C),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CrimeBorder, CrimeBorder))),
        modifier = Modifier
            .width(42.dp)
            .height(58.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = symbol, fontSize = 24.sp, color = CrimeTextMuted)
        }
    }
}

private fun formatNum(value: Long): String {
    return "%,d".format(value).replace(',', ' ')
}
