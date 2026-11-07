package ca.georgebrown3074.p66.personalrestaurantguide.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import org.json.JSONArray

/** Phones and tags are short string lists, so they are stored as JSON in one column. */
class StringListConverter {
    @TypeConverter
    fun fromList(values: List<String>): String = JSONArray(values).toString()

    @TypeConverter
    fun toList(json: String): List<String> {
        val array = JSONArray(json)
        return List(array.length()) { index -> array.getString(index) }
    }
}

@Database(entities = [Restaurant::class], version = 1, exportSchema = false)
@TypeConverters(StringListConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun restaurantDao(): RestaurantDao

    /** Adds the sample restaurants the first time the database file is created. */
    private class SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            val converter = StringListConverter()
            SampleData.restaurants.forEach { restaurant ->
                val values = ContentValues().apply {
                    put("id", restaurant.id)
                    put("name", restaurant.name)
                    put("address", restaurant.address)
                    put("latitude", restaurant.latitude)
                    put("longitude", restaurant.longitude)
                    put("phones", converter.fromList(restaurant.phones))
                    put("description", restaurant.description)
                    put("tags", converter.fromList(restaurant.tags))
                    put("rating", restaurant.rating)
                }
                db.insert("restaurants", SQLiteDatabase.CONFLICT_ABORT, values)
            }
        }
    }

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "restaurants.db"
                ).addCallback(SeedCallback()).build().also { instance = it }
            }
        }
    }
}
