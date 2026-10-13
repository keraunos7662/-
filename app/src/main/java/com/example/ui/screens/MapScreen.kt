package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.model.District
import com.example.ui.theme.*
import com.example.viewmodel.GameUiState

@Composable
fun MapScreen(
    uiState: GameUiState,
    onExpandDistrict: (District) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalTribute = uiState.districts.sumOf { (it.dailyTribute * it.controlPercent) / 100 }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("map_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Карта влияния в городе",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CrimeTextPrimary
                )
                Text(
                    text = "Отжимай районы у конкурирующих группировок, собирай дань и держи весь город под своим контролем.",
                    fontSize = 12.sp,
                    color = CrimeTextSecondary
                )
            }
        }

        // Overview Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CrimeSurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CrimeGold, CrimeBorder)))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Общая дань с города:", fontSize = 12.sp, color = CrimeTextMuted)
                        Text(text = "+${formatNum(totalTribute)} ₽/сутки", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CrimeCashGreen)
                    }
                    Surface(shape = RoundedCornerShape(6.dp), color = CrimeSurfaceHighlight) {
                        Text(
                            text = "5 районов города",
                            fontSize = 11.sp,
                            color = CrimeGoldLight,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // District Cards
        items(uiState.districts) { district ->
            DistrictCard(
                district = district,
                onAttackRivals = { onExpandDistrict(district) }
            )
        }
    }
}

@Composable
private fun DistrictCard(
    district: District,
    onAttackRivals: () -> Unit
) {
    val progress = (district.controlPercent / 100f).coerceIn(0f, 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("district_card_${district.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CrimeSurfaceCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = if (district.controlPercent >= 50) Brush.horizontalGradient(listOf(CrimeGoldDark, CrimeBorder))
            else Brush.horizontalGradient(listOf(CrimeBorder, CrimeSurfaceHighlight))
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
                Text(
                    text = district.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = CrimeTextPrimary
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = CrimeCrimsonDark.copy(alpha = 0.4f)
                ) {
                    Text(
                        text = "ОПГ: «${district.rivalGang}»",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CrimeCrimsonLight,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = district.description,
                fontSize = 12.sp,
                color = CrimeTextSecondary,
                lineHeight = 16.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Control progress bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Контроль твоего авторитета:", fontSize = 11.sp, color = CrimeTextMuted)
                Text(
                    text = "${district.controlPercent}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (district.controlPercent >= 50) CrimeGoldLight else CrimeTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(CrimeSurfaceDark)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(
                            Brush.horizontalGradient(listOf(CrimeCrimsonDark, CrimeGold))
                        )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Макс. дань района:", fontSize = 10.sp, color = CrimeTextMuted)
                    Text(text = "${formatNum(district.dailyTribute)} ₽/сутки", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CrimeCashGreen)
                }

                Button(
                    onClick = onAttackRivals,
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("attack_district_${district.id}"),
                    colors = ButtonDefaults.buttonColors(containerColor = CrimeCrimson),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.SportsMartialArts, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Забить стрелку!", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun formatNum(value: Long): String {
    return "%,d".format(value).replace(',', ' ')
}
