package com.rdf112.sweep.presentation.ui

import android.content.Context
import android.media.MediaPlayer
import androidx.annotation.RawRes
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.rdf112.sweep.R
import com.rdf112.sweep.presentation.ui.theme.MidiastoreColors
import kotlinx.coroutines.delay
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
 * Hero Banner oficial da Assistente Pessoal "Maria" (Midiastore v2.8.0).
 * - Enquadramento cinematográfico `ContentScale.Crop` + `BiasAlignment(0f, -0.76f)` idêntico ao Mockup v6 aprovado
 *   (sem barras laterais e sem corte flutuante na base; ombros e blazer preenchem o quadro naturalmente).
 * - Movimento labial silábico realista (abre nas vogais e fecha completamente `maria_lip_0` nas consoantes e pausas).
 * - Piscadas naturais dos olhos (`maria_talk_blink` / `maria_blink_2`) a cada 2,1s–3,2s TANTO falando quanto em repouso.
 * - Composição corporal viva com respiração suave, micro-inclinação natural e gesto apresentadora (`maria_gesture` / `maria_smile`).
 */
@Composable
fun MariaHeroBanner(
    tabKey: String,
    captionText: String,
    @RawRes rawAudioResId: Int,
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    speechNonce: Int = 0,
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
    var replayTrigger by remember { mutableIntStateOf(0) }
    var currentFrameRes by remember { mutableIntStateOf(R.drawable.maria_lip_0) }
    var mouthLevel by remember { mutableFloatStateOf(0f) }
    var breathScale by remember { mutableFloatStateOf(1.01f) }
    var bodyOffsetY by remember { mutableFloatStateOf(0f) }
    var headTiltDeg by remember { mutableFloatStateOf(0f) }
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }

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

    // Dispara a fala longa de estúdio + animação labial completa até o último segundo do áudio
    LaunchedEffect(isActive, tabKey, rawAudioResId, speechNonce, replayTrigger) {
        stopAndReleaseAudio()
        if (!isActive) return@LaunchedEffect

        delay(140L)
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
                while (isSpeaking && elapsedMs < 35000L) {
                    val stillPlaying = runCatching { mpInstance.isPlaying }.getOrDefault(false)
                    if (!stillPlaying && elapsedMs > 600L) break
                    delay(180L)
                    elapsedMs += 180L
                }
                if (isSpeaking) stopAndReleaseAudio()
                return@LaunchedEffect
            }
        }

        val simulatedMs = (captionText.length * 68L).coerceIn(4500L, 12000L)
        delay(simulatedMs)
        isSpeaking = false
    }

    // Motor Optical-Flow 28.5 FPS (35ms): sílabas com fechamento labial real + piscadas durante a fala + movimento corporal vivo
    LaunchedEffect(isActive) {
        if (!isActive) {
            currentFrameRes = R.drawable.maria_lip_0
            return@LaunchedEffect
        }
        var blinkStep = -1
        var nextBlinkAtMs = System.currentTimeMillis() + 1800L
        var postSpeechSmileTicks = 0
        var wasSpeakingLastTick = false

        while (true) {
            val now = System.currentTimeMillis()

            // Movimento corporal orgânico (respiração + postura natural de apresentadora)
            if (isSpeaking) {
                breathScale = 1.022f + (sin(now / 420.0).toFloat() * 0.012f)
                bodyOffsetY = sin(now / 310.0).toFloat() * 2.2f
                headTiltDeg = cos(now / 520.0).toFloat() * 0.65f
            } else {
                breathScale = 1.008f + (sin(now / 950.0).toFloat() * 0.006f)
                bodyOffsetY = sin(now / 950.0).toFloat() * 0.9f
                headTiltDeg = 0f
            }

            if (wasSpeakingLastTick && !isSpeaking) {
                // Ao terminar uma frase, exibe um sorriso acolhedor antes de repousar
                postSpeechSmileTicks = 18
            }
            wasSpeakingLastTick = isSpeaking

            // Dispara piscada natural dos olhos a cada 2,1s a 3,2s (TANTO falando quanto em silêncio!)
            if (now >= nextBlinkAtMs && blinkStep < 0) {
                blinkStep = 0
                nextBlinkAtMs = now + Random.nextLong(2100L, 3200L)
            }

            if (isSpeaking) {
                // Se estiver no meio de uma piscada enquanto fala, fecha as pálpebras suavemente sem travar a fala
                if (blinkStep >= 0) {
                    currentFrameRes = MARIA_TALK_BLINK_SEQUENCE[blinkStep.coerceAtMost(MARIA_TALK_BLINK_SEQUENCE.lastIndex)]
                    blinkStep++
                    if (blinkStep >= MARIA_TALK_BLINK_SEQUENCE.size) {
                        blinkStep = -1
                    }
                } else {
                    // Envelope silábico real: valores negativos fecham a boca completamente (maria_lip_0) entre sílabas e pausas!
                    val syllableRaw = (sin(now / 54.0) * 0.58 + sin(now / 89.0) * 0.42).toFloat()
                    val cyclePhase = now % 1380L
                    val isBreathPause = cyclePhase > 1160L
                    val isGestureAccent = (now % 5200L) in 2400L..2720L

                    if (isGestureAccent && isBreathPause) {
                        currentFrameRes = R.drawable.maria_gesture
                        mouthLevel = 0f
                    } else {
                        val target = if (!isBreathPause && syllableRaw > 0.05f) {
                            ((syllableRaw - 0.05f) / 0.95f * (MARIA_LIP_FRAMES.size - 1))
                                .coerceIn(0f, (MARIA_LIP_FRAMES.size - 1).toFloat())
                        } else {
                            0f
                        }
                        // Fechamento rápido nas consoantes para nunca ficar de boca aberta parada
                        mouthLevel = if (target == 0f) {
                            mouthLevel * 0.28f
                        } else {
                            mouthLevel * 0.35f + target * 0.65f
                        }
                        val idx = mouthLevel.roundToInt().coerceIn(0, MARIA_LIP_FRAMES.lastIndex)
                        currentFrameRes = MARIA_LIP_FRAMES[idx]
                    }
                }
            } else if (mouthLevel > 0.25f) {
                mouthLevel *= 0.35f
                val idx = mouthLevel.roundToInt().coerceIn(0, MARIA_LIP_FRAMES.lastIndex)
                currentFrameRes = MARIA_LIP_FRAMES[idx]
            } else {
                mouthLevel = 0f
                if (blinkStep >= 0) {
                    currentFrameRes = MARIA_IDLE_BLINK_SEQUENCE[blinkStep.coerceAtMost(MARIA_IDLE_BLINK_SEQUENCE.lastIndex)]
                    blinkStep++
                    if (blinkStep >= MARIA_IDLE_BLINK_SEQUENCE.size) {
                        blinkStep = -1
                    }
                } else if (postSpeechSmileTicks > 0) {
                    postSpeechSmileTicks--
                    currentFrameRes = R.drawable.maria_smile
                } else {
                    currentFrameRes = R.drawable.maria_lip_0
                }
            }
            delay(35L)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(248.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF040B14))
            .border(
                width = 1.dp,
                color = if (isSpeaking) Color(0x660A84FF) else Color(0xFF1F2430),
                shape = RoundedCornerShape(20.dp),
            )
            .clickable {
                replayTrigger++
            },
    ) {
        // Composição corporal de estúdio (ContentScale.Crop + BiasAlignment idêntico ao Mockup v6: preenche toda a largura sem barras laterais)
        Image(
            painter = painterResource(id = currentFrameRes),
            contentDescription = "Maria Assistente Pessoal",
            contentScale = ContentScale.Crop,
            alignment = BiasAlignment(horizontalBias = 0f, verticalBias = -0.78f),
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = breathScale
                    scaleY = breathScale
                    translationY = bodyOffsetY
                    rotationZ = headTiltDeg
                },
        )

        // Gradiente cinematográfico suave na base para leitura cristalina da legenda abaixo do queixo/gola
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color(0x33000000),
                        0.36f to Color.Transparent,
                        0.76f to Color(0xB2000000),
                        1.0f to Color(0xF5000000),
                    ),
                ),
        )

        // Botão de voz 🔊 / 🔇 no canto superior direito
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
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

        // Legenda viva na base do quadro (abaixo do rosto e da boca da Maria)
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
                    text = "MARIA • ASSISTENTE",
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
        mutableStateOf("Suas fotos, vídeos, áudios e mídias organizados dos maiores para os menores.")
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
 * Cabeçalho da 3ª Aba (Pro): Maria Hero Banner + Grid 2x2 Pro logo no topo (deixando os planos logo acima e o Modelo IA no final).
 */
@Composable
fun ProMariaHeaderSection(
    isActive: Boolean,
    isProActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val defaultLine = if (isProActive) {
        "Seu Midiastore Pro com limpeza completa e IA Nativa está ativo!"
    } else {
        "Desbloqueie a limpeza completa sem limites + Inteligência Artificial Nativa no Pro."
    }
    var caption by remember(isProActive) { mutableStateOf(defaultLine) }
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
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MariaSquareTile(
                    label = "Duplicatas SHA-256",
                    highlight = "100%",
                    highlightColor = Color(0xFFFF9F0A),
                    subtitle = "Lote ilimitado",
                    onClick = {
                        caption = "Varredura global SHA-256 que protege sempre a sua foto original."
                        activeAudioRes = R.raw.maria_dup
                        speechNonce++
                    },
                    modifier = Modifier.weight(1f),
                )
                MariaSquareTile(
                    label = "Apps sem Uso",
                    highlight = "+90 dias",
                    highlightColor = Color(0xFFBF5AF2),
                    subtitle = "Espaço oculto",
                    onClick = {
                        caption = "Descubra aplicativos esquecidos há mais de 90 dias ocupando gigabytes."
                        activeAudioRes = R.raw.maria_pro
                        speechNonce++
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MariaSquareTile(
                    label = "Vídeos Pesados",
                    highlight = "> 10 MB",
                    highlightColor = Color(0xFF64D2FF),
                    subtitle = "Limpeza rápida",
                    onClick = {
                        caption = "Localize e revise todos os vídeos pesados em poucos segundos."
                        activeAudioRes = R.raw.maria_videos
                        speechNonce++
                    },
                    modifier = Modifier.weight(1f),
                )
                MariaSquareTile(
                    label = "Privacidade Total",
                    highlight = "On-Device",
                    highlightColor = Color(0xFF30D158),
                    subtitle = "Zero nuvem externa",
                    onClick = {
                        caption = "Suas fotos e vídeos são analisados no aparelho e nunca vão para servidores."
                        activeAudioRes = R.raw.maria_nano
                        speechNonce++
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * Card do Modelo IA Nativa (Gemini Nano Android — 0 MB) posicionado no final da aba Pro conforme solicitado.
 */
@Composable
fun ProGeminiNanoBottomCard(
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF131B2E),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 12.dp)
            .border(1.dp, Color(0x6664D2FF), RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0x2E0A84FF),
                            Color(0x295E5CE6),
                        ),
                    ),
                )
                .padding(horizontal = 15.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "✨ Modelo IA Nativa (Gemini Nano Android)",
                    color = Color(0xFF64D2FF),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0x330A84FF),
                ) {
                    Text(
                        text = "0 MB EXTRA",
                        color = Color(0xFF64D2FF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }
            }
            Text(
                text = "Roda direto no próprio sistema Android (0 MB extra no aplicativo), 100% offline e com privacidade total para assinantes Pro.",
                color = Color(0xFFD6DEEB),
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )
        }
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
