package com.hkode.h3nrican3.app

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private lateinit var linear1: LinearLayout
    private lateinit var linear2: LinearLayout
    private lateinit var logo: ImageView
    private lateinit var linear3: LinearLayout
    private lateinit var textview1: TextView
    private lateinit var textview2: TextView

    private var hasNavigated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash)
        initialize()
        initializeLogic()
    }

    private fun initialize() {
        linear1 = findViewById(R.id.linear1)
        linear2 = findViewById(R.id.linear2)
        logo = findViewById(R.id.logo)
        linear3 = findViewById(R.id.linear3)
        textview1 = findViewById(R.id.textview1)
        textview2 = findViewById(R.id.textview2)
    }

    private fun initializeLogic() {
        setupSystemBars()
        applyWindowInsets()
        applyFonts()

        ViewCompat.setTransitionName(logo, "app_logo")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        lifecycleScope.launch {
            delay(2500)
            if (!isFinishing && !isDestroyed && !hasNavigated) {
                hasNavigated = true
                navigateToDashboard()
            }
        }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
            this,
            logo,
            "app_logo"
        )
        startActivity(intent, options.toBundle())
    }

    override fun onStop() {
        super.onStop()
        // Once Dashboard is presented and Splash is in background, finish Splash completely
        // so it cannot be returned to and doesn't hold memory.
        if (hasNavigated && !isFinishing) {
            finish()
        }
    }

    private fun setupSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                            WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                            WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                )
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                        View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            )
        }
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE
    }

    private fun applyWindowInsets() {
        val originalLeft = linear1.paddingLeft
        val originalTop = linear1.paddingTop
        val originalRight = linear1.paddingRight
        val originalBottom = linear1.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(linear1) { v, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            v.updatePadding(
                left = originalLeft + statusBarInsets.left,
                top = originalTop + statusBarInsets.top,
                right = originalRight + statusBarInsets.right,
                bottom = originalBottom + navBarInsets.bottom
            )
            insets
        }
    }

    private fun applyFonts() {
        try {
            textview1.typeface = Typeface.createFromAsset(assets, "fonts/outfit_nomal.ttf")
            textview2.typeface = Typeface.createFromAsset(assets, "fonts/outfit_bold.ttf")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
