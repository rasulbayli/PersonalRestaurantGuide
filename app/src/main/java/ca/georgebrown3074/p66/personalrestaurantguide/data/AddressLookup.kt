package ca.georgebrown3074.p66.personalrestaurantguide.data

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

sealed interface LookupResult {
    data class Found(val address: String, val latitude: Double, val longitude: Double) : LookupResult
    data object NotFound : LookupResult

    /** No network or no geocoding service: the address may still be correct. */
    data object Unavailable : LookupResult
}

/** Turns a typed address into a formatted address and coordinates with the platform Geocoder. */
object AddressLookup {

    private const val TIMEOUT_MS = 8_000L

    suspend fun find(context: Context, address: String): LookupResult {
        if (!Geocoder.isPresent()) return LookupResult.Unavailable
        val geocoder = Geocoder(context.applicationContext)
        return withTimeoutOrNull(TIMEOUT_MS) {
            try {
                toResult(search(geocoder, address))
            } catch (_: IOException) {
                LookupResult.Unavailable
            }
        } ?: LookupResult.Unavailable
    }

    private suspend fun search(geocoder: Geocoder, address: String): List<Address> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocationName(address, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        if (continuation.isActive) continuation.resume(addresses)
                    }

                    override fun onError(errorMessage: String?) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(IOException(errorMessage))
                        }
                    }
                })
            }
        }
        return withContext(Dispatchers.IO) {
            @Suppress("DEPRECATION") // The listener version only exists on API 33+.
            geocoder.getFromLocationName(address, 1).orEmpty()
        }
    }

    private fun toResult(addresses: List<Address>): LookupResult {
        val match = addresses.firstOrNull() ?: return LookupResult.NotFound
        if (!match.hasLatitude() || !match.hasLongitude()) return LookupResult.NotFound
        return LookupResult.Found(
            address = match.getAddressLine(0).orEmpty(),
            latitude = match.latitude,
            longitude = match.longitude
        )
    }
}
