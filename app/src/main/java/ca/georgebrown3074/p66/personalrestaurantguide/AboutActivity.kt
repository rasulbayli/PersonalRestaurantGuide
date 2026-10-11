package ca.georgebrown3074.p66.personalrestaurantguide

// Kerem Bas: list, splash, and about screens. 

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import ca.georgebrown3074.p66.personalrestaurantguide.ui.applySystemBarInsets
import com.google.android.material.appbar.MaterialToolbar

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_about)
        findViewById<View>(R.id.aboutRoot).applySystemBarInsets()

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        findViewById<TextView>(R.id.txtVersion).text =
            getString(R.string.version_format, appVersionName())
    }

    private fun appVersionName(): String {
        return try {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            info.versionName ?: getString(R.string.version_unknown)
        } catch (_: PackageManager.NameNotFoundException) {
            getString(R.string.version_unknown)
        }
    }
}
