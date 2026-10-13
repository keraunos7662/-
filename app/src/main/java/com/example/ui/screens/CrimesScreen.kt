package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.model.CrimeCategory
import com.example.model.CrimeItem
import com.example.model.ReputationRank
import com.example.ui.theme.*
import com.example.viewmodel.GameUiState

@Composable
fun CrimesScreen(
    uiState: GameUiState,
    onExecuteCrime: (CrimeItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<CrimeCategory?>(null) }

    val filteredCrimes = remember(selectedCategory, uiState.crimes) {
        if (selectedCategory == null) uiState.crimes
        else uiState.crimes.filter { it.category == selectedCategory }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("crimes_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Криминальные дела и движухи",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CrimeTextPrimary
                )
                Text(
                    text = "Выбирай дело по масти. Чем выше куш, тем злее менты и опаснее риск.",
                    fontSize = 12.sp,
                    color = CrimeTextSecondary
                )
            }
        }

        // Category Filter Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("Все дела") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CrimeGoldDark,
                            selectedLabelColor = Color.Black,
                            containerColor = CrimeSurfaceCard,
                            labelColor = CrimeTextSecondary
                        )
                    )
                }
                items(CrimeCategory.entries.toTypedArray()) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category.title) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CrimeGoldDark,
                            selectedLabelColor = Color.Black,
                            containerColor = CrimeSurfaceCard,
                            labelColor = CrimeTextSecondary
                        )
                    )
                }
            }
        }

        // Crime Cards List
        items(filteredCrimes) { crime ->
            CrimeCard(
                crime = crime,
                playerRank = uiState.rank,
                onExecute = { onExecuteCrime(crime) }
            )
        }
    }
}

@Composable
private fun CrimeCard(
    crime: CrimeItem,
    playerRank: ReputationRank,
    onExecute: () -> Unit
) {
    val isRankAllowed = playerRank.minRespect >= crime.requiredRank.minRespect

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("crime_card_${crime.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRankAllowed) CrimeSurfaceCard else CrimeSurfaceDark.copy(alpha = 0.7f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = if (isRankAllowed) Brush.horizontalGradient(listOf(CrimeBorder, CrimeSurfaceHighlight))
            else Brush.horizontalGradient(listOf(CrimeBorder.copy(alpha = 0.4f), CrimeBorder.copy(alpha = 0.2f)))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val categoryColor = when (crime.category) {
                        CrimeCategory.STREET -> CrimeSteel
                        CrimeCategory.RACKET -> CrimeCrimsonLight
                        CrimeCategory.HEIST -> CrimeGold
                        CrimeCategory.SYNDICATE -> CrimePoliceBlue
                    }
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(categoryColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = crime.category.title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = categoryColor
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isRankAllowed) CrimeSurfaceHighlight else CrimeCrimsonDark.copy(alpha = 0.4f)
                ) {
                    Text(
                        text = "От: ${crime.requiredRank.title}",
                        fontSize = 10.sp,
                        color = if (isRankAllowed) CrimeTextSecondary else CrimeCrimsonLight,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = crime.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isRankAllowed) CrimeTextPrimary else CrimeTextMuted
            )

            Text(
                text = crime.description,
                fontSize = 12.sp,
                color = CrimeTextSecondary,
                lineHeight = 16.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Metrics row: Profit, Respect, Heat, Chance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumn(
                    icon = Icons.Default.MonetizationOn,
                    label = "Куш",
                    value = "${formatNum(crime.minMoney)} - ${formatNum(crime.maxMoney)} ₽",
                    color = CrimeCashGreen
                )
                MetricColumn(
                    icon = Icons.Default.MilitaryTech,
                    label = "Авторитет",
                    value = "+${crime.respectReward}",
                    color = CrimeGold
                )
                MetricColumn(
                    icon = Icons.Default.Warning,
                    label = "Розыск",
                    value = "+${crime.heatPenalty}",
                    color = CrimeHeatRed
                )
                MetricColumn(
                    icon = Icons.Default.Percent,
                    label = "Шанс",
                    value = "~${crime.baseSuccessRate}%",
                    color = if (crime.baseSuccessRate >= 70) CrimeCashGreen else CrimeGold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action button
            if (isRankAllowed) {
                Button(
                    onClick = onExecute,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("execute_crime_${crime.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CrimeCrimson,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.SportsMartialArts, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Пойти на дело!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = CrimeSurfaceDark
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = CrimeTextMuted, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Недостаточно авторитета! Нужен ранг «${crime.requiredRank.title}»",
                            fontSize = 12.sp,
                            color = CrimeTextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricColumn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(11.dp))
            Spacer(modifier = Modifier.width(2.dp))
            Text(label, fontSize = 10.sp, color = CrimeTextMuted)
        }
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

private fun formatNum(value: Long): String {
    return "%,d".format(value).replace(',', ' ')
}
