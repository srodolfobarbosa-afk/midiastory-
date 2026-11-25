package com.rdf112.sweep.presentation.ui

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.os.Environment
import android.os.StatFs
import android.view.Surface
import android.view.TextureView
import androidx.activity.compose.BackHandler
import androidx.annotation.RawRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.runtime.withFrameNanos
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
import com.rdf112.sweep.data.maria.MariaCdnVideoCache
import com.rdf112.sweep.presentation.ui.theme.MidiastoreColors
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileInputStream
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private val MARIA_IDLE_BLINK_SEQUENCE = intArrayOf(
    R.drawable.maria_blink_0,
    R.drawable.maria_blink_1,
    R.drawable.maria_blink_2,
    R.drawable.maria_blink_2,
    R.drawable.maria_blink_2,
    R.drawable.maria_blink_1,
    R.drawable.maria_blink_0,
)

private const val PREFS_NAME = "midiastore_prefs"
private const val KEY_MARIA_MUTED = "maria_voice_muted_v280"

/**
 * Tradução 100% completa nas 14 línguas oficiais do Midiastore + Adaptação Regional de Mensageiros
 * (WhatsApp no Brasil/LatAm/Índia/Europa; Chats & Downloads nos EUA/Reino Unido; WeChat na China; LINE no Japão; KakaoTalk na Coreia; Telegram na Rússia).
 */
object MariaI18n {
    fun lang(): String = Locale.getDefault().language.lowercase(Locale.ROOT)
    fun isPt(): Boolean = lang().startsWith("pt")

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

    /** Nome regional inteligente para países que usam ou não usam WhatsApp! */
    fun socialChatTileTitle(): String = when (lang()) {
        "pt" -> "WhatsApp & Chats"
        "es" -> "WhatsApp y Chats"
        "hi" -> "WhatsApp और चैट"
        "bn" -> "WhatsApp ও চ্যাট"
        "in", "id" -> "WhatsApp & Chat"
        "ar" -> "واتساب والدردشات"
        "de" -> "WhatsApp & Chats"
        "it" -> "WhatsApp e Chat"
        "fr" -> "Chats & Réseaux"
        "ru" -> "Telegram и Чаты"
        "zh" -> "微信与聊天缓存"
        "ja" -> "LINE・SNSメディア"
        "ko" -> "카카오톡 및 채팅 미디어"
        else -> "Chats & Downloads"
    }

    fun socialChatTileSub(): String = when (lang()) {
        "pt" -> "Mídias e downloads"
        "es" -> "Videos y descargas"
        "fr" -> "Médias & fichiers"
        "de" -> "Medien & Downloads"
        "it" -> "Media e download"
        "ru" -> "Медиа и загрузки"
        "zh" -> "视频、图片与下载"
        "ja" -> "動画・ダウンロード"
        "ko" -> "동영상 및 다운로드"
        "ar" -> "الوسائط والتنزيلات"
        "hi" -> "मीडिया और डाउनलोड"
        "bn" -> "মিডিয়া ও ডাউনলোড"
        "in", "id" -> "Media & unduhan"
        else -> "Media & downloads"
    }

    fun tapToBringMariaBack(): String = when (lang()) {
        "pt" -> "Toque aqui ou aperte Voltar para abrir a Maria ▼"
        "es" -> "Toca aquí o pulsa Atrás para abrir a Maria ▼"
        "fr" -> "Touchez ici ou Retour pour ouvrir Maria ▼"
        "de" -> "Tippen oder Zurück, um Maria zu öffnen ▼"
        "zh" -> "点击此处或按返回键唤出 Maria ▼"
        "ja" -> "タップまたは戻るでMariaを表示 ▼"
        "ko" -> "탭하거나 뒤로 버튼으로 Maria 열기 ▼"
        "ru" -> "Нажмите здесь или Назад, чтобы открыть Марию ▼"
        "ar" -> "اضغط هنا أو رجوع لفتح ماريا ▼"
        "hi" -> "मारिया को खोलने के लिए यहाँ टैप करें ▼"
        "bn" -> "মারিয়া খুলতে এখানে ট্যাপ করুন ▼"
        "in", "id" -> "Ketuk di sini untuk membuka Maria ▼"
        else -> "Tap here or press Back to open Maria ▼"
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
        "ar" -> "▲ طي"
        "hi" -> "▲ छोटा करें"
        "bn" -> "▲ লুকান"
        "in", "id" -> "▲ Tutup"
        else -> "▲ Minimize"
    }

    fun homeDefaultGreeting(recoverableLabel: String?): String = when (lang()) {
        "pt" -> if (!recoverableLabel.isNullOrBlank()) {
            "Olá! Eu sou a Maria! Encontrei $recoverableLabel prontos para liberar com segurança."
        } else {
            "Olá! Eu sou a Maria! Já analisei seu celular para liberar espaço com segurança."
        }
        "es" -> if (!recoverableLabel.isNullOrBlank()) {
            "¡Hola! Soy Maria. Encontré $recoverableLabel listos para liberar con seguridad."
        } else {
            "¡Hola! Soy Maria. Analicé tu teléfono para liberar espacio con seguridad."
        }
        "fr" -> if (!recoverableLabel.isNullOrBlank()) {
            "Bonjour ! Je suis Maria. J'ai trouvé $recoverableLabel prêts à être libérés."
        } else {
            "Bonjour ! Je suis Maria. J'ai analysé votre téléphone en toute sécurité."
        }
        "de" -> if (!recoverableLabel.isNullOrBlank()) {
            "Hallo! Ich bin Maria. $recoverableLabel können sicher freigegeben werden."
        } else {
            "Hallo! Ich bin Maria. Ihr Speicher wurde sicher analysiert."
        }
        "zh" -> if (!recoverableLabel.isNullOrBlank()) {
            "您好！我是 Maria。已发现 $recoverableLabel 可安全释放的空间。"
        } else {
            "您好！我是 Maria。已为您安全分析手机存储空间。"
        }
        "ja" -> if (!recoverableLabel.isNullOrBlank()) {
            "こんにちは！Mariaです。安全に解放できる $recoverableLabel が見つかりました。"
        } else {
            "こんにちは！Mariaです。ストレージを安全に分析しました。"
        }
        "ko" -> if (!recoverableLabel.isNullOrBlank()) {
            "안녕하세요! Maria입니다. 안전하게 확보 가능한 $recoverableLabel 를 찾았습니다."
        } else {
            "안녕하세요! Maria입니다. 저장 공간을 안전하게 분석했습니다."
        }
        "ru" -> if (!recoverableLabel.isNullOrBlank()) {
            "Привет! Я Мария. Найдено $recoverableLabel для безопасной очистки."
        } else {
            "Привет! Я Мария. Память устройства проверена."
        }
        "ar" -> if (!recoverableLabel.isNullOrBlank()) {
            "مرحباً! أنا ماريا. وجدت $recoverableLabel جاهزة للتحرير بأمان."
        } else {
            "مرحباً! أنا ماريا. قمت بتحليل ذاكرة هاتفك بأمان."
        }
        "hi" -> if (!recoverableLabel.isNullOrBlank()) {
            "नमस्ते! मैं मारिया हूँ। सुरक्षित रूप से खाली करने के लिए $recoverableLabel मिले हैं।"
        } else {
            "नमस्ते! मैं मारिया हूँ। मैंने आपके फ़ोन की स्टोरेज का विश्लेषण किया है।"
        }
        "bn" -> if (!recoverableLabel.isNullOrBlank()) {
            "হ্যালো! আমি মারিয়া। নিরাপদে খালি করার জন্য $recoverableLabel পাওয়া গেছে।"
        } else {
            "হ্যালো! আমি মারিয়া। আপনার ফোনের স্টোরেজ বিশ্লেষণ করা হয়েছে।"
        }
        "in", "id" -> if (!recoverableLabel.isNullOrBlank()) {
            "Halo! Saya Maria. Menemukan $recoverableLabel siap dibersihkan dengan aman."
        } else {
            "Halo! Saya Maria. Penyimpanan ponsel Anda telah dianalisis."
        }
        else -> if (!recoverableLabel.isNullOrBlank()) {
            "Hi! I'm Maria. I found $recoverableLabel ready to free safely."
        } else {
            "Hi! I'm Maria. I analyzed your phone storage for safe cleanup."
        }
    }

    fun filesDefaultGreeting(): String = when (lang()) {
        "pt" -> "Olá! Suas fotos, vídeos, áudios e downloads organizados dos maiores para os menores."
        "es" -> "¡Hola! Tus fotos, videos, audios y descargas ordenados de mayor a menor."
        "fr" -> "Vos photos, vidéos, audios et téléchargements triés par taille."
        "de" -> "Ihre Fotos, Videos, Audios und Downloads nach Größe sortiert."
        "zh" -> "您的照片、视频、音频和下载已按从大到小排序。"
        "ja" -> "写真、動画、音声、ダウンロードをサイズ順に整理しました。"
        "ko" -> "사진, 동영상, 오디오 및 다운로드가 크기순으로 정리되었습니다."
        "ru" -> "Ваши фото, видео, аудио и загрузки отсортированы по размеру."
        "ar" -> "صورك وفيديوهاتك وتنزيلاتك مرتبة من الأكبر إلى الأصغر."
        "hi" -> "आपकी फ़ोटो, वीडियो, ऑडियो और डाउनलोड आकार के अनुसार व्यवस्थित हैं।"
        "bn" -> "আপনার ছবি, ভিডিও, অডিও এবং ডাউনলোড আকার অনুযায়ী সাজানো।"
        "in", "id" -> "Foto, video, audio, dan unduhan Anda diurutkan dari yang terbesar."
        else -> "Hi! Your photos, videos, audio, and downloads sorted from largest to smallest."
    }

    fun proDefaultGreeting(): String = when (lang()) {
        "pt" -> "Olá! Eu sou a Maria! Destrave Apps sem uso (+90d), Fotos Borradas e Relatórios Auditados com 56% OFF no Anual!"
        "es" -> "¡Hola! Soy Maria. ¡Desbloquea Apps sin uso (+90d), Fotos Borrosas e Informes Auditados con 56% OFF anual!"
        "fr" -> "Bonjour ! Débloquez Apps inutilisées (+90j), Photos floues et Rapports audités (-56% sur l'annuel) !"
        "de" -> "Hallo! Unbenutzte Apps (+90T), Unscharfe Fotos & Prüfberichte mit 56% Rabatt im Jahresabo!"
        "zh" -> "您好！解锁闲置应用（+90天）、模糊照片检测与审计报告（年付立省56%）！"
        "ja" -> "未使用アプリ(+90日)・ブレ写真診断・監査レポートを年間プラン56%OFFで解放！"
        "ko" -> "미사용 앱(+90일), 흐린 사진 진단 및 감사 보고서를 연간 플랜 56% 할인으로 이용하세요!"
        "ru" -> "Откройте поиск неиспользуемых приложений (+90 дн.), размытых фото и отчёты со скидкой 56% на год!"
        "ar" -> "افتح كشف التطبيقات غير المستخدمة (+90 يوماً) والصور الضبابية والتقارير المدققة بخصم 56% سنوياً!"
        "hi" -> "वार्षिक प्लान पर 56% छूट के साथ अप्रयुक्त ऐप्स (+90d), धुंधली फ़ोटो जाँच और ऑडिट रिपोर्ट अनलॉक करें!"
        "bn" -> "বার্ষিক প্ল্যানে ৫৬% ছাড়ে অব্যবহৃত অ্যাপ (+৯০ দিন), ঝাপসা ছবি যাচাই ও অডিট রিপোর্ট আনলক করুন!"
        "in", "id" -> "Buka deteksi aplikasi tak terpakai (+90h), foto buram & laporan audit hemat 56% di paket Tahunan!"
        else -> "Hi! I'm Maria! Unlock Unused Apps (90d+), Blurry Photos & Audited Reports with 56% OFF Annual!"
    }

    fun cameraPhotosTitle(): String = when (lang()) {
        "pt" -> "Fotos da Câmera"
        "es" -> "Fotos de Cámara"
        "fr" -> "Photos Caméra"
        "de" -> "Kamerafotos"
        "zh" -> "相机照片"
        "ja" -> "カメラ写真"
        "ko" -> "카메라 사진"
        "ru" -> "Фото с камеры"
        "ar" -> "صور الكاميرا"
        "hi" -> "कैमरा फ़ोटो"
        "bn" -> "ক্যামেরা ছবি"
        "in", "id" -> "Foto Kamera"
        else -> "Camera Photos"
    }

    fun galleryHighlight(): String = when (lang()) {
        "pt", "es", "it" -> "Galeria"
        "fr" -> "Galerie"
        "de" -> "Galerie"
        "zh" -> "相册"
        "ja" -> "ギャラリー"
        "ko" -> "갤러리"
        "ru" -> "Галерея"
        "ar" -> "المعرض"
        "hi" -> "गैलरी"
        "bn" -> "গ্যালারি"
        "in", "id" -> "Galeri"
        else -> "Gallery"
    }

    fun sortedBySizeSub(): String = when (lang()) {
        "pt" -> "Maiores primeiro"
        "es" -> "Mayores primero"
        "fr" -> "Triées par taille"
        "de" -> "Nach Größe"
        "zh" -> "按大小排序"
        "ja" -> "サイズ順"
        "ko" -> "크기순 정렬"
        "ru" -> "По размеру"
        "ar" -> "حسب الحجم"
        "hi" -> "आकार के अनुसार"
        "bn" -> "আকার অনুযায়ী"
        "in", "id" -> "Urut ukuran"
        else -> "Largest first"
    }

    fun largeVideosTitle(): String = when (lang()) {
        "pt" -> "Vídeos Grandes"
        "es" -> "Videos Grandes"
        "fr" -> "Grandes Vidéos"
        "de" -> "Große Videos"
        "zh" -> "大视频文件"
        "ja" -> "大容量動画"
        "ko" -> "대용량 동영상"
        "ru" -> "Большие видео"
        "ar" -> "فيديوهات كبيرة"
        "hi" -> "बड़े वीडियो"
        "bn" -> "বড় ভিডিও"
        "in", "id" -> "Video Besar"
        else -> "Large Videos"
    }

    fun quickReviewSub(): String = when (lang()) {
        "pt" -> "Revisão rápida"
        "es" -> "Revisión rápida"
        "fr" -> "Révision rapide"
        "de" -> "Schnellprüfung"
        "zh" -> "快速检查"
        "ja" -> "クイック確認"
        "ko" -> "빠른 검토"
        "ru" -> "Быстрый обзор"
        "ar" -> "مراجعة سريعة"
        "hi" -> "त्वरित समीक्षा"
        "bn" -> "দ্রুত রিভিউ"
        "in", "id" -> "Tinjau cepat"
        else -> "Quick review"
    }

    fun oldScreenshotsTitle(): String = when (lang()) {
        "pt" -> "Capturas Antigas"
        "es" -> "Capturas Antiguas"
        "fr" -> "Captures Anciennes"
        "de" -> "Alte Screenshots"
        "zh" -> "旧截图"
        "ja" -> "古いスクショ"
        "ko" -> "오래된 스크린샷"
        "ru" -> "Старые скриншоты"
        "ar" -> "لقطات قديمة"
        "hi" -> "पुराने स्क्रीनशॉट"
        "bn" -> "পুরনো স্ক্রিনশট"
        "in", "id" -> "Tangkapan Lama"
        else -> "Old Screenshots"
    }

    fun moreThan30DaysSub(): String = when (lang()) {
        "pt" -> "Mais de 30 dias"
        "es" -> "Más de 30 días"
        "fr" -> "Plus de 30 jours"
        "de" -> "Älter als 30 Tage"
        "zh" -> "超过30天"
        "ja" -> "30日以上前"
        "ko" -> "30일 이상"
        "ru" -> "Старше 30 дней"
        "ar" -> "أكثر من 30 يوماً"
        "hi" -> "30+ दिन पुराने"
        "bn" -> "৩০+ দিন পুরনো"
        "in", "id" -> "Lebih dari 30 hari"
        else -> "Older than 30 days"
    }

    fun duplicatesTitle(): String = when (lang()) {
        "pt" -> "Duplicatas SHA-256"
        "es" -> "Duplicados SHA-256"
        "fr" -> "Doublons SHA-256"
        "de" -> "Duplikate SHA-256"
        "zh" -> "重复文件 SHA-256"
        "ja" -> "重複ファイル"
        "ko" -> "중복 파일 SHA-256"
        "ru" -> "Дубликаты SHA-256"
        "ar" -> "الملفات المكررة"
        "hi" -> "डुप्लिकेट फ़ाइलें"
        "bn" -> "ডুপ্লিকেট ফাইল"
        "in", "id" -> "Duplikat SHA-256"
        else -> "Duplicates SHA-256"
    }

    fun unusedAppsTitle(): String = when (lang()) {
        "pt" -> "Apps sem Uso"
        "es" -> "Apps sin Uso"
        "fr" -> "Apps Inutilisées"
        "de" -> "Unbenutzte Apps"
        "zh" -> "闲置应用"
        "ja" -> "未使用アプリ"
        "ko" -> "미사용 앱"
        "ru" -> "Неиспользуемые"
        "ar" -> "تطبيقات غير مستخدمة"
        "hi" -> "अप्रयुक्त ऐप्स"
        "bn" -> "অব্যবহৃত অ্যাপ"
        "in", "id" -> "Aplikasi Tak Terpakai"
        else -> "Unused Apps"
    }

    fun blurryPhotosTitle(): String = when (lang()) {
        "pt" -> "Fotos Borradas IA"
        "es" -> "Fotos Borrosas IA"
        "fr" -> "Photos Floues IA"
        "de" -> "Unscharfe Fotos KI"
        "zh" -> "模糊照片 AI"
        "ja" -> "ブレ写真 AI"
        "ko" -> "흐린 사진 AI"
        "ru" -> "Размытые фото ИИ"
        "ar" -> "صور ضبابية"
        "hi" -> "धुंधली तस्वीरें AI"
        "bn" -> "ঝাপসা ছবি AI"
        "in", "id" -> "Foto Buram AI"
        else -> "Blurry Photos AI"
    }

    fun similarPhotosTitle(): String = when (lang()) {
        "pt" -> "Fotos Parecidas IA"
        "es" -> "Fotos Similares IA"
        "fr" -> "Photos Similaires IA"
        "de" -> "Ähnliche Fotos KI"
        "zh" -> "相似连拍照片 AI"
        "ja" -> "類似・連写写真 AI"
        "ko" -> "유사·연사 사진 AI"
        "ru" -> "Похожие фото ИИ"
        "ar" -> "الصور المتشابهة AI"
        "hi" -> "मिलती-जुलती फ़ोटो AI"
        "bn" -> "একই রকম ছবি AI"
        "in", "id" -> "Foto Mirip AI"
        else -> "Similar Photos AI"
    }

    fun bestShotProtectedSub(): String = when (lang()) {
        "pt" -> "Melhor foto protegida"
        "es" -> "Mejor foto protegida"
        "fr" -> "Meilleure photo gardée"
        "de" -> "Bestes Foto geschützt"
        "zh" -> "自动保留最清晰一张"
        "ja" -> "ベストショット保護"
        "ko" -> "베스트 사진 자동 보호"
        "ru" -> "Лучший кадр защищён"
        "ar" -> "حماية أفضل لقطة"
        "hi" -> "सबसे साफ़ फ़ोटो सुरक्षित"
        "bn" -> "সেরা ছবিটি সুরক্ষিত"
        "in", "id" -> "Foto terbaik dilindungi"
        else -> "Best shot protected"
    }

    fun videoCompressorTitle(): String = when (lang()) {
        "pt" -> "Compressor H.265"
        "es" -> "Compresor H.265"
        "fr" -> "Compresseur H.265"
        "de" -> "H.265-Kompressor"
        "zh" -> "H.265 视频压缩"
        "ja" -> "H.265 動画圧縮"
        "ko" -> "H.265 동영상 압축"
        "ru" -> "Сжатие видео H.265"
        "ar" -> "ضاغط الفيديو H.265"
        "hi" -> "H.265 वीडियो कंप्रेसर"
        "bn" -> "H.265 ভিডিও কমপ্রেসর"
        "in", "id" -> "Kompresor H.265"
        else -> "H.265 Compressor"
    }

    fun videoCompressorSub(): String = when (lang()) {
        "pt" -> "Reduz vídeo sem nuvem"
        "es" -> "Reduce sin perder nitidez"
        "fr" -> "Réduit sur l'appareil"
        "de" -> "Lokal verkleinern"
        "zh" -> "本地无损画质瘦身"
        "ja" -> "端末内で高画質圧縮"
        "ko" -> "기기 내 고화질 압축"
        "ru" -> "Локальное сжатие GPU"
        "ar" -> "ضغط محلي بجودة عالية"
        "hi" -> "बिना क्लाउड आकार घटाएँ"
        "bn" -> "ডিভাইসেই ভিডিও ছোট করুন"
        "in", "id" -> "Kecilkan ukuran lokal"
        else -> "On-device GPU shrink"
    }

    fun proBannerTitle(): String = when (lang()) {
        "pt" -> "Apps sem uso (+90d), Fotos Borradas e Relatórios Auditados"
        "es" -> "Apps sin uso (+90d), Fotos Borrosas e Informes Auditados"
        "fr" -> "Apps inutilisées (+90j), Photos floues et Rapports audités"
        "de" -> "Unbenutzte Apps (+90T), Unscharfe Fotos & Prüfberichte"
        "zh" -> "闲置应用（+90天）、模糊照片检测与审计报告"
        "ja" -> "未使用アプリ(+90日)・ブレ写真診断・監査レポート"
        "ko" -> "미사용 앱(+90일), 흐린 사진 진단 및 감사 보고서"
        "ru" -> "Неиспользуемые приложения (+90 дн.), размытые фото и отчёты"
        "ar" -> "تطبيقات غير مستخدمة (+90 يوماً) وصور ضبابية وتقارير مدققة"
        "hi" -> "अप्रयुक्त ऐप्स (+90d), धुंधली फ़ोटो और ऑडिट रिपोर्ट"
        "bn" -> "অব্যবহৃত অ্যাপ (+৯০ দিন), ঝাপসা ছবি ও অডিট রিপোর্ট"
        "in", "id" -> "Aplikasi tak terpakai (+90h), Foto Buram & Laporan Audit"
        else -> "Unused Apps (90d+), Blurry Photos & Audited Reports"
    }

    fun proCardBadge(): String = when (lang()) {
        "pt" -> "MIDIASTORE PRO • -56% NO ANUAL"
        "es" -> "MIDIASTORE PRO • -56% ANUAL"
        "fr" -> "MIDIASTORE PRO • -56% ANNUEL"
        "de" -> "MIDIASTORE PRO • -56% JÄHRLICH"
        "zh" -> "MIDIASTORE PRO • 年付省56%"
        "ja" -> "MIDIASTORE PRO • 年額56%OFF"
        "ko" -> "MIDIASTORE PRO • 연간 56% 할인"
        "ru" -> "MIDIASTORE PRO • -56% НА ГОД"
        "ar" -> "MIDIASTORE PRO • خصم 56% سنوياً"
        "hi" -> "MIDIASTORE PRO • वार्षिक पर 56% छूट"
        "bn" -> "MIDIASTORE PRO • বার্ষিকে ৫৬% ছাড়"
        "in", "id" -> "MIDIASTORE PRO • HEMAT 56%"
        else -> "MIDIASTORE PRO • SAVE 56%"
    }

    fun plus30DaysBadge(): String = when (lang()) {
        "pt" -> "+30 dias"
        "es" -> "+30 días"
        "fr" -> "+30j"
        "de" -> "+30T"
        "zh" -> "+30天"
        "ja" -> "+30日"
        "ko" -> "+30일"
        "ru" -> "+30 дн."
        "ar" -> "+30 يوماً"
        "hi" -> "+30 दिन"
        "bn" -> "+৩০ দিন"
        "in", "id" -> "+30 hari"
        else -> "+30d"
    }

    fun onDeviceAiSummary(): String = when (lang()) {
        "pt" -> "Análise inteligente de fotos borradas, duplicatas SHA-256 e voz neural 100% privada no seu aparelho."
        "es" -> "Análisis inteligente de fotos borrosas, duplicados SHA-256 y voz neural 100% privada en tu dispositivo."
        "fr" -> "Analyse intelligente des photos floues, doublons SHA-256 et voix neurale 100 % privée sur votre appareil."
        "de" -> "Lokale Analyse unscharfer Fotos, SHA-256-Duplikate und 100 % private Sprachassistentin auf Ihrem Gerät."
        "zh" -> "本地智能检测模糊照片、SHA-256 重复文件，配备 100% 隐私保护的语音助手。"
        "ja" -> "端末内でのブレ写真検出、SHA-256重複分析、100%プライベートな音声アシスタント。"
        "ko" -> "기기 내 흐린 사진 분석, SHA-256 중복 파일 검사 및 100% 개인정보 보호 음성 어시스턴트."
        "ru" -> "Локальный поиск размытых фото, дубликатов SHA-256 и 100% приватный голосовой ассистент на устройстве."
        "ar" -> "فحص ذكي للصور الضبابية والملفات المكررة SHA-256 ومساعدة صوتية خاصة 100% على جهازك."
        "hi" -> "डिवाइस पर धुंधली फ़ोटो की स्मार्ट जाँच, SHA-256 डुप्लिकेट और 100% निजी वॉइस असिस्टेंट।"
        "bn" -> "ডিভাইসে ঝাপসা ছবি বিশ্লেষণ, SHA-256 ডুপ্লিকেট শনাক্তকরণ এবং ১০০% গোপনীয় ভয়েস সহকারী।"
        "in", "id" -> "Analisis cerdas foto buram di perangkat, duplikat SHA-256, dan asisten suara 100% privat."
        else -> "Smart on-device blurry photo check, SHA-256 duplicates, and 100% private neural voice assistant."
    }

    fun proBannerSub(): String = when (lang()) {
        "pt" -> "Planos Mensal, Anual (-56%) e Vitalício"
        "es" -> "Planes Mensual, Anual (-56%) y Vitalicio"
        "fr" -> "Mensuel, Annuel (-56%) et À vie"
        "de" -> "Monatlich, Jährlich (-56%) & Lifetime"
        "zh" -> "月付、年付（省56%）及终身版"
        "ja" -> "月額・年額（56%OFF）・永久プラン"
        "ko" -> "월간, 연간(56% 할인) 및 평생 플랜"
        "ru" -> "Месяц, Год (-56%) и Навсегда"
        "ar" -> "شهري وسنوي (خصم 56%) ومدى الحياة"
        "hi" -> "मासिक, वार्षिक (-56%) और लाइफटाइम"
        "bn" -> "মাসিক, বার্ষিক (-৫৬%) ও লাইফটাইম"
        "in", "id" -> "Bulanan, Tahunan (-56%) & Selamanya"
        else -> "Monthly, Annual (Save 56%) & Lifetime"
    }

    fun chatPlaceholder(): String = when (lang()) {
        "pt" -> "Pergunte qualquer coisa..."
        "es" -> "Pregunta cualquier cosa..."
        "fr" -> "Posez une question..."
        "de" -> "Frag irgendetwas..."
        "zh" -> "询问任何问题..."
        "ja" -> "何でも聞いてください..."
        "ko" -> "무엇이든 물어보세요..."
        "ru" -> "Спросите что угодно..."
        "ar" -> "اسأل أي شيء..."
        "hi" -> "कुछ भी पूछें..."
        "bn" -> "যেকোনো কিছু জিজ্ঞাসা করুন..."
        "in", "id" -> "Tanyakan apa saja..."
        else -> "Ask anything..."
    }
}

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
    var isNeuralVideoSpeaking by remember { mutableStateOf(false) }
    var userPlayTrigger by remember { mutableIntStateOf(0) }
    var currentFrameRes by remember { mutableIntStateOf(R.drawable.maria_smile) }
    var voiceEnergy by remember { mutableFloatStateOf(0f) }
    var wavePhase by remember { mutableFloatStateOf(0f) }
    var breathScale by remember { mutableFloatStateOf(1.01f) }

    // Maria 2.0 Native H.265 Player State (Hardware TextureView + CDN Cache + Offline Safe Fallback)
    var surfaceRef by remember { mutableStateOf<Surface?>(null) }
    var videoPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }
    var fallbackAudioPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }
    var videoFrameReady by remember { mutableStateOf(false) }
    var videoHardwareSupported by remember { mutableStateOf(true) }

    fun releaseFallbackAudio() {
        runCatching {
            fallbackAudioPlayerRef?.let { mp ->
                if (mp.isPlaying) mp.stop()
                mp.release()
            }
        }
        fallbackAudioPlayerRef = null
    }

    fun releaseVideoPlayer() {
        runCatching {
            videoPlayerRef?.let { mp ->
                if (mp.isPlaying) mp.stop()
                mp.release()
            }
        }
        videoPlayerRef = null
    }

    fun startIdleLoopOnSurface(surface: Surface?) {
        if (!isActive || !videoHardwareSupported || surface == null || !surface.isValid) return
        releaseVideoPlayer()
        runCatching {
            val afd = context.resources.openRawResourceFd(R.raw.maria2_idle) ?: return
            val mp = MediaPlayer()
            videoPlayerRef = mp
            mp.setSurface(surface)
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            mp.isLooping = true
            mp.setVolume(0f, 0f)
            mp.setOnInfoListener { _, what, _ ->
                if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                    videoFrameReady = true
                }
                false
            }
            mp.setOnPreparedListener { prepared ->
                runCatching {
                    prepared.start()
                    videoFrameReady = true
                }
            }
            mp.setOnErrorListener { failedMp, _, _ ->
                videoHardwareSupported = false
                videoFrameReady = false
                runCatching { failedMp.release() }
                if (videoPlayerRef === failedMp) videoPlayerRef = null
                true
            }
            mp.prepareAsync()
        }.onFailure {
            videoHardwareSupported = false
            videoFrameReady = false
        }
    }

    fun stopSpeechAndReturnToIdle() {
        releaseFallbackAudio()
        val wasNeural = isNeuralVideoSpeaking
        isSpeaking = false
        isNeuralVideoSpeaking = false
        voiceEnergy = 0f
        if (wasNeural) {
            startIdleLoopOnSurface(surfaceRef)
        }
    }

    // Background pre-warm of primary CDN neural clips (<= 24 MB local cache)
    LaunchedEffect(isActive) {
        if (isActive) {
            MariaCdnVideoCache.prefetchPrimaryClips(context)
        }
    }

    LaunchedEffect(surfaceRef, isActive, videoHardwareSupported) {
        if (isActive && videoHardwareSupported && surfaceRef != null && !isNeuralVideoSpeaking) {
            startIdleLoopOnSurface(surfaceRef)
        } else if (!isActive) {
            releaseFallbackAudio()
            releaseVideoPlayer()
            isSpeaking = false
            isNeuralVideoSpeaking = false
            voiceEnergy = 0f
        }
    }

    DisposableEffect(lifecycleOwner, isActive) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                releaseFallbackAudio()
                releaseVideoPlayer()
                isSpeaking = false
                isNeuralVideoSpeaking = false
                voiceEnergy = 0f
            } else if (event == Lifecycle.Event.ON_RESUME && isActive && surfaceRef != null && !isSpeaking) {
                startIdleLoopOnSurface(surfaceRef)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            releaseFallbackAudio()
            releaseVideoPlayer()
            runCatching { surfaceRef?.release() }
            surfaceRef = null
        }
    }

    // Padrão Apple / Gemini Live: fala quando o usuário toca no card ou botão de som.
    // Toca primeiro o micro-vídeo neural MoDA H.265 sincronizado (do cache local / Firebase CDN),
    // caindo com segurança para o áudio offline de estúdio se estiver sem internet!
    LaunchedEffect(userPlayTrigger, rawAudioResId, isActive) {
        if (!isActive || userPlayTrigger == 0) {
            if (!isActive) stopSpeechAndReturnToIdle()
            return@LaunchedEffect
        }
        stopSpeechAndReturnToIdle()
        if (isMuted) return@LaunchedEffect

        isSpeaking = true
        val lang = MariaCdnVideoCache.normalizedLang()
        val clipKey = MariaCdnVideoCache.clipKeyForAudioRes(rawAudioResId)
        val surface = surfaceRef
        val neuralClipFile: File? = if (videoHardwareSupported && surface != null && surface.isValid) {
            MariaCdnVideoCache.resolveOrFetchClip(context, lang, clipKey, timeoutMs = 3800L)
        } else {
            null
        }

        if (neuralClipFile != null && surface != null && surface.isValid) {
            var clipCompleted = false
            val startedNeural = runCatching {
                releaseVideoPlayer()
                val mp = MediaPlayer()
                videoPlayerRef = mp
                isNeuralVideoSpeaking = true
                mp.setSurface(surface)
                FileInputStream(neuralClipFile).use { fis ->
                    mp.setDataSource(fis.fd)
                }
                mp.isLooping = false
                mp.setVolume(1f, 1f)
                mp.setOnInfoListener { _, what, _ ->
                    if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                        videoFrameReady = true
                    }
                    false
                }
                mp.setOnCompletionListener {
                    clipCompleted = true
                }
                mp.setOnErrorListener { _, _, _ ->
                    clipCompleted = true
                    true
                }
                mp.prepare()
                mp.start()
                videoFrameReady = true
                mp
            }.getOrNull()

            if (startedNeural != null) {
                var elapsedMs = 0L
                while (isSpeaking && !clipCompleted && elapsedMs < 45_000L) {
                    val playing = runCatching { startedNeural.isPlaying }.getOrDefault(false)
                    if (!playing && elapsedMs > 650L) break
                    delay(90L)
                    elapsedMs += 90L
                }
                stopSpeechAndReturnToIdle()
                return@LaunchedEffect
            } else {
                isNeuralVideoSpeaking = false
                startIdleLoopOnSurface(surface)
            }
        }

        // Safe Offline Fallback: local bundled studio audio (PT-BR) + idle breathing H.265 / editorial portrait
        if (MariaI18n.isPt()) {
            val mpInstance = runCatching {
                val mp = MediaPlayer.create(context, rawAudioResId)
                if (mp != null) {
                    fallbackAudioPlayerRef = mp
                    mp.setOnCompletionListener {
                        isSpeaking = false
                        voiceEnergy = 0f
                        runCatching { it.release() }
                        if (fallbackAudioPlayerRef === it) fallbackAudioPlayerRef = null
                    }
                    mp.setOnErrorListener { player, _, _ ->
                        isSpeaking = false
                        voiceEnergy = 0f
                        runCatching { player.release() }
                        if (fallbackAudioPlayerRef === player) fallbackAudioPlayerRef = null
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
                while (isSpeaking && elapsedMs < 40_000L) {
                    val stillPlaying = runCatching { mpInstance.isPlaying }.getOrDefault(false)
                    if (!stillPlaying && elapsedMs > 500L) break
                    delay(120L)
                    elapsedMs += 120L
                }
                if (isSpeaking) stopSpeechAndReturnToIdle()
                return@LaunchedEffect
            }
        }

        delay(3200L)
        isSpeaking = false
        voiceEnergy = 0f
    }

    // 60 FPS VSYNC: sincronia do anel/onda sonora + fallback editorial quando necessário
    LaunchedEffect(isActive, rawAudioResId) {
        if (!isActive) {
            currentFrameRes = R.drawable.maria_smile
            voiceEnergy = 0f
            return@LaunchedEffect
        }
        val visemeTrack = MariaLipSyncEnvelopes.trackForAudioRes(rawAudioResId)
        var blinkTimerMs = -1L
        var nextBlinkAtMs = System.currentTimeMillis() + 2600L
        var lastFrameNs = 0L

        while (true) {
            withFrameNanos { frameNs ->
                val dtMs = if (lastFrameNs == 0L) 16.67f else ((frameNs - lastFrameNs) / 1_000_000f).coerceIn(1f, 50f)
                lastFrameNs = frameNs
                val now = System.currentTimeMillis()
                breathScale = 1.015f + (sin(now / 900.0).toFloat() * 0.012f)

                if (now >= nextBlinkAtMs && blinkTimerMs < 0L) {
                    blinkTimerMs = 0L
                    nextBlinkAtMs = now + Random.nextLong(2800L, 4800L)
                }

                if (blinkTimerMs >= 0L) {
                    val stepIdx = (blinkTimerMs / 34L).toInt()
                    blinkTimerMs += dtMs.roundToInt().toLong()
                    if (stepIdx < MARIA_IDLE_BLINK_SEQUENCE.size) {
                        currentFrameRes = MARIA_IDLE_BLINK_SEQUENCE[stepIdx]
                    } else {
                        blinkTimerMs = -1L
                        currentFrameRes = R.drawable.maria_smile
                    }
                } else {
                    currentFrameRes = R.drawable.maria_smile
                }

                if (isSpeaking) {
                    wavePhase += dtMs * 0.013f
                    val activeAudioPlayer = fallbackAudioPlayerRef
                    val posMs = runCatching {
                        activeAudioPlayer?.takeIf { it.isPlaying }?.currentPosition
                    }.getOrNull()
                    val targetEnergy = if (posMs != null && visemeTrack.isNotEmpty()) {
                        val code = MariaLipSyncEnvelopes.visemeAtMs(visemeTrack, (posMs - 60).coerceAtLeast(0))
                        if (code in 1..7) (code / 7f) else 0.08f
                    } else {
                        (0.38f + 0.48f * sin(wavePhase.toDouble()).toFloat()).coerceIn(0.12f, 1f)
                    }
                    voiceEnergy = (voiceEnergy * 0.65f) + (targetEnergy * 0.35f)
                } else {
                    voiceEnergy = (voiceEnergy * 0.80f).let { if (it < 0.01f) 0f else it }
                }
            }
        }
    }

    val bannerHeight by animateDpAsState(
        targetValue = if (isSpeaking) 122.dp else 80.dp,
        animationSpec = tween(durationMillis = 260),
        label = "maria2BannerHeight",
    )
    val avatarRingSize by animateDpAsState(
        targetValue = if (isSpeaking) 88.dp else 56.dp,
        animationSpec = tween(durationMillis = 260),
        label = "maria2AvatarRingSize",
    )
    val avatarInnerSize by animateDpAsState(
        targetValue = if (isSpeaking) 80.dp else 48.dp,
        animationSpec = tween(durationMillis = 260),
        label = "maria2AvatarInnerSize",
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

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MidiastoreColors.CardSurface,
        modifier = modifier
            .fillMaxWidth()
            .height(bannerHeight)
            .border(
                width = if (isSpeaking) 1.5.dp else 1.dp,
                color = when {
                    isNeuralVideoSpeaking -> Color(0xCC30D158)
                    isSpeaking -> Color(0xAA0A84FF)
                    else -> MidiastoreColors.CardStroke
                },
                shape = RoundedCornerShape(18.dp),
            )
            .clip(RoundedCornerShape(18.dp))
            .clickable {
                if (isSpeaking) {
                    stopSpeechAndReturnToIdle()
                } else {
                    if (isMuted) {
                        isMuted = false
                        prefs.edit().putBoolean(KEY_MARIA_MUTED, false).apply()
                    }
                    userPlayTrigger++
                }
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Avatar Maria 2.0 (Vídeo Neural MoDA H.265 em TextureView + Fallback Editorial Zero-Flash)
            Box(
                modifier = Modifier.size(avatarRingSize),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val baseRadius = (size.minDimension / 2f) - 2.dp.toPx()
                    val activeGlowColor = if (isNeuralVideoSpeaking) Color(0xFF30D158) else Color(0xFF0A84FF)
                    if (isSpeaking && voiceEnergy > 0.02f) {
                        drawCircle(
                            color = activeGlowColor.copy(alpha = (0.20f + voiceEnergy * 0.34f).coerceIn(0f, 0.58f)),
                            radius = baseRadius + (voiceEnergy * 2.6.dp.toPx()),
                            style = Stroke(width = (2.2f + voiceEnergy * 2.2f).dp.toPx()),
                        )
                    }
                    drawCircle(
                        color = when {
                            isNeuralVideoSpeaking -> Color(0xFF30D158)
                            isSpeaking -> Color(0xFF64D2FF)
                            else -> Color(0x660A84FF)
                        },
                        radius = baseRadius,
                        style = Stroke(width = 1.8.dp.toPx()),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(avatarInnerSize)
                        .clip(CircleShape)
                        .background(Color(0xFF050507)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (videoHardwareSupported) {
                        AndroidView(
                            factory = { ctx ->
                                TextureView(ctx).apply {
                                    isOpaque = false
                                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                        override fun onSurfaceTextureAvailable(
                                            st: SurfaceTexture,
                                            width: Int,
                                            height: Int,
                                        ) {
                                            runCatching { surfaceRef?.release() }
                                            surfaceRef = Surface(st)
                                        }

                                        override fun onSurfaceTextureSizeChanged(
                                            st: SurfaceTexture,
                                            width: Int,
                                            height: Int,
                                        ) = Unit

                                        override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                                            releaseVideoPlayer()
                                            runCatching { surfaceRef?.release() }
                                            surfaceRef = null
                                            videoFrameReady = false
                                            return true
                                        }

                                        override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    if (!videoFrameReady || !videoHardwareSupported) {
                        Image(
                            painter = painterResource(id = currentFrameRes),
                            contentDescription = "Maria",
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.TopCenter,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = breathScale
                                    scaleY = breathScale
                                },
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = brandAnnotated,
                        fontSize = if (isSpeaking) 14.5.sp else 16.sp,
                        letterSpacing = (-0.3).sp,
                        maxLines = 1,
                    )
                    if (isProBadgeActive) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0x330A84FF),
                            modifier = Modifier.border(1.dp, Color(0xFF0A84FF), RoundedCornerShape(6.dp)),
                        ) {
                            Text(
                                text = "PRO",
                                color = Color(0xFF64D2FF),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isSpeaking) Color(0xFF30D158) else MidiastoreColors.AccentBlue),
                        )
                        Text(
                            text = if (isNeuralVideoSpeaking) "MARIA 2.0" else "MARIA",
                            color = if (isNeuralVideoSpeaking) Color(0xFF30D158) else Color(0xFF64D2FF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.6.sp,
                            maxLines = 1,
                        )
                    }
                }
                Text(
                    text = captionText,
                    color = if (isSpeaking) MidiastoreColors.TextPrimary else MidiastoreColors.TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = if (isSpeaking) 3 else 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Onda sonora viva 60 FPS quando falando ou botão discreto de áudio sob demanda
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isSpeaking) Color(0x2E0A84FF) else MidiastoreColors.CardSurfaceAlt)
                    .border(
                        width = 1.dp,
                        color = if (isSpeaking) MidiastoreColors.AccentBlue else Color(0x28FFFFFF),
                        shape = CircleShape,
                    )
                    .clickable {
                        if (isSpeaking) {
                            stopSpeechAndReturnToIdle()
                        } else {
                            if (isMuted) {
                                isMuted = false
                                prefs.edit().putBoolean(KEY_MARIA_MUTED, false).apply()
                            }
                            userPlayTrigger++
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (isSpeaking) {
                    Canvas(modifier = Modifier.size(18.dp)) {
                        val barCount = 4
                        val spacing = size.width / (barCount + 1)
                        val centerY = size.height / 2f
                        for (i in 0 until barCount) {
                            val osc = (0.45f + 0.55f * sin((wavePhase * 1.4f + i * 1.15f).toDouble()).toFloat())
                            val halfH = (size.height * 0.42f * (0.25f + voiceEnergy * 0.75f) * osc).coerceAtLeast(2.dp.toPx())
                            val x = spacing * (i + 1)
                            drawLine(
                                color = Color(0xFF64D2FF),
                                start = Offset(x, centerY - halfH),
                                end = Offset(x, centerY + halfH),
                                strokeWidth = 2.2.dp.toPx(),
                                cap = StrokeCap.Round,
                            )
                        }
                    }
                } else {
                    Canvas(modifier = Modifier.size(16.dp)) {
                        val barColor = if (isMuted) Color(0xFF8E8E93) else Color.White
                        val strokePx = 1.8.dp.toPx()
                        val centerY = size.height / 2f
                        val barHeights = floatArrayOf(0.32f, 0.68f, 0.48f, 0.30f)
                        val stepX = size.width / 5f
                        for (i in barHeights.indices) {
                            val x = stepX * (i + 1)
                            val halfH = (size.height * barHeights[i]) / 2f
                            drawLine(
                                color = barColor,
                                start = Offset(x, centerY - halfH),
                                end = Offset(x, centerY + halfH),
                                strokeWidth = strokePx,
                                cap = StrokeCap.Round,
                            )
                        }
                        if (isMuted) {
                            drawLine(
                                color = Color(0xFFFF453A),
                                start = Offset(size.width * 0.15f, size.height * 0.85f),
                                end = Offset(size.width * 0.85f, size.height * 0.15f),
                                strokeWidth = strokePx,
                                cap = StrokeCap.Round,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilesMariaHeaderSection(
    isActive: Boolean,
    onSelectPhotos: () -> Unit,
    onSelectLargeVideos: () -> Unit,
    onSelectWhatsApp: () -> Unit,
    onSelectScreenshots: () -> Unit,
    photosSizeLabel: String? = null,
    videosSizeLabel: String? = null,
    chatsSizeLabel: String? = null,
    modifier: Modifier = Modifier,
) {
    var caption by remember {
        mutableStateOf(MariaI18n.filesDefaultGreeting())
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
                    label = MariaI18n.cameraPhotosTitle(),
                    highlight = photosSizeLabel?.takeIf { it.isNotBlank() && it != "—" } ?: MariaI18n.galleryHighlight(),
                    highlightColor = Color(0xFF64D2FF),
                    subtitle = MariaI18n.sortedBySizeSub(),
                    onClick = {
                        caption = MariaI18n.cameraPhotosTitle() + " • " + MariaI18n.sortedBySizeSub()
                        activeAudioRes = R.raw.maria_files
                        speechNonce++
                        onSelectPhotos()
                    },
                    modifier = Modifier.weight(1f),
                )
                MariaSquareTile(
                    label = MariaI18n.largeVideosTitle(),
                    highlight = videosSizeLabel?.takeIf { it.isNotBlank() && it != "—" } ?: "> 10 MB",
                    highlightColor = Color(0xFFBF5AF2),
                    subtitle = MariaI18n.quickReviewSub(),
                    onClick = {
                        caption = MariaI18n.largeVideosTitle() + " (> 10 MB) • " + MariaI18n.quickReviewSub()
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
                    label = MariaI18n.socialChatTileTitle(),
                    highlight = chatsSizeLabel?.takeIf { it.isNotBlank() && it != "—" } ?: "MediaStore",
                    highlightColor = Color(0xFF30D158),
                    subtitle = MariaI18n.socialChatTileSub(),
                    onClick = {
                        caption = MariaI18n.socialChatTileTitle() + " • " + MariaI18n.socialChatTileSub()
                        activeAudioRes = R.raw.maria_whatsapp
                        speechNonce++
                        onSelectWhatsApp()
                    },
                    modifier = Modifier.weight(1f),
                )
                MariaSquareTile(
                    label = MariaI18n.oldScreenshotsTitle(),
                    highlight = "+30d",
                    highlightColor = Color(0xFFFF9F0A),
                    subtitle = MariaI18n.moreThan30DaysSub(),
                    onClick = {
                        caption = MariaI18n.oldScreenshotsTitle() + " • " + MariaI18n.moreThan30DaysSub()
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

@Composable
fun ProMariaHeaderSection(
    isActive: Boolean,
    isProActive: Boolean,
    onRunSmartDiagnosis: () -> Unit = {},
    onRunSimilarPhotos: () -> Unit = {},
    onRunVideoCompressor: () -> Unit = {},
    onRunBlurScan: () -> Unit = {},
    onRunWhatsAppClean: () -> Unit = {},
    onRunUnusedApps: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var caption by remember { mutableStateOf(MariaI18n.proDefaultGreeting()) }
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
            isProBadgeActive = isProActive,
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MariaSquareTile(
                    label = MariaI18n.similarPhotosTitle(),
                    highlight = "dHash",
                    highlightColor = Color(0xFF0A84FF),
                    subtitle = MariaI18n.bestShotProtectedSub(),
                    onClick = {
                        caption = MariaI18n.similarPhotosTitle() + " • " + MariaI18n.bestShotProtectedSub()
                        activeAudioRes = R.raw.maria_dup
                        speechNonce++
                        onRunSimilarPhotos()
                    },
                    modifier = Modifier.weight(1f),
                )
                MariaSquareTile(
                    label = MariaI18n.videoCompressorTitle(),
                    highlight = "H.265",
                    highlightColor = Color(0xFF30D158),
                    subtitle = MariaI18n.videoCompressorSub(),
                    onClick = {
                        caption = MariaI18n.videoCompressorTitle() + " • " + MariaI18n.videoCompressorSub()
                        activeAudioRes = R.raw.maria_videos
                        speechNonce++
                        onRunVideoCompressor()
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MariaSquareTile(
                    label = MariaI18n.duplicatesTitle(),
                    highlight = "100%",
                    highlightColor = Color(0xFFFF9F0A),
                    subtitle = MariaI18n.quickReviewSub(),
                    onClick = {
                        caption = MariaI18n.duplicatesTitle() + " • 100% SHA-256"
                        activeAudioRes = R.raw.maria_dup
                        speechNonce++
                        onRunSmartDiagnosis()
                    },
                    modifier = Modifier.weight(1f),
                )
                MariaSquareTile(
                    label = MariaI18n.unusedAppsTitle(),
                    highlight = "+90d",
                    highlightColor = Color(0xFFBF5AF2),
                    subtitle = MariaI18n.quickReviewSub(),
                    onClick = {
                        caption = MariaI18n.unusedAppsTitle() + " (+90d)"
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
                    label = MariaI18n.blurryPhotosTitle(),
                    highlight = "On-Device",
                    highlightColor = Color(0xFF64D2FF),
                    subtitle = MariaI18n.quickReviewSub(),
                    onClick = {
                        caption = MariaI18n.blurryPhotosTitle() + " • On-Device"
                        activeAudioRes = R.raw.maria_nano
                        speechNonce++
                        onRunBlurScan()
                    },
                    modifier = Modifier.weight(1f),
                )
                MariaSquareTile(
                    label = MariaI18n.socialChatTileTitle(),
                    highlight = "Pro",
                    highlightColor = Color(0xFF30D158),
                    subtitle = MariaI18n.socialChatTileSub(),
                    onClick = {
                        caption = MariaI18n.socialChatTileTitle() + " • " + MariaI18n.socialChatTileSub()
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

/**
 * Selo limpo de Inteligência Artificial On-Device no final da aba Pro (chat retirado conforme decisão do fundador para manter 100% foco em conversão e limpeza em 1 clique).
 */
@Composable
fun ProGeminiNanoBottomCard(
    onRunSmartDiagnosis: () -> Unit = {},
    onRunBlurScan: () -> Unit = {},
    onRunWhatsAppClean: () -> Unit = {},
    onRunUnusedApps: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MidiastoreColors.CardSurface,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 10.dp)
            .border(1.dp, MidiastoreColors.CardStroke, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable { onRunBlurScan() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = MariaI18n.assistantBadge() + " • ON-DEVICE (0 MB)",
                    color = Color(0xFF64D2FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = MariaI18n.onDeviceAiSummary(),
                    color = MidiastoreColors.TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
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
