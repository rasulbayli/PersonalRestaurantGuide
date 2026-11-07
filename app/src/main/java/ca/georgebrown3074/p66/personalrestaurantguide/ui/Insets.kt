package ca.georgebrown3074.p66.personalrestaurantguide.ui

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Pads [this] so content clears the status bar, navigation bar, and keyboard.
 * Call it on the screen root after enableEdgeToEdge().
 */
fun View.applySystemBarInsets() {
    val startLeft = paddingLeft
    val startTop = paddingTop
    val startRight = paddingRight
    val startBottom = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val bars = windowInsets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
        )
        val left = startLeft + bars.left
        val top = startTop + bars.top
        val right = startRight + bars.right
        val bottom = startBottom + bars.bottom
        // setPadding requests a layout. Skip it when nothing changed so inset
        // passes cannot keep scheduling another layout.
        if (view.paddingLeft != left || view.paddingTop != top ||
            view.paddingRight != right || view.paddingBottom != bottom
        ) {
            view.setPadding(left, top, right, bottom)
        }
        WindowInsetsCompat.CONSUMED
    }
    ViewCompat.requestApplyInsets(this)
}
