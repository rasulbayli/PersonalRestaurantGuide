package ca.georgebrown3074.p66.personalrestaurantguide.data

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

sealed interface LookupResult {
    data class Found(val address: String, val latitude: Double, val longitude: Double) : LookupResult
    data object NotFound : LookupResult

    /** No network or no geocoding service: the address may still be correct. */
    data object Unavailable : LookupResult
}

/**
 * Turns a typed address into a formatted address and coordinates with the platform Geocoder.
 * The platform call can block for a long time, so it always runs on a background thread and
 * this function returns [LookupResult.Unavailable] if it has not finished within [TIMEOUT_MS].
 */
object AddressLookup {

    private const val TIMEOUT_MS = 8_000L

    private val executor = Executors.newCachedThreadPool { runnable ->
        Thread(runnable, "address-lookup").apply { isDaemon = true }
    }

    suspend fun find(context: Context, address: String): LookupResult {
        val query = address.trim()
        if (query.isEmpty()) return LookupResult.NotFound
        val appContext = context.applicationContext
        val result = withTimeoutOrNull(TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val task = executor.submit {
                    val lookup = try {
                        locate(appContext, query)
                    } catch (_: Exception) {
                        LookupResult.Unavailable
                    }
                    if (continuation.isActive) {
                        try {
                            continuation.resume(lookup)
                        } catch (_: IllegalStateException) {
                            // The screen gave up on this lookup (timeout or close) before we resumed.
                        }
                    }
                }
                continuation.invokeOnCancellation { task.cancel(true) }
            }
        }
        return result ?: LookupResult.Unavailable
    }

    private fun locate(context: Context, query: String): LookupResult {
        if (!Geocoder.isPresent()) return LookupResult.Unavailable
        val geocoder = Geocoder(context)
        return try {
            toResult(search(geocoder, query))
        } catch (_: IOException) {
            LookupResult.Unavailable
        }
    }

    private fun search(geocoder: Geocoder, address: String): List<Address> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val latch = CountDownLatch(1)
            val found = AtomicReference<List<Address>>(emptyList())
            val failed = AtomicBoolean(false)
            geocoder.getFromLocationName(address, 1, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<Address>) {
                    found.set(addresses)
                    latch.countDown()
                }

                override fun onError(errorMessage: String?) {
                    failed.set(true)
                    latch.countDown()
                }
            })
            if (!latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS) || failed.get()) {
                throw IOException("Geocoder failed")
            }
            return found.get()
        }
        @Suppress("DEPRECATION") // The listener version only exists on API 33+.
        return geocoder.getFromLocationName(address, 1).orEmpty()
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
