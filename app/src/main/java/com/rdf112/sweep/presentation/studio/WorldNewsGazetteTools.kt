package com.rdf112.sweep.presentation.studio

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WorldNewsItem(
    val id: String,
    val category: String,
    val categoryBadgeHex: String,
    val timeLabel: String,
    val titlePt: String,
    val titleEn: String,
    val summaryPt: String,
    val summaryEn: String,
    val fullBodyPt: String,
    val fullBodyEn: String,
    val source: String,
)

/**
 * Motor 100% offline de Notícias & Curiosidades Diárias (Padrão Jornal / Feed Netvasco).
 * Apresenta manchetes limpas, cronológicas, por editoria com leitura agradável e sem poluição.
 */
object WorldNewsGazetteTools {

    fun getTodayDateFormatted(isPt: Boolean): String {
        val sdf = SimpleDateFormat(if (isPt) "EEEE, d 'de' MMMM" else "EEEE, MMMM d", if (isPt) Locale("pt", "BR") else Locale.US)
        return sdf.format(Date()).replaceFirstChar { it.uppercase() }
    }

    val DAILY_EDITION: List<WorldNewsItem> = listOf(
        WorldNewsItem(
            id = "news_01",
            category = "TECNOLOGIA",
            categoryBadgeHex = "#0A84FF",
            timeLabel = "Hoje às 14:15",
            titlePt = "Como duplicatas invisíveis consomem até 15 GB do seu celular",
            titleEn = "How hidden duplicates silently eat up to 15 GB of storage",
            summaryPt = "Grupos de mensagens e downloads repetem fotos e áudios idênticos sem o usuário perceber. Limpeza matemática recupera espaço sem tocar nos originais.",
            summaryEn = "Messaging groups and downloads duplicate identical photos and audios. Mathematical hash analysis frees up space while keeping original memories safe.",
            fullBodyPt = "Estudos em aparelhos Android revelam que até 30% do armazenamento ocupado por fotos e áudios consiste em cópias idênticas recebidas repetidamente em grupos de família e trabalho.\n\nCom a análise SHA-256 local, o Midiastore identifica matematicamente os bytes idênticos e mantém um original intacto e protegido, liberando espaço com total segurança.",
            fullBodyEn = "Android storage benchmarks show up to 30% of gallery space is cluttered by identical files forwarded multiple times.\n\nWith local SHA-256 hashing, Midiastore pinpoints exact duplicates while shielding one original master copy.",
            source = "Midiastore Labs Tech"
        ),
        WorldNewsItem(
            id = "news_02",
            category = "CIÊNCIA & ESPAÇO",
            categoryBadgeHex = "#BF5AF2",
            timeLabel = "Hoje às 11:30",
            titlePt = "Telescópios espaciais descobrem nova galáxia brilhante a bilhões de anos-luz",
            titleEn = "Space telescopes discover luminous newborn galaxy billions of light-years away",
            summaryPt = "Astrônomos registraram uma estrutura cósmica primitiva com altíssima taxa de nascimento de estrelas, revelando segredos do início do cosmos.",
            summaryEn = "Astronomers captured an early cosmic structure with extreme star-formation rates, unlocking secrets of the primordial cosmos.",
            fullBodyPt = "Novas imagens obtidas pela astronomia moderna revelaram uma galáxia excepcionalmente jovem e brilhante, apelidada de 'Faísca Cósmica'.\n\nOs cientistas explicam que a luz observada hoje viajou por mais de 12 bilhões de anos até alcançar nossos detectores, permitindo olhar diretamente para a infância do universo.",
            fullBodyEn = "Astronomical observations revealed an exceptionally bright young galaxy shining from the cosmic dawn.\n\nThe detected photons traveled for over 12 billion years, providing a direct glimpse into the universe's infancy.",
            source = "Agências Científicas Globais"
        ),
        WorldNewsItem(
            id = "news_03",
            category = "DICAS & FAMÍLIA",
            categoryBadgeHex = "#30D158",
            timeLabel = "Hoje às 09:45",
            titlePt = "O segredo para conservar a bateria do seu smartphone por anos",
            titleEn = "The simple secret to prolonging smartphone battery life for years",
            summaryPt = "Evitar deixar o celular descarregar até 0% e controlar o calor durante o carregamento prolonga a vida útil da bateria em mais de 50%.",
            summaryEn = "Avoiding total battery drains to 0% and minimizing heat during charging keeps modern lithium cells healthy 50% longer.",
            fullBodyPt = "Especialistas em hardware recomendam manter a carga da bateria preferencialmente entre 20% e 80% no dia a dia.\n\nO principal inimigo das baterias de íons de lítio é o calor excessivo: evitar usar capas muito grossas ao carregar e não deixar o celular no painel quente do carro são atitudes que dobram a durabilidade do aparelho.",
            fullBodyEn = "Hardware engineers suggest maintaining charge levels between 20% and 80% for optimal cycle retention.\n\nExcessive thermal buildup during charging degrades cells fastest—removing thick cases while fast-charging significantly extends device lifespan.",
            source = "Hardware & Mobile Care"
        ),
        WorldNewsItem(
            id = "news_04",
            category = "CURIOSIDADES",
            categoryBadgeHex = "#FF9F0A",
            timeLabel = "Hoje às 08:20",
            titlePt = "Você sabia? A Biblioteca de Alexandria antiga e o saber da humanidade",
            titleEn = "Did you know? The Great Library of Alexandria and human heritage",
            summaryPt = "Considerada a maior guardiã do conhecimento antigo, a biblioteca reunia pergaminhos de navegadores do mundo inteiro para serem copiados.",
            summaryEn = "Once humanity's greatest center of knowledge, ancient scholars copied every scroll aboard visiting harbor ships.",
            fullBodyPt = "No Egito antigo, todo navio que atracava no porto de Alexandria era obrigado a entregar seus manuscritos e livros para escribas copiarem.\n\nO original muitas vezes ficava na biblioteca e uma cópia fiel era devolvida ao capitão, criando o maior centro de preservação cultural da história antiga.",
            fullBodyEn = "In ancient Alexandria, all arriving harbor vessels surrendered their scrolls to official scribes.\n\nThe library retained the originals and returned verified copies to ships, assembling the ancient world's most comprehensive archive.",
            source = "Almanaque de História Clássica"
        ),
        WorldNewsItem(
            id = "news_05",
            category = "SAÚDE & BEM-ESTAR",
            categoryBadgeHex = "#64D2FF",
            timeLabel = "Hoje às 07:10",
            titlePt = "20 minutos de caminhada matinal transformam a clareza mental",
            titleEn = "20 minutes of morning walking radically elevates mental clarity",
            summaryPt = "A luz solar da manhã combinada com movimento leve sincroniza o relógio biológico, reduz a ansiedade e melhora o humor o dia inteiro.",
            summaryEn = "Morning sunlight paired with light pacing sets circadian rhythms, suppresses anxiety, and sharpens cognitive mood all day.",
            fullBodyPt = "Neurocientistas comprovam que receber luz natural nos olhos nos primeiros 30 minutos após acordar aciona a produção de dopamina e regula o cortisol.\n\nUma caminhada rápida de 15 a 20 minutos é suficiente para aumentar o foco no trabalho e garantir um sono profundo à noite.",
            fullBodyEn = "Neuroscience studies verify natural morning photon exposure calibrates biological circadian clocks and dopamine tone.\n\nA gentle 20-minute morning stroll improves daytime cognitive sharpness and sets up restorative nighttime sleep.",
            source = "Saúde & Neurociência"
        )
    )
}
