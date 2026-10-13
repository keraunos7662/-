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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CrewMember
import com.example.ui.theme.*
import com.example.viewmodel.GameUiState

@Composable
fun CrewScreen(
    uiState: GameUiState,
    onHireCrew: (CrewMember) -> Unit,
    onFireCrew: (CrewMember) -> Unit,
    modifier: Modifier = Modifier
) {
    val hiredCount = uiState.crew.count { it.isHired }
    val totalSalaries = uiState.crew.filter { it.isHired }.sumOf { it.dailySalary }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("crew_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Бригада и криминальные связи",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CrimeTextPrimary
                )
                Text(
                    text = "Свои люди решают всё: силовики прикроют на стрелках, а адвокат и майор спасут от тюрьмы.",
                    fontSize = 12.sp,
                    color = CrimeTextSecondary
                )
            }
        }

        // Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CrimeSurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CrimeBorder, CrimeSurfaceHighlight)))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "В твоей бригаде:", fontSize = 12.sp, color = CrimeTextMuted)
                        Text(text = "$hiredCount из ${uiState.crew.size} чел.", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CrimeGold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Суточное жалование:", fontSize = 12.sp, color = CrimeTextMuted)
                        Text(text = "-${formatNum(totalSalaries)} ₽/сутки", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CrimeCrimsonLight)
                    }
                }
            }
        }

        // Crew Members List
        items(uiState.crew) { member ->
            CrewCard(
                member = member,
                playerMoney = uiState.money,
                onHire = { onHireCrew(member) },
                onFire = { onFireCrew(member) }
            )
        }
    }
}

@Composable
private fun CrewCard(
    member: CrewMember,
    playerMoney: Long,
    onHire: () -> Unit,
    onFire: () -> Unit
) {
    val canAfford = playerMoney >= member.hireCost

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("crew_card_${member.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CrimeSurfaceCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = if (member.isHired) Brush.horizontalGradient(listOf(CrimeGoldDark, CrimeBorder))
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
                Column {
                    Text(
                        text = member.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CrimeTextPrimary
                    )
                    Text(
                        text = member.role,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CrimeGold
                    )
                }

                if (member.isHired) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = CrimeCashGreen.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "В СТРОЮ",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrimeCashGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = member.description,
                fontSize = 12.sp,
                color = CrimeTextSecondary,
                lineHeight = 16.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Perks Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (member.combatBonus > 0) {
                    Surface(shape = RoundedCornerShape(4.dp), color = CrimeCrimson.copy(alpha = 0.2f)) {
                        Text("Урон на стрелках +${member.combatBonus}", color = CrimeCrimsonLight, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                    }
                }
                if (member.incomePercentBonus > 0) {
                    Surface(shape = RoundedCornerShape(4.dp), color = CrimeCashGreen.copy(alpha = 0.2f)) {
                        Text("Куш от дел +${member.incomePercentBonus}%", color = CrimeCashGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                    }
                }
                if (member.heatReductionBonus > 0) {
                    Surface(shape = RoundedCornerShape(4.dp), color = CrimePoliceBlue.copy(alpha = 0.2f)) {
                        Text("Снижение розыска -${member.heatReductionBonus}", color = CrimePoliceBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Row
            if (!member.isHired) {
                Button(
                    onClick = onHire,
                    enabled = canAfford,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("hire_crew_${member.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canAfford) CrimeGoldDark else CrimeSurfaceHighlight,
                        contentColor = if (canAfford) Color.Black else CrimeTextMuted
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Нанять за ${formatNum(member.hireCost)} ₽ (${formatNum(member.dailySalary)} ₽/день)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onFire,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .testTag("fire_crew_${member.id}"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimeCrimsonLight),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(CrimeBorder, CrimeBorder))),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Распустить (Освободить от обязанностей)", fontSize = 11.sp)
                }
            }
        }
    }
}

private fun formatNum(value: Long): String {
    return "%,d".format(value).replace(',', ' ')
}
