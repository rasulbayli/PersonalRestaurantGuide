package ca.georgebrown3074.p66.personalrestaurantguide.ui

import ca.georgebrown3074.p66.personalrestaurantguide.BuildConfig
import ca.georgebrown3074.p66.personalrestaurantguide.data.Restaurant
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

/** Shared Google Maps helpers for the details preview and the full-screen map. */
object MapSupport {

    const val DEFAULT_ZOOM = 16f

    /** The map SDK crashes without a key, so screens check this before creating a map. */
    val isConfigured: Boolean get() = BuildConfig.MAPS_CONFIGURED

    fun positionOf(restaurant: Restaurant): LatLng? {
        val latitude = restaurant.latitude ?: return null
        val longitude = restaurant.longitude ?: return null
        return LatLng(latitude, longitude)
    }

    fun showMarker(map: GoogleMap, restaurant: Restaurant, position: LatLng) {
        map.clear()
        map.addMarker(
            MarkerOptions()
                .position(position)
                .title(restaurant.name)
                .snippet(restaurant.address)
        )
    }

    fun moveTo(map: GoogleMap, position: LatLng) {
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, DEFAULT_ZOOM))
    }
}
