package ca.georgebrown3074.p66.personalrestaurantguide
// Pintoo Himani: details, map, and share.

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import ca.georgebrown3074.p66.personalrestaurantguide.data.AddressLookup
import ca.georgebrown3074.p66.personalrestaurantguide.data.AppDatabase
import ca.georgebrown3074.p66.personalrestaurantguide.data.LookupResult
import ca.georgebrown3074.p66.personalrestaurantguide.data.Restaurant
import ca.georgebrown3074.p66.personalrestaurantguide.data.RestaurantDao
import ca.georgebrown3074.p66.personalrestaurantguide.ui.MapSupport
import ca.georgebrown3074.p66.personalrestaurantguide.ui.RestaurantIntents
import ca.georgebrown3074.p66.personalrestaurantguide.ui.applySystemBarInsets
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class RestaurantDetailsActivity : AppCompatActivity() {

    private var restaurantId: Long = -1L
    private var restaurant: Restaurant? = null
    private var bound: Restaurant? = null
    private var lastLookupAddress: String? = null
    private var previewMap: GoogleMap? = null
    private var mapAttachStarted = false
    private var renderedMarkerKey: String? = null
    private var bindingRating = false

    private lateinit var dao: RestaurantDao
    private lateinit var toolbar: MaterialToolbar
    private lateinit var ratingBar: RatingBar
    private lateinit var tagChipGroup: ChipGroup
    private lateinit var noTags: TextView
    private lateinit var addressText: TextView
    private lateinit var phoneContainer: LinearLayout
    private lateinit var descriptionText: TextView
    private lateinit var mapContainer: View
    private lateinit var mapEmpty: View
    private lateinit var mapEmptyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        restaurantId = intent.getLongExtra(Restaurant.EXTRA_RESTAURANT_ID, -1L)
        if (restaurantId < 0L) {
            finish()
            return
        }
        dao = AppDatabase.get(this).restaurantDao()

        setContentView(R.layout.activity_restaurant_details)
        findViewById<View>(R.id.detailsRoot).applySystemBarInsets()

        toolbar = findViewById(R.id.toolbar)
        ratingBar = findViewById(R.id.ratingBar)
        tagChipGroup = findViewById(R.id.tagChipGroup)
        noTags = findViewById(R.id.txtNoTags)
        addressText = findViewById(R.id.txtAddress)
        phoneContainer = findViewById(R.id.phoneContainer)
        descriptionText = findViewById(R.id.txtDescription)
        mapContainer = findViewById(R.id.mapPreviewContainer)
        mapEmpty = findViewById(R.id.mapPreviewEmpty)
        mapEmptyText = findViewById(R.id.txtMapPreviewEmpty)

        toolbar.setNavigationOnClickListener { finish() }
        toolbar.inflateMenu(R.menu.menu_details)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit -> {
                    openEditor()
                    true
                }
                R.id.action_delete -> {
                    confirmDelete()
                    true
                }
                else -> false
            }
        }

        ratingBar.setOnRatingBarChangeListener { _, rating, fromUser ->
            if (!fromUser || bindingRating) return@setOnRatingBarChangeListener
            val chosen = rating.coerceIn(0f, 5f)
            if (restaurant?.rating == chosen) return@setOnRatingBarChangeListener
            lifecycleScope.launch {
                dao.updateRating(restaurantId, chosen)
                Toast.makeText(
                    this@RestaurantDetailsActivity,
                    R.string.rating_saved,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val openMap = View.OnClickListener { openFullMap() }
        findViewById<View>(R.id.mapPreviewTouch).setOnClickListener(openMap)
        findViewById<View>(R.id.btnMap).setOnClickListener(openMap)
        findViewById<View>(R.id.btnDirections).setOnClickListener {
            restaurant?.let { RestaurantIntents.openDirections(this, it) }
        }
        findViewById<View>(R.id.btnShare).setOnClickListener {
            restaurant?.let { RestaurantIntents.shareByEmail(this, it) }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                dao.observeById(restaurantId).distinctUntilChanged().collect { saved ->
                    if (saved == null) {
                        finish()
                    } else {
                        restaurant = saved
                        bind(saved)
                        locateIfNeeded(saved)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!mapAttachStarted && restaurant?.let { MapSupport.positionOf(it) } != null) {
            mapContainer.post { attachPreviewMap() }
        }
    }

    private fun bind(restaurant: Restaurant) {
        val previous = bound
        toolbar.title = restaurant.name
        val ratingValue = restaurant.rating.coerceIn(0f, 5f)
        if (ratingBar.rating != ratingValue) {
            bindingRating = true
            ratingBar.rating = ratingValue
            bindingRating = false
        }
        if (previous?.address != restaurant.address) addressText.text = restaurant.address
        if (previous?.description != restaurant.description) {
            descriptionText.text = restaurant.description.ifBlank {
                getString(R.string.no_description)
            }
        }
        if (previous?.tags != restaurant.tags) showTags(restaurant.tags)
        if (previous?.phones != restaurant.phones) showPhones(restaurant.phones)
        bindMapPreview(restaurant)
        bound = restaurant
    }

    private fun showTags(tags: List<String>) {
        while (tagChipGroup.childCount > tags.size) {
            tagChipGroup.removeViewAt(tagChipGroup.childCount - 1)
        }
        tags.forEachIndexed { index, tag ->
            val chip = if (index < tagChipGroup.childCount) {
                tagChipGroup.getChildAt(index) as? Chip
            } else {
                (layoutInflater.inflate(R.layout.view_tag_chip, tagChipGroup, false) as? Chip)?.also {
                    tagChipGroup.addView(it)
                }
            } ?: return@forEachIndexed
            if (chip.text?.toString() != tag) chip.text = tag
        }
        noTags.isVisible = tags.isEmpty()
    }

    private fun showPhones(phones: List<String>) {
        val labels = phones.ifEmpty { listOf(getString(R.string.no_phone)) }
        while (phoneContainer.childCount > labels.size) {
            phoneContainer.removeViewAt(phoneContainer.childCount - 1)
        }
        labels.forEachIndexed { index, phone ->
            val row = if (index < phoneContainer.childCount) {
                phoneContainer.getChildAt(index)
            } else {
                layoutInflater.inflate(R.layout.item_phone_display, phoneContainer, false).also {
                    phoneContainer.addView(it)
                }
            }
            row.findViewById<TextView>(R.id.txtPhone).text = phone
        }
    }

    private fun bindMapPreview(restaurant: Restaurant) {
        val hasLocation = MapSupport.positionOf(restaurant) != null
        val canShowMap = MapSupport.isConfigured && hasLocation
        mapContainer.isVisible = canShowMap
        mapEmpty.isVisible = !canShowMap
        mapEmptyText.setText(
            if (MapSupport.isConfigured) R.string.map_no_location else R.string.map_no_api_key
        )
        if (canShowMap && !mapAttachStarted) mapContainer.post { attachPreviewMap() }
        renderPreview()
    }

    /**
     * Created only once there is something to show, so the lite map gets its real size.
     * A second copy of the map (easy to start while the first transaction is still pending)
     * freezes this screen.
     */
    private fun attachPreviewMap() {
        if (mapAttachStarted || isDestroyed || supportFragmentManager.isStateSaved) return
        val fragment = supportFragmentManager.findFragmentById(R.id.mapPreviewContainer)
            as? SupportMapFragment
            ?: SupportMapFragment.newInstance(GoogleMapOptions().liteMode(true)).also { created ->
                supportFragmentManager.beginTransaction()
                    .replace(R.id.mapPreviewContainer, created)
                    .commit()
            }
        mapAttachStarted = true
        fragment.getMapAsync { map ->
            if (isDestroyed) return@getMapAsync
            previewMap = map
            map.uiSettings.isMapToolbarEnabled = false
            map.uiSettings.setAllGesturesEnabled(false)
            renderPreview()
        }
    }

    private fun renderPreview() {
        val map = previewMap ?: return
        val saved = restaurant ?: return
        val position = MapSupport.positionOf(saved) ?: return
        val key = "${saved.id}|${position.latitude}|${position.longitude}|${saved.name}|${saved.address}"
        if (key == renderedMarkerKey) return
        renderedMarkerKey = key
        MapSupport.showMarker(map, saved, position)
        MapSupport.moveTo(map, position)
    }

    /** A restaurant saved offline has no coordinates yet, so try again when it is opened. */
    private fun locateIfNeeded(restaurant: Restaurant) {
        if (restaurant.latitude != null && restaurant.longitude != null) return
        if (restaurant.address == lastLookupAddress) return
        lastLookupAddress = restaurant.address
        lifecycleScope.launch {
            val result = AddressLookup.find(this@RestaurantDetailsActivity, restaurant.address)
            if (result is LookupResult.Found) {
                dao.updateLocation(restaurant.id, result.latitude, result.longitude)
            }
        }
    }

    private fun openEditor() {
        startActivity(
            Intent(this, AddEditRestaurantActivity::class.java)
                .putExtra(Restaurant.EXTRA_RESTAURANT_ID, restaurantId)
        )
    }

    private fun openFullMap() {
        startActivity(
            Intent(this, FullMapActivity::class.java)
                .putExtra(Restaurant.EXTRA_RESTAURANT_ID, restaurantId)
        )
    }

    private fun confirmDelete() {
        val current = restaurant ?: return
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_title)
            .setMessage(getString(R.string.delete_message, current.name))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                lifecycleScope.launch {
                    dao.deleteById(current.id)
                    finish()
                }
            }
            .show()
    }
}
