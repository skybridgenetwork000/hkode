package com.hkode.h3nrican3.app

import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.ChangeImageTransform
import android.transition.ChangeTransform
import android.transition.Transition
import android.transition.TransitionSet
import android.view.View
import android.view.WindowInsetsController
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

class DashboardActivity : AppCompatActivity() {

    private lateinit var linear1: LinearLayout
    private lateinit var linear2: LinearLayout
    private lateinit var logo: ImageView
    private lateinit var textview1: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dashboard)

        setupSharedElementTransitions()
        initialize()
        initializeLogic()
    }

    private fun setupSharedElementTransitions() {
        // Configure a smooth enter transition for the shared logo
        val transitionSet = TransitionSet().apply {
            addTransition(ChangeBounds())
            addTransition(ChangeTransform())
            addTransition(ChangeImageTransform())
            duration = 400
            interpolator = DecelerateInterpolator(1.5f)
        }
        window.sharedElementEnterTransition = transitionSet

        // CRITICAL FIX: Explicitly disable return and exit shared element transitions.
        // This prevents Android from attempting to animate the logo back to SplashActivity
        // (which is already finished) or leaving a ghost overlay view on the home screen!
        window.sharedElementReturnTransition = null
        window.sharedElementExitTransition = null
        window.returnTransition = null
        window.exitTransition = null

        transitionSet.addListener(object : Transition.TransitionListener {
            override fun onTransitionStart(transition: Transition) {}
            override fun onTransitionEnd(transition: Transition) {
                // Once enter transition completes, ensure return transition remains null
                window.sharedElementReturnTransition = null
            }
            override fun onTransitionCancel(transition: Transition) {
                window.sharedElementReturnTransition = null
            }
            override fun onTransitionPause(transition: Transition) {}
            override fun onTransitionResume(transition: Transition) {}
        })
    }

    private fun initialize() {
        linear1 = findViewById(R.id.linear1)
        linear2 = findViewById(R.id.linear2)
        logo = findViewById(R.id.logo)
        textview1 = findViewById(R.id.textview1)
    }

    private fun initializeLogic() {
        ViewCompat.setTransitionName(logo, "app_logo")
        setupSystemBars()
        applyWindowInsets()
        applyFonts()

        // Handle back press to exit cleanly to home screen without triggering return transition
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    override fun finish() {
        // Clear any transition coordinators to ensure no overlay is drawn on launcher
        window.sharedElementReturnTransition = null
        window.sharedElementExitTransition = null
        super.finish()
        overridePendingTransition(0, android.R.anim.fade_out)
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
            textview1.typeface = Typeface.createFromAsset(assets, "fonts/outfit_bold.ttf")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
