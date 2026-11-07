package ca.georgebrown3074.p66.personalrestaurantguide

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ca.georgebrown3074.p66.personalrestaurantguide.data.AppDatabase
import ca.georgebrown3074.p66.personalrestaurantguide.data.Restaurant
import ca.georgebrown3074.p66.personalrestaurantguide.data.RestaurantDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RestaurantDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: RestaurantDao

    private val sample = Restaurant(
        name = "Test Kitchen",
        address = "1 Front St, Toronto, ON",
        latitude = null,
        longitude = null,
        phones = listOf("416-555-0100", "416-555-0101"),
        description = "Has \"quotes\", commas and\nnew lines",
        tags = listOf("Thai", "Spicy"),
        rating = 3f
    )

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = database.restaurantDao()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun insertKeepsEveryFieldIncludingLists() = runBlocking {
        val id = dao.insert(sample)
        assertEquals(sample.copy(id = id), dao.getById(id))
    }

    @Test
    fun updateRatingAndLocationChangeOnlyThoseFields() = runBlocking {
        val id = dao.insert(sample)
        dao.updateRating(id, 5f)
        dao.updateLocation(id, 43.65, -79.38)
        val expected = sample.copy(id = id, rating = 5f, latitude = 43.65, longitude = -79.38)
        assertEquals(expected, dao.getById(id))
    }

    @Test
    fun deleteRemovesRestaurantAndObserverSeesNull() = runBlocking {
        val id = dao.insert(sample)
        dao.deleteById(id)
        assertNull(dao.observeById(id).first())
        assertEquals(emptyList<Restaurant>(), dao.observeAll().first())
    }

    @Test
    fun listIsSortedByNameIgnoringCase() = runBlocking {
        dao.insert(sample.copy(name = "zeta"))
        dao.insert(sample.copy(name = "Alpha"))
        dao.insert(sample.copy(name = "beta"))
        assertEquals(listOf("Alpha", "beta", "zeta"), dao.observeAll().first().map { it.name })
    }
}
