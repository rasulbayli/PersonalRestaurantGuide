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
        val restaurant = getItem(position) ?: return
        holder.bind(restaurant)
        holder.itemView.setOnClickListener { onRestaurantClick(restaurant) }
    }

    class RestaurantViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val name: TextView = view.findViewById(R.id.txtRestaurantName)
        private val rating: RatingBar = view.findViewById(R.id.ratingBar)
        private val address: TextView = view.findViewById(R.id.txtRestaurantAddress)
        private val tags: ChipGroup = view.findViewById(R.id.tagChipGroup)

        fun bind(restaurant: Restaurant) {
            name.text = restaurant.name
            rating.rating = restaurant.rating.coerceIn(0f, 5f)
            address.text = restaurant.address
            tags.removeAllViews()
            val inflater = LayoutInflater.from(itemView.context)
            restaurant.tags.forEach { tag ->
                val chip = inflater.inflate(R.layout.view_tag_chip, tags, false) as? Chip
                    ?: return@forEach
                chip.text = tag
                tags.addView(chip)
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
        }
    }
}
