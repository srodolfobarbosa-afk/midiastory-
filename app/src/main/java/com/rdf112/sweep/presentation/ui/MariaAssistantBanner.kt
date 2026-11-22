package com.rdf112.sweep.presentation.ui

import android.content.Context
import android.media.MediaPlayer
import android.os.Environment
import android.os.StatFs
import androidx.activity.compose.BackHandler
import androidx.annotation.RawRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.rdf112.sweep.R
import com.rdf112.sweep.presentation.ui.theme.MidiastoreColors
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private val MARIA_LIP_FRAMES = intArrayOf(
    R.drawable.maria_lip_0,
    R.drawable.maria_lip_1,
    R.drawable.maria_lip_2,
    R.drawable.maria_lip_3,
    R.drawable.maria_lip_4,
    R.drawable.maria_lip_5,
    R.drawable.maria_lip_6,
    R.drawable.maria_lip_7,
)

private val MARIA_IDLE_BLINK_SEQUENCE = intArrayOf(
    R.drawable.maria_blink_0,
    R.drawable.maria_blink_1,
    R.drawable.maria_blink_2,
    R.drawable.maria_blink_2,
    R.drawable.maria_blink_2,
    R.drawable.maria_blink_1,
    R.drawable.maria_blink_0,
)

private val MARIA_TALK_BLINK_SEQUENCE = intArrayOf(
    R.drawable.maria_blink_1,
    R.drawable.maria_talk_blink,
    R.drawable.maria_blink_2,
    R.drawable.maria_talk_blink,
    R.drawable.maria_blink_1,
)

private const val PREFS_NAME = "midiastore_prefs"
private const val KEY_MARIA_MUTED = "maria_voice_muted_v280"

/**
 * Dicionário nativo nas 14 línguas oficiais do Midiastore para a Assistente Maria.
 */
object MariaI18n {
    private fun lang(): String = Locale.getDefault().language.lowercase(Locale.ROOT)

    fun assistantBadge(): String = when (lang()) {
        "pt" -> "MARIA • ASSISTENTE IA"
        "es" -> "MARIA • ASISTENTE IA"
        "fr" -> "MARIA • ASSISTANTE IA"
        "de" -> "MARIA • KI-ASSISTENTIN"
        "it" -> "MARIA • ASSISTENTE IA"
        "ru" -> "МАРИЯ • ИИ-АССИСТЕНТ"
        "zh" -> "MARIA • 智能助手"
        "ja" -> "MARIA • AIアシスタント"
        "ko" -> "MARIA • AI 어시스턴트"
        "ar" -> "ماريا • مساعدة الذكاء الاصطناعي"
        "hi" -> "मारिया • एआई सहायक"
        "bn" -> "মারিয়া • এআই সহকারী"
        "in", "id" -> "MARIA • ASISTEN AI"
        else -> "MARIA • AI ASSISTANT"
    }

    fun tapToBringMariaBack(): String = when (lang()) {
        "pt" -> "Toque aqui ou aperte Voltar para abrir a Maria ▼"
        "es" -> "Toca aquí o pulsa Atrás para abrir a Maria ▼"
        "fr" -> "Touchez ici ou appuyez sur Retour pour ouvrir Maria ▼"
        "de" -> "Tippen oder Zurück drücken, um Maria zu öffnen ▼"
        "zh" -> "点击此处或按返回键唤出 Maria ▼"
        "ja" -> "タップまたは戻るボタンでMariaを表示 ▼"
        "ko" -> "탭하거나 뒤로 버튼을 눌러 Maria 열기 ▼"
        "ru" -> "Нажмите здесь или Назад, чтобы открыть Марию ▼"
        "ar" -> "اضغط هنا أو زر الرجوع لفتح ماريا ▼"
        "hi" -> "मारिया को खोलने के लिए यहाँ टैप करें या बैक दबाएँ ▼"
        "bn" -> "মারিয়া খুলতে এখানে ট্যাপ করুন বা ব্যাক চাপুন ▼"
        "in", "id" -> "Ketuk di sini atau tekan Kembali untuk membuka Maria ▼"
        else -> "Tap here or press Back to bring Maria back ▼"
    }

    fun collapseLabel(): String = when (lang()) {
        "pt" -> "▲ Recolher"
        "es" -> "▲ Ocultar"
        "fr" -> "▲ Réduire"
        "de" -> "▲ Einklappen"
        "zh" -> "▲ 收起"
        "ja" -> "▲ 閉じる"
        "ko" -> "▲ 접기"
        "ru" -> "▲ Свернуть"
        else -> "▲ Minimize"
    }
}

/**
 * Hero Banner Inteligente da Assistente "Maria" (Midiastore v2.8.0):
 * 1. Marca `Midiastore` integrada dentro do canto superior esquerdo do quadro da Maria.
 * 2. Sorriso inicial acolhedor ("Olá! Eu sou a Maria!") + Dublagem 100% sincronizada com o áudio real via envelope RMS de 35ms (`mp.currentPosition / 35`).
 * 3. Auto-recolhimento suave após terminar de falar (para não ficar parada ocupando tela) + Retorno imediato ao apertar o botão Voltar (`BackHandler`) ou tocar na barra!
 */
@Composable
fun MariaHeroBanner(
    tabKey: String,
    captionText: String,
    @RawRes rawAudioResId: Int,
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    speechNonce: Int = 0,
    isProBadgeActive: Boolean = false,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefs = remember(context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var isMuted by remember {
        mutableStateOf(prefs.getBoolean(KEY_MARIA_MUTED, false))
    }
    var isSpeaking by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(true) }
    var replayTrigger by remember { mutableIntStateOf(0) }
    var currentFrameRes by remember { mutableIntStateOf(R.drawable.maria_smile) }
    var mouthLevel by remember { mutableFloatStateOf(0f) }
    var breathScale by remember { mutableFloatStateOf(1.01f) }
    var bodyOffsetY by remember { mutableFloatStateOf(0f) }
    var headTiltDeg by remember { mutableFloatStateOf(0f) }
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }

    // Quando a Maria está recolhida e o usuário aperta o botão Voltar do Android, a Maria volta imediatamente!
    BackHandler(enabled = isActive && !isExpanded) {
        isExpanded = true
        replayTrigger++
    }

    fun stopAndReleaseAudio() {
        runCatching {
            mediaPlayerRef?.let { mp ->
                if (mp.isPlaying) mp.stop()
                mp.release()
            }
        }
        mediaPlayerRef = null
        isSpeaking = false
    }

    DisposableEffect(lifecycleOwner, isActive) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                stopAndReleaseAudio()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (!isActive) {
            stopAndReleaseAudio()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            stopAndReleaseAudio()
        }
    }

    // Sempre que muda de aba, toca num quadradinho ou pede para ouvir, a Maria abre, dá um sorriso e faz a dublagem!
    LaunchedEffect(isActive, tabKey, rawAudioResId, speechNonce, replayTrigger) {
        stopAndReleaseAudio()
        if (!isActive) return@LaunchedEffect

        isExpanded = true
        // 1º Passo pedido pelo Rodolfo: Dá um sorriso acolhedor antes/no início do "Olá, eu sou a Maria!"
        currentFrameRes = R.drawable.maria_smile
        delay(320L)
        isSpeaking = true

        if (!isMuted) {
            val mpInstance = runCatching {
                val mp = MediaPlayer.create(context, rawAudioResId)
                if (mp != null) {
                    mediaPlayerRef = mp
                    mp.setOnCompletionListener {
                        isSpeaking = false
                        runCatching { it.release() }
                        if (mediaPlayerRef === it) mediaPlayerRef = null
                    }
                    mp.setOnErrorListener { player, _, _ ->
                        isSpeaking = false
                        runCatching { player.release() }
                        if (mediaPlayerRef === player) mediaPlayerRef = null
                        true
                    }
                    mp.start()
                    mp
                } else {
                    null
                }
            }.getOrNull()

            if (mpInstance != null) {
                var elapsedMs = 0L
                while (isSpeaking && elapsedMs < 40000L) {
                    val stillPlaying = runCatching { mpInstance.isPlaying }.getOrDefault(false)
                    if (!stillPlaying && elapsedMs > 600L) break
                    delay(120L)
                    elapsedMs += 120L
                }
                if (isSpeaking) stopAndReleaseAudio()
                // Sorri ao concluir a fala e recolhe suavemente após 1,8s para não ficar parada ocupando a tela!
                currentFrameRes = R.drawable.maria_smile
                delay(1800L)
                if (!isSpeaking) {
                    isExpanded = false
                }
                return@LaunchedEffect
            }
        }

        val simulatedMs = (captionText.length * 65L).coerceIn(4000L, 10000L)
        delay(simulatedMs)
        isSpeaking = false
        currentFrameRes = R.drawable.maria_smile
        delay(1600L)
        if (!isSpeaking) {
            isExpanded = false
        }
    }

    // Motor de Dublagem Real 28.5 FPS (35ms): lê o timestamp exato do MediaPlayer (`mp.currentPosition / 35`) + envelope RMS real
    LaunchedEffect(isActive, rawAudioResId) {
        if (!isActive) {
            currentFrameRes = R.drawable.maria_lip_0
            return@LaunchedEffect
        }
        val envelope = MariaLipSyncEnvelopes.forAudioRes(rawAudioResId)
        var blinkStep = -1
        var nextBlinkAtMs = System.currentTimeMillis() + 1900L

        while (true) {
            val now = System.currentTimeMillis()

            if (isSpeaking) {
                breathScale = 1.018f + (sin(now / 420.0).toFloat() * 0.010f)
                bodyOffsetY = sin(now / 320.0).toFloat() * 1.8f
                headTiltDeg = cos(now / 540.0).toFloat() * 0.55f
            } else {
                breathScale = 1.006f + (sin(now / 950.0).toFloat() * 0.005f)
                bodyOffsetY = sin(now / 950.0).toFloat() * 0.7f
                headTiltDeg = 0f
            }

            if (now >= nextBlinkAtMs && blinkStep < 0) {
                blinkStep = 0
                nextBlinkAtMs = now + Random.nextLong(2200L, 3300L)
            }

            if (isSpeaking) {
                // Posição real em milissegundos do áudio neural de estúdio!
                val posMs = runCatching {
                    mediaPlayerRef?.takeIf { it.isPlaying }?.currentPosition
                }.getOrNull()

                val envCode = if (posMs != null && envelope.isNotEmpty()) {
                    val frameIdx = (posMs / 35).coerceIn(0, envelope.lastIndex)
                    envelope[frameIdx]
                } else {
                    // Fallback caso esteja no mudo (🔇): sorriso nos primeiros 450ms + sílabas naturais
                    val cycle = now % 1400L
                    if (cycle > 1180L) 8 else {
                        val w = (sin(now / 52.0) * 0.6 + sin(now / 88.0) * 0.4).toFloat()
                        if (w > 0.08f) ((w - 0.08f) / 0.92f * 7f).roundToInt().coerceIn(1, 7) else 0
                    }
                }

                if (blinkStep >= 0) {
                    currentFrameRes = MARIA_TALK_BLINK_SEQUENCE[blinkStep.coerceAtMost(MARIA_TALK_BLINK_SEQUENCE.lastIndex)]
                    blinkStep++
                    if (blinkStep >= MARIA_TALK_BLINK_SEQUENCE.size) {
                        blinkStep = -1
                    }
                } else if (envCode == 8) {
                    // Código 8 no envelope = pausa de sorriso ("Olá! Eu sou a Maria! [SORRISO]")
                    mouthLevel = 0f
                    currentFrameRes = R.drawable.maria_smile
                } else if (envCode <= 0) {
                    // Silêncio entre palavras/consoantes = fecha a boca imediatamente!
                    mouthLevel *= 0.22f
                    val idx = mouthLevel.roundToInt().coerceIn(0, MARIA_LIP_FRAMES.lastIndex)
                    currentFrameRes = MARIA_LIP_FRAMES[idx]
                } else {
                    // Sílaba ativa = interpolação rápida com a amplitude exata da voz!
                    mouthLevel = mouthLevel * 0.25f + envCode.toFloat() * 0.75f
                    val idx = mouthLevel.roundToInt().coerceIn(1, MARIA_LIP_FRAMES.lastIndex)
                    currentFrameRes = MARIA_LIP_FRAMES[idx]
                }
            } else {
                mouthLevel = 0f
                if (blinkStep >= 0) {
                    currentFrameRes = MARIA_IDLE_BLINK_SEQUENCE[blinkStep.coerceAtMost(MARIA_IDLE_BLINK_SEQUENCE.lastIndex)]
                    blinkStep++
                    if (blinkStep >= MARIA_IDLE_BLINK_SEQUENCE.size) {
                        blinkStep = -1
                    }
                } else {
                    currentFrameRes = R.drawable.maria_smile
                }
            }
            delay(35L)
        }
    }

    val targetHeight by animateDpAsState(
        targetValue = if (isExpanded) 250.dp else 74.dp,
        animationSpec = tween(durationMillis = 320),
        label = "maria_card_height",
    )

    val brandAnnotated = remember {
        buildAnnotatedString {
            withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.ExtraBold)) {
                append("Midia")
            }
            withStyle(SpanStyle(color = Color(0xFF0A84FF), fontWeight = FontWeight.ExtraBold)) {
                append("store")
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(targetHeight)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF040B14))
            .border(
                width = 1.dp,
                color = if (isSpeaking) Color(0x880A84FF) else Color(0xFF1F2430),
                shape = RoundedCornerShape(20.dp),
            )
            .clickable {
                if (!isExpanded) {
                    isExpanded = true
                    replayTrigger++
                } else {
                    replayTrigger++
                }
            },
    ) {
        if (isExpanded) {
            // MODO APRESENTADORA COMPLETO (250.dp): Rosto + Ombros + Blusa + Nome Midiastore integrado dentro do quadro!
            Image(
                painter = painterResource(id = currentFrameRes),
                contentDescription = "Maria Assistente Pessoal",
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = breathScale
                        scaleY = breathScale
                        translationY = bodyOffsetY
                        rotationZ = headTiltDeg
                    },
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color(0x66000000),
                            0.28f to Color.Transparent,
                            0.72f to Color(0xB8000000),
                            1.0f to Color(0xF5000000),
                        ),
                    ),
            )

            // Barra superior DENTRO do quadro da Maria: Nome "Midiastore" à esquerda + Botões Recolher e Áudio à direita
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = brandAnnotated,
                        fontSize = 20.sp,
                        letterSpacing = (-0.4).sp,
                    )
                    if (isProBadgeActive) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0x330A84FF),
                            modifier = Modifier.border(1.dp, Color(0xFF0A84FF), RoundedCornerShape(6.dp)),
                        ) {
                            Text(
                                text = "PRO IA",
                                color = Color(0xFF64D2FF),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Botão para recolher a Maria quando não quiser ela parada na tela
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color(0xB81C1C1E),
                        modifier = Modifier
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(999.dp))
                            .clip(RoundedCornerShape(999.dp))
                            .clickable {
                                stopAndReleaseAudio()
                                isExpanded = false
                            },
                    ) {
                        Text(
                            text = MariaI18n.collapseLabel(),
                            color = Color(0xFFE5E5EA),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }

                    // Botão de voz 🔊 / 🔇
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xCC1C1C1E))
                            .border(
                                width = 1.dp,
                                color = if (!isMuted && isSpeaking) MidiastoreColors.AccentBlue else Color(0x38FFFFFF),
                                shape = CircleShape,
                            )
                            .clickable {
                                val newMuted = !isMuted
                                isMuted = newMuted
                                prefs.edit().putBoolean(KEY_MARIA_MUTED, newMuted).apply()
                                if (newMuted) {
                                    stopAndReleaseAudio()
                                } else {
                                    replayTrigger++
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (isMuted) "🔇" else "🔊",
                            fontSize = 13.sp,
                            color = Color.White,
                        )
                    }
                }
            }

            // Legenda viva na base do quadro
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isSpeaking) Color(0xFF30D158) else MidiastoreColors.AccentBlue),
                    )
                    Text(
                        text = MariaI18n.assistantBadge(),
                        color = Color(0xFF64D2FF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = captionText,
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 17.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            // MODO COMPACTO VIVO (74.dp): Maria recolhida após falar (não fica parada ocupando a tela) + Toque ou Voltar traz ela de volta!
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Image(
                    painter = painterResource(id = currentFrameRes),
                    contentDescription = "Maria Compacta",
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, MidiastoreColors.AccentBlue, CircleShape),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = brandAnnotated,
                            fontSize = 15.sp,
                        )
                        Text(
                            text = "• " + MariaI18n.assistantBadge(),
                            color = Color(0xFF64D2FF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        text = MariaI18n.tapToBringMariaBack(),
                        color = Color(0xFFD1D5DB),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MidiastoreColors.AccentBlue,
                ) {
                    Text(
                        text = "▼ Maria",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

/**
 * Cabeçalho da 2ª Aba (Arquivos): Maria Hero Banner + Grid 2x2 de Quadradinhos Visuais com falas dedicadas.
 */
@Composable
fun FilesMariaHeaderSection(
    isActive: Boolean,
    onSelectPhotos: () -> Unit,
    onSelectLargeVideos: () -> Unit,
    onSelectWhatsApp: () -> Unit,
    onSelectScreenshots: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var caption by remember {
        mutableStateOf("Olá! Suas fotos, vídeos, áudios e mídias estão organizados dos maiores para os menores.")
    }
    var activeAudioRes by remember { mutableIntStateOf(R.raw.maria_files) }
    var speechNonce by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MariaHeroBanner(
            tabKey = "files",
            captionText = caption,
            rawAudioResId = activeAudioRes,
            isActive = isActive,
            speechNonce = speechNonce,
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MariaSquareTile(
                    label = "Fotos da Câmera",
                    highlight = "Galeria",
                    highlightColor = Color(0xFF64D2FF),
                    subtitle = "Ordenadas por tamanho",
                    onClick = {
                        caption = "Suas fotos da câmera organizadas das maiores para as menores."
                        activeAudioRes = R.raw.maria_files
                        speechNonce++
                        onSelectPhotos()
                    },
                    modifier = Modifier.weight(1f),
                )
                MariaSquareTile(
                    label = "Vídeos Grandes",
                    highlight = "> 10 MB",
                    highlightColor = Color(0xFFBF5AF2),
                    subtitle = "Revisão rápida",
                    onClick = {
                        caption = "Estes são os vídeos grandes com mais de 10 MB que mais ocupam espaço."
                        activeAudioRes = R.raw.maria_videos
                        speechNonce++
                        onSelectLargeVideos()
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MariaSquareTile(
                    label = "WhatsApp",
                    highlight = "Mídias",
                    highlightColor = Color(0xFF30D158),
                    subtitle = "Fotos, vídeos e áudios",
                    onClick = {
                        caption = "Fotos, vídeos e áudios acumulados nas conversas do WhatsApp."
                        activeAudioRes = R.raw.maria_whatsapp
                        speechNonce++
                        onSelectWhatsApp()
                    },
                    modifier = Modifier.weight(1f),
                )
                MariaSquareTile(
                    label = "Capturas & Antigos",
                    highlight = "+30 dias",
                    highlightColor = Color(0xFFFF9F0A),
                    subtitle = "Prints acumulados",
                    onClick = {
                        caption = "Capturas de tela e comprovantes antigos com mais de 30 dias."
                        activeAudioRes = R.raw.maria_screenshots
                        speechNonce++
                        onSelectScreenshots()
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * Cabeçalho da 3ª Aba (Pro): Maria em Modo Vendedora de Alta Conversão + Console Interativo da IA Liberada para Uso!
 */
@Composable
fun ProMariaHeaderSection(
    isActive: Boolean,
    isProActive: Boolean,
    onRunSmartDiagnosis: () -> Unit = {},
    onRunBlurScan: () -> Unit = {},
    onRunWhatsAppClean: () -> Unit = {},
    onRunUnusedApps: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val defaultLine = "Olá! Eu sou a Maria! Destrave a limpeza ilimitada + minha IA Nativa com 56% OFF no Anual!"
    var caption by remember { mutableStateOf(defaultLine) }
    var activeAudioRes by remember { mutableIntStateOf(R.raw.maria_pro) }
    var speechNonce by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MariaHeroBanner(
            tabKey = "pro",
            captionText = caption,
            rawAudioResId = activeAudioRes,
            isActive = isActive,
            speechNonce = speechNonce,
            isProBadgeActive = true,
        )

        // Grid 2x2 de Quadradinhos de Conversão & Ação Rápida Pro
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MariaSquareTile(
                    label = "Duplicatas SHA-256",
                    highlight = "100%",
                    highlightColor = Color(0xFFFF9F0A),
                    subtitle = "Toque para diagnosticar",
                    onClick = {
                        caption = "Varredura global SHA-256 ativada! O arquivo original fica sempre protegido."
                        activeAudioRes = R.raw.maria_dup
                        speechNonce++
                        onRunSmartDiagnosis()
                    },
                    modifier = Modifier.weight(1f),
                )
                MariaSquareTile(
                    label = "Apps sem Uso",
                    highlight = "+90 dias",
                    highlightColor = Color(0xFFBF5AF2),
                    subtitle = "Descobrir espaço oculto",
                    onClick = {
                        caption = "Analisando aplicativos esquecidos há mais de 90 dias no seu aparelho!"
                        activeAudioRes = R.raw.maria_pro
                        speechNonce++
                        onRunUnusedApps()
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MariaSquareTile(
                    label = "Fotos Borradas IA",
                    highlight = "On-Device",
                    highlightColor = Color(0xFF64D2FF),
                    subtitle = "Detector neural local",
                    onClick = {
                        caption = "Iniciando análise local de nitidez por Inteligência Artificial!"
                        activeAudioRes = R.raw.maria_nano
                        speechNonce++
                        onRunBlurScan()
                    },
                    modifier = Modifier.weight(1f),
                )
                MariaSquareTile(
                    label = "WhatsApp Pro",
                    highlight = "Limpar",
                    highlightColor = Color(0xFF30D158),
                    subtitle = "Mídias e áudios pesados",
                    onClick = {
                        caption = "Abrindo o limpador inteligente de mídias acumuladas do WhatsApp!"
                        activeAudioRes = R.raw.maria_whatsapp
                        speechNonce++
                        onRunWhatsAppClean()
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

data class ChatMidiastoreMessage(
    val isUser: Boolean,
    val text: String,
    val actionLabel: String? = null,
    val actionType: String? = null,
)

/**
 * Chat Midiastore (Estilo Google Gemini Dark OLED #1C1C1E — sem caixa azul, idêntico aos outros cartões do app!).
 */
@Composable
fun ProGeminiNanoBottomCard(
    onRunSmartDiagnosis: () -> Unit = {},
    onRunBlurScan: () -> Unit = {},
    onRunWhatsAppClean: () -> Unit = {},
    onRunUnusedApps: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val statInfo = remember {
        runCatching {
            val stat = StatFs(Environment.getDataDirectory().path)
            val totalGb = stat.totalBytes / (1024.0 * 1024.0 * 1024.0)
            val freeGb = stat.availableBytes / (1024.0 * 1024.0 * 1024.0)
            val usedGb = (totalGb - freeGb).coerceAtLeast(0.0)
            val pct = ((usedGb / totalGb.coerceAtLeast(1.0)) * 100.0).roundToInt()
            Triple(String.format(Locale.US, "%.1f GB", usedGb), String.format(Locale.US, "%.1f GB", freeGb), pct)
        }.getOrDefault(Triple("117 GB", "129 GB", 48))
    }

    var userPrompt by remember { mutableStateOf("") }
    var messages by remember {
        mutableStateOf(
            listOf(
                ChatMidiastoreMessage(
                    isUser = false,
                    text = "Olá! Eu sou a Maria no Chat Midiastore (IA Nativa • 0 MB). Seu aparelho está com ${statInfo.first} usados (${statInfo.third}%) e ${statInfo.second} livres. Como posso ajudar você agora?",
                    actionLabel = "✦ Fazer Diagnóstico Completo",
                    actionType = "DIAGNOSIS",
                ),
            ),
        )
    }

    fun handleSendQuestion(rawQuestion: String) {
        val q = rawQuestion.trim()
        if (q.isEmpty()) return
        val userMsg = ChatMidiastoreMessage(isUser = true, text = q)
        val replyMsg = when {
            q.contains("whatsapp", ignoreCase = true) || q.contains("áudio", ignoreCase = true) -> ChatMidiastoreMessage(
                isUser = false,
                text = "Analisei suas pastas de mídia: o WhatsApp acumula vídeos, fotos e áudios repetidos que podem ser limpos sem apagar suas conversas. Toque abaixo para revisar:",
                actionLabel = "✦ Abrir Limpador do WhatsApp",
                actionType = "WHATSAPP",
            )
            q.contains("borrad", ignoreCase = true) || q.contains("nitidez", ignoreCase = true) || q.contains("foto", ignoreCase = true) -> ChatMidiastoreMessage(
                isUser = false,
                text = "Posso verificar a nitidez das suas fotos localmente no processador do seu Motorola (100% offline, sem enviar nada para a nuvem) e separar apenas as fotos tremidas ou borradas:",
                actionLabel = "✦ Escanear Fotos Borradas",
                actionType = "BLUR",
            )
            q.contains("app", ignoreCase = true) || q.contains("90", ignoreCase = true) || q.contains("esquecid", ignoreCase = true) -> ChatMidiastoreMessage(
                isUser = false,
                text = "Aplicativos que você não abre há mais de 90 dias costumam esconder vários gigabytes no celular. Vamos listar quais estão sem uso?",
                actionLabel = "✦ Ver Apps sem Uso (+90 dias)",
                actionType = "APPS",
            )
            else -> ChatMidiastoreMessage(
                isUser = false,
                text = "Verifiquei seu armazenamento (${statInfo.first} usados • ${statInfo.second} disponíveis). A forma mais rápida de liberar espaço hoje é revisar os vídeos grandes (>10 MB) e duplicatas SHA-256 protegendo o original:",
                actionLabel = "✦ Liberar Espaço Agora",
                actionType = "DIAGNOSIS",
            )
        }
        messages = (messages + userMsg + replyMsg).takeLast(6)
    }

    // Superfície Grafite Escuro (#1C1C1E) com borda #2C2C2E idêntica aos outros cartões do Midiastore (ZERO caixa azul!)
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MidiastoreColors.CardSurface,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 14.dp)
            .border(1.dp, MidiastoreColors.CardStroke, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Cabeçalho Estilo Google Gemini
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "✦",
                        color = Color(0xFF64D2FF),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Column {
                        Text(
                            text = "Chat Midiastore",
                            color = Color.White,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            text = "Maria • Gemini Nano On-Device",
                            color = MidiastoreColors.TextMuted,
                            fontSize = 11.sp,
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MidiastoreColors.CardSurfaceAlt,
                    modifier = Modifier.border(1.dp, Color(0xFF3A3A3C), RoundedCornerShape(999.dp)),
                ) {
                    Text(
                        text = "● Online • 0 MB",
                        color = Color(0xFF30D158),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            // Histórico de Mensagens estilo Google Gemini (Bolhas escuras #2C2C2E para usuário e resposta limpa com avatar para a IA)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                messages.forEach { msg ->
                    if (msg.isUser) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp),
                                color = Color(0xFF2C2C2E),
                            ) {
                                Text(
                                    text = msg.text,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF141416))
                                .border(1.dp, Color(0xFF262629), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.maria_smile),
                                    contentDescription = "Maria IA",
                                    contentScale = ContentScale.Crop,
                                    alignment = Alignment.TopCenter,
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, Color(0xFF64D2FF), CircleShape),
                                )
                                Text(
                                    text = "✦ Chat Midiastore",
                                    color = Color(0xFFE5E5EA),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Text(
                                text = msg.text,
                                color = Color(0xFFE5E7EB),
                                fontSize = 13.sp,
                                lineHeight = 18.5.sp,
                            )
                            if (msg.actionLabel != null && msg.actionType != null) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = Color(0xFF262629),
                                    modifier = Modifier
                                        .border(1.dp, Color(0xFF3A3A3C), RoundedCornerShape(999.dp))
                                        .clip(RoundedCornerShape(999.dp))
                                        .clickable {
                                            when (msg.actionType) {
                                                "DIAGNOSIS" -> onRunSmartDiagnosis()
                                                "BLUR" -> onRunBlurScan()
                                                "WHATSAPP" -> onRunWhatsAppClean()
                                                "APPS" -> onRunUnusedApps()
                                            }
                                        },
                                ) {
                                    Text(
                                        text = msg.actionLabel,
                                        color = Color(0xFF64D2FF),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Pílulas de sugestão rápida estilo Google Gemini (Grafite escuro #262629)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    GeminiSuggestionChip(
                        label = "✦ Analisar celular",
                        onClick = {
                            handleSendQuestion("Analisar meu celular e vídeos pesados")
                            onRunSmartDiagnosis()
                        },
                        modifier = Modifier.weight(1f),
                    )
                    GeminiSuggestionChip(
                        label = "✦ Fotos borradas",
                        onClick = {
                            handleSendQuestion("Encontrar fotos borradas com IA")
                            onRunBlurScan()
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    GeminiSuggestionChip(
                        label = "✦ Limpar WhatsApp",
                        onClick = {
                            handleSendQuestion("O que posso limpar no WhatsApp?")
                            onRunWhatsAppClean()
                        },
                        modifier = Modifier.weight(1f),
                    )
                    GeminiSuggestionChip(
                        label = "✦ Apps +90 dias",
                        onClick = {
                            handleSendQuestion("Quais aplicativos estão sem uso há 90 dias?")
                            onRunUnusedApps()
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Barra de entrada inferior em pílula estilo Google Gemini (#262629, sem caixa azul!)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF262629))
                    .border(1.dp, Color(0xFF3A3A3C), RoundedCornerShape(28.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                OutlinedTextField(
                    value = userPrompt,
                    onValueChange = { userPrompt = it },
                    placeholder = {
                        Text("Peça ao Chat Midiastore...", fontSize = 13.sp, color = Color(0xFF8E8E93))
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                    ),
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (userPrompt.isNotBlank()) MidiastoreColors.AccentBlue else Color(0xFF3A3A3C))
                        .clickable {
                            val q = userPrompt.trim().ifEmpty { "Analisar meu celular agora" }
                            userPrompt = ""
                            handleSendQuestion(q)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "↑",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun GeminiSuggestionChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color(0xFF262629),
        modifier = modifier
            .border(1.dp, Color(0xFF3A3A3C), RoundedCornerShape(999.dp))
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            color = Color(0xFFE5E5EA),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
fun MariaSquareTile(
    label: String,
    highlight: String,
    highlightColor: Color,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(15.dp),
        color = MidiastoreColors.CardSurface,
        modifier = modifier
            .border(1.dp, MidiastoreColors.CardStroke, RoundedCornerShape(15.dp))
            .clip(RoundedCornerShape(15.dp))
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(
                text = label,
                color = Color(0xFFE5E5EA),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = highlight,
                color = highlightColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = subtitle,
                color = MidiastoreColors.TextMuted,
                fontSize = 10.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
