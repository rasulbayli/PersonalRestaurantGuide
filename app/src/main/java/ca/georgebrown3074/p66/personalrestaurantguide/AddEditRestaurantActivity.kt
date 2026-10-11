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
import ca.georgebrown3074.p66.personalrestaurantguide.data.Restaurant
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

class AddEditRestaurantActivity : AppCompatActivity() {

    private var editingId: Long? = null
    private val tagsAddedThisSession = mutableListOf<String>()

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val existing = restaurantFromIntent()
        if (intent.hasExtra(Restaurant.EXTRA_RESTAURANT_ID) && existing == null) {
            finish()
            return
        }
        editingId = existing?.id

        setContentView(R.layout.activity_add_edit_restaurant)
        findViewById<View>(R.id.addEditRoot).applySystemBarInsets()

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.title = getString(
            if (existing == null) R.string.add_restaurant else R.string.edit_restaurant
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

        // Dynamic rows are restored by us, not by the view hierarchy (ids would collide).
        phoneContainer.setSaveFromParentEnabled(false)
        tagChipGroup.setSaveFromParentEnabled(false)

        if (savedInstanceState == null) {
            if (existing != null) {
                fillFrom(existing)
            } else {
                addPhoneField("")
                showTagChips(emptySet())
            }
        } else {
            restoreDynamicFields(savedInstanceState)
        }

        findViewById<MaterialButton>(R.id.btnVerify).setOnClickListener { verifyAddress() }
        findViewById<MaterialButton>(R.id.btnAddPhone).setOnClickListener { addPhoneField("") }
        findViewById<MaterialButton>(R.id.btnAddTag).setOnClickListener { addNewTag() }
        findViewById<MaterialButton>(R.id.btnSave).setOnClickListener { save() }
        findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener { finish() }

        nameEdit.clearErrorOnType(nameLayout)
        addressEdit.clearErrorOnType(addressLayout)
        newTagEdit.clearErrorOnType(newTagLayout)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(STATE_PHONES, phoneFieldTexts())
        outState.putStringArrayList(STATE_CHECKED_TAGS, ArrayList(selectedTags()))
        outState.putStringArrayList(STATE_NEW_TAGS, ArrayList(tagsAddedThisSession))
    }

    private fun restaurantFromIntent(): Restaurant? {
        if (!intent.hasExtra(Restaurant.EXTRA_RESTAURANT_ID)) return null
        val id = intent.getLongExtra(Restaurant.EXTRA_RESTAURANT_ID, -1L)
        if (id < 0L) return null
        return SampleData.getById(id)
    }

    private fun fillFrom(restaurant: Restaurant) {
        nameEdit.setText(restaurant.name)
        addressEdit.setText(restaurant.address)
        descriptionEdit.setText(restaurant.description)
        ratingBar.rating = restaurant.rating.coerceIn(0f, 5f)
        val phones = restaurant.phones
        if (phones.isEmpty()) addPhoneField("") else phones.forEach { addPhoneField(it) }
        showTagChips(restaurant.tags.toSet())
    }

    private fun restoreDynamicFields(state: Bundle) {
        state.getStringArrayList(STATE_NEW_TAGS)?.forEach { tag ->
            SampleData.registerTag(tag)
            tagsAddedThisSession.add(tag)
        }
        val phones = state.getStringArrayList(STATE_PHONES)
        if (phones.isNullOrEmpty()) addPhoneField("") else phones.forEach { addPhoneField(it) }
        val checked = state.getStringArrayList(STATE_CHECKED_TAGS)?.toSet().orEmpty()
        showTagChips(checked)
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
        SampleData.allTags().forEach { tag ->
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
        SampleData.registerTag(raw)
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
        val tidy = capitaliseWords(addressEdit.text?.toString().orEmpty())
        addressEdit.setText(tidy)
        addressEdit.setSelection(tidy.length)
        hideKeyboard()
        // TODO Milestone 3: Geocoder
        Snackbar.make(
            findViewById(R.id.addEditRoot),
            R.string.address_verify_later,
            Snackbar.LENGTH_LONG
        ).show()
    }

    private fun capitaliseWords(raw: String): String {
        return raw.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .joinToString(" ") { word ->
                word.lowercase(Locale.getDefault()).replaceFirstChar { character ->
                    if (character.isLowerCase()) {
                        character.titlecase(Locale.getDefault())
                    } else {
                        character.toString()
                    }
                }
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

        if (id == null) {
            // Latitude and longitude stay empty until Geocoder runs in Milestone 3.
            SampleData.add(
                Restaurant(
                    id = 0,
                    name = name,
                    address = address,
                    latitude = null,
                    longitude = null,
                    phones = phones,
                    description = description,
                    tags = tags,
                    rating = rating
                )
            )
        } else {
            val existing = SampleData.getById(id)
            if (existing == null) {
                finish()
                return
            }
            existing.name = name
            existing.address = address
            existing.phones = phones
            existing.description = description
            existing.tags = tags
            existing.rating = rating
            SampleData.update(existing)
        }

        Toast.makeText(this, R.string.restaurant_saved, Toast.LENGTH_SHORT).show()
        finish()
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
    }
}
