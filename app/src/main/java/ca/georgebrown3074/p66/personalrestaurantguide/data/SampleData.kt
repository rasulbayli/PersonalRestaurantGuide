package ca.georgebrown3074.p66.personalrestaurantguide.data

/**
 * Temporary in-memory store so every screen can share one list.
 * TODO Milestone 3: replace with RestaurantRepository + Room.
 */
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

    val restaurants: MutableList<Restaurant> = mutableListOf(
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

    private val extraTags = mutableListOf<String>()
    private var nextId = 7L

    fun getById(id: Long): Restaurant? = restaurants.find { it.id == id }

    /** id 0 means "give this restaurant the next id". */
    fun add(restaurant: Restaurant): Restaurant {
        val stored = if (restaurant.id > 0L) {
            if (restaurant.id >= nextId) nextId = restaurant.id + 1
            restaurant
        } else {
            restaurant.copy(id = nextId++)
        }
        restaurants.add(stored)
        return stored
    }

    fun update(restaurant: Restaurant): Boolean {
        val index = restaurants.indexOfFirst { it.id == restaurant.id }
        if (index < 0) return false
        restaurants[index] = restaurant
        return true
    }

    fun delete(id: Long): Boolean = restaurants.removeAll { it.id == id }

    fun registerTag(tag: String) {
        val clean = tag.trim()
        if (clean.isEmpty()) return
        if (allTags().any { it.equals(clean, ignoreCase = true) }) return
        extraTags.add(clean)
    }

    fun allTags(): List<String> {
        val seen = LinkedHashSet<String>()
        fun addTag(tag: String) {
            val clean = tag.trim()
            if (clean.isEmpty()) return
            if (seen.none { it.equals(clean, ignoreCase = true) }) seen.add(clean)
        }
        defaultTags.forEach(::addTag)
        extraTags.forEach(::addTag)
        restaurants.forEach { restaurant -> restaurant.tags.forEach(::addTag) }
        return seen.toList()
    }
}
