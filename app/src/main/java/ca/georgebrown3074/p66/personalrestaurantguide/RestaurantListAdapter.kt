package ca.georgebrown3074.p66.personalrestaurantguide

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ca.georgebrown3074.p66.personalrestaurantguide.data.Restaurant
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class RestaurantListAdapter(
    private val onRestaurantClick: (Restaurant) -> Unit
) : ListAdapter<Restaurant, RestaurantListAdapter.RestaurantViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RestaurantViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_restaurant, parent, false)
        return RestaurantViewHolder(view)
    }

    override fun onBindViewHolder(holder: RestaurantViewHolder, position: Int) {
        bindCard(holder, position, full = true)
    }

    override fun onBindViewHolder(
        holder: RestaurantViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            // Coordinates can update in the background. The card does not show them.
            bindCard(holder, position, full = false)
        }
    }

    private fun bindCard(holder: RestaurantViewHolder, position: Int, full: Boolean) {
        val restaurant = getItem(position) ?: return
        holder.itemView.setOnClickListener { onRestaurantClick(restaurant) }
        if (full) holder.bind(restaurant)
    }

    class RestaurantViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val name: TextView = view.findViewById(R.id.txtRestaurantName)
        private val rating: RatingBar = view.findViewById(R.id.ratingBar)
        private val address: TextView = view.findViewById(R.id.txtRestaurantAddress)
        private val tags: ChipGroup = view.findViewById(R.id.tagChipGroup)

        fun bind(restaurant: Restaurant) {
            name.text = restaurant.name
            val ratingValue = restaurant.rating.coerceIn(0f, 5f)
            if (rating.rating != ratingValue) rating.rating = ratingValue
            address.text = restaurant.address
            syncTags(restaurant.tags)
        }

        /** Reuses chip views so scrolling does not inflate a new chip on every bind. */
        private fun syncTags(labels: List<String>) {
            while (tags.childCount > labels.size) {
                tags.removeViewAt(tags.childCount - 1)
            }
            val inflater = LayoutInflater.from(itemView.context)
            labels.forEachIndexed { index, label ->
                val chip = if (index < tags.childCount) {
                    tags.getChildAt(index) as? Chip
                } else {
                    (inflater.inflate(R.layout.view_tag_chip, tags, false) as? Chip)?.also {
                        tags.addView(it)
                    }
                } ?: return@forEachIndexed
                if (chip.text?.toString() != label) chip.text = label
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Restaurant>() {
            override fun areItemsTheSame(oldItem: Restaurant, newItem: Restaurant): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Restaurant, newItem: Restaurant): Boolean {
                return oldItem == newItem
            }

            override fun getChangePayload(oldItem: Restaurant, newItem: Restaurant): Any? {
                val sameCard = oldItem.name == newItem.name &&
                    oldItem.address == newItem.address &&
                    oldItem.rating == newItem.rating &&
                    oldItem.tags == newItem.tags
                return if (sameCard) PAYLOAD_SAME_CARD else null
            }
        }

        private val PAYLOAD_SAME_CARD = Any()
    }
}
