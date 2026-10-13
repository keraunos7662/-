package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.model.AssetCategory
import com.example.model.AssetItem
import com.example.ui.theme.*
import com.example.viewmodel.GameUiState

@Composable
fun AssetsScreen(
    uiState: GameUiState,
    onBuyAsset: (AssetItem) -> Unit,
    onToggleEquip: (AssetItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(AssetCategory.WEAPON) }

    val categoryAssets = remember(selectedCategory, uiState.assets) {
        uiState.assets.filter { it.category == selectedCategory }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("assets_screen")
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text(
                text = "Имущество, Стволы и Бизнесы",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CrimeTextPrimary
            )
            Text(
                text = "Экипируй ствол, садись за руль бумера и открывай точки для пассивного дохода.",
                fontSize = 12.sp,
                color = CrimeTextSecondary
            )
        }

        // Category Tabs
        TabRow(
            selectedTabIndex = selectedCategory.ordinal,
            containerColor = CrimeSurfaceDark,
            contentColor = CrimeGold,
            indicator = {},
            divider = {},
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, CrimeBorder, RoundedCornerShape(10.dp))
        ) {
            AssetCategory.entries.forEach { category ->
                val selected = selectedCategory == category
                Tab(
                    selected = selected,
                    onClick = { selectedCategory = category },
                    modifier = Modifier
                        .background(if (selected) CrimeSurfaceHighlight else Color.Transparent)
                        .padding(vertical = 10.dp)
                ) {
                    Text(
                        text = when (category) {
                            AssetCategory.WEAPON -> "Стволы"
                            AssetCategory.VEHICLE -> "Тачки"
                            AssetCategory.CLOTHING -> "Шмот"
                            AssetCategory.BUSINESS -> "Бизнесы"
                        },
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) CrimeGoldLight else CrimeTextMuted
                    )
                }
            }
        }

        // Assets List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(categoryAssets) { asset ->
                AssetCard(
                    asset = asset,
                    playerMoney = uiState.money,
                    onBuy = { onBuyAsset(asset) },
                    onToggleEquip = { onToggleEquip(asset) }
                )
            }
        }
    }
}

@Composable
private fun AssetCard(
    asset: AssetItem,
    playerMoney: Long,
    onBuy: () -> Unit,
    onToggleEquip: () -> Unit
) {
    val canAfford = playerMoney >= asset.price

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("asset_card_${asset.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CrimeSurfaceCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = if (asset.isEquipped) Brush.horizontalGradient(listOf(CrimeGold, CrimeBorder))
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
                    text = asset.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = CrimeTextPrimary
                )

                if (asset.isEquipped) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = CrimeGoldDark.copy(alpha = 0.4f)
                    ) {
                        Text(
                            text = "АКТИВНО",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrimeGoldLight,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else if (asset.isPurchased) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = CrimeCashGreen.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "КУПЛЕНО",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrimeCashGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = asset.description,
                fontSize = 12.sp,
                color = CrimeTextSecondary,
                lineHeight = 16.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Bonuses row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (asset.attackBonus > 0) {
                    BonusBadge("Урон +${asset.attackBonus}", CrimeCrimsonLight)
                }
                if (asset.defenseBonus > 0) {
                    BonusBadge("Защита +${asset.defenseBonus}", CrimeSteel)
                }
                if (asset.respectBonus > 0) {
                    BonusBadge("Респект +${asset.respectBonus}", CrimeGold)
                }
                if (asset.dailyIncome > 0) {
                    BonusBadge("+${formatNum(asset.dailyIncome)} ₽/день", CrimeCashGreen)
                }
                if (asset.heatReductionDaily > 0) {
                    BonusBadge("Сброс розыска -${asset.heatReductionDaily}", CrimePoliceBlue)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action row
            if (!asset.isPurchased) {
                Button(
                    onClick = onBuy,
                    enabled = canAfford,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("buy_asset_${asset.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canAfford) CrimeCashGreen else CrimeSurfaceHighlight,
                        contentColor = if (canAfford) Color.Black else CrimeTextMuted
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Купить за ${formatNum(asset.price)} ₽",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (asset.category != AssetCategory.BUSINESS) {
                OutlinedButton(
                    onClick = onToggleEquip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("equip_asset_${asset.id}"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (asset.isEquipped) CrimeGoldLight else CrimeTextPrimary
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(
                            if (asset.isEquipped) listOf(CrimeGold, CrimeGoldDark)
                            else listOf(CrimeBorder, CrimeBorder)
                        )
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (asset.isEquipped) "Снять" else "Экипировать / Выбрать",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = CrimeSurfaceDark
                ) {
                    Text(
                        text = "Точка работает и приносит стабильный доход в общак",
                        fontSize = 11.sp,
                        color = CrimeCashGreen,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BonusBadge(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

private fun formatNum(value: Long): String {
    return "%,d".format(value).replace(',', ' ')
}
