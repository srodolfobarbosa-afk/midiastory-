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
        ),
        WorldNewsItem(
            id = "news_06",
            category = "HISTÓRIA",
            categoryBadgeHex = "#FF453A",
            timeLabel = "Ontem às 21:40",
            titlePt = "Como antigos cartógrafos desenhavam monstros marinhos nos oceanos",
            titleEn = "How ancient mapmakers drew sea monsters in uncharted oceans",
            summaryPt = "Nos séculos XV e XVI, os limites dos mapas exibiam leviatãs e sereias para indicar águas perigosas e tempestades reais aos marinheiros.",
            summaryEn = "In the 15th century, unexplored oceans displayed mythical leviathans signaling real coral reefs and treacherous gale currents.",
            fullBodyPt = "Na época das grandes navegações, mapas como a 'Carta Marina' de 1539 enchiam as bordas do mundo conhecido de monstros fantásticos.\n\nHistoriadores modernos descobriram que essas ilustrações não eram mera superstição: serviam como avisos de corais perigosos, redemoinhos e rotas onde caravelas anteriores haviam naufragado.",
            fullBodyEn = "During the Age of Discovery, famous nautical charts such as the 1539 Carta Marina filled ocean borders with mythical beasts.\n\nModern cartography historians revealed these illustrations served as nautical hazard warnings for shallow reefs and lethal gale lanes.",
            source = "Sociedade Geográfica Histórica"
        ),
        WorldNewsItem(
            id = "news_07",
            category = "MEIO AMBIENTE",
            categoryBadgeHex = "#30D158",
            timeLabel = "Ontem às 19:15",
            titlePt = "Recifes de coral do Pacífico mostram surpreendente regeneração natural",
            titleEn = "Pacific coral reefs display surprising rapid natural regeneration",
            summaryPt = "Espécies de corais adaptadas ao calor recorde voltaram a crescer rapidamente em santuários marinhos protegidos.",
            summaryEn = "Heat-resilient coral colonies repopulate marine sanctuaries at unprecedented recovery velocities.",
            fullBodyPt = "Biólogos marinhos monitorando ilhas remotas no Pacífico Sul registraram o florescimento de novas colônias de corais que resistem a águas mais quentes.\n\nA proibição de pesca predatória na região permitiu que peixes herbívoros limpassem as algas das pedras, criando a base perfeita para os novos brotos de coral crescerem saudáveis.",
            fullBodyEn = "Marine ecologists surveying remote southern atolls recorded thriving micro-colonies demonstrating thermal resilience.\n\nLocal sanctuary protections enabled herbivorous fish to graze competing algae, fostering ideal conditions for rapid coral recruitment.",
            source = "EcoPlaneta & Oceanos"
        ),
        WorldNewsItem(
            id = "news_08",
            category = "INOVAÇÃO",
            categoryBadgeHex = "#0A84FF",
            timeLabel = "Ontem às 16:50",
            titlePt = "Baterias de estado sólido prometem recarga completa em menos de 10 minutos",
            titleEn = "Solid-state batteries promise complete charge in under 10 minutes",
            summaryPt = "Novos eletrólitos cerâmicos eliminam risco de superaquecimento e aumentam a densidade de energia de celulares e carros elétricos.",
            summaryEn = "Ceramic solid electrolytes eliminate thermal runaway hazards while dramatically increasing mobile energy density.",
            fullBodyPt = "Laboratórios líderes de semicondutores anunciaram protótipos de baterias com eletrólito de cerâmica pura, substituindo os líquidos inflamáveis atuais.\n\nAlém de não pegarem fogo mesmo se perfuradas, essas baterias suportam correntes ultrarrápidas, permitindo atingir 100% de carga no tempo de um café expresso.",
            fullBodyEn = "Advanced semiconductor labs unveiled solid ceramic electrolyte prototypes, replacing volatile liquid battery chemistries.\n\nIn addition to extreme thermal safety, solid-state cells accept ultra-high charging currents, reaching full capacity in minutes.",
            source = "Engenharia & Futuro"
        ),
        WorldNewsItem(
            id = "news_09",
            category = "JOGOS & MENTE",
            categoryBadgeHex = "#BF5AF2",
            timeLabel = "Ontem às 14:05",
            titlePt = "Caça-palavras e jogos dos 7 erros rejuvenescem o cérebro em até 10 anos",
            titleEn = "Word puzzles and spot-the-difference games sharpen mental age by 10 years",
            summaryPt = "Pesquisas de neurociência cognitiva mostram que 15 minutos diários de passatempos visuais estimulam novas conexões neurais e protegem a memória.",
            summaryEn = "Cognitive neuroscience reveals 15 minutes of daily visual puzzles stimulate neurogenesis and reinforce long-term memory.",
            fullBodyPt = "Um estudo acompanhando adultos acima de 50 anos concluiu que praticantes regulares de caça-palavras, quebra-cabeças e jogos de diferenças possuem velocidade de raciocínio equivalente a pessoas 10 anos mais jovens.\n\nA busca visual ativa exercita os lobos frontal e parietal, regiões essenciais para a concentração, memória de curto prazo e agilidade no cotidiano.",
            fullBodyEn = "Longitudinal cognitive studies on adults over 50 demonstrate regular puzzle enthusiasts maintain processing speeds typical of peers a decade younger.\n\nActive visual target searches engage frontal and parietal cortices, reinforcing working memory and daily cognitive stamina.",
            source = "Revista de Neurociência Cognitiva"
        )
    )

    fun getFilterCategories(isPt: Boolean): List<String> = if (isPt) {
        listOf("TODAS", "TECNOLOGIA", "CIÊNCIA & ESPAÇO", "SAÚDE & BEM-ESTAR", "DICAS & FAMÍLIA", "CURIOSIDADES", "HISTÓRIA", "INOVAÇÃO", "JOGOS & MENTE")
    } else {
        listOf("ALL", "TECNOLOGIA", "CIÊNCIA & ESPAÇO", "SAÚDE & BEM-ESTAR", "DICAS & FAMÍLIA", "CURIOSIDADES", "HISTÓRIA", "INOVAÇÃO", "JOGOS & MENTE")
    }
}
