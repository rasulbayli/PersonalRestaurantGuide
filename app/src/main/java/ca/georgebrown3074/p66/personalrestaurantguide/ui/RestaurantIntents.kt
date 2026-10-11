package ca.georgebrown3074.p66.personalrestaurantguide.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import ca.georgebrown3074.p66.personalrestaurantguide.R
import ca.georgebrown3074.p66.personalrestaurantguide.data.Restaurant

/**
 * Directions and share used by the details screen and the full-screen map.
 * Milestone 3 can swap the map link for the Maps SDK without changing the buttons.
 */
object RestaurantIntents {

    fun openDirections(context: Context, restaurant: Restaurant) {
        val uri = Uri.parse(
            "https://www.google.com/maps/dir/?api=1&destination=${destinationQuery(restaurant)}"
        )
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, R.string.no_maps_app, Toast.LENGTH_SHORT).show()
        }
    }

    fun shareByEmail(context: Context, restaurant: Restaurant) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_subject, restaurant.name))
            putExtra(Intent.EXTRA_TEXT, shareBody(context, restaurant))
        }
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, R.string.no_email_app, Toast.LENGTH_SHORT).show()
        }
    }

    private fun destinationQuery(restaurant: Restaurant): String {
        val latitude = restaurant.latitude
        val longitude = restaurant.longitude
        return if (latitude != null && longitude != null) {
            "$latitude,$longitude"
        } else {
            Uri.encode(restaurant.address.ifBlank { restaurant.name })
        }
    }

    private fun shareBody(context: Context, restaurant: Restaurant): String {
        val phones = if (restaurant.phones.isEmpty()) {
            context.getString(R.string.no_phone)
        } else {
            restaurant.phones.joinToString(", ")
        }
        val tags = if (restaurant.tags.isEmpty()) {
            context.getString(R.string.no_tags)
        } else {
            restaurant.tags.joinToString(", ")
        }
        val rating = if (restaurant.rating <= 0f) {
            context.getString(R.string.not_rated)
        } else {
            context.getString(R.string.rating_value, restaurant.rating.toInt())
        }
        val description = restaurant.description.ifBlank {
            context.getString(R.string.no_description)
        }
        val mapLink = "https://www.google.com/maps/search/?api=1&query=${destinationQuery(restaurant)}"
        return context.getString(
            R.string.share_body,
            restaurant.name,
            restaurant.address,
            phones,
            rating,
            tags,
            description,
            mapLink
        )
    }
}
