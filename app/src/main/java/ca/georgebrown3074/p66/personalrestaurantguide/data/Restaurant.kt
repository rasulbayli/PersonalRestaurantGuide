package ca.georgebrown3074.p66.personalrestaurantguide.data

/**
 * In-memory restaurant. Field names match the Room entity planned for Milestone 3.
 * A rating of 0 means the place has not been rated yet.
 */
data class Restaurant(
    val id: Long,
    var name: String,
    var address: String,
    var latitude: Double?,
    var longitude: Double?,
    var phones: List<String>,
    var description: String,
    var tags: List<String>,
    var rating: Float
) {
    companion object {
        const val EXTRA_RESTAURANT_ID = "extra_restaurant_id"
    }
}
