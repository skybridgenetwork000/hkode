package com.hkode.h3nrican3.app

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import android.widget.TextView
import android.widget.ViewFlipper
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.button.MaterialButton

class OnboardingActivity : AppCompatActivity() {

    private lateinit var flipper: ViewFlipper
    private lateinit var btnSkip: MaterialButton
    private lateinit var btnAction: MaterialButton
    private lateinit var dot1: View
    private lateinit var dot2: View
    private lateinit var dot3: View
    private lateinit var dot4: View
    private lateinit var dot5: View
    private lateinit var layoutRoot: View
    private lateinit var headerOnboarding: View
    private lateinit var footerOnboarding: View

    private var currentStep = 0
    private val totalSteps = 5

    private lateinit var gestureDetector: android.view.GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        initializeViews()
        setupSystemBars()
        applyWindowInsets()
        applyFonts()
        setupListeners()
        setupSwipeGestures()
        updateDots()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentStep > 0) {
                    showPreviousStep()
                } else {
                    finish()
                }
            }
        })
    }

    private fun setupSwipeGestures() {
        gestureDetector = android.view.GestureDetector(this, object : android.view.GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 80
            private val SWIPE_VELOCITY_THRESHOLD = 80

            override fun onFling(
                e1: android.view.MotionEvent?,
                e2: android.view.MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX < 0) {
                            // Swipe left -> Next step
                            showNextStep()
                        } else {
                            // Swipe right -> Previous step
                            showPreviousStep()
                        }
                        return true
                    }
                }
                return false
            }
        })

        val touchListener = View.OnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        flipper.setOnTouchListener(touchListener)
        layoutRoot.setOnTouchListener(touchListener)
    }

    private fun showNextStep() {
        if (currentStep < totalSteps - 1) {
            currentStep++
            flipper.setInAnimation(this, R.anim.slide_in_right)
            flipper.setOutAnimation(this, R.anim.slide_out_left)
            flipper.showNext()
            animateCurrentView()
            updateDots()
        } else {
            finishOnboarding()
        }
    }

    private fun showPreviousStep() {
        if (currentStep > 0) {
            currentStep--
            flipper.setInAnimation(this, R.anim.slide_in_left)
            flipper.setOutAnimation(this, R.anim.slide_out_right)
            flipper.showPrevious()
            animateCurrentView()
            updateDots()
        }
    }

    private fun animateCurrentView() {
        val currentView = flipper.currentView as? android.view.ViewGroup ?: return
        currentView.scaleX = 0.88f
        currentView.scaleY = 0.88f
        currentView.alpha = 0f
        currentView.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .alpha(1.0f)
            .setDuration(400)
            .setInterpolator(android.view.animation.OvershootInterpolator(1.6f))
            .start()

        for (i in 0 until currentView.childCount) {
            val child = currentView.getChildAt(i)
            child.translationY = (30f + i * 15f) * resources.displayMetrics.density
            child.alpha = 0f
            child.animate()
                .translationY(0f)
                .alpha(1f)
                .setStartDelay((i * 45).toLong())
                .setDuration(360)
                .setInterpolator(android.view.animation.OvershootInterpolator(1.4f))
                .start()
        }
    }

    private fun initializeViews() {
        layoutRoot = findViewById(R.id.layout_onboarding_root)
        headerOnboarding = findViewById(R.id.header_onboarding)
        footerOnboarding = findViewById(R.id.footer_onboarding)
        flipper = findViewById(R.id.flipper_onboarding)
        btnSkip = findViewById(R.id.btn_onboarding_skip)
        btnAction = findViewById(R.id.btn_onboarding_action)
        dot1 = findViewById(R.id.dot_1)
        dot2 = findViewById(R.id.dot_2)
        dot3 = findViewById(R.id.dot_3)
        dot4 = findViewById(R.id.dot_4)
        dot5 = findViewById(R.id.dot_5)
    }

    private fun setupListeners() {
        btnSkip.setOnClickListener {
            finishOnboarding()
        }

        btnAction.setOnClickListener {
            showNextStep()
        }
    }

    private fun updateDots() {
        val primaryColor = ContextCompat.getColor(this, R.color.colorPrimary)
        val mutedColor = ContextCompat.getColor(this, R.color.segmented_stroke)

        val dots = listOf(dot1, dot2, dot3, dot4, dot5)
        for (i in dots.indices) {
            val dot = dots[i]
            val isCurrent = (i == currentStep)
            dot.layoutParams.width = if (isCurrent) dpToPx(20) else dpToPx(6)
            dot.setBackgroundColor(if (isCurrent) primaryColor else mutedColor)
            dot.requestLayout()
        }

        btnAction.text = if (currentStep == totalSteps - 1) "Get Started" else "Continue"
        btnSkip.visibility = if (currentStep == totalSteps - 1) View.INVISIBLE else View.VISIBLE
    }

    private fun finishOnboarding() {
        getSharedPreferences("hkode_app_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("is_first_launch", false)
            .apply()

        val intent = Intent(this, DashboardActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
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
        val originalTop = headerOnboarding.paddingTop
        val originalBottom = footerOnboarding.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(layoutRoot) { _, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            headerOnboarding.updatePadding(
                top = originalTop + statusBarInsets.top
            )
            footerOnboarding.updatePadding(
                bottom = originalBottom + navBarInsets.bottom
            )
            insets
        }
    }

    private fun applyFonts() {
        try {
            findViewById<TextView>(R.id.tv_onboarding_title_1)?.typeface = Typeface.createFromAsset(assets, "fonts/outfit_bold.ttf")
            findViewById<TextView>(R.id.tv_onboarding_title_2)?.typeface = Typeface.createFromAsset(assets, "fonts/outfit_bold.ttf")
            findViewById<TextView>(R.id.tv_onboarding_title_3)?.typeface = Typeface.createFromAsset(assets, "fonts/outfit_bold.ttf")
            findViewById<TextView>(R.id.tv_onboarding_title_4)?.typeface = Typeface.createFromAsset(assets, "fonts/outfit_bold.ttf")
            findViewById<TextView>(R.id.tv_onboarding_title_5)?.typeface = Typeface.createFromAsset(assets, "fonts/outfit_bold.ttf")
            btnAction.typeface = Typeface.createFromAsset(assets, "fonts/outfit_semibold.ttf")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
