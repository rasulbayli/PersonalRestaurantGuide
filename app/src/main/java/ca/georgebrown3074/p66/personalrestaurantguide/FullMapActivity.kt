package ca.georgebrown3074.p66.personalrestaurantguide

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import ca.georgebrown3074.p66.personalrestaurantguide.data.Restaurant
import ca.georgebrown3074.p66.personalrestaurantguide.data.SampleData
import ca.georgebrown3074.p66.personalrestaurantguide.ui.RestaurantIntents
import ca.georgebrown3074.p66.personalrestaurantguide.ui.applySystemBarInsets
import com.google.android.material.appbar.MaterialToolbar

class FullMapActivity : AppCompatActivity() {

    private var restaurantId: Long = -1L
    private var zoomLevel = 15

    private lateinit var toolbar: MaterialToolbar
    private lateinit var nameText: TextView
    private lateinit var addressText: TextView
    private lateinit var zoomText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        restaurantId = intent.getLongExtra(Restaurant.EXTRA_RESTAURANT_ID, -1L)
        if (restaurantId < 0L || SampleData.getById(restaurantId) == null) {
            finish()
            return
        }
        zoomLevel = savedInstanceState?.getInt(STATE_ZOOM, zoomLevel) ?: zoomLevel

        setContentView(R.layout.activity_full_map)
        findViewById<View>(R.id.mapRoot).applySystemBarInsets()

        // TODO Milestone 3: Maps SDK + Fused Location
        toolbar = findViewById(R.id.toolbar)
        nameText = findViewById(R.id.txtMapName)
        addressText = findViewById(R.id.txtMapAddress)
        zoomText = findViewById(R.id.txtZoomLevel)

        toolbar.setNavigationOnClickListener { finish() }
        findViewById<View>(R.id.btnZoomIn).setOnClickListener {
            if (zoomLevel < MAX_ZOOM) zoomLevel += 1
            showZoom()
        }
        findViewById<View>(R.id.btnZoomOut).setOnClickListener {
            if (zoomLevel > MIN_ZOOM) zoomLevel -= 1
            showZoom()
        }
        findViewById<View>(R.id.btnGetDirections).setOnClickListener {
            val restaurant = SampleData.getById(restaurantId) ?: run {
                finish()
                return@setOnClickListener
            }
            RestaurantIntents.openDirections(this, restaurant)
        }
        showZoom()
    }

    override fun onResume() {
        super.onResume()
        val restaurant = SampleData.getById(restaurantId)
        if (restaurant == null) {
            finish()
            return
        }
        toolbar.title = restaurant.name
        nameText.text = restaurant.name
        addressText.text = restaurant.address
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_ZOOM, zoomLevel)
    }

    private fun showZoom() {
        zoomText.text = getString(R.string.zoom_level, zoomLevel)
    }

    companion object {
        private const val STATE_ZOOM = "state_zoom"
        private const val MIN_ZOOM = 1
        private const val MAX_ZOOM = 21
    }
}
