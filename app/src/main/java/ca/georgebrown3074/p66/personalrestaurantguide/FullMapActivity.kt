package ca.georgebrown3074.p66.personalrestaurantguide

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import ca.georgebrown3074.p66.personalrestaurantguide.data.AppDatabase
import ca.georgebrown3074.p66.personalrestaurantguide.data.Restaurant
import ca.georgebrown3074.p66.personalrestaurantguide.ui.MapSupport
import ca.georgebrown3074.p66.personalrestaurantguide.ui.RestaurantIntents
import ca.georgebrown3074.p66.personalrestaurantguide.ui.applySystemBarInsets
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class FullMapActivity : AppCompatActivity() {

    private var restaurant: Restaurant? = null
    private var googleMap: GoogleMap? = null
    private var shownPosition: LatLng? = null
    private var renderedMarkerKey: String? = null
    private var mapAttachStarted = false

    // After a rotation the map fragment restores its own camera, so leave it where it is.
    private var keepRestoredCamera = false

    private lateinit var toolbar: MaterialToolbar
    private lateinit var emptyView: View
    private lateinit var emptyText: TextView
    private lateinit var controls: View
    private lateinit var nameText: TextView
    private lateinit var addressText: TextView

    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.any { it }) {
            showMyLocation()
        } else {
            showMessage(R.string.location_permission_denied)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val restaurantId = intent.getLongExtra(Restaurant.EXTRA_RESTAURANT_ID, -1L)
        if (restaurantId < 0L) {
            finish()
            return
        }
        keepRestoredCamera = savedInstanceState != null

        setContentView(R.layout.activity_full_map)
        findViewById<View>(R.id.mapRoot).applySystemBarInsets()

        toolbar = findViewById(R.id.toolbar)
        emptyView = findViewById(R.id.mapEmpty)
        emptyText = findViewById(R.id.txtMapEmpty)
        controls = findViewById(R.id.mapControls)
        nameText = findViewById(R.id.txtMapName)
        addressText = findViewById(R.id.txtMapAddress)

        toolbar.setNavigationOnClickListener { finish() }
        findViewById<View>(R.id.btnZoomIn).setOnClickListener {
            googleMap?.animateCamera(CameraUpdateFactory.zoomIn())
        }
        findViewById<View>(R.id.btnZoomOut).setOnClickListener {
            googleMap?.animateCamera(CameraUpdateFactory.zoomOut())
        }
        findViewById<View>(R.id.btnMyLocation).setOnClickListener { onMyLocationClicked() }
        findViewById<View>(R.id.btnGetDirections).setOnClickListener {
            restaurant?.let { RestaurantIntents.openDirections(this, it) }
        }

        if (MapSupport.isConfigured) {
            findViewById<View>(R.id.fullMapContainer).post { attachMap() }
        }

        val dao = AppDatabase.get(this).restaurantDao()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                dao.observeById(restaurantId).distinctUntilChanged().collect { saved ->
                    if (saved == null) {
                        finish()
                    } else {
                        restaurant = saved
                        bind(saved)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (MapSupport.isConfigured && !mapAttachStarted) {
            findViewById<View>(R.id.fullMapContainer).post { attachMap() }
        }
    }

    private fun attachMap() {
        if (mapAttachStarted || isDestroyed || supportFragmentManager.isStateSaved) return
        val fragment = supportFragmentManager.findFragmentById(R.id.fullMapContainer)
            as? SupportMapFragment
            ?: SupportMapFragment.newInstance().also { created ->
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fullMapContainer, created)
                    .commit()
            }
        mapAttachStarted = true
        fragment.getMapAsync { map ->
            if (isDestroyed) return@getMapAsync
            googleMap = map
            map.uiSettings.isMyLocationButtonEnabled = false
            if (hasLocationPermission()) enableLocationLayer(map)
            restaurant?.let { renderMap(it) }
        }
    }

    private fun bind(saved: Restaurant) {
        toolbar.title = saved.name
        nameText.text = saved.name
        addressText.text = saved.address

        val position = MapSupport.positionOf(saved)
        val canShowMap = MapSupport.isConfigured && position != null
        emptyView.isVisible = !canShowMap
        controls.isVisible = canShowMap
        emptyText.setText(
            if (MapSupport.isConfigured) R.string.map_no_location else R.string.map_no_api_key
        )
        renderMap(saved)
    }

    private fun renderMap(saved: Restaurant) {
        val map = googleMap ?: return
        val position = MapSupport.positionOf(saved) ?: return
        val key = "${saved.id}|${position.latitude}|${position.longitude}|${saved.name}|${saved.address}"
        if (key != renderedMarkerKey) {
            MapSupport.showMarker(map, saved, position)
            renderedMarkerKey = key
        }
        if (position != shownPosition) {
            if (!keepRestoredCamera) MapSupport.moveTo(map, position)
            keepRestoredCamera = false
            shownPosition = position
        }
    }

    private fun onMyLocationClicked() {
        if (hasLocationPermission()) {
            showMyLocation()
        } else {
            locationPermission.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun hasLocationPermission(): Boolean {
        return listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ).any { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    @SuppressLint("MissingPermission") // Only called after hasLocationPermission() is true.
    private fun enableLocationLayer(map: GoogleMap) {
        map.isMyLocationEnabled = true
    }

    @SuppressLint("MissingPermission") // Only called after hasLocationPermission() is true.
    private fun showMyLocation() {
        val map = googleMap ?: return
        enableLocationLayer(map)
        showMessage(R.string.finding_location)
        LocationServices.getFusedLocationProviderClient(this)
            .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener(this) { location ->
                if (location == null) {
                    showMessage(R.string.location_off)
                } else {
                    map.animateCamera(
                        CameraUpdateFactory.newLatLng(LatLng(location.latitude, location.longitude))
                    )
                }
            }
            .addOnFailureListener(this) { showMessage(R.string.location_off) }
    }

    private fun showMessage(message: Int) {
        Snackbar.make(findViewById(R.id.mapRoot), message, Snackbar.LENGTH_LONG).show()
    }
}
