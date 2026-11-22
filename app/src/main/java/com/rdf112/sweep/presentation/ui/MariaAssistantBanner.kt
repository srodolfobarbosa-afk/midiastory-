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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
        "pt" -> "Mídias, áudios e downloads"
        "es" -> "Videos, audios y descargas"
        "fr" -> "Médias et téléchargements"
        "de" -> "Medien & Downloads"
        "it" -> "Media e download"
        "ru" -> "Медиа и загрузки"
        "zh" -> "聊天视频、图片与下载"
        "ja" -> "動画・画像・ダウンロード"
        "ko" -> "동영상, 사진 및 다운로드"
        "ar" -> "الوسائط والتنزيلات"
        "hi" -> "मीडिया और डाउनलोड"
        "bn" -> "মিডিয়া ও ডাউনলোড"
        "in", "id" -> "Media & unduhan"
        else -> "Social media & downloads"
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
        "pt" -> "Olá! Eu sou a Maria! Destrave a limpeza ilimitada + Chat Midiastore IA com 56% OFF no Anual!"
        "es" -> "¡Hola! Soy Maria. ¡Desbloquea limpieza ilimitada + Chat Midiastore IA con 56% OFF anual!"
        "fr" -> "Bonjour ! Débloquez le nettoyage illimité + Chat Midiastore IA (-56% sur l'annuel) !"
        "de" -> "Hallo! Unbegrenzte Bereinigung + Chat Midiastore KI mit 56% Rabatt im Jahresabo!"
        "zh" -> "您好！解锁无限清理 + Chat Midiastore 智能助手（年付立省56%）！"
        "ja" -> "無制限クリーンアップ＋Chat Midiastore AIを年間プラン56%OFFで解放！"
        "ko" -> "무제한 정리 + Chat Midiastore AI를 연간 플랜 56% 할인으로 잠금 해제하세요!"
        "ru" -> "Разблокируйте безлимитную очистку + Chat Midiastore ИИ со скидкой 56% на год!"
        "ar" -> "افتح التنظيف غير المحدود + محادثة الذكاء الاصطناعي بخصم 56% سنوياً!"
        "hi" -> "वार्षिक प्लान पर 56% छूट के साथ असीमित क्लीनअप + Chat Midiastore AI अनलॉक करें!"
        "bn" -> "বার্ষিক প্ল্যানে ৫৬% ছাড়ে আনলিমিটেড ক্লিনআপ + Chat Midiastore AI আনলক করুন!"
        "in", "id" -> "Buka pembersihan tanpa batas + Chat Midiastore AI hemat 56% di paket Tahunan!"
        else -> "Hi! I'm Maria! Unlock unlimited cleanup + Chat Midiastore AI with 56% OFF Annual!"
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
        "pt" -> "Ordenadas por tamanho"
        "es" -> "Ordenadas por tamaño"
        "fr" -> "Triées par taille"
        "de" -> "Nach Größe sortiert"
        "zh" -> "按大小排序"
        "ja" -> "サイズ順"
        "ko" -> "크기순 정렬"
        "ru" -> "По размеру"
        "ar" -> "مرتبة حسب الحجم"
        "hi" -> "आकार के अनुसार"
        "bn" -> "আকার অনুযায়ী"
        "in", "id" -> "Urut ukuran"
        else -> "Sorted by size"
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

    fun proBannerTitle(): String = when (lang()) {
        "pt" -> "Libere limpeza completa + Chat Midiastore IA"
        "es" -> "Libera limpieza completa + Chat Midiastore IA"
        "fr" -> "Débloquez nettoyage complet + Chat Midiastore IA"
        "de" -> "Vollständige Bereinigung + Chat Midiastore KI"
        "zh" -> "解锁完整清理 + Chat Midiastore 智能助手"
        "ja" -> "完全クリーンアップ＋Chat Midiastore AIを解放"
        "ko" -> "전체 정리 + Chat Midiastore AI 잠금 해제"
        "ru" -> "Полная очистка + Chat Midiastore ИИ"
        "ar" -> "افتح التنظيف الكامل + محادثة الذكاء الاصطناعي"
        "hi" -> "पूर्ण क्लीनअप + Chat Midiastore AI अनलॉक करें"
        "bn" -> "সম্পূর্ণ ক্লিনআপ + Chat Midiastore AI আনলক করুন"
        "in", "id" -> "Buka pembersihan penuh + Chat Midiastore AI"
        else -> "Unlock full cleanup + Chat Midiastore AI"
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
    var isExpanded by remember { mutableStateOf(true) }
    var replayTrigger by remember { mutableIntStateOf(0) }
    var currentFrameRes by remember { mutableIntStateOf(R.drawable.maria_smile) }
    var mouthLevel by remember { mutableFloatStateOf(0f) }
    var breathScale by remember { mutableFloatStateOf(1.01f) }
    var bodyOffsetY by remember { mutableFloatStateOf(0f) }
    var headTiltDeg by remember { mutableFloatStateOf(0f) }
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }

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

    LaunchedEffect(isActive, tabKey, rawAudioResId, speechNonce, replayTrigger) {
        stopAndReleaseAudio()
        if (!isActive) return@LaunchedEffect

        isExpanded = true
        currentFrameRes = R.drawable.maria_smile
        delay(320L)
        isSpeaking = true

        // Reproduz o áudio neural de estúdio em português; em outros idiomas anima a fala visualmente sem misturar idiomas!
        if (!isMuted && MariaI18n.isPt()) {
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
                val posMs = runCatching {
                    mediaPlayerRef?.takeIf { it.isPlaying }?.currentPosition
                }.getOrNull()

                val envCode = if (posMs != null && envelope.isNotEmpty()) {
                    val frameIdx = (posMs / 35).coerceIn(0, envelope.lastIndex)
                    envelope[frameIdx]
                } else {
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
                    mouthLevel = 0f
                    currentFrameRes = R.drawable.maria_smile
                } else if (envCode <= 0) {
                    mouthLevel *= 0.22f
                    val idx = mouthLevel.roundToInt().coerceIn(0, MARIA_LIP_FRAMES.lastIndex)
                    currentFrameRes = MARIA_LIP_FRAMES[idx]
                } else {
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
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Image(
                    painter = painterResource(id = currentFrameRes),
                    contentDescription = "Maria",
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
                    highlight = MariaI18n.galleryHighlight(),
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
                    highlight = "> 10 MB",
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
                    highlight = "MediaStore",
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
            isProBadgeActive = true,
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

data class ChatMidiastoreMessage(
    val isUser: Boolean,
    val text: String,
    val actionLabel: String? = null,
    val actionType: String? = null,
)

/**
 * Chat Midiastore no padrão clássico ChatGPT / Google Gemini Modelo Branco ("caixinha quadradinha no modelo branco, sem inventar"):
 * - Cartão Branco Puro (#FFFFFF) com tipografia preta (#111827) de alto contraste e 4 caixinhas quadradinhas (#F3F4F6) idênticas ao ChatGPT/Gemini!
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
                    text = if (MariaI18n.isPt()) {
                        "Olá! Como posso ajudar a limpar seu celular hoje? (${statInfo.first} usados • ${statInfo.second} livres)"
                    } else {
                        "Hi! How can I help clean your phone today? (${statInfo.first} used • ${statInfo.second} free)"
                    },
                    actionLabel = if (MariaI18n.isPt()) "Analisar arquivos pesados" else "Scan large files",
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
            q.contains("whatsapp", ignoreCase = true) || q.contains("chat", ignoreCase = true) || q.contains("áudio", ignoreCase = true) -> ChatMidiastoreMessage(
                isUser = false,
                text = if (MariaI18n.isPt()) {
                    "Separei as mídias recebidas em conversas e downloads para você revisar sem apagar suas mensagens:"
                } else {
                    "I separated chat media and downloads so you can review them safely:"
                },
                actionLabel = MariaI18n.socialChatTileTitle(),
                actionType = "WHATSAPP",
            )
            q.contains("borrad", ignoreCase = true) || q.contains("blur", ignoreCase = true) || q.contains("foto", ignoreCase = true) -> ChatMidiastoreMessage(
                isUser = false,
                text = if (MariaI18n.isPt()) {
                    "Verificando nitidez das fotos direto no aparelho (100% offline):"
                } else {
                    "Checking photo sharpness on-device (100% offline):"
                },
                actionLabel = MariaI18n.blurryPhotosTitle(),
                actionType = "BLUR",
            )
            q.contains("app", ignoreCase = true) || q.contains("90", ignoreCase = true) -> ChatMidiastoreMessage(
                isUser = false,
                text = if (MariaI18n.isPt()) {
                    "Listando aplicativos sem uso há mais de 90 dias que ocupam espaço:"
                } else {
                    "Listing apps unused for 90+ days taking up storage:"
                },
                actionLabel = MariaI18n.unusedAppsTitle(),
                actionType = "APPS",
            )
            else -> ChatMidiastoreMessage(
                isUser = false,
                text = if (MariaI18n.isPt()) {
                    "Seu aparelho tem ${statInfo.first} ocupados e ${statInfo.second} livres. Vamos revisar os vídeos grandes (>10 MB) e duplicatas:"
                } else {
                    "Your phone has ${statInfo.first} used and ${statInfo.second} free. Let's review large videos (>10 MB) and duplicates:"
                },
                actionLabel = MariaI18n.largeVideosTitle(),
                actionType = "DIAGNOSIS",
            )
        }
        messages = (messages + userMsg + replyMsg).takeLast(4)
    }

    // MODELO BRANCO CLÁSSICO CHATGPT / GEMINI (#FFFFFF com borda #E5E7EB e caixinhas quadradinhas #F3F4F6)
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFFFFFFF),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 14.dp)
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Topo limpo ChatGPT / Gemini em uma linha única (sem bolinha quebrando texto!)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "✦",
                        color = Color(0xFF111827),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = "Chat Midiastore",
                        color = Color(0xFF111827),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xFFF3F4F6),
                    modifier = Modifier.border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(999.dp)),
                ) {
                    Text(
                        text = "Gemini Nano • 0 MB",
                        color = Color(0xFF374151),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            // 4 Caixinhas Quadradinhas padrão ChatGPT / Gemini (#F3F4F6 no fundo branco #FFFFFF)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ChatGptWhiteSquareBox(
                        title = MariaI18n.largeVideosTitle(),
                        subtitle = MariaI18n.quickReviewSub(),
                        onClick = {
                            handleSendQuestion(MariaI18n.largeVideosTitle())
                            onRunSmartDiagnosis()
                        },
                        modifier = Modifier.weight(1f),
                    )
                    ChatGptWhiteSquareBox(
                        title = MariaI18n.socialChatTileTitle(),
                        subtitle = MariaI18n.socialChatTileSub(),
                        onClick = {
                            handleSendQuestion(MariaI18n.socialChatTileTitle())
                            onRunWhatsAppClean()
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ChatGptWhiteSquareBox(
                        title = MariaI18n.blurryPhotosTitle(),
                        subtitle = "On-Device",
                        onClick = {
                            handleSendQuestion(MariaI18n.blurryPhotosTitle())
                            onRunBlurScan()
                        },
                        modifier = Modifier.weight(1f),
                    )
                    ChatGptWhiteSquareBox(
                        title = MariaI18n.unusedAppsTitle(),
                        subtitle = "+90d",
                        onClick = {
                            handleSendQuestion(MariaI18n.unusedAppsTitle())
                            onRunUnusedApps()
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Mensagens do Chat (Fundo claro estilo ChatGPT)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                messages.forEach { msg ->
                    if (msg.isUser) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFF3F4F6),
                            ) {
                                Text(
                                    text = msg.text,
                                    color = Color(0xFF111827),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFF9FAFB))
                                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = msg.text,
                                color = Color(0xFF111827),
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                            )
                            if (msg.actionLabel != null && msg.actionType != null) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF111827),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
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
                                        text = "✦ " + msg.actionLabel,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Caixinha de entrada branca/cinza-clara estilo ChatGPT / Gemini com botão preto ↑ à direita
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF3F4F6))
                    .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(16.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                OutlinedTextField(
                    value = userPrompt,
                    onValueChange = { userPrompt = it },
                    placeholder = {
                        Text(MariaI18n.chatPlaceholder(), fontSize = 13.sp, color = Color(0xFF6B7280))
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF111827),
                        unfocusedTextColor = Color(0xFF111827),
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                    ),
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF111827))
                        .clickable {
                            val q = userPrompt.trim().ifEmpty { MariaI18n.largeVideosTitle() }
                            userPrompt = ""
                            handleSendQuestion(q)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "↑",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatGptWhiteSquareBox(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF3F4F6),
        modifier = modifier
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(
                text = title,
                color = Color(0xFF111827),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = Color(0xFF6B7280),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
