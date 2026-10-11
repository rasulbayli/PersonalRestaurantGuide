package ca.georgebrown3074.p66.personalrestaurantguide

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ca.georgebrown3074.p66.personalrestaurantguide.data.Restaurant
import ca.georgebrown3074.p66.personalrestaurantguide.data.SampleData
import ca.georgebrown3074.p66.personalrestaurantguide.ui.applySystemBarInsets
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {

    private lateinit var searchEdit: TextInputEditText
    private lateinit var filterChipGroup: ChipGroup
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var adapter: RestaurantListAdapter

    private var selectedTag = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.main).applySystemBarInsets()

        selectedTag = savedInstanceState?.getString(STATE_TAG) ?: getString(R.string.tag_all)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.inflateMenu(R.menu.menu_home)
        toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_about) {
                startActivity(Intent(this, AboutActivity::class.java))
                true
            } else {
                false
            }
        }

        searchEdit = findViewById(R.id.searchEditText)
        filterChipGroup = findViewById(R.id.filterChipGroup)
        recyclerView = findViewById(R.id.restaurantRecyclerView)
        emptyState = findViewById(R.id.emptyState)

        adapter = RestaurantListAdapter { restaurant ->
            startActivity(
                Intent(this, RestaurantDetailsActivity::class.java)
                    .putExtra(Restaurant.EXTRA_RESTAURANT_ID, restaurant.id)
            )
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        searchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                showRestaurants()
            }
        })

        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            startActivity(Intent(this, AddEditRestaurantActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        rebuildFilterChips()
        showRestaurants()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_TAG, selectedTag)
    }

    private fun rebuildFilterChips() {
        val group = filterChipGroup
        group.setOnCheckedStateChangeListener(null)
        group.isSelectionRequired = false
        group.removeAllViews()

        val tags = listOf(getString(R.string.tag_all)) + SampleData.allTags()
        var chipToCheck: Chip? = null
        tags.forEach { tag ->
            val chip = layoutInflater.inflate(R.layout.view_filter_chip, group, false) as? Chip
                ?: return@forEach
            chip.id = View.generateViewId()
            chip.text = tag
            chip.isCheckable = true
            group.addView(chip)
            if (tag.equals(selectedTag, ignoreCase = true)) chipToCheck = chip
        }

        val selected = chipToCheck ?: group.getChildAt(0) as? Chip
        selected?.isChecked = true
        if (selected != null) {
            selectedTag = selected.text?.toString() ?: getString(R.string.tag_all)
        }
        group.isSingleSelection = true
        group.isSelectionRequired = true
        group.setOnCheckedStateChangeListener { _, checkedIds ->
            val id = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            val chip = group.findViewById<Chip>(id) ?: return@setOnCheckedStateChangeListener
            selectedTag = chip.text?.toString().orEmpty()
            showRestaurants()
        }
    }

    private fun showRestaurants() {
        val query = searchEdit.text?.toString()?.trim().orEmpty()
        val allLabel = getString(R.string.tag_all)
        val filtered = SampleData.restaurants.filter { restaurant ->
            val matchesQuery = query.isEmpty() ||
                restaurant.name.contains(query, ignoreCase = true) ||
                restaurant.tags.any { tag -> tag.contains(query, ignoreCase = true) }
            val matchesTag = selectedTag.equals(allLabel, ignoreCase = true) ||
                restaurant.tags.any { tag -> tag.equals(selectedTag, ignoreCase = true) }
            matchesQuery && matchesTag
        }
        // Copies so DiffUtil can see later edits to the objects stored in SampleData.
        adapter.submitList(filtered.map { it.copy() })
        val isEmpty = filtered.isEmpty()
        recyclerView.isVisible = !isEmpty
        emptyState.isVisible = isEmpty
    }

    companion object {
        private const val STATE_TAG = "state_tag"
    }
}
