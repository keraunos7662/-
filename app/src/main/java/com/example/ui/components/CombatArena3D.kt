package com.example.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.SoundType
import com.example.data.sound.SoundManager
import com.example.model.ShowdownState
import com.example.ui.theme.*
import com.example.viewmodel.ShowdownAction
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*
import kotlin.random.Random

/**
 * Visual particle for hit sparks, blood, and gun smoke in the 3D combat arena.
 */
data class HitSpark(
    val id: Long,
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    var life: Float = 1f,
    val color: Color,
    val radius: Float
)

/**
 * Floating damage or combat text in the 3D arena.
 */
data class CombatPopup(
    val id: Long,
    val text: String,
    val color: Color,
    val xPercent: Float,
    var yPercent: Float,
    var alpha: Float = 1f,
    val isCritical: Boolean = false
)

/**
 * First-person player action states.
 */
enum class BrawlerActionState {
    IDLE,
    PUNCH_LEFT,
    PUNCH_RIGHT,
    SHOOT,
    GUARD,
    HEAL,
    RAGE_COMBO
}

/**
 * Dynamic boss combat animation states.
 */
enum class BossCombatState {
    IDLE_STANCE,
    WINDUP,
    LUNGE_ATTACK,
    STAGGER_HIT,
    KNOCKOUT
}

/**
 * Maps boss identity to photorealistic artwork.
 */
fun getBossArtwork(rivalName: String, rivalAvatarEmoji: String): Int {
    return when {
        rivalName.contains("Штырь", ignoreCase = true) || rivalAvatarEmoji == "🔪" -> R.drawable.img_boss_gopnik
        rivalName.contains("Кастет", ignoreCase = true) || rivalName.contains("Клык", ignoreCase = true) || rivalAvatarEmoji == "🥊" || rivalAvatarEmoji == "🐺" -> R.drawable.img_boss_muscled
        rivalName.contains("Мрамор", ignoreCase = true) || rivalName.contains("Барон", ignoreCase = true) || rivalAvatarEmoji == "💣" || rivalAvatarEmoji == "👑" -> R.drawable.img_boss_godfather
        else -> R.drawable.img_boss_gopnik
    }
}

/**
 * High-octane, visceral 3D Combat Arena with realistic artwork, parallax background,
 * full 3D boss motion physics, real first-person hands/weapons, hidden patience attack AI,
 * hit reactions, dodging, particle physics, and arcade HUD.
 */
@Composable
fun CombatArena3D(
    showdown: ShowdownState,
    onAction: (ShowdownAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }
    val coroutineScope = rememberCoroutineScope()

    // 3D Camera / Dodge physics
    var cameraDodgeX by remember { mutableFloatStateOf(0f) }
    val animatedCamX by animateFloatAsState(
        targetValue = cameraDodgeX,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
        label = "cam_dodge_x"
    )

    // Player Animation state
    var actionState by remember { mutableStateOf(BrawlerActionState.IDLE) }
    var muzzleFlashVisible by remember { mutableStateOf(false) }
    var screenShakeAmount by remember { mutableFloatStateOf(0f) }
    var redHurtFlash by remember { mutableFloatStateOf(0f) }
    var guardActive by remember { mutableStateOf(false) }
    var healPulseActive by remember { mutableStateOf(false) }
    var comboCount by remember { mutableIntStateOf(0) }

    // Dynamic Boss 3D Position, Motion & Animation States
    var bossState by remember { mutableStateOf(BossCombatState.IDLE_STANCE) }
    var bossZ by remember { mutableFloatStateOf(300f) } // 140 = close lunge, 300 = normal, 380 = knocked back
    var bossTilt by remember { mutableFloatStateOf(0f) }
    var bossRotationY by remember { mutableFloatStateOf(0f) }
    var bossLeanForward by remember { mutableFloatStateOf(0f) }
    var bossHitFlash by remember { mutableFloatStateOf(0f) }
    var bossDialogue by remember { mutableStateOf("") }
    var bossSlashTrailVisible by remember { mutableStateOf(false) }

    // Hidden Boss Patience Meter (Player doesn't see this bar, but when it fills -> Boss attacks!)
    var hiddenBossPatience by remember { mutableFloatStateOf(0f) }

    // Floating combat effects
    val hitSparks = remember { mutableStateListOf<HitSpark>() }
    val popups = remember { mutableStateListOf<CombatPopup>() }

    // Continuous combat clock for organic 3D body motion
    val infiniteTransition = rememberInfiniteTransition(label = "brawler_motion")
    val combatClock by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "combat_clock"
    )

    val rainOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rain_anim"
    )

    // Boss taunts based on battle rounds and health
    LaunchedEffect(showdown.round, showdown.rivalCurrentHp) {
        val healthPercent = showdown.rivalCurrentHp.toFloat() / showdown.rivalMaxHp.coerceAtLeast(1)
        bossDialogue = when {
            showdown.isFinished && showdown.isPlayerWon -> "Всё... твоя взяла, пацан..."
            showdown.isFinished && !showdown.isPlayerWon -> "Знай своё место на районе!"
            healthPercent < 0.25f -> "Сука... щас зубами рвать буду!"
            healthPercent < 0.5f -> "Ах ты сучонок, зацепил!"
            showdown.round == 1 -> "Ты на кого батон крошишь?!"
            showdown.round % 2 == 0 -> "Слышь, пацанчик, попутал берега?!"
            else -> "Щас я тебе все рёбра пересчитаю!"
        }
    }

    // React to state changes (Player damage / Boss damage from external turn engine)
    LaunchedEffect(showdown.playerCurrentHp, showdown.rivalCurrentHp) {
        if (showdown.wasPlayerHit) {
            screenShakeAmount = 26f
            redHurtFlash = 0.85f
            soundManager.playSound(SoundType.PUNCH)
        }
    }

    // Helper functions for triggering actions with visceral animations
    fun spawnSparks(x: Float, y: Float, count: Int = 12, isBlood: Boolean = false) {
        repeat(count) {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val speed = Random.nextFloat() * 14f + 4f
            hitSparks.add(
                HitSpark(
                    id = System.nanoTime() + Random.nextLong(1000),
                    x = x,
                    y = y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    life = 1f,
                    color = if (isBlood) Color(0xFFD50000) else if (Random.nextBoolean()) Color(0xFFFFD600) else Color(0xFFFF3D00),
                    radius = Random.nextFloat() * 4.5f + 2f
                )
            )
        }
    }

    // Boss dynamic AI lunge attack triggered by hidden patience meter
    fun triggerBossLungeAttack() {
        if (showdown.isFinished || bossState == BossCombatState.KNOCKOUT) return
        coroutineScope.launch {
            bossState = BossCombatState.WINDUP
            bossDialogue = listOf("СУКА, ПОЛУЧАЙ!", "НА ПЕРО!", "ЩАС ЗАВАЛЮ!", "ТЕРПЕНИЕ ЛОПНУЛО!").random()
            bossZ = 340f
            bossLeanForward = 12f

            // Wind-up pause (telegraph for player to dodge or block!)
            delay(420)
            if (showdown.isFinished) return@launch

            // Explosive lunge toward camera
            bossState = BossCombatState.LUNGE_ATTACK
            bossZ = 135f // Surges right in player's face
            bossLeanForward = -15f
            bossSlashTrailVisible = true
            soundManager.playSound(SoundType.PUNCH)

            delay(120)

            // Check if player dodged or blocked
            if (cameraDodgeX != 0f) {
                // Player successfully dodged!
                soundManager.playSound(SoundType.CLICK)
                popups.add(
                    CombatPopup(
                        id = System.nanoTime(),
                        text = "УКЛОН! МИМО! 💨",
                        color = CrimeGoldLight,
                        xPercent = 0.5f,
                        yPercent = 0.35f,
                        isCritical = true
                    )
                )
                bossTilt = 18f
                bossRotationY = 22f
            } else if (guardActive) {
                // Player blocked!
                soundManager.playSound(SoundType.CLICK)
                screenShakeAmount = 10f
                popups.add(
                    CombatPopup(
                        id = System.nanoTime(),
                        text = "БЛОКИРОВАНО! 🛡️",
                        color = CrimePoliceBlue,
                        xPercent = 0.5f,
                        yPercent = 0.42f
                    )
                )
            } else {
                // Player took full hit!
                screenShakeAmount = 32f
                redHurtFlash = 0.9f
                soundManager.playSound(SoundType.PUNCH)
                popups.add(
                    CombatPopup(
                        id = System.nanoTime(),
                        text = "УДАР БОССА! -25 🩸",
                        color = Color(0xFFFF1744),
                        xPercent = 0.5f,
                        yPercent = 0.38f,
                        isCritical = true
                    )
                )
            }

            delay(200)
            bossSlashTrailVisible = false

            // Return to stance
            delay(250)
            bossZ = 300f
            bossLeanForward = 0f
            bossTilt = 0f
            bossRotationY = 0f
            bossState = BossCombatState.IDLE_STANCE
        }
    }

    // Hidden patience meter loop & frame physics
    LaunchedEffect(showdown.isFinished) {
        while (!showdown.isFinished) {
            delay(100)
            // Accumulate hidden patience gradually (~7-9 seconds per cycle)
            hiddenBossPatience += 1.3f
            if (hiddenBossPatience >= 100f && bossState == BossCombatState.IDLE_STANCE) {
                hiddenBossPatience = 0f
                triggerBossLungeAttack()
            }
        }
    }

    // Frame ticker for sparks, popups, and physics decay
    LaunchedEffect(Unit) {
        while (true) {
            delay(16) // ~60 FPS
            if (screenShakeAmount > 0.5f) {
                screenShakeAmount *= 0.82f
            } else {
                screenShakeAmount = 0f
            }

            if (redHurtFlash > 0.02f) {
                redHurtFlash *= 0.88f
            } else {
                redHurtFlash = 0f
            }

            if (bossHitFlash > 0.02f) {
                bossHitFlash *= 0.85f
            } else {
                bossHitFlash = 0f
            }

            if (bossTilt != 0f) {
                bossTilt *= 0.86f
                if (abs(bossTilt) < 0.5f) bossTilt = 0f
            }

            if (bossRotationY != 0f) {
                bossRotationY *= 0.88f
                if (abs(bossRotationY) < 0.5f) bossRotationY = 0f
            }

            if (bossState == BossCombatState.IDLE_STANCE) {
                if (bossZ > 300f) {
                    bossZ -= (bossZ - 300f) * 0.12f
                } else if (bossZ < 300f) {
                    bossZ += (300f - bossZ) * 0.12f
                }
            }

            // Update hit sparks
            val iterator = hitSparks.listIterator()
            while (iterator.hasNext()) {
                val spark = iterator.next()
                spark.x += spark.vx
                spark.y += spark.vy
                spark.life -= 0.045f
                if (spark.life <= 0f) {
                    iterator.remove()
                }
            }

            // Update combat popups
            val popupIterator = popups.listIterator()
            while (popupIterator.hasNext()) {
                val popup = popupIterator.next()
                popup.yPercent -= 0.0035f
                popup.alpha -= 0.025f
                if (popup.alpha <= 0f) {
                    popupIterator.remove()
                }
            }
        }
    }

    fun triggerPlayerPunch(isRight: Boolean) {
        if (showdown.isFinished) return
        coroutineScope.launch {
            actionState = if (isRight) BrawlerActionState.PUNCH_RIGHT else BrawlerActionState.PUNCH_LEFT
            soundManager.playSound(SoundType.PUNCH)
            comboCount++

            // Boss impact and stagger reaction
            delay(90)
            screenShakeAmount = 16f
            bossHitFlash = 1f
            bossTilt = if (isRight) -16f else 16f
            bossRotationY = if (isRight) -22f else 22f
            bossZ = 350f
            bossState = BossCombatState.STAGGER_HIT

            // Add popup
            val isCrit = Random.nextInt(100) < 30
            val damageVal = Random.nextInt(22, 38) * (if (isCrit) 2 else 1)
            popups.add(
                CombatPopup(
                    id = System.nanoTime(),
                    text = if (isCrit) "КРИТ! -$damageVal 💥" else "-$damageVal 👊",
                    color = if (isCrit) CrimeGoldLight else Color(0xFFFF5252),
                    xPercent = 0.45f + (Random.nextFloat() - 0.5f) * 0.2f,
                    yPercent = 0.38f + (Random.nextFloat() - 0.5f) * 0.1f,
                    isCritical = isCrit
                )
            )

            onAction(ShowdownAction.PUNCH)
            delay(160)
            actionState = BrawlerActionState.IDLE
            delay(150)
            if (bossState == BossCombatState.STAGGER_HIT) {
                bossState = BossCombatState.IDLE_STANCE
            }
        }
    }

    fun triggerPlayerShoot() {
        if (showdown.isFinished || showdown.ammoCount <= 0) return
        coroutineScope.launch {
            actionState = BrawlerActionState.SHOOT
            muzzleFlashVisible = true
            soundManager.playSound(SoundType.SHOT)
            screenShakeAmount = 28f
            comboCount++

            bossHitFlash = 1f
            bossTilt = 22f
            bossRotationY = 15f
            bossZ = 370f
            bossState = BossCombatState.STAGGER_HIT

            val damageVal = Random.nextInt(55, 85)
            popups.add(
                CombatPopup(
                    id = System.nanoTime(),
                    text = "ВЫСТРЕЛ ТТ! -$damageVal 🔫",
                    color = CrimeGoldLight,
                    xPercent = 0.5f,
                    yPercent = 0.32f,
                    isCritical = true
                )
            )

            onAction(ShowdownAction.SHOOT)
            delay(110)
            muzzleFlashVisible = false
            delay(220)
            actionState = BrawlerActionState.IDLE
            delay(150)
            if (bossState == BossCombatState.STAGGER_HIT) {
                bossState = BossCombatState.IDLE_STANCE
            }
        }
    }

    fun triggerPlayerGuard() {
        if (showdown.isFinished) return
        coroutineScope.launch {
            guardActive = true
            actionState = BrawlerActionState.GUARD
            soundManager.playSound(SoundType.CLICK)
            popups.add(
                CombatPopup(
                    id = System.nanoTime(),
                    text = "ГЛУХОЙ БЛОК 🛡️",
                    color = CrimePoliceBlue,
                    xPercent = 0.5f,
                    yPercent = 0.45f
                )
            )
            onAction(ShowdownAction.GUARD)
            delay(400)
            guardActive = false
            actionState = BrawlerActionState.IDLE
        }
    }

    fun triggerPlayerHeal() {
        if (showdown.isFinished || showdown.firstAidKits <= 0) return
        coroutineScope.launch {
            actionState = BrawlerActionState.HEAL
            healPulseActive = true
            soundManager.playSound(SoundType.CASH)
            popups.add(
                CombatPopup(
                    id = System.nanoTime(),
                    text = "+60 HP 💉",
                    color = CrimeCashGreen,
                    xPercent = 0.5f,
                    yPercent = 0.5f,
                    isCritical = true
                )
            )
            onAction(ShowdownAction.FIRST_AID)
            delay(400)
            healPulseActive = false
            actionState = BrawlerActionState.IDLE
        }
    }

    fun triggerPlayerRage() {
        if (showdown.isFinished || showdown.rageMeter < 100) return
        coroutineScope.launch {
            actionState = BrawlerActionState.RAGE_COMBO
            soundManager.playSound(SoundType.RAGE)

            // Multi-punch flurry with real hands
            for (i in 1..4) {
                delay(90)
                screenShakeAmount = 18f
                bossTilt = if (i % 2 == 0) 18f else -18f
                bossRotationY = if (i % 2 == 0) 25f else -25f
                bossHitFlash = 1f
                soundManager.playSound(SoundType.PUNCH)
                popups.add(
                    CombatPopup(
                        id = System.nanoTime(),
                        text = "КОМБО x$i! ⚡",
                        color = CrimeCrimsonLight,
                        xPercent = 0.35f + (i * 0.08f),
                        yPercent = 0.4f - (i * 0.03f)
                    )
                )
            }

            // Devastating final hit
            delay(120)
            screenShakeAmount = 38f
            bossZ = 390f
            bossTilt = -25f
            bossHitFlash = 1f
            soundManager.playSound(SoundType.SHOT)
            popups.add(
                CombatPopup(
                    id = System.nanoTime(),
                    text = "ФАТАЛЬНЫЙ НОКАУТ! -140 💀",
                    color = CrimeGoldLight,
                    xPercent = 0.5f,
                    yPercent = 0.28f,
                    isCritical = true
                )
            )

            onAction(ShowdownAction.RAGE_KO)
            delay(350)
            actionState = BrawlerActionState.IDLE
        }
    }

    fun performDodge(direction: Float) {
        coroutineScope.launch {
            cameraDodgeX = direction * 160f
            soundManager.playSound(SoundType.CLICK)
            popups.add(
                CombatPopup(
                    id = System.nanoTime(),
                    text = "УКЛОН! СВИСТ! 💨",
                    color = CrimeGold,
                    xPercent = if (direction > 0) 0.7f else 0.3f,
                    yPercent = 0.5f
                )
            )
            delay(320)
            cameraDodgeX = 0f
        }
    }

    // MAIN CONTAINER WITH SCREEN SHAKE & DODGE TRANSFORM
    val shakeOffsetX = if (screenShakeAmount > 0f) (Random.nextFloat() - 0.5f) * screenShakeAmount else 0f
    val shakeOffsetY = if (screenShakeAmount > 0f) (Random.nextFloat() - 0.5f) * screenShakeAmount else 0f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .offset(x = (shakeOffsetX + animatedCamX * 0.25f).dp, y = shakeOffsetY.dp)
            .pointerInput(showdown.isFinished) {
                // Tap anywhere in arena to punch with corresponding hand!
                detectTapGestures { offset ->
                    if (!showdown.isFinished) {
                        spawnSparks(offset.x, offset.y, count = 10)
                        triggerPlayerPunch(isRight = offset.x > size.width / 2f)
                    }
                }
            }
            .testTag("combat_arena_3d_box")
    ) {
        // 1. LAYER: PARALLAX 3D BACKGROUND (90s Rainy Russian Street Garages Alley)
        Image(
            painter = painterResource(id = R.drawable.img_battle_arena_bg),
            contentDescription = "Улица 90-х, Гаражи",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .scale(1.12f)
                .offset(x = (-animatedCamX * 0.18f).dp, y = (sin(combatClock) * 3f).dp)
        )

        // 2. LAYER: ATMOSPHERIC RAIN & LIGHTING CANVAS
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Night dark vignette
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0x55000000),
                        Color(0xDD000000)
                    ),
                    center = Offset(width / 2f, height / 2f),
                    radius = max(width, height) * 0.75f
                )
            )

            // Sodium streetlamp glow pulse at top center
            val lampPulse = 0.75f + sin(combatClock) * 0.15f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x99FFA000).copy(alpha = lampPulse * 0.35f),
                        Color(0x33FF6F00).copy(alpha = lampPulse * 0.15f),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.58f, height * 0.12f),
                    radius = width * 0.55f
                ),
                radius = width * 0.55f,
                center = Offset(width * 0.58f, height * 0.12f)
            )

            // Animated slanted 3D rain streaks
            for (i in 0..40) {
                val seed = (i * 97) % 1000
                val rx = (seed * 11.3f) % width
                val ry = ((seed * 17.7f + rainOffset * 1.8f) % height)
                drawLine(
                    color = Color(0x33FFFFFF),
                    start = Offset(rx, ry),
                    end = Offset(rx - 8f, ry + 24f),
                    strokeWidth = 1.5f
                )
            }

            // Hit Sparks & Particles
            hitSparks.forEach { spark ->
                drawCircle(
                    color = spark.color.copy(alpha = spark.life.coerceIn(0f, 1f)),
                    radius = spark.radius * spark.life,
                    center = Offset(spark.x, spark.y)
                )
            }
        }

        // 3. LAYER: 3D DYNAMIC BOSS CHARACTER MODEL (Alive, Moving, Stepping, Rotating in 3D)
        val bossDrawable = remember(showdown.rivalName, showdown.rivalAvatarEmoji) {
            getBossArtwork(showdown.rivalName, showdown.rivalAvatarEmoji)
        }

        // Full 3D motion calculations:
        // Boss sidesteps left and right, shifts weight, bobs up/down, and turns in 3D perspective
        val bossPacingX = sin(combatClock * 1.8f) * 28f
        val bossBobbingY = abs(sin(combatClock * 3.6f)) * 8f
        val bossNaturalRotY = -12f * sin(combatClock * 1.8f) + (animatedCamX * 0.12f)
        val bossDepthScale = (320f / bossZ.coerceAtLeast(120f)).coerceIn(0.75f, 1.85f)

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(
                    x = (bossPacingX + animatedCamX * 0.35f).dp,
                    y = (-15 + bossBobbingY + (if (showdown.isFinished && showdown.isPlayerWon) 130 else 0)).dp
                )
                .graphicsLayer {
                    // True 3D rotation and perspective
                    rotationY = bossNaturalRotY + bossRotationY
                    rotationX = bossLeanForward
                    rotationZ = bossTilt + (if (showdown.isFinished && showdown.isPlayerWon) 75f else 0f)
                    scaleX = bossDepthScale
                    scaleY = bossDepthScale
                    cameraDistance = 12f * density
                    alpha = if (showdown.isFinished && showdown.isPlayerWon) 0.35f else 1f
                }
                .testTag("boss_3d_model")
        ) {
            // Realistic dynamic ground contact shadow on wet asphalt that tracks foot movement
            Canvas(
                modifier = Modifier
                    .size(240.dp, 44.dp)
                    .align(Alignment.BottomCenter)
                    .offset(y = 20.dp)
            ) {
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.9f),
                            Color.Black.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    )
                )
            }

            // High resolution character portrait with soft feathered blending into the dark street scene
            Box(
                modifier = Modifier
                    .size(width = 290.dp, height = 390.dp)
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            ) {
                Image(
                    painter = painterResource(id = bossDrawable),
                    contentDescription = showdown.rivalName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    colorFilter = if (bossHitFlash > 0.05f) {
                        ColorFilter.lighting(
                            multiply = Color.White,
                            add = Color(0xFFFF2222).copy(alpha = bossHitFlash * 0.85f)
                        )
                    } else null
                )

                // Soft bottom gradient so the boss blends seamlessly into the wet asphalt ground
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xCC050508), Color(0xFF000000))
                            )
                        )
                )
            }

            // BOSS STRIKE SLASH TRAIL VFX (When boss lunges at player)
            if (bossSlashTrailVisible) {
                Canvas(
                    modifier = Modifier
                        .size(320.dp, 280.dp)
                        .align(Alignment.Center)
                ) {
                    val p = Path().apply {
                        moveTo(size.width * 0.1f, size.height * 0.2f)
                        quadraticBezierTo(
                            size.width * 0.6f, size.height * 0.5f,
                            size.width * 0.9f, size.height * 0.85f
                        )
                    }
                    drawPath(
                        path = p,
                        brush = Brush.linearGradient(
                            listOf(Color(0xFFFF1744), Color(0xFFFFD600), Color.White)
                        ),
                        style = Stroke(width = 16f, cap = StrokeCap.Round)
                    )
                }
            }

            // Boss Dialogue Speech Bubble
            if (bossDialogue.isNotEmpty() && !showdown.isFinished) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-40).dp)
                        .shadow(8.dp, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xEE1A1320),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(CrimeCrimsonLight, CrimeGold))
                    )
                ) {
                    Text(
                        text = "«$bossDialogue»",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CrimeGoldLight,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Danger indicator when boss intent warns of strike
            if (!showdown.isFinished && showdown.rivalIntent.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 25.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = CrimeCrimsonDark.copy(alpha = 0.9f),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CrimeHeatRed, CrimeGold)))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = CrimeGold, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = showdown.rivalIntent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // 4. LAYER: REAL FIRST-PERSON PLAYER HANDS & WEAPONS (Photorealistic Brawler Arms)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Punch animations for Left Hand
            val leftPunchOffsetY by animateDpAsState(
                targetValue = if (actionState == BrawlerActionState.PUNCH_LEFT) (-110).dp else 0.dp,
                animationSpec = tween(durationMillis = 110, easing = FastOutLinearInEasing),
                label = "punch_left_y"
            )
            val leftPunchOffsetX by animateDpAsState(
                targetValue = if (actionState == BrawlerActionState.PUNCH_LEFT) 50.dp else 0.dp,
                animationSpec = tween(durationMillis = 110),
                label = "punch_left_x"
            )
            val leftPunchScale by animateFloatAsState(
                targetValue = if (actionState == BrawlerActionState.PUNCH_LEFT) 1.45f else 1f,
                animationSpec = tween(durationMillis = 110),
                label = "punch_left_scale"
            )

            // REAL LEFT HAND (First person muscular arm, leather glove & wrist wraps)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(
                        x = (-15 + (sin(combatClock) * 6f) + leftPunchOffsetX.value).dp,
                        y = (35 + leftPunchOffsetY.value).dp
                    )
                    .graphicsLayer {
                        scaleX = leftPunchScale
                        scaleY = leftPunchScale
                        rotationZ = if (actionState == BrawlerActionState.PUNCH_LEFT) 18f else 6f
                        rotationY = if (actionState == BrawlerActionState.GUARD) 30f else 0f
                    }
                    .clickable { triggerPlayerPunch(isRight = false) }
                    .testTag("player_left_fist")
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_fist_left),
                    contentDescription = "Левый кулак игрока",
                    modifier = Modifier
                        .size(190.dp, 190.dp)
                        .clip(RoundedCornerShape(topEnd = 40.dp)),
                    contentScale = ContentScale.Fit
                )
            }

            // Punch animations for Right Hand
            val rightPunchOffsetY by animateDpAsState(
                targetValue = if (actionState == BrawlerActionState.PUNCH_RIGHT) (-110).dp else 0.dp,
                animationSpec = tween(durationMillis = 110, easing = FastOutLinearInEasing),
                label = "punch_right_y"
            )
            val rightPunchOffsetX by animateDpAsState(
                targetValue = if (actionState == BrawlerActionState.PUNCH_RIGHT) (-50).dp else 0.dp,
                animationSpec = tween(durationMillis = 110),
                label = "punch_right_x"
            )
            val rightPunchScale by animateFloatAsState(
                targetValue = if (actionState == BrawlerActionState.PUNCH_RIGHT) 1.45f else 1f,
                animationSpec = tween(durationMillis = 110),
                label = "punch_right_scale"
            )

            // REAL RIGHT HAND / PISTOL (First person muscular arm with brass knuckles & jacket)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(
                        x = (15 - (sin(combatClock) * 6f) + rightPunchOffsetX.value).dp,
                        y = (35 + rightPunchOffsetY.value).dp
                    )
                    .graphicsLayer {
                        scaleX = rightPunchScale
                        scaleY = rightPunchScale
                        rotationZ = if (actionState == BrawlerActionState.PUNCH_RIGHT) -18f else -6f
                        rotationY = if (actionState == BrawlerActionState.GUARD) -30f else 0f
                    }
                    .clickable { triggerPlayerPunch(isRight = true) }
                    .testTag("player_right_hand")
            ) {
                if (actionState == BrawlerActionState.SHOOT || muzzleFlashVisible) {
                    // REAL HAND HOLDING TT PISTOL WITH RECOIL
                    Image(
                        painter = painterResource(id = R.drawable.img_gun_hand),
                        contentDescription = "Пистолет ТТ в руке",
                        modifier = Modifier
                            .size(210.dp, 210.dp)
                            .graphicsLayer {
                                translationY = if (muzzleFlashVisible) -15f else 0f
                                rotationZ = if (muzzleFlashVisible) -10f else 0f
                            }
                            .clip(RoundedCornerShape(topStart = 40.dp)),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    // REAL RIGHT FIST WITH BRASS KNUCKLES
                    Image(
                        painter = painterResource(id = R.drawable.img_fist_right),
                        contentDescription = "Правый кулак с кастетом",
                        modifier = Modifier
                            .size(190.dp, 190.dp)
                            .clip(RoundedCornerShape(topStart = 40.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // REALISTIC DYNAMIC MUZZLE FLASH BURST
            if (muzzleFlashVisible) {
                Canvas(
                    modifier = Modifier
                        .size(180.dp)
                        .align(Alignment.CenterEnd)
                        .offset(x = (-40).dp, y = (-50).dp)
                ) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White,
                                Color(0xFFFFEE55),
                                Color(0xFFFF5722),
                                Color.Transparent
                            )
                        )
                    )
                }
            }

            // GUARD / BLOCK DEFENSE CROSS-STANCE
            if (guardActive || actionState == BrawlerActionState.GUARD) {
                Surface(
                    modifier = Modifier
                        .size(width = 240.dp, height = 180.dp)
                        .align(Alignment.Center)
                        .shadow(16.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    color = CrimePoliceBlue.copy(alpha = 0.25f),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.radialGradient(listOf(Color.White, CrimePoliceBlue))
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = "Блок",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(72.dp)
                        )
                        Text(
                            text = "ГЛУХОЙ БЛОК",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }

            // ADRENALINE HEAL PULSE
            if (healPulseActive) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0x6600E676),
                                Color.Transparent
                            )
                        )
                    )
                }
            }
        }

        // 5. LAYER: FLOATING 3D COMBAT DAMAGE POPUPS
        popups.forEach { popup ->
            Text(
                text = popup.text,
                fontSize = if (popup.isCritical) 20.sp else 16.sp,
                fontWeight = FontWeight.Black,
                color = popup.color,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(
                        x = (popup.xPercent * 340).dp,
                        y = (popup.yPercent * 500).dp
                    )
                    .alpha(popup.alpha)
                    .shadow(4.dp)
            )
        }

        // 6. LAYER: RED HURT VIGNETTE
        if (redHurtFlash > 0.01f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x77FF1744).copy(alpha = redHurtFlash * 0.7f),
                            Color(0xDD990000).copy(alpha = redHurtFlash)
                        )
                    )
                )
            }
        }

        // 7. LAYER: ARCADE TOP HUD BAR (Boss Name, HP, Danger)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .align(Alignment.TopCenter)
                .shadow(12.dp, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xD9100D14),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.horizontalGradient(listOf(CrimeCrimsonDark, CrimeGold))
            )
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = CrimeCrimsonDark,
                            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(CrimeGold, CrimeCrimsonLight)))
                        ) {
                            Text(
                                text = showdown.rivalAvatarEmoji.ifEmpty { "👊" },
                                fontSize = 16.sp,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = showdown.rivalName.uppercase(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = CrimeGoldLight
                            )
                            Text(
                                text = showdown.rivalTitle,
                                fontSize = 10.sp,
                                color = CrimeTextMuted
                            )
                        }
                    }

                    // Boss HP digits
                    Text(
                        text = "${showdown.rivalCurrentHp}/${showdown.rivalMaxHp} HP",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (showdown.rivalCurrentHp < showdown.rivalMaxHp * 0.3f) CrimeCrimsonLight else Color.White
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Glowing Neon Boss HP Gauge
                val bossHpFraction = (showdown.rivalCurrentHp.toFloat() / showdown.rivalMaxHp.coerceAtLeast(1)).coerceIn(0f, 1f)
                val animatedBossHp by animateFloatAsState(
                    targetValue = bossHpFraction,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                    label = "boss_hp"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFF201625))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedBossHp)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(CrimeCrimsonLight, CrimeHeatRed, CrimeGold)
                                )
                            )
                    )
                }
            }
        }

        // 8. LAYER: FLOATING QUICK DODGE BUTTONS [⬅️ УКЛОН] & [УКЛОН ➡️]
        if (!showdown.isFinished) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Dodge Button
                FilledTonalButton(
                    onClick = { performDodge(-1f) },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xCC1A1020),
                        contentColor = CrimeGoldLight
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(CrimeGold, CrimeCrimsonDark))),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("dodge_left_button")
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("УКЛОН", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Right Dodge Button
                FilledTonalButton(
                    onClick = { performDodge(1f) },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xCC1A1020),
                        contentColor = CrimeGoldLight
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(CrimeCrimsonDark, CrimeGold))),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("dodge_right_button")
                ) {
                    Text("УКЛОН", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }

        // 9. LAYER: ARCADE BOTTOM ACTION CONTROLS HUD (Action Buttons Bar)
        if (!showdown.isFinished) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .shadow(16.dp),
                color = Color(0xF20D0914),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(CrimeCrimsonDark, CrimeGold, CrimeCrimsonDark))
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    // Row 1: Primary Brawler Moves
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Punch / Hook
                        ArcadeActionButton(
                            title = "В ЧЕЛЮСТЬ",
                            subtitle = "Удар кулаком",
                            icon = Icons.Default.SportsMma,
                            primaryColor = CrimeCrimsonLight,
                            onClick = { triggerPlayerPunch(isRight = true) },
                            modifier = Modifier.weight(1f).testTag("action_punch")
                        )

                        // Pistol Shot
                        ArcadeActionButton(
                            title = "ПАЛЬНУТЬ",
                            subtitle = if (showdown.ammoCount > 0) "${showdown.ammoCount} патр." else "Нет патронов",
                            icon = Icons.Default.GpsFixed,
                            primaryColor = if (showdown.ammoCount > 0) CrimeGoldLight else Color.Gray,
                            enabled = showdown.ammoCount > 0,
                            onClick = { triggerPlayerShoot() },
                            modifier = Modifier.weight(1f).testTag("action_shoot")
                        )

                        // Block / Guard
                        ArcadeActionButton(
                            title = "БЛОК",
                            subtitle = "Защита -60%",
                            icon = Icons.Default.Shield,
                            primaryColor = CrimePoliceBlue,
                            onClick = { triggerPlayerGuard() },
                            modifier = Modifier.weight(1f).testTag("action_guard")
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Row 2: Secondary / Tactical Support Moves
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // First Aid
                        ArcadeActionButton(
                            title = "АПТЕЧКА",
                            subtitle = if (showdown.firstAidKits > 0) "${showdown.firstAidKits} шт." else "Пусто",
                            icon = Icons.Default.MedicalServices,
                            primaryColor = if (showdown.firstAidKits > 0) CrimeCashGreen else Color.Gray,
                            enabled = showdown.firstAidKits > 0,
                            onClick = { triggerPlayerHeal() },
                            modifier = Modifier.weight(1f).testTag("action_heal")
                        )

                        // Super Rage Combo
                        ArcadeActionButton(
                            title = "ЯРОСТЬ",
                            subtitle = "${showdown.rageMeter}%",
                            icon = Icons.Default.Bolt,
                            primaryColor = if (showdown.rageMeter >= 100) Color(0xFFFFD600) else Color.DarkGray,
                            enabled = showdown.rageMeter >= 100,
                            onClick = { triggerPlayerRage() },
                            modifier = Modifier.weight(1f).testTag("action_rage")
                        )

                        // Call Crew
                        ArcadeActionButton(
                            title = "БРАТВА",
                            subtitle = "Подмога",
                            icon = Icons.Default.Groups,
                            primaryColor = CrimeGold,
                            onClick = { onAction(ShowdownAction.CALL_CREW) },
                            modifier = Modifier.weight(1f).testTag("action_crew_help")
                        )
                    }
                }
            }
        }
    }
}

/**
 * Polished Fighting Game Arcade Button.
 */
@Composable
fun ArcadeActionButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    primaryColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        color = if (enabled) Color(0xFF1E1528) else Color(0xFF14101A),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                listOf(
                    primaryColor.copy(alpha = if (enabled) 0.8f else 0.2f),
                    primaryColor.copy(alpha = if (enabled) 0.3f else 0.1f)
                )
            )
        ),
        modifier = modifier.height(44.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (enabled) primaryColor else Color.Gray,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = if (enabled) Color.White else Color.Gray,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    fontSize = 9.sp,
                    color = if (enabled) primaryColor else Color.DarkGray,
                    maxLines = 1
                )
            }
        }
    }
}
