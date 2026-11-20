package com.sijui.charades.data

import com.sijui.charades.data.model.CharadesCard
import com.sijui.charades.data.model.CharadesDeck

/**
 * Bundled Kenyan decks matching scripts/init-db.js so Classic Charades works 100% offline.
 */
object DefaultDecks {
    private fun deck(
        id: String,
        name: String,
        description: String,
        icon: String,
        theme: String,
        category: String,
        words: List<String>,
    ): CharadesDeck = CharadesDeck(
        id = id,
        name = name,
        description = description,
        icon = icon,
        theme = theme,
        cards = words.mapIndexed { idx, w ->
            CharadesCard(
                id = "${id}_$idx",
                text = w,
                category = category,
                difficulty = "medium",
                sortOrder = idx,
            )
        },
    )

    val all: List<CharadesDeck> = listOf(
        deck(
            id = "offline-chakula",
            name = "Chakula",
            description = "Kenyan food and drink",
            icon = "🍲",
            theme = "sunset",
            category = "Food and drink",
            words = listOf(
                "Ugali", "Sukuma wiki", "Nyama choma", "Chapati", "Mandazi", "Githeri",
                "Pilau", "Mutura", "Samosa", "Kachumbari", "Irio", "Mukimo", "Mursik",
                "Smokie na kachumbari", "Chai ya tangawizi", "Mahindi choma", "Uji",
                "Mahamri", "Viazi karai", "Ugali na fish", "Matoke", "Chapo Madondo",
            ),
        ),
        deck(
            id = "offline-usafiri",
            name = "Usafiri",
            description = "Getting around Kenya",
            icon = "🚌",
            theme = "mint",
            category = "Transport",
            words = listOf(
                "Matatu", "Boda boda", "Nduthi Rider", "Tuk tuk", "Nganya", "Makanga",
                "Bus stage", "Madaraka Express", "Nairobi Expressway", "Thika Road traffic",
                "Kenya Airways", "Bolt ride", "Likoni Ferry", "JKIA", "Pikipiki",
                "Squeezed into a full matatu", "Window seat", "Lorry", "Traffic police",
            ),
        ),
        deck(
            id = "offline-sheng",
            name = "Sheng",
            description = "Slang and street talk",
            icon = "🗣️",
            theme = "berry",
            category = "Sheng",
            words = listOf(
                "Msee", "Manzi", "Ganji", "Chapaa", "Mbogi", "Mtaa", "Kejani", "Buda",
                "Fiti", "Poa", "Sherehe", "Kuchill", "Niaje", "Wueh!", "Aki wewe!",
                "Kubonga", "Jobless corner", "Mtoi", "Kuna form?", "Mambo vipi",
            ),
        ),
        deck(
            id = "offline-maisha",
            name = "Maisha",
            description = "Everyday Kenyan life",
            icon = "🌍",
            theme = "classic",
            category = "Everyday life",
            words = listOf(
                "M-Pesa", "Power blackout", "Mama mboga", "Mitumba shopping",
                "Chama meeting", "Harambee", "Rain in Nairobi traffic", "Going to shags",
                "Kiosk", "Buying airtime", "Ruracio", "Sunday service",
                "Premier League at the local", "Bargaining at Gikomba", "Side hustle",
                "Kanga", "Kikoi", "Jiko", "Tea break", "Fuel price hike",
            ),
        ),
        deck(
            id = "offline-kenya",
            name = "Kenya",
            description = "Places and pride",
            icon = "🇰🇪",
            theme = "sunset",
            category = "Kenya",
            words = listOf(
                "Maasai Mara", "Mount Kenya", "Diani Beach", "Lamu", "Fort Jesus",
                "Great Rift Valley", "Hell's Gate", "Lake Nakuru", "KICC", "Uhuru Park",
                "Karura Forest", "Nairobi National Park", "Safaricom", "Tusker",
                "Harambee Stars", "Eliud Kipchoge", "Nyayo Stadium", "Kisumu", "Mombasa",
                "Wildebeest crossing", "Lake Turkana",
            ),
        ),
        deck(
            id = "offline-anatomy",
            name = "Anatomy",
            description = "Body parts and organs",
            icon = "🫀",
            theme = "berry",
            category = "Anatomy",
            words = listOf(
                "Heart", "Brain", "Lungs", "Stomach", "Skeleton", "Tongue",
                "Wisdom tooth", "Backbone / Spine", "Kneecap (Patella)", "Eyebrows",
                "Adam's Apple", "Funny bone", "Rib cage", "Kidneys", "Liver",
                "Skull", "Collarbone", "Tonsils", "Biceps", "Achilles tendon",
                "Goosebumps", "Vocal cords", "Eardrum", "Kitambi", "Upara",
                "Kisogo", "Kupaliwa", "Muscle cramp",
            ),
        ),
    )

    fun withRandomMix(decks: List<CharadesDeck>): List<CharadesDeck> {
        val allCards = decks.flatMap { it.cards }
        if (allCards.isEmpty()) return decks
        val mix = CharadesDeck(
            id = "everything-mix",
            name = "Everything Mix",
            description = "All ${allCards.size} words shuffled together!",
            icon = "🎲",
            theme = "red",
            cards = allCards,
        )
        return listOf(mix) + decks
    }
}
