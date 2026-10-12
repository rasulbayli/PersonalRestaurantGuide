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
import ca.georgebrown3074.p66.personalrestaurantguide.data.Restaurant
import ca.georgebrown3074.p66.personalrestaurantguide.data.SampleData
import ca.georgebrown3074.p66.personalrestaurantguide.ui.RestaurantIntents
import ca.georgebrown3074.p66.personalrestaurantguide.ui.applySystemBarInsets
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class RestaurantDetailsActivity : AppCompatActivity() {

    private var restaurantId: Long = -1L
    private var bindingRating = false

    private lateinit var toolbar: MaterialToolbar
    private lateinit var ratingBar: RatingBar
    private lateinit var tagChipGroup: ChipGroup
    private lateinit var noTags: TextView
    private lateinit var addressText: TextView
    private lateinit var phoneContainer: LinearLayout
    private lateinit var descriptionText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        restaurantId = intent.getLongExtra(Restaurant.EXTRA_RESTAURANT_ID, -1L)
        if (restaurantId < 0L || SampleData.getById(restaurantId) == null) {
            finish()
            return
        }

        setContentView(R.layout.activity_restaurant_details)
        findViewById<View>(R.id.detailsRoot).applySystemBarInsets()

        toolbar = findViewById(R.id.toolbar)
        ratingBar = findViewById(R.id.ratingBar)
        tagChipGroup = findViewById(R.id.tagChipGroup)
        noTags = findViewById(R.id.txtNoTags)
        addressText = findViewById(R.id.txtAddress)
        phoneContainer = findViewById(R.id.phoneContainer)
        descriptionText = findViewById(R.id.txtDescription)

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
            val restaurant = SampleData.getById(restaurantId) ?: return@setOnRatingBarChangeListener
            restaurant.rating = rating.coerceIn(0f, 5f)
            SampleData.update(restaurant)
            Toast.makeText(this, R.string.rating_saved, Toast.LENGTH_SHORT).show()
        }

        val openMap = View.OnClickListener { openFullMap() }
        findViewById<View>(R.id.mapPreviewCard).setOnClickListener(openMap)
        findViewById<View>(R.id.btnMap).setOnClickListener(openMap)
        findViewById<View>(R.id.btnDirections).setOnClickListener {
            val restaurant = SampleData.getById(restaurantId) ?: run {
                finish()
                return@setOnClickListener
            }
            RestaurantIntents.openDirections(this, restaurant)
        }
        findViewById<View>(R.id.btnShare).setOnClickListener {
            val restaurant = SampleData.getById(restaurantId) ?: run {
                finish()
                return@setOnClickListener
            }
            RestaurantIntents.shareByEmail(this, restaurant)
        }
    }

    override fun onResume() {
        super.onResume()
        val restaurant = SampleData.getById(restaurantId)
        if (restaurant == null) {
            finish()
            return
        }
        bind(restaurant)
    }

    private fun bind(restaurant: Restaurant) {
        toolbar.title = restaurant.name
        bindingRating = true
        ratingBar.rating = restaurant.rating.coerceIn(0f, 5f)
        bindingRating = false
        addressText.text = restaurant.address
        descriptionText.text = restaurant.description.ifBlank {
            getString(R.string.no_description)
        }

        tagChipGroup.removeAllViews()
        restaurant.tags.forEach { tag ->
            val chip = layoutInflater.inflate(R.layout.view_tag_chip, tagChipGroup, false) as? Chip
                ?: return@forEach
            chip.text = tag
            tagChipGroup.addView(chip)
        }
        noTags.isVisible = restaurant.tags.isEmpty()

        phoneContainer.removeAllViews()
        val phones = restaurant.phones.ifEmpty { listOf(getString(R.string.no_phone)) }
        phones.forEach { phone ->
            val row = layoutInflater.inflate(R.layout.item_phone_display, phoneContainer, false)
            row.findViewById<TextView>(R.id.txtPhone).text = phone
            phoneContainer.addView(row)
        }
    }

    private fun openEditor() {
        if (SampleData.getById(restaurantId) == null) {
            finish()
            return
        }
        startActivity(
            Intent(this, AddEditRestaurantActivity::class.java)
                .putExtra(Restaurant.EXTRA_RESTAURANT_ID, restaurantId)
        )
    }

    private fun openFullMap() {
        if (SampleData.getById(restaurantId) == null) {
            finish()
            return
        }
        // TODO Milestone 3: SupportMapFragment
        startActivity(
            Intent(this, FullMapActivity::class.java)
                .putExtra(Restaurant.EXTRA_RESTAURANT_ID, restaurantId)
        )
    }

    private fun confirmDelete() {
        val restaurant = SampleData.getById(restaurantId) ?: run {
            finish()
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_title)
            .setMessage(getString(R.string.delete_message, restaurant.name))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                SampleData.delete(restaurant.id)
                finish()
            }
            .show()
    }
}
