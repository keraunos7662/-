package com.example.ui.screens

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
import com.example.model.AssetCategory
import com.example.ui.theme.*
import com.example.viewmodel.GameUiState

@Composable
fun DashboardScreen(
    uiState: GameUiState,
    onNavigateToCrimes: () -> Unit,
    onNavigateToAssets: () -> Unit,
    onNavigateToCrew: () -> Unit,
    onNavigateToDistricts: () -> Unit,
    onNewGameClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val equippedWeapon = uiState.assets.firstOrNull { it.isEquipped && it.category == AssetCategory.WEAPON }
    val equippedCar = uiState.assets.firstOrNull { it.isEquipped && it.category == AssetCategory.VEHICLE }
    val equippedClothing = uiState.assets.firstOrNull { it.isEquipped && it.category == AssetCategory.CLOTHING }

    val dailyBizIncome = uiState.assets.filter { it.isPurchased && it.category == AssetCategory.BUSINESS }.sumOf { it.dailyIncome }
    val dailyDistIncome = uiState.districts.sumOf { (it.dailyTribute * it.controlPercent) / 100 }
    val dailySalaries = uiState.crew.filter { it.isHired }.sumOf { it.dailySalary }
    val netDaily = dailyBizIncome + dailyDistIncome - dailySalaries

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Character Overview Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("character_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CrimeSurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CrimeGoldDark, CrimeBorder)))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Vor Star Avatar Badge
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(CrimeGoldDark, CrimeSurfaceDark)))
                                .border(2.dp, CrimeGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stars,
                                contentDescription = null,
                                tint = CrimeGoldLight,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.nickname,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CrimeTextPrimary
                            )
                            Text(
                                text = "Масть: ${uiState.rank.title}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CrimeGold
                            )
                            Text(
                                text = uiState.rank.perksDescription,
                                fontSize = 11.sp,
                                color = CrimeTextSecondary,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Основные характеристики главного героя: Сила, Авторитет, Деньги, Здоровье
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CrimeSurfaceHighlight)
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HeroStatItem(
                            title = "Сила",
                            value = "${uiState.strength}",
                            icon = Icons.Default.FitnessCenter,
                            color = CrimeCrimsonLight
                        )
                        HeroStatItem(
                            title = "Авторитет",
                            value = "${uiState.respect}",
                            icon = Icons.Default.MilitaryTech,
                            color = CrimeGold
                        )
                        HeroStatItem(
                            title = "Деньги",
                            value = "${formatNum(uiState.money)} ₽",
                            icon = Icons.Default.MonetizationOn,
                            color = CrimeCashGreen
                        )
                        HeroStatItem(
                            title = "Здоровье",
                            value = "${uiState.health}%",
                            icon = Icons.Default.Favorite,
                            color = CrimeCrimsonLight
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = CrimeBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Equipment Badges Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        EquipmentMiniBadge(
                            title = "Ствол",
                            name = equippedWeapon?.name ?: "Кулаки",
                            icon = Icons.Default.FlashOn,
                            color = CrimeCrimsonLight,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        EquipmentMiniBadge(
                            title = "Колёса",
                            name = equippedCar?.name ?: "Пешком",
                            icon = Icons.Default.DirectionsCar,
                            color = CrimeGold,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        EquipmentMiniBadge(
                            title = "Шмот",
                            name = equippedClothing?.name ?: "Треники",
                            icon = Icons.Default.Checkroom,
                            color = CrimePoliceBlue,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Daily Financial Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CrimeSurfaceCard),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(CrimeBorder, CrimeSurfaceHighlight)))
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
                            text = "Суточный баланс общака",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrimeTextPrimary
                        )
                        Text(
                            text = if (netDaily >= 0) "+${formatNum(netDaily)} ₽/сутки" else "${formatNum(netDaily)} ₽/сутки",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (netDaily >= 0) CrimeCashGreen else CrimeHeatRed
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Прибыль от точек и рэкета:", fontSize = 12.sp, color = CrimeTextMuted)
                        Text(text = "+${formatNum(dailyBizIncome + dailyDistIncome)} ₽", fontSize = 12.sp, color = CrimeCashGreen)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Выплаты братве:", fontSize = 12.sp, color = CrimeTextMuted)
                        Text(text = "-${formatNum(dailySalaries)} ₽", fontSize = 12.sp, color = CrimeCrimsonLight)
                    }
                }
            }
        }

        // Quick Hub Navigation Cards
        item {
            Text(
                text = "Криминальные направления",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = CrimeTextPrimary
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickHubTile(
                    title = "Дела & Мутки",
                    subtitle = "Гоп-стоп, рэкет, налёты",
                    icon = Icons.Default.Gavel,
                    color = CrimeCrimson,
                    onClick = onNavigateToCrimes,
                    modifier = Modifier.weight(1f)
                )
                QuickHubTile(
                    title = "Районы & Стрелки",
                    subtitle = "Контроль города",
                    icon = Icons.Default.Place,
                    color = CrimeGold,
                    onClick = onNavigateToDistricts,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickHubTile(
                    title = "Арсенал & Тачки",
                    subtitle = "Оружие, бумеры, бизнесы",
                    icon = Icons.Default.DirectionsCar,
                    color = CrimePoliceBlue,
                    onClick = onNavigateToAssets,
                    modifier = Modifier.weight(1f)
                )
                QuickHubTile(
                    title = "Братва & Связи",
                    subtitle = "Бойцы, адвокат, менты",
                    icon = Icons.Default.Groups,
                    color = CrimeSteel,
                    onClick = onNavigateToCrew,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Stats Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CrimeSurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CrimeBorder, CrimeBorder)))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Воровское досье",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CrimeTextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem(label = "Дел совершено", value = "${uiState.stats.totalCrimes}")
                        StatItem(label = "Успешных", value = "${uiState.stats.successfulCrimes}")
                        StatItem(label = "Стрелок выиграно", value = "${uiState.stats.showdownsWon}")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem(label = "Заработано всего", value = "${formatNum(uiState.stats.totalEarned)} ₽")
                        StatItem(label = "Отсидок в СИЗО", value = "${uiState.stats.timesArrested}")
                        StatItem(label = "Дней прожито", value = "${uiState.stats.daysSurvived}")
                    }
                }
            }
        }

        // History Log Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Хроника событий",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = CrimeTextPrimary
                )
                TextButton(onClick = onNewGameClick) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = CrimeTextMuted)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Новая игра", fontSize = 12.sp, color = CrimeTextMuted)
                }
            }
        }

        // History items
        items(uiState.history.reversed().take(15)) { entry ->
            HistoryItemCard(entry = entry)
        }
    }
}

@Composable
private fun EquipmentMiniBadge(
    title: String,
    name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = CrimeSurfaceHighlight
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(title, fontSize = 10.sp, color = CrimeTextMuted)
            }
            Text(
                name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CrimeTextPrimary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun QuickHubTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(86.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CrimeSurfaceCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(color.copy(alpha = 0.5f), CrimeBorder)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = CrimeTextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = CrimeTextMuted,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 11.sp, color = CrimeTextMuted)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CrimeGoldLight)
    }
}

@Composable
private fun HistoryItemCard(entry: com.example.model.GameHistoryEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CrimeSurfaceCard)
            .padding(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (entry.isPositive) CrimeCashGreen.copy(alpha = 0.2f) else CrimeHeatRed.copy(alpha = 0.2f)
        ) {
            Text(
                text = "День ${entry.day}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (entry.isPositive) CrimeCashGreen else CrimeCrimsonLight,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = entry.text,
            fontSize = 12.sp,
            color = CrimeTextSecondary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HeroStatItem(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text(title, fontSize = 10.sp, color = CrimeTextMuted)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

private fun formatNum(value: Long): String {
    return "%,d".format(value).replace(',', ' ')
}
