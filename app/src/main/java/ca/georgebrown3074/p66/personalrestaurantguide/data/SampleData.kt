package ca.georgebrown3074.p66.personalrestaurantguide.data
// Alikhan Zhilkibayev: restaurant search and sample data.

object SampleData {

    val defaultTags = listOf(
        "Italian",
        "Vegetarian",
        "Vegan",
        "Thai",
        "Spicy",
        "Organic",
        "Burgers",
        "Halal",
        "Gluten-free"
    )

    /** Added to the database the first time the app runs. */
    val restaurants: List<Restaurant> = listOf(
        Restaurant(
            id = 1,
            name = "Pasta Nonna",
            address = "123 Queen St W, Toronto, ON",
            latitude = 43.6503,
            longitude = -79.3848,
            phones = listOf("416-555-0142"),
            description = "Fresh pasta and a quiet vegetarian menu on Queen West.",
            tags = listOf("Italian", "Vegetarian"),
            rating = 5f
        ),
        Restaurant(
            id = 2,
            name = "Thai Orchid",
            address = "45 Dundas St E, Toronto, ON",
            latitude = 43.6568,
            longitude = -79.3785,
            phones = listOf("416-555-0177", "416-555-0188"),
            description = "Northern Thai curries and spicy noodle bowls.",
            tags = listOf("Thai", "Spicy"),
            rating = 4f
        ),
        Restaurant(
            id = 3,
            name = "Green Leaf Cafe",
            address = "780 College St, Toronto, ON",
            latitude = 43.6545,
            longitude = -79.4195,
            phones = listOf("416-555-0119"),
            description = "Plant-based plates and organic coffee.",
            tags = listOf("Vegan", "Organic"),
            rating = 4f
        ),
        Restaurant(
            id = 4,
            name = "Burger Barn",
            address = "32 King St E, Toronto, ON",
            latitude = 43.6490,
            longitude = -79.3765,
            phones = listOf("416-555-0160"),
            description = "Smash burgers and thick-cut fries.",
            tags = listOf("Burgers"),
            rating = 3f
        ),
        Restaurant(
            id = 5,
            name = "Saffron Grill",
            address = "200 Yonge St, Toronto, ON",
            latitude = 43.6544,
            longitude = -79.3807,
            phones = listOf("416-555-0133"),
            description = "Halal charcoal grill with rice platters.",
            tags = listOf("Halal"),
            rating = 4f
        ),
        Restaurant(
            id = 6,
            name = "Harvest Bowl",
            address = "500 Bloor St W, Toronto, ON",
            latitude = 43.6652,
            longitude = -79.4105,
            phones = listOf("416-555-0194"),
            description = "Gluten-free bowls and a short vegetarian list.",
            tags = listOf("Gluten-free", "Vegetarian"),
            rating = 0f
        )
    )

    /** Default tags first, then any other tag in use, ignoring case and duplicates. */
    fun allTags(restaurants: List<Restaurant>, extra: Collection<String> = emptyList()): List<String> {
        val seen = LinkedHashSet<String>()
        fun addTag(tag: String) {
            val clean = tag.trim()
            if (clean.isEmpty()) return
            if (seen.none { it.equals(clean, ignoreCase = true) }) seen.add(clean)
        }
        defaultTags.forEach(::addTag)
        restaurants.forEach { restaurant -> restaurant.tags.forEach(::addTag) }
        extra.forEach(::addTag)
        return seen.toList()
    }
}
