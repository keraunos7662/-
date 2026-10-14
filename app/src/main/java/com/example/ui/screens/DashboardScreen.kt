package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AssetCategory
import com.example.model.GameHistoryEntry
import com.example.ui.theme.*
import com.example.viewmodel.GameUiState

@Composable
fun DashboardScreen(
    uiState: GameUiState,
    onNavigateToCrimes: () -> Unit,
    onNavigateToAssets: () -> Unit,
    onNavigateToCrew: () -> Unit,
    onNavigateToDistricts: () -> Unit,
    onOpenBossArena: () -> Unit,
    onOpenBossChat: () -> Unit,
    onOpenCasino: () -> Unit,
    onOpenGym: () -> Unit,
    onOpenSafe: () -> Unit,
    onOpenSkills: () -> Unit,
    onNewGameClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val equippedWeapon = uiState.assets.firstOrNull { it.isEquipped && it.category == AssetCategory.WEAPON }
    val equippedCar = uiState.assets.firstOrNull { it.isEquipped && it.category == AssetCategory.VEHICLE }
    val equippedClothing = uiState.assets.firstOrNull { it.isEquipped && it.category == AssetCategory.CLOTHING }

    val dailyBizIncome = uiState.assets.filter { it.isPurchased && it.category == AssetCategory.BUSINESS }.sumOf { it.dailyIncome }
    val dailyDistIncome = uiState.districts.sumOf { (it.dailyTribute * it.controlPercent) / 100 }
    val dailySalaries = uiState.crew.filter { it.isHired }.sumOf { it.dailySalary }
    val baseRent = (400L + (uiState.day * 10L)).coerceAtMost(2500L)
    val netDaily = dailyBizIncome + dailyDistIncome - dailySalaries - baseRent

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Character Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("character_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CrimeSurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(CrimeGoldDark, CrimeBorder))
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Vor Star Avatar Badge
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(CrimeGoldDark, CrimeSurfaceDark)))
                                .border(2.dp, CrimeGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stars,
                                contentDescription = null,
                                tint = CrimeGoldLight,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = uiState.nickname,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CrimeTextPrimary
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = CrimeSurfaceHighlight
                                ) {
                                    Text(
                                        text = "⚡ ${uiState.energy}% сил",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (uiState.energy > 30) CrimeGoldLight else CrimeHeatRed,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
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
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 4 Core Hero Stats: Сила, Авторитет, Деньги, Здоровье (Fixed layout with NO disappearing text)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CrimeSurfaceHighlight)
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HeroStatItem(
                            title = "Сила",
                            value = "${uiState.strength}",
                            icon = Icons.Default.FitnessCenter,
                            color = CrimeCrimsonLight,
                            modifier = Modifier.weight(1f)
                        )
                        HeroStatItem(
                            title = "Авторитет",
                            value = "${uiState.respect}",
                            icon = Icons.Default.MilitaryTech,
                            color = CrimeGold,
                            modifier = Modifier.weight(1f)
                        )
                        HeroStatItem(
                            title = "Деньги",
                            value = "${formatNumShort(uiState.money)} ₽",
                            icon = Icons.Default.MonetizationOn,
                            color = CrimeCashGreen,
                            modifier = Modifier.weight(1f)
                        )
                        HeroStatItem(
                            title = "Здоровье",
                            value = "${uiState.health}%",
                            icon = Icons.Default.Favorite,
                            color = CrimeCrimsonLight,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = CrimeBorder)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Equipment Badges Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        EquipmentMiniBadge(
                            title = "Ствол",
                            name = equippedWeapon?.name ?: "Кулаки",
                            icon = Icons.Default.FlashOn,
                            color = CrimeCrimsonLight,
                            modifier = Modifier.weight(1f)
                        )
                        EquipmentMiniBadge(
                            title = "Колёса",
                            name = equippedCar?.name ?: "Пешком",
                            icon = Icons.Default.DirectionsCar,
                            color = CrimeGold,
                            modifier = Modifier.weight(1f)
                        )
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
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(listOf(CrimeBorder, CrimeSurfaceHighlight))
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = CrimeGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Суточный баланс общака",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CrimeTextPrimary
                            )
                        }
                        Text(
                            text = if (netDaily >= 0) "+${formatNum(netDaily)} ₽/день" else "${formatNum(netDaily)} ₽/день",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (netDaily >= 0) CrimeCashGreen else CrimeHeatRed
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Доход (точки + дань районов):", fontSize = 11.sp, color = CrimeTextMuted)
                        Text(text = "+${formatNum(dailyBizIncome + dailyDistIncome)} ₽", fontSize = 11.sp, color = CrimeCashGreen)
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Расходы (зарплаты братвы + аренда):", fontSize = 11.sp, color = CrimeTextMuted)
                        Text(text = "-${formatNum(dailySalaries + baseRent)} ₽", fontSize = 11.sp, color = CrimeCrimsonLight)
                    }
                }
            }
        }

        // Feature Action Hub: New and High Priority Actions
        item {
            Text(
                text = "Криминальные направления & Движухи",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = CrimeTextPrimary
            )
        }

        // AI Boss Talk Featured Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenBossChat() }
                    .testTag("ai_boss_chat_tile"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CrimeSurfaceCard),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(CrimeCrimson, CrimeGold, CrimeBorder))
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(CrimeSurfaceHighlight)
                            .border(1.dp, CrimeGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🗣️", fontSize = 22.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Базар с ИИ-боссами",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CrimeGoldLight
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = CrimeCrimsonDark
                            ) {
                                Text(
                                    text = "ИИ ЧАТ",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CrimeTextPrimary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Живые переговоры по понятиям с авторитетами",
                            fontSize = 11.sp,
                            color = CrimeTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Icon(
                        Icons.Default.PhoneInTalk,
                        contentDescription = "Позвонить",
                        tint = CrimeGold,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Row 1: Crimes & Showdowns
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                    title = "Стрелки & Боссы",
                    subtitle = "Разборки с авторитетами",
                    icon = Icons.Default.Dangerous,
                    color = CrimeHeatRed,
                    onClick = onOpenBossArena,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Row 2: Katran & Gym
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickHubTile(
                    title = "Катран & Очко",
                    subtitle = "Карты на деньги (21)",
                    icon = Icons.Default.Casino,
                    color = CrimeGold,
                    onClick = onOpenCasino,
                    modifier = Modifier.weight(1f)
                )
                QuickHubTile(
                    title = "Качалка в подвале",
                    subtitle = "Качать силу и меткость",
                    icon = Icons.Default.FitnessCenter,
                    color = CrimePoliceBlue,
                    onClick = onOpenGym,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Row 3: Districts & Safe
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickHubTile(
                    title = "Районы города",
                    subtitle = "Отжим территорий и дань",
                    icon = Icons.Default.Place,
                    color = CrimeSteel,
                    onClick = onNavigateToDistricts,
                    modifier = Modifier.weight(1f)
                )
                QuickHubTile(
                    title = "Общак & Сейф",
                    subtitle = "Спрятать бабки (+2%/день)",
                    icon = Icons.Default.Lock,
                    color = CrimeCashGreen,
                    onClick = onOpenSafe,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Stats Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CrimeSurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CrimeBorder, CrimeBorder)))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Воровское досье",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CrimeTextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem(label = "Дел совершено", value = "${uiState.stats.totalCrimes}")
                        StatItem(label = "Успешных", value = "${uiState.stats.successfulCrimes}")
                        StatItem(label = "Стрелок выиграно", value = "${uiState.stats.showdownsWon}")
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem(label = "Поднято всего", value = "${formatNum(uiState.stats.totalEarned)} ₽")
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
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CrimeTextPrimary
                )
                TextButton(onClick = onNewGameClick) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp), tint = CrimeTextMuted)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Новая игра", fontSize = 11.sp, color = CrimeTextMuted)
                }
            }
        }

        // History items
        items(uiState.history.reversed().take(12)) { entry ->
            HistoryItemCard(entry = entry)
        }
    }
}

@Composable
private fun HeroStatItem(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = title,
                fontSize = 10.sp,
                color = CrimeTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
        modifier = modifier.heightIn(min = 46.dp),
        shape = RoundedCornerShape(8.dp),
        color = CrimeSurfaceHighlight
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(title, fontSize = 9.sp, color = CrimeTextMuted)
            }
            Text(
                text = name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CrimeTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
        modifier = modifier.heightIn(min = 72.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CrimeSurfaceCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(listOf(color.copy(alpha = 0.5f), CrimeBorder))
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CrimeTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = CrimeTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 10.sp, color = CrimeTextMuted)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CrimeGoldLight)
    }
}

@Composable
private fun HistoryItemCard(entry: GameHistoryEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(CrimeSurfaceCard)
            .padding(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (entry.isPositive) CrimeCashGreen.copy(alpha = 0.2f) else CrimeHeatRed.copy(alpha = 0.2f)
        ) {
            Text(
                text = "День ${entry.day}",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (entry.isPositive) CrimeCashGreen else CrimeCrimsonLight,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = entry.text,
            fontSize = 11.sp,
            color = CrimeTextSecondary,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun formatNum(value: Long): String {
    return "%,d".format(value).replace(',', ' ')
}

private fun formatNumShort(value: Long): String {
    return if (value >= 1_000_000) {
        "%.1fM".format(value / 1_000_000f)
    } else if (value >= 100_000) {
        "${value / 1000}k"
    } else {
        "%,d".format(value).replace(',', ' ')
    }
}
