package ca.georgebrown3074.p66.personalrestaurantguide.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A saved restaurant. An id of 0 lets Room assign the next id on insert.
 * A rating of 0 means the place has not been rated yet.
 * Latitude and longitude stay null until the address has been located.
 */
@Entity(tableName = "restaurants")
data class Restaurant(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val address: String,
    val latitude: Double?,
    val longitude: Double?,
    val phones: List<String>,
    val description: String,
    val tags: List<String>,
    val rating: Float
) {
    companion object {
        const val EXTRA_RESTAURANT_ID = "extra_restaurant_id"
    }
}
