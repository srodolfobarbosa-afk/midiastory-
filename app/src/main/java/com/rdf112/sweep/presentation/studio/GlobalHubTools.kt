package com.rdf112.sweep.presentation.studio

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

data class CountryHubInfo(
    val id: String,
    val flag: String,
    val nameEn: String,
    val namePt: String,
    val cityName: String,
    val timeZoneId: String,
    val currencyCode: String,
    val currencySymbol: String,
    val latitude: Double,
    val longitude: Double,
    val seasonSummaryEn: String,
    val seasonSummaryPt: String,
    val landmarksEn: List<String>,
    val landmarksPt: List<String>,
)

data class CountryClockSnapshot(
    val time24h: String,
    val dateLabel: String,
    val utcOffsetLabel: String,
    val deltaMinutes: Int,
    val dayDelta: Int,
)

data class CountryWeatherSnapshot(
    val tempC: Double,
    val windKmh: Double,
    val weatherCode: Int,
    val observedLabel: String,
    val isCachedOffline: Boolean,
)

/**
 * Offline-first Global Hub utilities (World Clock, Global Currency Converter,
 * Country Seasonal/Landmark Guide, and optional zero-PII Live Exchange / Open-Meteo weather updates).
 */
object GlobalHubTools {

    private const val PREFS_NAME = "midiastore_global_hub"
    private const val KEY_RATES_JSON = "cached_usd_rates_json_v1"
    private const val KEY_RATES_LABEL = "cached_usd_rates_label_v1"
    private const val KEY_WEATHER_PREFIX = "cached_weather_v1_"

    val BASELINE_USD_RATES: Map<String, Double> = linkedMapOf(
        "USD" to 1.0,
        "BRL" to 5.75,
        "EUR" to 0.93,
        "GBP" to 0.79,
        "INR" to 84.40,
        "AED" to 3.6725,
        "SAR" to 3.75,
        "RUB" to 97.50,
        "JPY" to 154.0,
        "CNY" to 7.22,
        "IDR" to 15800.0,
        "BDT" to 119.5,
        "KRW" to 1395.0,
        "MXN" to 20.40,
        "ARS" to 995.0,
    )

    val SUPPORTED_CURRENCIES: List<String> = BASELINE_USD_RATES.keys.toList()

    val COUNTRIES: List<CountryHubInfo> = listOf(
        CountryHubInfo(
            id = "BR",
            flag = "🇧🇷",
            nameEn = "Brazil",
            namePt = "Brasil",
            cityName = "São Paulo / Rio",
            timeZoneId = "America/Sao_Paulo",
            currencyCode = "BRL",
            currencySymbol = "R$",
            latitude = -23.5505,
            longitude = -46.6333,
            seasonSummaryEn = "Tropical to subtropical · Rainy Summer (Dec–Mar), Mild Winter (Jun–Aug).",
            seasonSummaryPt = "Tropical e subtropical · Verão chuvoso (Dez–Mar), Inverno ameno (Jun–Ago).",
            landmarksEn = listOf("Cristo Redentor (Rio)", "Iguazu Falls", "Sugarloaf Mountain", "Pelourinho (Salvador)"),
            landmarksPt = listOf("Cristo Redentor (Rio)", "Cataratas do Iguaçu", "Pão de Açúcar", "Pelourinho (Salvador)"),
        ),
        CountryHubInfo(
            id = "IN",
            flag = "🇮🇳",
            nameEn = "India",
            namePt = "Índia",
            cityName = "New Delhi / Mumbai",
            timeZoneId = "Asia/Kolkata",
            currencyCode = "INR",
            currencySymbol = "₹",
            latitude = 28.6139,
            longitude = 77.2090,
            seasonSummaryEn = "Pleasant Winter (Nov–Feb), Hot Summer (Mar–Jun), Monsoon rains (Jul–Sep).",
            seasonSummaryPt = "Inverno agradável (Nov–Fev), Verão quente (Mar–Jun), Monções (Jul–Set).",
            landmarksEn = listOf("Taj Mahal (Agra)", "Amber Fort (Jaipur)", "Gateway of India (Mumbai)", "Kerala Backwaters"),
            landmarksPt = listOf("Taj Mahal (Agra)", "Forte Amber (Jaipur)", "Portal da Índia (Mumbai)", "Canais de Kerala"),
        ),
        CountryHubInfo(
            id = "US",
            flag = "🇺🇸",
            nameEn = "United States",
            namePt = "Estados Unidos",
            cityName = "New York",
            timeZoneId = "America/New_York",
            currencyCode = "USD",
            currencySymbol = "$",
            latitude = 40.7128,
            longitude = -74.0060,
            seasonSummaryEn = "4 distinct seasons · Warm Summer (Jun–Aug), Cold Winter (Dec–Feb).",
            seasonSummaryPt = "4 estações definidas · Verão quente (Jun–Ago), Inverno frio (Dez–Fev).",
            landmarksEn = listOf("Statue of Liberty", "Grand Canyon", "Golden Gate Bridge", "Central Park"),
            landmarksPt = listOf("Estátua da Liberdade", "Grand Canyon", "Ponte Golden Gate", "Central Park"),
        ),
        CountryHubInfo(
            id = "AE",
            flag = "🇦🇪",
            nameEn = "United Arab Emirates",
            namePt = "Emirados Árabes",
            cityName = "Dubai",
            timeZoneId = "Asia/Dubai",
            currencyCode = "AED",
            currencySymbol = "د.إ",
            latitude = 25.2048,
            longitude = 55.2708,
            seasonSummaryEn = "Desert sun year-round · Best outdoor weather Oct–Apr; very hot Jun–Sep.",
            seasonSummaryPt = "Sol o ano todo · Melhor época amena de Out–Abr; muito quente de Jun–Set.",
            landmarksEn = listOf("Burj Khalifa", "Sheikh Zayed Grand Mosque", "Museum of the Future", "Palm Jumeirah"),
            landmarksPt = listOf("Burj Khalifa", "Grande Mesquita Sheikh Zayed", "Museu do Futuro", "Palm Jumeirah"),
        ),
        CountryHubInfo(
            id = "SA",
            flag = "🇸🇦",
            nameEn = "Saudi Arabia",
            namePt = "Arábia Saudita",
            cityName = "Riyadh",
            timeZoneId = "Asia/Riyadh",
            currencyCode = "SAR",
            currencySymbol = "﷼",
            latitude = 24.7136,
            longitude = 46.6753,
            seasonSummaryEn = "Arid desert climate · Cool pleasant evenings Nov–Mar; intense summer heat.",
            seasonSummaryPt = "Clima desértico · Noites amenas de Nov–Mar; verão bem quente.",
            landmarksEn = listOf("AlUla Hegra", "Kingdom Centre Sky Bridge", "Diriyah Historical City", "Red Sea Coast"),
            landmarksPt = listOf("AlUla Hegra", "Sky Bridge de Riad", "Cidade Histórica Diriyah", "Mar Vermelho"),
        ),
        CountryHubInfo(
            id = "RU",
            flag = "🇷🇺",
            nameEn = "Russia",
            namePt = "Rússia",
            cityName = "Moscow",
            timeZoneId = "Europe/Moscow",
            currencyCode = "RUB",
            currencySymbol = "₽",
            latitude = 55.7558,
            longitude = 37.6173,
            seasonSummaryEn = "Continental · Snowy Winter (Nov–Mar), Mild Sunny Summer (Jun–Aug).",
            seasonSummaryPt = "Continental · Inverno com neve (Nov–Mar), Verão ensolarado e ameno (Jun–Ago).",
            landmarksEn = listOf("Red Square & Kremlin", "Hermitage Museum (St. Petersburg)", "Lake Baikal", "Peterhof Palace"),
            landmarksPt = listOf("Praça Vermelha e Kremlin", "Museu Hermitage", "Lago Baikal", "Palácio Peterhof"),
        ),
        CountryHubInfo(
            id = "CN",
            flag = "🇨🇳",
            nameEn = "China",
            namePt = "China",
            cityName = "Beijing / Shanghai",
            timeZoneId = "Asia/Shanghai",
            currencyCode = "CNY",
            currencySymbol = "¥",
            latitude = 39.9042,
            longitude = 116.4074,
            seasonSummaryEn = "Crisp Spring & Autumn (Apr–May, Sep–Oct); hot Summer, cold northern Winter.",
            seasonSummaryPt = "Primavera e Outono amenos (Abr–Mai, Set–Out); verão quente, inverno frio no norte.",
            landmarksEn = listOf("Great Wall of China", "Forbidden City (Beijing)", "Terracotta Army (Xi'an)", "The Bund (Shanghai)"),
            landmarksPt = listOf("Grande Muralha da China", "Cidade Proibida", "Guerreiros de Terracota", "The Bund (Xangai)"),
        ),
        CountryHubInfo(
            id = "JP",
            flag = "🇯🇵",
            nameEn = "Japan",
            namePt = "Japão",
            cityName = "Tokyo",
            timeZoneId = "Asia/Tokyo",
            currencyCode = "JPY",
            currencySymbol = "¥",
            latitude = 35.6762,
            longitude = 139.6503,
            seasonSummaryEn = "Cherry Blossoms (Mar–Apr), Rainy Jun, Vibrant Autumn leaves (Oct–Nov).",
            seasonSummaryPt = "Cerejeiras (Mar–Abr), Chuvas em Junho, Folhas de Outono (Out–Nov).",
            landmarksEn = listOf("Mount Fuji", "Fushimi Inari (Kyoto)", "Senso-ji Temple (Tokyo)", "Miyajima Torii"),
            landmarksPt = listOf("Monte Fuji", "Fushimi Inari (Kyoto)", "Templo Senso-ji", "Ilha Miyajima"),
        ),
        CountryHubInfo(
            id = "ID",
            flag = "🇮🇩",
            nameEn = "Indonesia",
            namePt = "Indonésia",
            cityName = "Jakarta / Bali",
            timeZoneId = "Asia/Jakarta",
            currencyCode = "IDR",
            currencySymbol = "Rp",
            latitude = -6.2088,
            longitude = 106.8456,
            seasonSummaryEn = "Equatorial warm climate · Dry sunny season (May–Sep), Wet season (Nov–Mar).",
            seasonSummaryPt = "Tropical equatorial · Estação seca ensolarada (Mai–Set), Chuvas (Nov–Mar).",
            landmarksEn = listOf("Borobudur Temple", "Uluwatu Temple (Bali)", "Komodo National Park", "Prambanan"),
            landmarksPt = listOf("Templo Borobudur", "Templo Uluwatu (Bali)", "Parque Komodo", "Prambanan"),
        ),
        CountryHubInfo(
            id = "BD",
            flag = "🇧🇩",
            nameEn = "Bangladesh",
            namePt = "Bangladesh",
            cityName = "Dhaka",
            timeZoneId = "Asia/Dhaka",
            currencyCode = "BDT",
            currencySymbol = "৳",
            latitude = 23.8103,
            longitude = 90.4125,
            seasonSummaryEn = "Cool Winter (Nov–Feb), Humid Summer & Monsoon rivers (Jun–Oct).",
            seasonSummaryPt = "Inverno ameno (Nov–Fev), Verão úmido e Monções (Jun–Out).",
            landmarksEn = listOf("Sundarbans Mangrove", "Cox's Bazar Sea Beach", "Lalbagh Fort", "Sixty Dome Mosque"),
            landmarksPt = listOf("Manguezal Sundarbans", "Praia Cox's Bazar", "Forte Lalbagh", "Mesquita 60 Cúpulas"),
        ),
        CountryHubInfo(
            id = "KR",
            flag = "🇰🇷",
            nameEn = "South Korea",
            namePt = "Coreia do Sul",
            cityName = "Seoul",
            timeZoneId = "Asia/Seoul",
            currencyCode = "KRW",
            currencySymbol = "₩",
            latitude = 37.5665,
            longitude = 126.9780,
            seasonSummaryEn = "Mild Spring/Autumn (Apr–May, Sep–Oct); cold dry Winter, humid Jul–Aug.",
            seasonSummaryPt = "Primavera/Outono suaves; inverno seco e frio, verão úmido (Jul–Ago).",
            landmarksEn = listOf("Gyeongbokgung Palace", "Bukchon Hanok Village", "Jeju Island", "N Seoul Tower"),
            landmarksPt = listOf("Palácio Gyeongbokgung", "Vila Bukchon Hanok", "Ilha de Jeju", "Torre N Seoul"),
        ),
        CountryHubInfo(
            id = "ES",
            flag = "🇪🇸",
            nameEn = "Spain",
            namePt = "Espanha",
            cityName = "Madrid / Barcelona",
            timeZoneId = "Europe/Madrid",
            currencyCode = "EUR",
            currencySymbol = "€",
            latitude = 40.4168,
            longitude = -3.7038,
            seasonSummaryEn = "Mediterranean & continental · Sunny dry Summers, mild Winters on coast.",
            seasonSummaryPt = "Mediterrâneo · Verões quentes e secos, invernos suaves no litoral.",
            landmarksEn = listOf("Sagrada Família (Barcelona)", "Alhambra (Granada)", "Prado Museum (Madrid)", "Plaza de España (Seville)"),
            landmarksPt = listOf("Sagrada Família", "Alhambra (Granada)", "Museu do Prado", "Praça de Espanha (Sevilha)"),
        ),
        CountryHubInfo(
            id = "FR",
            flag = "🇫🇷",
            nameEn = "France",
            namePt = "França",
            cityName = "Paris",
            timeZoneId = "Europe/Paris",
            currencyCode = "EUR",
            currencySymbol = "€",
            latitude = 48.8566,
            longitude = 2.3522,
            seasonSummaryEn = "Temperate · Best Spring & Autumn (Apr–Jun, Sep–Oct); crisp Winter.",
            seasonSummaryPt = "Temperado · Melhor na Primavera/Outono; inverno fresco.",
            landmarksEn = listOf("Eiffel Tower", "Louvre Museum", "Mont-Saint-Michel", "Palace of Versailles"),
            landmarksPt = listOf("Torre Eiffel", "Museu do Louvre", "Monte Saint-Michel", "Palácio de Versalhes"),
        ),
        CountryHubInfo(
            id = "DE",
            flag = "🇩🇪",
            nameEn = "Germany",
            namePt = "Alemanha",
            cityName = "Berlin / Munich",
            timeZoneId = "Europe/Berlin",
            currencyCode = "EUR",
            currencySymbol = "€",
            latitude = 52.5200,
            longitude = 13.4050,
            seasonSummaryEn = "Temperate · Warm Summer festivals (Jun–Aug), festive snowy Decembers.",
            seasonSummaryPt = "Temperado · Verões agradáveis (Jun–Ago) e mercados de Natal em Dezembro.",
            landmarksEn = listOf("Brandenburg Gate (Berlin)", "Neuschwanstein Castle", "Cologne Cathedral", "Black Forest"),
            landmarksPt = listOf("Portão de Brandemburgo", "Castelo Neuschwanstein", "Catedral de Colônia", "Floresta Negra"),
        ),
        CountryHubInfo(
            id = "GB",
            flag = "🇬🇧",
            nameEn = "United Kingdom",
            namePt = "Reino Unido",
            cityName = "London",
            timeZoneId = "Europe/London",
            currencyCode = "GBP",
            currencySymbol = "£",
            latitude = 51.5072,
            longitude = -0.1276,
            seasonSummaryEn = "Mild maritime · Carry an umbrella anytime; brightest days May–Sep.",
            seasonSummaryPt = "Marítimo ameno · Sempre leve capa de chuva; dias longos de Mai–Set.",
            landmarksEn = listOf("Big Ben & Westminster", "British Museum", "Tower Bridge", "Edinburgh Castle"),
            landmarksPt = listOf("Big Ben e Westminster", "Museu Britânico", "Tower Bridge", "Castelo de Edimburgo"),
        ),
        CountryHubInfo(
            id = "MX",
            flag = "🇲🇽",
            nameEn = "Mexico",
            namePt = "México",
            cityName = "Mexico City",
            timeZoneId = "America/Mexico_City",
            currencyCode = "MXN",
            currencySymbol = "$",
            latitude = 19.4326,
            longitude = -99.1332,
            seasonSummaryEn = "Pleasant dry season Nov–Apr; afternoon summer rains Jun–Sep.",
            seasonSummaryPt = "Estação seca agradável Nov–Abr; chuvas à tarde Jun–Set.",
            landmarksEn = listOf("Chichén Itzá", "Teotihuacan Pyramids", "Chapultepec Castle", "Cancún & Riviera Maya"),
            landmarksPt = listOf("Chichén Itzá", "Pirâmides de Teotihuacan", "Castelo de Chapultepec", "Riviera Maya"),
        ),
    )

    /** Offline-safe calculation using JVM TimeZone database (zero network required). */
    fun formatCountryClock(
        country: CountryHubInfo,
        referenceTz: TimeZone = TimeZone.getDefault(),
        nowMillis: Long = System.currentTimeMillis(),
        locale: Locale = Locale.getDefault(),
    ): CountryClockSnapshot {
        val targetTz = TimeZone.getTimeZone(country.timeZoneId)
        val now = Date(nowMillis)
        val timeFmt = SimpleDateFormat("HH:mm:ss", locale).apply { timeZone = targetTz }
        val dateFmt = SimpleDateFormat("EEE, dd MMM", locale).apply { timeZone = targetTz }

        val targetOffsetMs = targetTz.getOffset(nowMillis)
        val localOffsetMs = referenceTz.getOffset(nowMillis)
        val deltaMinutes = (targetOffsetMs - localOffsetMs) / 60_000

        val targetCal = Calendar.getInstance(targetTz).apply { timeInMillis = nowMillis }
        val localCal = Calendar.getInstance(referenceTz).apply { timeInMillis = nowMillis }
        val targetDay = targetCal.get(Calendar.YEAR) * 400 + targetCal.get(Calendar.DAY_OF_YEAR)
        val localDay = localCal.get(Calendar.YEAR) * 400 + localCal.get(Calendar.DAY_OF_YEAR)
        val dayDelta = (targetDay - localDay).coerceIn(-1, 1)

        val offsetMinTotal = targetOffsetMs / 60_000
        val sign = if (offsetMinTotal >= 0) "+" else "-"
        val absMin = abs(offsetMinTotal)
        val hrs = absMin / 60
        val mins = absMin % 60
        val utcLabel = if (mins == 0) "UTC$sign$hrs" else String.format(Locale.US, "UTC%s%d:%02d", sign, hrs, mins)

        return CountryClockSnapshot(
            time24h = timeFmt.format(now),
            dateLabel = dateFmt.format(now),
            utcOffsetLabel = utcLabel,
            deltaMinutes = deltaMinutes,
            dayDelta = dayDelta,
        )
    }

    fun formatOffsetDelta(deltaMinutes: Int): String {
        if (deltaMinutes == 0) return "±0h"
        val sign = if (deltaMinutes > 0) "+" else "-"
        val absMin = abs(deltaMinutes)
        val h = absMin / 60
        val m = absMin % 60
        return if (m == 0) "$sign${h}h" else "$sign${h}h${String.format(Locale.US, "%02d", m)}"
    }

    /** Pure deterministic conversion across any supported currencies via USD pivot. */
    fun convertCurrency(
        amount: Double,
        fromCode: String,
        toCode: String,
        usdRates: Map<String, Double> = BASELINE_USD_RATES,
    ): Double? {
        if (!amount.isFinite()) return null
        val fromRate = usdRates[fromCode] ?: return null
        val toRate = usdRates[toCode] ?: return null
        if (fromRate <= 0.0 || toRate <= 0.0) return null
        return (amount / fromRate) * toRate
    }

    fun loadCachedRates(context: Context): Pair<Map<String, Double>, String?> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawJson = prefs.getString(KEY_RATES_JSON, null)
        val label = prefs.getString(KEY_RATES_LABEL, null)
        if (rawJson.isNullOrBlank()) return BASELINE_USD_RATES to null
        return try {
            val obj = JSONObject(rawJson)
            val parsed = LinkedHashMap<String, Double>(BASELINE_USD_RATES)
            SUPPORTED_CURRENCIES.forEach { code ->
                val v = obj.optDouble(code, Double.NaN)
                if (v.isFinite() && v > 0.0) parsed[code] = v
            }
            parsed to label
        } catch (_: Exception) {
            BASELINE_USD_RATES to null
        }
    }

    suspend fun fetchLiveUsdRatesIfOnline(context: Context): Pair<Map<String, Double>, String?> =
        withContext(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                conn = (URL("https://open.er-api.com/v6/latest/USD").openConnection() as HttpURLConnection).apply {
                    connectTimeout = 4500
                    readTimeout = 4500
                    requestMethod = "GET"
                }
                if (conn.responseCode in 200..299) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    val ratesObj = json.optJSONObject("rates")
                    if (ratesObj != null) {
                        val merged = LinkedHashMap<String, Double>(BASELINE_USD_RATES)
                        val storeObj = JSONObject()
                        SUPPORTED_CURRENCIES.forEach { code ->
                            val r = ratesObj.optDouble(code, Double.NaN)
                            if (r.isFinite() && r > 0.0) {
                                merged[code] = r
                            }
                            storeObj.put(code, merged[code])
                        }
                        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
                        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                            .putString(KEY_RATES_JSON, storeObj.toString())
                            .putString(KEY_RATES_LABEL, stamp)
                            .apply()
                        return@withContext merged to stamp
                    }
                }
            } catch (_: Exception) {
            } finally {
                conn?.disconnect()
            }
            loadCachedRates(context)
        }

    fun loadCachedWeather(context: Context, countryId: String): CountryWeatherSnapshot? {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("$KEY_WEATHER_PREFIX$countryId", null)
            ?: return null
        return try {
            val obj = JSONObject(raw)
            CountryWeatherSnapshot(
                tempC = obj.getDouble("tempC"),
                windKmh = obj.getDouble("windKmh"),
                weatherCode = obj.getInt("weatherCode"),
                observedLabel = obj.getString("observedLabel"),
                isCachedOffline = true,
            )
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchLiveCountryWeather(
        context: Context,
        country: CountryHubInfo,
    ): CountryWeatherSnapshot? = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val endpoint = String.format(
                Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current_weather=true",
                country.latitude,
                country.longitude,
            )
            conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                connectTimeout = 4500
                readTimeout = 4500
                requestMethod = "GET"
            }
            if (conn.responseCode in 200..299) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val current = JSONObject(body).optJSONObject("current_weather")
                if (current != null) {
                    val tempC = current.optDouble("temperature", Double.NaN)
                    val windKmh = current.optDouble("windspeed", 0.0)
                    val code = current.optInt("weathercode", 0)
                    if (tempC.isFinite()) {
                        val stamp = SimpleDateFormat("HH:mm", Locale.US).format(Date())
                        val snap = CountryWeatherSnapshot(
                            tempC = tempC,
                            windKmh = windKmh,
                            weatherCode = code,
                            observedLabel = stamp,
                            isCachedOffline = false,
                        )
                        val store = JSONObject().apply {
                            put("tempC", tempC)
                            put("windKmh", windKmh)
                            put("weatherCode", code)
                            put("observedLabel", stamp)
                        }
                        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                            .putString("$KEY_WEATHER_PREFIX${country.id}", store.toString())
                            .apply()
                        return@withContext snap
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            conn?.disconnect()
        }
        loadCachedWeather(context, country.id)
    }

    fun describeWmoWeatherCode(code: Int, isPortuguese: Boolean): String = when (code) {
        0 -> if (isPortuguese) "☀️ Céu limpo" else "☀️ Clear sky"
        1, 2 -> if (isPortuguese) "🌤️ Parcialmente nublado" else "🌤️ Partly cloudy"
        3 -> if (isPortuguese) "☁️ Nublado" else "☁️ Overcast"
        45, 48 -> if (isPortuguese) "🌫️ Neblina" else "🌫️ Foggy"
        51, 53, 55, 61, 63, 65 -> if (isPortuguese) "🌧️ Chuva" else "🌧️ Rain"
        71, 73, 75 -> if (isPortuguese) "❄️ Neve" else "❄️ Snow"
        80, 81, 82 -> if (isPortuguese) "🌦️ Pancadas de chuva" else "🌦️ Rain showers"
        95, 96, 99 -> if (isPortuguese) "⛈️ Tempestade" else "⛈️ Thunderstorm"
        else -> if (isPortuguese) "🌡️ Clima ativo" else "🌡️ Active weather"
    }

    fun buildMapsIntent(context: Context, query: String): Intent {
        val geoIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(query)}"))
        return if (geoIntent.resolveActivity(context.packageManager) != null) {
            geoIntent
        } else {
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}"),
            )
        }
    }
}
