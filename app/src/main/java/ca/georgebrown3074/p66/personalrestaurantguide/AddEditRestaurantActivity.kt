package ca.georgebrown3074.p66.personalrestaurantguide

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import ca.georgebrown3074.p66.personalrestaurantguide.data.AddressLookup
import ca.georgebrown3074.p66.personalrestaurantguide.data.AppDatabase
import ca.georgebrown3074.p66.personalrestaurantguide.data.LookupResult
import ca.georgebrown3074.p66.personalrestaurantguide.data.Restaurant
import ca.georgebrown3074.p66.personalrestaurantguide.data.RestaurantDao
import ca.georgebrown3074.p66.personalrestaurantguide.data.SampleData
import ca.georgebrown3074.p66.personalrestaurantguide.ui.applySystemBarInsets
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.Locale
import kotlinx.coroutines.launch

class AddEditRestaurantActivity : AppCompatActivity() {

    private var editingId: Long? = null
    private val tagsAddedThisSession = mutableListOf<String>()
    private var knownRestaurants: List<Restaurant> = emptyList()

    // The address text that these coordinates belong to. Editing the text invalidates them.
    private var locatedAddress: String? = null
    private var locatedLatitude: Double? = null
    private var locatedLongitude: Double? = null

    private lateinit var dao: RestaurantDao
    private lateinit var nameLayout: TextInputLayout
    private lateinit var nameEdit: TextInputEditText
    private lateinit var addressLayout: TextInputLayout
    private lateinit var addressEdit: TextInputEditText
    private lateinit var descriptionEdit: TextInputEditText
    private lateinit var newTagLayout: TextInputLayout
    private lateinit var newTagEdit: TextInputEditText
    private lateinit var phoneContainer: LinearLayout
    private lateinit var tagChipGroup: ChipGroup
    private lateinit var ratingBar: RatingBar
    private lateinit var verifyButton: MaterialButton
    private lateinit var saveButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val requestedId = if (intent.hasExtra(Restaurant.EXTRA_RESTAURANT_ID)) {
            intent.getLongExtra(Restaurant.EXTRA_RESTAURANT_ID, -1L)
        } else {
            null
        }
        if (requestedId != null && requestedId < 0L) {
            finish()
            return
        }
        editingId = requestedId
        dao = AppDatabase.get(this).restaurantDao()

        setContentView(R.layout.activity_add_edit_restaurant)
        findViewById<View>(R.id.addEditRoot).applySystemBarInsets()

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.title = getString(
            if (requestedId == null) R.string.add_restaurant else R.string.edit_restaurant
        )
        toolbar.setNavigationOnClickListener { finish() }

        nameLayout = findViewById(R.id.nameLayout)
        nameEdit = findViewById(R.id.nameEditText)
        addressLayout = findViewById(R.id.addressLayout)
        addressEdit = findViewById(R.id.addressEditText)
        descriptionEdit = findViewById(R.id.descriptionEditText)
        newTagLayout = findViewById(R.id.newTagLayout)
        newTagEdit = findViewById(R.id.newTagEditText)
        phoneContainer = findViewById(R.id.phoneContainer)
        tagChipGroup = findViewById(R.id.tagChipGroup)
        ratingBar = findViewById(R.id.ratingBar)
        verifyButton = findViewById(R.id.btnVerify)
        saveButton = findViewById(R.id.btnSave)

        // Dynamic rows are restored by us, not by the view hierarchy (ids would collide).
        phoneContainer.setSaveFromParentEnabled(false)
        tagChipGroup.setSaveFromParentEnabled(false)

        verifyButton.setOnClickListener { verifyAddress() }
        findViewById<MaterialButton>(R.id.btnAddPhone).setOnClickListener { addPhoneField("") }
        findViewById<MaterialButton>(R.id.btnAddTag).setOnClickListener { addNewTag() }
        saveButton.setOnClickListener { save() }
        findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener { finish() }

        nameEdit.clearErrorOnType(nameLayout)
        addressEdit.clearErrorOnType(addressLayout)
        newTagEdit.clearErrorOnType(newTagLayout)

        // Saving stays off until the stored data has been loaded into the form.
        saveButton.isEnabled = false
        lifecycleScope.launch { loadForm(requestedId, savedInstanceState) }
    }

    private suspend fun loadForm(requestedId: Long?, savedState: Bundle?) {
        knownRestaurants = dao.getAll()
        val existing = requestedId?.let { dao.getById(it) }
        if (requestedId != null && existing == null) {
            finish()
            return
        }
        if (savedState != null) {
            restoreDynamicFields(savedState)
        } else if (existing != null) {
            fillFrom(existing)
        } else {
            addPhoneField("")
            showTagChips(emptySet())
        }
        saveButton.isEnabled = true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(STATE_PHONES, phoneFieldTexts())
        outState.putStringArrayList(STATE_CHECKED_TAGS, ArrayList(selectedTags()))
        outState.putStringArrayList(STATE_NEW_TAGS, ArrayList(tagsAddedThisSession))
        val latitude = locatedLatitude
        val longitude = locatedLongitude
        if (latitude != null && longitude != null) {
            outState.putString(STATE_LOCATED_ADDRESS, locatedAddress)
            outState.putDouble(STATE_LATITUDE, latitude)
            outState.putDouble(STATE_LONGITUDE, longitude)
        }
    }

    private fun fillFrom(restaurant: Restaurant) {
        nameEdit.setText(restaurant.name)
        addressEdit.setText(restaurant.address)
        descriptionEdit.setText(restaurant.description)
        ratingBar.rating = restaurant.rating.coerceIn(0f, 5f)
        val latitude = restaurant.latitude
        val longitude = restaurant.longitude
        if (latitude != null && longitude != null) {
            rememberLocation(restaurant.address, latitude, longitude)
        }
        val phones = restaurant.phones
        if (phones.isEmpty()) addPhoneField("") else phones.forEach { addPhoneField(it) }
        showTagChips(restaurant.tags.toSet())
    }

    private fun restoreDynamicFields(state: Bundle) {
        state.getStringArrayList(STATE_NEW_TAGS)?.let { tagsAddedThisSession.addAll(it) }
        val phones = state.getStringArrayList(STATE_PHONES)
        if (phones.isNullOrEmpty()) addPhoneField("") else phones.forEach { addPhoneField(it) }
        val checked = state.getStringArrayList(STATE_CHECKED_TAGS)?.toSet().orEmpty()
        showTagChips(checked)
        val located = state.getString(STATE_LOCATED_ADDRESS)
        if (located != null) {
            rememberLocation(
                located,
                state.getDouble(STATE_LATITUDE),
                state.getDouble(STATE_LONGITUDE)
            )
        }
    }

    private fun rememberLocation(address: String, latitude: Double, longitude: Double) {
        locatedAddress = address
        locatedLatitude = latitude
        locatedLongitude = longitude
    }

    @Suppress("DEPRECATION") // Course spec: format as the user types. Country CA matches Toronto.
    private fun addPhoneField(number: String) {
        val row = layoutInflater.inflate(R.layout.item_phone_field, phoneContainer, false)
        row.setSaveFromParentEnabled(false)
        val edit = row.findViewById<TextInputEditText>(R.id.phoneEditText)
        edit.addTextChangedListener(android.telephony.PhoneNumberFormattingTextWatcher("CA"))
        if (number.isNotEmpty()) edit.setText(number)
        row.findViewById<View>(R.id.btnRemovePhone).setOnClickListener {
            phoneContainer.removeView(row)
            if (phoneContainer.childCount == 0) addPhoneField("")
        }
        phoneContainer.addView(row)
    }

    private fun phoneFieldTexts(): ArrayList<String> {
        val values = ArrayList<String>()
        for (index in 0 until phoneContainer.childCount) {
            val edit = phoneContainer.getChildAt(index)
                .findViewById<TextInputEditText>(R.id.phoneEditText)
            values.add(edit?.text?.toString().orEmpty())
        }
        return values
    }

    private fun collectPhones(): List<String> {
        return phoneFieldTexts().map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun showTagChips(selected: Set<String>) {
        tagChipGroup.removeAllViews()
        SampleData.allTags(knownRestaurants, tagsAddedThisSession + selected).forEach { tag ->
            val checked = selected.any { it.equals(tag, ignoreCase = true) }
            addTagChip(tag, checked)
        }
    }

    private fun addTagChip(label: String, checked: Boolean) {
        val chip = layoutInflater.inflate(R.layout.view_filter_chip, tagChipGroup, false) as? Chip
            ?: return
        chip.text = label
        chip.isCheckable = true
        chip.isChecked = checked
        tagChipGroup.addView(chip)
    }

    private fun findTagChip(label: String): Chip? {
        for (index in 0 until tagChipGroup.childCount) {
            val chip = tagChipGroup.getChildAt(index) as? Chip ?: continue
            if (chip.text?.toString().equals(label, ignoreCase = true)) return chip
        }
        return null
    }

    private fun selectedTags(): List<String> {
        val tags = mutableListOf<String>()
        for (index in 0 until tagChipGroup.childCount) {
            val chip = tagChipGroup.getChildAt(index) as? Chip ?: continue
            if (!chip.isChecked) continue
            val label = chip.text?.toString()?.trim().orEmpty()
            if (label.isNotEmpty()) tags.add(label)
        }
        return tags.distinctBy { it.lowercase(Locale.getDefault()) }
    }

    private fun addNewTag() {
        val raw = newTagEdit.text?.toString()?.trim().orEmpty()
        if (raw.isEmpty()) {
            newTagLayout.error = getString(R.string.error_tag_required)
            return
        }
        if (raw.equals(getString(R.string.tag_all), ignoreCase = true)) {
            newTagLayout.error = getString(R.string.error_tag_reserved)
            return
        }
        newTagLayout.error = null
        if (tagsAddedThisSession.none { it.equals(raw, ignoreCase = true) }) {
            tagsAddedThisSession.add(raw)
        }
        val existing = findTagChip(raw)
        if (existing != null) {
            existing.isChecked = true
        } else {
            addTagChip(raw, checked = true)
        }
        newTagEdit.text?.clear()
    }

    private fun verifyAddress() {
        val address = addressEdit.text?.toString()?.trim().orEmpty()
        if (address.isEmpty()) {
            addressLayout.error = getString(R.string.error_address_required)
            return
        }
        hideKeyboard()
        verifyButton.isEnabled = false
        lifecycleScope.launch {
            when (val result = AddressLookup.find(this@AddEditRestaurantActivity, address)) {
                is LookupResult.Found -> {
                    val formatted = result.address.ifBlank { address }
                    addressEdit.setText(formatted)
                    addressEdit.setSelection(formatted.length)
                    rememberLocation(formatted, result.latitude, result.longitude)
                    showMessage(R.string.address_verified)
                }
                LookupResult.NotFound -> {
                    addressLayout.error = getString(R.string.address_not_found)
                }
                LookupResult.Unavailable -> showMessage(R.string.address_lookup_unavailable)
            }
            verifyButton.isEnabled = true
        }
    }

    private fun save() {
        val name = nameEdit.text?.toString()?.trim().orEmpty()
        val address = addressEdit.text?.toString()?.trim().orEmpty()
        var valid = true
        if (name.isEmpty()) {
            nameLayout.error = getString(R.string.error_name_required)
            valid = false
        }
        if (address.isEmpty()) {
            addressLayout.error = getString(R.string.error_address_required)
            valid = false
        }
        if (!valid) {
            if (name.isEmpty()) nameEdit.requestFocus() else addressEdit.requestFocus()
            return
        }

        val phones = collectPhones()
        val tags = selectedTags()
        val description = descriptionEdit.text?.toString()?.trim().orEmpty()
        val rating = ratingBar.rating.coerceIn(0f, 5f)
        val id = editingId

        hideKeyboard()
        saveButton.isEnabled = false
        lifecycleScope.launch {
            // Saving never depends on the network: an address that can't be located yet is
            // stored without coordinates, and the details screen tries again later.
            if (address != locatedAddress) {
                locatedAddress = null
                locatedLatitude = null
                locatedLongitude = null
                val result = AddressLookup.find(this@AddEditRestaurantActivity, address)
                if (result is LookupResult.Found) {
                    rememberLocation(address, result.latitude, result.longitude)
                }
            }
            val restaurant = Restaurant(
                id = id ?: 0L,
                name = name,
                address = address,
                latitude = locatedLatitude,
                longitude = locatedLongitude,
                phones = phones,
                description = description,
                tags = tags,
                rating = rating
            )
            if (id == null) dao.insert(restaurant) else dao.update(restaurant)

            Toast.makeText(
                this@AddEditRestaurantActivity,
                R.string.restaurant_saved,
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }

    private fun showMessage(message: Int) {
        Snackbar.make(findViewById(R.id.addEditRoot), message, Snackbar.LENGTH_LONG).show()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(InputMethodManager::class.java) ?: return
        val token = currentFocus?.windowToken ?: addressEdit.windowToken
        imm.hideSoftInputFromWindow(token, 0)
    }

    private fun TextInputEditText.clearErrorOnType(layout: TextInputLayout) {
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                layout.error = null
            }
        })
    }

    companion object {
        private const val STATE_PHONES = "state_phones"
        private const val STATE_CHECKED_TAGS = "state_checked_tags"
        private const val STATE_NEW_TAGS = "state_new_tags"
        private const val STATE_LOCATED_ADDRESS = "state_located_address"
        private const val STATE_LATITUDE = "state_latitude"
        private const val STATE_LONGITUDE = "state_longitude"
    }
}
