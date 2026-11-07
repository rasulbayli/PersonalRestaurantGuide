package ca.georgebrown3074.p66.personalrestaurantguide.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RestaurantDao {

    @Query("SELECT * FROM restaurants ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Restaurant>>

    /** Emits null once the restaurant has been deleted. */
    @Query("SELECT * FROM restaurants WHERE id = :id")
    fun observeById(id: Long): Flow<Restaurant?>

    @Query("SELECT * FROM restaurants WHERE id = :id")
    suspend fun getById(id: Long): Restaurant?

    @Query("SELECT * FROM restaurants")
    suspend fun getAll(): List<Restaurant>

    @Insert
    suspend fun insert(restaurant: Restaurant): Long

    @Update
    suspend fun update(restaurant: Restaurant)

    @Query("UPDATE restaurants SET rating = :rating WHERE id = :id")
    suspend fun updateRating(id: Long, rating: Float)

    @Query("UPDATE restaurants SET latitude = :latitude, longitude = :longitude WHERE id = :id")
    suspend fun updateLocation(id: Long, latitude: Double, longitude: Double)

    @Query("DELETE FROM restaurants WHERE id = :id")
    suspend fun deleteById(id: Long)
}
