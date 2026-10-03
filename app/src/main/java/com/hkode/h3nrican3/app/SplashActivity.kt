package com.hkode.h3nrican3.app

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
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
import androidx.core.content.ContextCompat
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

        // Prepare initial animation state
        logo.scaleX = 0.2f
        logo.scaleY = 0.2f
        logo.alpha = 0f

        textview1.translationY = 30f
        textview1.alpha = 0f

        textview2.translationY = 30f
        textview2.alpha = 0f

        // Bouncing logo animation (comes out, bounces, breathing pulse)
        logo.scaleX = 0.5f
        logo.scaleY = 0.5f
        logo.alpha = 0f
        logo.animate()
            .scaleX(1.15f)
            .scaleY(1.15f)
            .alpha(1f)
            .setDuration(700)
            .setInterpolator(android.view.animation.OvershootInterpolator(2.8f))
            .withEndAction {
                logo.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(350)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .withEndAction {
                        // Subtle breathing pulse
                        logo.animate()
                            .scaleX(1.05f)
                            .scaleY(1.05f)
                            .setDuration(550)
                            .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
                            .withEndAction {
                                logo.animate()
                                    .scaleX(1.0f)
                                    .scaleY(1.0f)
                                    .setDuration(450)
                                    .start()
                            }
                            .start()
                    }
                    .start()
            }
            .start()

        // Text title slide up & fade in with bounce
        textview1.translationY = 60f
        textview1.alpha = 0f
        textview1.animate()
            .translationY(0f)
            .alpha(1f)
            .setStartDelay(350)
            .setDuration(600)
            .setInterpolator(android.view.animation.OvershootInterpolator(1.5f))
            .start()

        textview2.translationY = 40f
        textview2.alpha = 0f
        textview2.animate()
            .translationY(0f)
            .alpha(1f)
            .setStartDelay(500)
            .setDuration(550)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        lifecycleScope.launch {
            delay(2400)
            if (!isFinishing && !isDestroyed && !hasNavigated) {
                hasNavigated = true
                navigateNext()
            }
        }
    }

    private fun navigateNext() {
        val prefs = getSharedPreferences("hkode_app_prefs", Context.MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean("is_first_launch", true)

        if (isFirstLaunch) {
            val intent = Intent(this, OnboardingActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            finish()
        } else {
            val dashboardIntent = Intent(this, DashboardActivity::class.java)
            if (AppSecurityManager.isPasscodeEnabled(this)) {
                val lockIntent = Intent(this, PasscodeLockActivity::class.java).apply {
                    putExtra(PasscodeLockActivity.EXTRA_MODE, PasscodeLockActivity.MODE_UNLOCK)
                    putExtra(PasscodeLockActivity.EXTRA_TARGET_INTENT, dashboardIntent)
                }
                startActivity(lockIntent)
                finish()
            } else {
                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    this,
                    logo,
                    "app_logo"
                )
                startActivity(dashboardIntent, options.toBundle())
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (hasNavigated && !isFinishing) {
            window.sharedElementReturnTransition = null
            window.sharedElementExitTransition = null
            finish()
            overridePendingTransition(0, 0)
        }
    }

    private fun setupSystemBars() {
        val bgColor = ContextCompat.getColor(this, R.color.app_background)
        val isDarkMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                val flags = if (!isDarkMode) {
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                            WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                } else {
                    0
                }
                controller.setSystemBarsAppearance(
                    flags,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                            WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                )
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = if (!isDarkMode) {
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            } else {
                0
            }
        }
        window.statusBarColor = bgColor
        window.navigationBarColor = bgColor
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

            textview2.post {
                val paint = textview2.paint
                val textWidth = paint.measureText(textview2.text.toString())
                if (textWidth > 0) {
                    val shader = LinearGradient(
                        0f, 0f, textWidth, 0f,
                        intArrayOf(
                            Color.parseColor("#00E676"),
                            Color.parseColor("#00E5FF"),
                            Color.parseColor("#2979FF")
                        ),
                        floatArrayOf(0f, 0.45f, 1f),
                        Shader.TileMode.CLAMP
                    )
                    textview2.paint.shader = shader
                    textview2.invalidate()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
