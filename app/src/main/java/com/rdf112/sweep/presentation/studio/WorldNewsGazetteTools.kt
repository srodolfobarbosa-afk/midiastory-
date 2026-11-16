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
 * Motor 100% offline de Notícias Diárias & Curiosidades (Padrão Jornal Netvasco / Dupla Perspectiva).
 * Organizado em duas edições selecionáveis:
 * 1. COUNTRY (Meu País): Alertas de segurança no WhatsApp, cuidados de bateria, saúde e utilidades do dia a dia.
 * 2. WORLD (Mundo & Descobertas): Astronomia, arqueologia antiga, inovações científicas e segredos do planeta.
 */
object WorldNewsGazetteTools {

    enum class NewsScope {
        COUNTRY,
        WORLD,
    }

    fun getTodayDateFormatted(isPt: Boolean): String {
        val sdf = SimpleDateFormat(if (isPt) "EEEE, d 'de' MMMM" else "EEEE, MMMM d", if (isPt) Locale("pt", "BR") else Locale.US)
        return sdf.format(Date()).replaceFirstChar { it.uppercase() }
    }

    val COUNTRY_EDITION: List<WorldNewsItem> = listOf(
        WorldNewsItem(
            id = "news_br_01",
            category = "SEGURANÇA & DICAS",
            categoryBadgeHex = "#0A84FF",
            timeLabel = "Hoje às 14:30",
            titlePt = "Alerta: Como blindar seu WhatsApp contra o golpe do falso parente",
            titleEn = "Alert: How to shield WhatsApp against family impersonation scams",
            summaryPt = "Criminosos usam fotos públicas para pedir dinheiro com números novos. Ativar confirmação em 2 etapas e checar por ligação bloqueia o golpe.",
            summaryEn = "Scammers use public profile photos to request emergency funds from new numbers. Enabling 2-step verification and verifying via voice calls defeats scams.",
            fullBodyPt = "Criminosos virtuais utilizam cada vez mais fotos públicas de perfis para abordar familiares alegando terem trocado de número.\n\nEspecialistas em segurança cibernética orientam duas regras de ouro: nunca faça transferências ou pagamentos sem antes ligar para o número antigo do familiar para ouvir a voz dele, e ative a 'Confirmação em duas etapas' nas configurações de conta do seu WhatsApp com uma senha PIN que só você conhece.",
            fullBodyEn = "Scammers increasingly scrape public profile photos to message family members claiming they switched numbers.\n\nCybersecurity experts emphasize two golden rules: never transfer funds without voice-calling the family member's known number first, and enable Two-Step PIN verification in WhatsApp settings.",
            source = "Segurança Digital Brasil"
        ),
        WorldNewsItem(
            id = "news_br_02",
            category = "DICAS DE CELULAR",
            categoryBadgeHex = "#30D158",
            timeLabel = "Hoje às 11:15",
            titlePt = "O hábito no carregador que estraga a bateria do seu smartphone",
            titleEn = "The charging habit silently degrading your smartphone battery",
            summaryPt = "Carregar o celular com capas grossas abafa o calor interno e acelera o desgaste das células. Tirar a capa durante a recarga dobra a vida útil.",
            summaryEn = "Charging phones with heavy protective cases traps heat and degrades lithium cells. Removing the case while charging doubles lifespan.",
            fullBodyPt = "O principal inimigo das baterias de íons de lítio modernas não é a quantidade de vezes que você carrega, mas sim o calor acumulado durante o carregamento rápido.\n\nEspecialistas em eletrônica recomendam retirar capas plásticas grossas ao colocar o celular na tomada e evitar deixar o aparelho carregando embaixo do travesseiro ou no painel quente do carro.",
            fullBodyEn = "The primary enemy of modern lithium-ion batteries is not charging frequency, but thermal buildup during fast-charging cycles.\n\nHardware engineers recommend removing thick plastic cases before plugging in and never leaving devices charging under pillows or on warm car dashboards.",
            source = "Hardware & Cuidados Mobile"
        ),
        WorldNewsItem(
            id = "news_br_03",
            category = "SAÚDE & FAMÍLIA",
            categoryBadgeHex = "#64D2FF",
            timeLabel = "Hoje às 08:40",
            titlePt = "20 minutos de caminhada matinal equilibram o sono e a pressão",
            titleEn = "20 minutes of morning walking balances nighttime sleep and blood pressure",
            summaryPt = "Receber a claridade da manhã logo após acordar aciona a produção de serotonina e prepara o corpo para um sono profundo à noite.",
            summaryEn = "Exposing your eyes to natural morning photons triggers serotonin and prepares circadian rhythm for deep restorative sleep.",
            fullBodyPt = "Médicos e neurocientistas comprovam que caminhar levemente ao ar livre nos primeiros 45 minutos da manhã sincroniza o relógio biológico humano.\n\nA luz natural nos olhos sinaliza ao cérebro para cessar a melatonina e produzir dopamina e serotonina, resultando em maior disposição durante o dia e um descanso profundo à noite.",
            fullBodyEn = "Physicians and neuroscientists verify light morning walking within 45 minutes of waking calibrates the biological circadian pacemaker.\n\nNatural photon exposure triggers daytime dopamine release, ensuring higher alertness by day and deep sleep at night.",
            source = "Medicina & Bem-Estar"
        ),
        WorldNewsItem(
            id = "news_br_04",
            category = "BEM-ESTAR & NATUREZA",
            categoryBadgeHex = "#30D158",
            timeLabel = "Ontem às 18:20",
            titlePt = "Chás brasileiros que ajudam na digestão e no relaxamento noturno",
            titleEn = "Natural herbal infusions that improve digestion and evening relaxation",
            summaryPt = "Erva-doce, camomila e capim-cidreira possuem compostos bioativos reconhecidos pela farmacologia para acalmar o estômago e a mente.",
            summaryEn = "Fennel, chamomile, and lemon balm contain verified bioactive compounds soothing digestion and calming the nervous system.",
            fullBodyPt = "O Brasil possui uma das maiores riquezas botânicas do mundo. Infusões preparadas com erva-cidreira, camomila e melissa liberam óleos essenciais como o linalol e flavonoides que relaxam as paredes do sistema digestivo.\n\nTomar uma xícara morna 30 minutos antes de se deitar é um ritual natural consagrado por gerações para desacelerar a mente após um dia corrido.",
            fullBodyEn = "Brazil harbors unmatched botanical diversity. Infusions of lemon balm, chamomile, and fennel release essential bioactive flavonoids relaxing gastrointestinal muscle.\n\nA warm cup 30 minutes before bed represents a time-tested natural ritual to decelerate evening cognitive pace.",
            source = "Guia de Fitoterapia & Saúde"
        ),
        WorldNewsItem(
            id = "news_br_05",
            category = "UTILIDADE & MEMÓRIA",
            categoryBadgeHex = "#0A84FF",
            timeLabel = "Ontem às 15:10",
            titlePt = "Como duplicatas invisíveis consomem até 15 GB do seu aparelho",
            titleEn = "How invisible duplicate files silently occupy up to 15 GB of storage",
            summaryPt = "Grupos de família e mensagens encaminhadas acumulam cópias idênticas de imagens e áudios. Limpeza inteligente recupera espaço sem tocar nas fotos reais.",
            summaryEn = "Family chat groups and forwarded memes hoard identical photo and audio copies. Smart hash cleaning reclaims space without touching precious memories.",
            fullBodyPt = "Estudos em celulares Android mostram que mais de 25% da galeria de fotos de famílias consiste em arquivos recebidos repetidamente em grupos de WhatsApp.\n\nO Midiastore utiliza verificação matemática SHA-256 local para encontrar cópias idênticas sem olhar suas fotos privadas, mantendo uma cópia original perfeitamente protegida.",
            fullBodyEn = "Diagnostic audits on Android family devices show over 25% of storage is cluttered by identical forwarded memes and audios.\n\nMidiastore uses mathematical SHA-256 hashing to find duplicates locally, preserving master originals with absolute privacy.",
            source = "Laboratório de Tecnologia Midiastore"
        ),
        WorldNewsItem(
            id = "news_br_06",
            category = "INOVAÇÃO BRASIL",
            categoryBadgeHex = "#FF9F0A",
            timeLabel = "Ontem às 10:30",
            titlePt = "Energia solar em telhados de residências bate recorde histórico no país",
            titleEn = "Residential rooftop solar installations reach historic milestone",
            summaryPt = "Mais de 2 milhões de lares brasileiros já geram a própria eletricidade limpa, reduzindo a conta de luz em até 80%.",
            summaryEn = "Over 2 million Brazilian households now generate clean electricity, cutting residential utility bills by up to 80%.",
            fullBodyPt = "A geração distribuída de energia solar em telhados de casas brasileiras ultrapassou marcas históricas este ano.\n\nA combinação de alta insolação no território nacional e equipamentos mais acessíveis permitiu que famílias de todas as regiões economizem nas contas mensais enquanto contribuem para a preservação ambiental.",
            fullBodyEn = "Decentralized rooftop solar across Brazilian households broke historical generation records this season.\n\nAbundant sunlight combined with accessible silicon technology allows families across all states to slash electricity bills while fostering sustainability.",
            source = "Associação de Energia & Sustentabilidade"
        )
    )

    val WORLD_EDITION: List<WorldNewsItem> = listOf(
        WorldNewsItem(
            id = "news_wd_01",
            category = "CIÊNCIA & COSMOS",
            categoryBadgeHex = "#BF5AF2",
            timeLabel = "Hoje às 13:45",
            titlePt = "Telescópios espaciais descobrem nova galáxia brilhante no início do cosmos",
            titleEn = "Space telescopes discover luminous newborn galaxy in early cosmos",
            summaryPt = "Astrônomos registraram uma estrutura primitiva brilhando a mais de 12 bilhões de anos-luz, revelando a infância do universo.",
            summaryEn = "Astronomers captured a primordial structure shining 12 billion light-years away, unlocking secrets of the universe's infancy.",
            fullBodyPt = "Novas imagens obtidas pela astronomia moderna revelaram uma galáxia excepcionalmente jovem e brilhante.\n\nOs cientistas explicam que a luz observada hoje viajou por mais de 12 bilhões de anos até alcançar nossos detectores, permitindo olhar diretamente para o momento em que as primeiras estrelas estavam nascendo.",
            fullBodyEn = "State-of-the-art space observatories captured photons emitted over 12 billion light-years ago from a newborn galaxy.\n\nThis cosmic glimpse allows astrophysicists to inspect primordial star nurseries active shortly after the Big Bang.",
            source = "Agências Científicas Globais"
        ),
        WorldNewsItem(
            id = "news_wd_02",
            category = "HISTÓRIA ANTIGA",
            categoryBadgeHex = "#FF453A",
            timeLabel = "Hoje às 10:20",
            titlePt = "O segredo do concreto romano que dura mais de 2.000 anos sem trincar",
            titleEn = "The secret of ancient Roman concrete surviving 2,000 years without cracking",
            summaryPt = "Cientistas do MIT descobriram que os romanos misturavam cal virgem quente, criando um concreto que se autorregenera com água da chuva.",
            summaryEn = "MIT researchers discovered ancient Romans mixed hot quicklime, creating concrete that naturally self-heals when touched by rain.",
            fullBodyPt = "Monumentos como o Panteão e o Coliseu em Roma continuam de pé após dois milênios desafiando o tempo. Pesquisadores descobriram que os romanos incluíam pedregulhos de cal virgem na massa.\n\nQuando uma microtrinca surge e a chuva entra, a cal reage quimicamente e cristaliza calcita, cicatrizando a rachadura automaticamente.",
            fullBodyEn = "Structures like the Pantheon in Rome survive after two millennia defying erosion. MIT scientists proved Roman masons incorporated quicklime clasts into mortar.\n\nWhen micro-cracks form and rainwater enters, the lime recrystallizes into calcite, self-healing fractures in situ.",
            source = "Ciência dos Materiais & Arqueologia"
        ),
        WorldNewsItem(
            id = "news_wd_03",
            category = "NATUREZA & OCEANOS",
            categoryBadgeHex = "#30D158",
            timeLabel = "Hoje às 07:50",
            titlePt = "Recifes de coral do Pacífico mostram surpreendente regeneração acelerada",
            titleEn = "Pacific coral reefs display surprising rapid natural regeneration",
            summaryPt = "Espécies adaptadas a águas quentes voltaram a florescer em santuários marinhos protegidos após proibição de pesca predatória.",
            summaryEn = "Heat-resilient coral species repopulate marine sanctuaries at record speeds following strict protections.",
            fullBodyPt = "Biólogos marinhos monitorando ilhas remotas no Pacífico Sul registraram o florescimento de novas colônias de corais que resistem a variações de temperatura.\n\nA criação de áreas de proteção ambiental permitiu que peixes herbívoros limpassem as algas das rochas, criando as condições ideais para a recuperação dos corais.",
            fullBodyEn = "Marine ecologists monitoring remote South Pacific atolls verified thriving micro-colonies demonstrating thermal resilience.\n\nMarine reserve protections permitted herbivorous fish to graze competing algae, fostering ideal conditions for rapid coral recruitment.",
            source = "EcoPlaneta & Oceanos"
        ),
        WorldNewsItem(
            id = "news_wd_04",
            category = "INOVAÇÃO & FUTURO",
            categoryBadgeHex = "#0A84FF",
            timeLabel = "Ontem às 20:10",
            titlePt = "Baterias de cerâmica pura prometem recarga completa em menos de 10 minutos",
            titleEn = "Ceramic solid-state batteries promise complete charge in under 10 minutes",
            summaryPt = "Novos eletrólitos de estado sólido eliminam risco de superaquecimento e multiplicam a capacidade de celulares e carros elétricos.",
            summaryEn = "Solid-state ceramic electrolytes eliminate fire hazards while dramatically increasing mobile battery energy density.",
            fullBodyPt = "Laboratórios globais de semicondutores apresentaram protótipos de baterias com eletrólitos de cerâmica sólida, dispensando líquidos inflamáveis.\n\nAlém de não superaquecerem mesmo se danificadas, essas células suportam correntes ultrarrápidas, permitindo atingir 100% de carga no tempo de um café.",
            fullBodyEn = "Advanced material laboratories demonstrated solid-state ceramic electrolyte prototypes, replacing volatile liquid chemistries.\n\nIn addition to fireproof safety, solid-state cells accept ultra-high charging currents, reaching full capacity in minutes.",
            source = "Engenharia & Futuro"
        ),
        WorldNewsItem(
            id = "news_wd_05",
            category = "CURIOSIDADES HISTÓRICAS",
            categoryBadgeHex = "#FF9F0A",
            timeLabel = "Ontem às 16:40",
            titlePt = "Como antigos marinheiros desenhavam monstros nos mapas oceânicos",
            titleEn = "How ancient navigators illustrated sea monsters on oceanic charts",
            summaryPt = "Nos séculos XV e XVI, os limites dos mapas exibiam leviatãs e sereias para alertar sobre recifes perigosos e tempestades reais.",
            summaryEn = "In the 15th century, unexplored oceans displayed mythical beasts signaling real coral reefs and deadly currents.",
            fullBodyPt = "Na época das grandes navegações marítimas, mapas como a famosa 'Carta Marina' de 1539 enchiam as bordas do mundo conhecido de monstros marinhos.\n\nHistoriadores revelaram que essas ilustrações não eram mera superstição: serviam como avisos de corais pontiagudos, redemoinhos e rotas onde embarcações anteriores haviam naufragado.",
            fullBodyEn = "During the Age of Discovery, famous nautical charts like the 1539 Carta Marina filled open waters with sea monsters.\n\nModern cartography historians revealed these illustrations served as nautical hazard warnings for shallow reefs and lethal gale lanes.",
            source = "Sociedade Geográfica Histórica"
        ),
        WorldNewsItem(
            id = "news_wd_06",
            category = "MENTE & JOGOS",
            categoryBadgeHex = "#BF5AF2",
            timeLabel = "Ontem às 11:15",
            titlePt = "Caça-palavras e jogos dos 7 erros rejuvenescem o cérebro em 10 anos",
            titleEn = "Word puzzles and spot-the-difference games sharpen mental age by 10 years",
            summaryPt = "Pesquisas em neurociência comprovam que 15 minutos diários de quebra-cabeças visuais estimulam conexões neurais e fortalecem a memória.",
            summaryEn = "Cognitive neuroscience reveals 15 minutes of daily visual puzzles stimulate neurogenesis and reinforce long-term memory.",
            fullBodyPt = "Um estudo rigoroso com adultos acima de 50 anos concluiu que praticantes regulares de caça-palavras e jogos de diferenças possuem velocidade de raciocínio comparável à de pessoas 10 anos mais jovens.\n\nA busca visual ativa exercita os lobos frontal e parietal, regiões essenciais para a concentração, memória de curto prazo e agilidade mental.",
            fullBodyEn = "Longitudinal cognitive studies on adults over 50 demonstrate regular puzzle enthusiasts maintain processing speeds typical of peers a decade younger.\n\nActive visual target searches engage frontal and parietal cortices, reinforcing working memory and daily cognitive stamina.",
            source = "Revista de Neurociência Cognitiva"
        )
    )

    fun getNewsForScope(scope: NewsScope): List<WorldNewsItem> = when (scope) {
        NewsScope.COUNTRY -> COUNTRY_EDITION
        NewsScope.WORLD -> WORLD_EDITION
    }

    fun getFilterCategories(scope: NewsScope, isPt: Boolean): List<String> {
        val allLabel = if (isPt) "TODAS" else "ALL"
        val edition = getNewsForScope(scope)
        val categories = edition.map { it.category }.distinct()
        return listOf(allLabel) + categories
    }
}
