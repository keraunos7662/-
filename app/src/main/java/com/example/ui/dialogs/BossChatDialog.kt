package com.example.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.BossChatMessage
import com.example.model.BossChatState
import com.example.model.MessageSender
import com.example.model.RivalBoss
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BossChatDialog(
    chatState: BossChatState,
    allBosses: List<RivalBoss>,
    playerMoney: Long,
    onSendMessage: (String) -> Unit,
    onSwitchBoss: (RivalBoss) -> Unit,
    onPayTribute: () -> Unit,
    onOfferCigarette: () -> Unit,
    onStartShowdownFromChat: (RivalBoss) -> Unit,
    onDismiss: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom on new messages or typing state
    LaunchedEffect(chatState.messages.size, chatState.isLoading) {
        if (chatState.messages.isNotEmpty()) {
            listState.animateScrollToItem(chatState.messages.size - 1)
        }
    }

    val moodColor = when {
        chatState.isAttacking -> CrimeCrimson
        chatState.moodEmoji == "⚠️" -> Color(0xFFFF5252)
        chatState.moodEmoji == "😠" -> Color(0xFFFF9800)
        chatState.moodEmoji == "🤨" -> CrimeGoldLight
        else -> CrimeCashGreen
    }

    // Avatar border pulse if angry or attacking
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val attackBorderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "attackBorderAlpha"
    )

    Dialog(
        onDismissRequest = {
            if (!chatState.isAttacking) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .testTag("boss_chat_dialog"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CrimeSurfaceDark),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.verticalGradient(
                    if (chatState.isAttacking) listOf(CrimeCrimson, Color.Red, CrimeCrimsonDark)
                    else listOf(CrimeCrimson, CrimeGold, CrimeBorder)
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Header: Boss Identity & Live Demeanor
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(CrimeSurfaceCard)
                            .border(
                                width = if (chatState.isAttacking) 2.5.dp else 1.5.dp,
                                color = if (chatState.isAttacking) CrimeCrimson.copy(alpha = attackBorderAlpha) else moodColor,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = chatState.boss.avatarEmoji, fontSize = 26.sp)

                        // Small mood emoji in corner
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(CrimeSurfaceDark)
                                .border(1.dp, moodColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = chatState.moodEmoji, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = chatState.boss.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CrimeTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = chatState.boss.title,
                            fontSize = 11.sp,
                            color = CrimeTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Living Boss Mood Status (No AI indicators!)
                        Text(
                            text = chatState.moodStatus,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = moodColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Relationship Indicator
                        val (relText, relColor) = getRelationshipInfo(chatState.relationshipScore)
                        Text(
                            text = "Отношение: $relText (${chatState.relationshipScore})",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = relColor
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("boss_chat_close_btn"),
                        enabled = !chatState.isAttacking
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = CrimeTextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Boss Selector Strip (Quick switch between bosses)
                if (!chatState.isAttacking) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(allBosses) { b ->
                            val isSelected = b.id == chatState.boss.id
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) CrimeCrimson.copy(alpha = 0.35f) else CrimeSurfaceCard,
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = Brush.horizontalGradient(
                                        if (isSelected) listOf(CrimeCrimson, CrimeGold)
                                        else listOf(CrimeBorder, CrimeBorder)
                                    )
                                ),
                                modifier = Modifier.clickable {
                                    if (!isSelected && !chatState.isLoading) onSwitchBoss(b)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = b.avatarEmoji, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = b.name.split(" ").firstOrNull() ?: b.name,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) CrimeGoldLight else CrimeTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    color = CrimeBorder.copy(alpha = 0.4f)
                )

                // Attack Banner when Patience Runs Out
                AnimatedVisibility(visible = chatState.isAttacking) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CrimeCrimson.copy(alpha = 0.95f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "💥", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ТЕРПЕНИЕ БОССА ЛОПНУЛО!",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "${chatState.boss.name} кричит: «Валите фраера!» и бросается в атаку!",
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = { onStartShowdownFromChat(chatState.boss) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "В БОЙ!",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CrimeGold
                                )
                            }
                        }
                    }
                }

                // Messages Chat Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CrimeSurfaceCard.copy(alpha = 0.5f))
                        .border(1.dp, CrimeBorder.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    if (chatState.messages.isEmpty() && !chatState.isLoading) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(text = "📞", fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Стрелка с ${chatState.boss.name}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = CrimeTextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Базарь по понятиям. Не дерзи без нужды — терпение авторитета не бесконечно.",
                                fontSize = 11.sp,
                                color = CrimeTextMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(chatState.messages, key = { it.id }) { msg ->
                                ChatMessageBubble(
                                    message = msg,
                                    bossAvatar = chatState.boss.avatarEmoji,
                                    bossName = chatState.boss.name
                                )
                            }

                            if (chatState.isLoading) {
                                item {
                                    LivelyTypingIndicator(
                                        bossName = chatState.boss.name,
                                        bossAvatar = chatState.boss.avatarEmoji
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Action Suggestion Chips
                if (!chatState.isAttacking) {
                    val quickPhrases = remember(chatState.boss.id) {
                        listOf(
                            "🤝 Давай решим миром",
                            "🚬 Угостить папиросой",
                            "💼 Предлагаю общий бизнес",
                            "❓ Ты под кем ходишь?",
                            "👊 Ты кто такой, чтоб быковать?",
                            "🔪 Слышь, не быкуй, перо словишь!"
                        )
                    }

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(quickPhrases) { rawPhrase ->
                            val cleanText = rawPhrase.substringAfter(" ")
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (cleanText.contains("быку") || cleanText.contains("перо")) CrimeCrimsonDark.copy(alpha = 0.4f) else CrimeSurfaceHighlight,
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = Brush.horizontalGradient(
                                        if (cleanText.contains("быку") || cleanText.contains("перо")) listOf(CrimeCrimson, CrimeCrimsonDark)
                                        else listOf(CrimeBorder, CrimeGoldDark.copy(alpha = 0.5f))
                                    )
                                ),
                                modifier = Modifier.clickable(enabled = !chatState.isLoading && !chatState.isAttacking) {
                                    if (cleanText.contains("папирос")) {
                                        onOfferCigarette()
                                    } else {
                                        onSendMessage(cleanText)
                                    }
                                }
                            ) {
                                Text(
                                    text = rawPhrase,
                                    fontSize = 11.sp,
                                    color = if (cleanText.contains("быку") || cleanText.contains("перо")) Color(0xFFFF8A80) else CrimeGoldLight,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Message Text Field & Send Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("boss_chat_input"),
                            placeholder = {
                                Text("Базар по понятиям...", fontSize = 12.sp, color = CrimeTextMuted)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CrimeCrimson,
                                unfocusedBorderColor = CrimeBorder,
                                focusedTextColor = CrimeTextPrimary,
                                unfocusedTextColor = CrimeTextPrimary,
                                cursorColor = CrimeGold
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            enabled = !chatState.isLoading && !chatState.isAttacking
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                val msg = inputText.trim()
                                if (msg.isNotEmpty() && !chatState.isLoading && !chatState.isAttacking) {
                                    onSendMessage(msg)
                                    inputText = ""
                                }
                            },
                            enabled = inputText.isNotBlank() && !chatState.isLoading && !chatState.isAttacking,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (inputText.isNotBlank() && !chatState.isLoading && !chatState.isAttacking) CrimeCrimson
                                    else CrimeSurfaceCard
                                )
                                .testTag("boss_chat_send_btn")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Отправить",
                                tint = if (inputText.isNotBlank()) CrimeTextPrimary else CrimeTextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Bottom Action Buttons: Smoke Peace, Tribute or Showdown Fight
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val tributeAmount = 3000L
                        val canAffordTribute = playerMoney >= tributeAmount

                        OutlinedButton(
                            onClick = onOfferCigarette,
                            enabled = !chatState.isLoading && !chatState.isAttacking,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimeGoldLight),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "🚬 Курево", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onPayTribute,
                            enabled = canAffordTribute && !chatState.isLoading && !chatState.isAttacking,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimeCashGreen),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Icon(Icons.Default.MonetizationOn, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(text = "Откуп (3к ₽)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onStartShowdownFromChat(chatState.boss) },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimeCrimson),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(text = "Бой", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: BossChatMessage,
    bossAvatar: String,
    bossName: String
) {
    val isPlayer = message.sender == MessageSender.PLAYER
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeStr = timeFormat.format(Date(message.timestamp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isPlayer) Arrangement.End else Arrangement.Start
    ) {
        if (!isPlayer) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(CrimeSurfaceHighlight),
                contentAlignment = Alignment.Center
            ) {
                Text(text = bossAvatar, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (isPlayer) 12.dp else 2.dp,
                bottomEnd = if (isPlayer) 2.dp else 12.dp
            ),
            color = if (isPlayer) CrimeGoldDark.copy(alpha = 0.35f) else CrimeSurfaceCard,
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.horizontalGradient(
                    if (isPlayer) listOf(CrimeGold.copy(alpha = 0.6f), CrimeGoldDark.copy(alpha = 0.4f))
                    else listOf(CrimeCrimson.copy(alpha = 0.4f), CrimeBorder)
                )
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                if (!isPlayer) {
                    Text(
                        text = bossName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CrimeCrimsonLight
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = message.text,
                    fontSize = 12.sp,
                    color = CrimeTextPrimary,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = timeStr,
                    fontSize = 9.sp,
                    color = CrimeTextMuted,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
private fun LivelyTypingIndicator(bossName: String, bossAvatar: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")

    val dot1Scale by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 0, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "d1"
    )

    val dot2Scale by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 150, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "d2"
    )

    val dot3Scale by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "d3"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(CrimeSurfaceHighlight),
            contentAlignment = Alignment.Center
        ) {
            Text(text = bossAvatar, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.width(6.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CrimeSurfaceCard,
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.horizontalGradient(listOf(CrimeCrimson.copy(alpha = 0.3f), CrimeBorder))
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "$bossName печатает",
                    fontSize = 11.sp,
                    color = CrimeGoldLight,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size((4 * dot1Scale).dp.coerceAtLeast(3.dp))
                            .clip(CircleShape)
                            .background(CrimeGold)
                    )
                    Box(
                        modifier = Modifier
                            .size((4 * dot2Scale).dp.coerceAtLeast(3.dp))
                            .clip(CircleShape)
                            .background(CrimeGold)
                    )
                    Box(
                        modifier = Modifier
                            .size((4 * dot3Scale).dp.coerceAtLeast(3.dp))
                            .clip(CircleShape)
                            .background(CrimeGold)
                    )
                }
            }
        }
    }
}

private fun getRelationshipInfo(score: Int): Pair<String, Color> {
    return when {
        score <= -40 -> Pair("Кровная вражда", CrimeCrimson)
        score in -39..-1 -> Pair("Напряжённое", CrimeGoldDark)
        score in 0..39 -> Pair("Нейтральное", CrimeTextSecondary)
        score in 40..69 -> Pair("Уважение", CrimeGold)
        else -> Pair("Кореша / Союзники", CrimeCashGreen)
    }
}
