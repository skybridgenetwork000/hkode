package com.hkode.h3nrican3.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

class PasscodeLockActivity : AppCompatActivity() {

    companion object {
        const val MODE_UNLOCK = "MODE_UNLOCK"
        const val EXTRA_MODE = "EXTRA_MODE"
        const val EXTRA_TARGET_INTENT = "EXTRA_TARGET_INTENT"
    }

    private lateinit var layoutRoot: View
    private lateinit var badgeLockIcon: FrameLayout
    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var tvError: TextView
    private lateinit var layoutSlots: LinearLayout
    private lateinit var tableKeypad: View
    private lateinit var btnKeyBiometric: FrameLayout
    private lateinit var ivBiometricIcon: ImageView
    private lateinit var btnKey0: TextView
    private lateinit var btnForgotPasscode: TextView

    private val slots = ArrayList<FrameLayout>()
    private val dots = ArrayList<View>()
    private val currentPin = StringBuilder()

    private var targetIntent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_passcode_lock)

        targetIntent = intent.getParcelableExtra(EXTRA_TARGET_INTENT)

        initializeViews()
        setupSystemBars()
        applyWindowInsets()
        setupKeypad()
        playEntranceAnimations()

        // Block bypassing via back button
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                moveTaskToBack(true)
            }
        })

        // Configure Biometric Button visibility
        val biometricAvailable = AppSecurityManager.checkBiometricStatus(this) == AppSecurityManager.BiometricStatus.AVAILABLE
        val biometricEnabled = AppSecurityManager.isBiometricEnabled(this)

        if (biometricAvailable && biometricEnabled) {
            btnKeyBiometric.visibility = View.VISIBLE
            val density = resources.displayMetrics.density
            val normalSize = (68 * density).toInt()
            btnKey0.layoutParams.width = normalSize
            btnKey0.setBackgroundResource(R.drawable.bg_keypad_button)

            btnKeyBiometric.setOnClickListener {
                triggerBiometricPrompt()
            }
            // Auto prompt biometric on cold launch / lock trigger
            layoutRoot.post {
                triggerBiometricPrompt()
            }
        } else {
            // If biometric is disabled or unavailable:
            // Remove fingerprint icon completely, 0 button fills both fingerprint space and itself
            btnKeyBiometric.visibility = View.GONE
            val density = resources.displayMetrics.density
            val wideWidth = ((68 * 2 + 12) * density).toInt()
            btnKey0.layoutParams.width = wideWidth
            btnKey0.setBackgroundResource(R.drawable.bg_keypad_button_wide)
        }
    }

    private fun initializeViews() {
        layoutRoot = findViewById(R.id.layout_lock_root)
        badgeLockIcon = findViewById(R.id.badge_lock_icon)
        tvTitle = findViewById(R.id.tv_lock_title)
        tvSubtitle = findViewById(R.id.tv_lock_subtitle)
        tvError = findViewById(R.id.tv_lock_error)
        layoutSlots = findViewById(R.id.layout_pin_slots)
        tableKeypad = findViewById(R.id.table_keypad)
        btnKeyBiometric = findViewById(R.id.btn_key_biometric)
        ivBiometricIcon = findViewById(R.id.iv_key_biometric_icon)
        btnKey0 = findViewById(R.id.btn_key_0)
        btnForgotPasscode = findViewById(R.id.btn_forgot_passcode)

        btnForgotPasscode.setOnClickListener {
            AppAnimationUtil.bouncePress(it)
            if (!AppSecurityManager.hasSecurityQuestions(this)) {
                CustomToast.showWarning(this, "No Questions Found", "No recovery security questions were set up.")
                return@setOnClickListener
            }
            SecurityQuestionsDialog.showVerify(
                context = this,
                onSuccess = {
                    AppSecurityManager.unlock()
                    CustomToast.showSuccess(this, "Recovery Verified", "Identity confirmed. You can now set a new passcode.")
                    PasscodeSetupDialog.show(
                        context = this,
                        isChangingPin = true,
                        onSuccess = {
                            onUnlockSuccess()
                        },
                        onCancel = {
                            onUnlockSuccess()
                        }
                    )
                }
            )
        }

        slots.add(findViewById(R.id.slot_pin_1))
        slots.add(findViewById(R.id.slot_pin_2))
        slots.add(findViewById(R.id.slot_pin_3))
        slots.add(findViewById(R.id.slot_pin_4))

        dots.add(findViewById(R.id.dot_pin_1))
        dots.add(findViewById(R.id.dot_pin_2))
        dots.add(findViewById(R.id.dot_pin_3))
        dots.add(findViewById(R.id.dot_pin_4))
    }

    private fun playEntranceAnimations() {
        layoutRoot.alpha = 0f
        val density = resources.displayMetrics.density
        layoutRoot.translationY = 90f * density
        layoutRoot.scaleX = 0.94f
        layoutRoot.scaleY = 0.94f
        layoutRoot.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(460)
            .setInterpolator(android.view.animation.OvershootInterpolator(1.35f))
            .start()

        AppAnimationUtil.bounceInDown(badgeLockIcon, 50, 40f)
        AppAnimationUtil.bounceInDown(tvTitle, 100, 30f)
        AppAnimationUtil.bounceInDown(tvSubtitle, 140, 20f)
        AppAnimationUtil.bounceInUp(layoutSlots, 190, 50f)
        AppAnimationUtil.bounceInUp(tableKeypad, 250, 70f)
    }

    private fun setupKeypad() {
        val keyIds = arrayOf(
            R.id.btn_key_0 to "0",
            R.id.btn_key_1 to "1",
            R.id.btn_key_2 to "2",
            R.id.btn_key_3 to "3",
            R.id.btn_key_4 to "4",
            R.id.btn_key_5 to "5",
            R.id.btn_key_6 to "6",
            R.id.btn_key_7 to "7",
            R.id.btn_key_8 to "8",
            R.id.btn_key_9 to "9"
        )

        for ((id, digit) in keyIds) {
            findViewById<View>(id)?.setOnClickListener { view ->
                view.animate().scaleX(0.86f).scaleY(0.86f).setDuration(60).withEndAction {
                    view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120)
                        .setInterpolator(android.view.animation.OvershootInterpolator(2.4f))
                        .start()
                }.start()
                onDigitEntered(digit)
            }
        }

        findViewById<View>(R.id.btn_key_backspace)?.setOnClickListener { view ->
            view.animate().scaleX(0.86f).scaleY(0.86f).setDuration(60).withEndAction {
                view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120)
                    .setInterpolator(android.view.animation.OvershootInterpolator(2.4f))
                    .start()
            }.start()
            onBackspace()
        }
    }

    private fun onDigitEntered(digit: String) {
        if (currentPin.length >= 4) return
        currentPin.append(digit)
        val enteredIndex = currentPin.length - 1
        updateSlotsUI()
        AppAnimationUtil.elasticPinPop(slots[enteredIndex])

        if (currentPin.length == 4) {
            val entered = currentPin.toString()
            if (AppSecurityManager.verifyPasscode(this, entered)) {
                AppAnimationUtil.elasticCheckSuccess(slots) {
                    onUnlockSuccess()
                }
            } else {
                onUnlockFailed()
            }
        }
    }

    private fun onBackspace() {
        if (currentPin.isNotEmpty()) {
            currentPin.deleteCharAt(currentPin.length - 1)
            tvError.visibility = View.INVISIBLE
            updateSlotsUI()
        }
    }

    private fun updateSlotsUI() {
        for (i in 0 until 4) {
            if (i < currentPin.length) {
                dots[i].visibility = View.VISIBLE
                slots[i].setBackgroundResource(R.drawable.bg_pin_box_active)
            } else {
                dots[i].visibility = View.GONE
                slots[i].setBackgroundResource(R.drawable.bg_pin_box)
            }
        }
    }

    private fun onUnlockSuccess() {
        AppSecurityManager.unlock()

        layoutRoot.animate()
            .alpha(0f)
            .scaleX(0.96f)
            .scaleY(0.96f)
            .setDuration(200)
            .withEndAction {
                if (targetIntent != null) {
                    startActivity(targetIntent)
                }
                finish()
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            }
            .start()
    }

    private fun onUnlockFailed() {
        tvError.visibility = View.VISIBLE
        tvError.text = "Incorrect passcode"

        val shake = AnimationUtils.loadAnimation(this, R.anim.shake_pin)
        layoutSlots.startAnimation(shake)

        for (slot in slots) {
            slot.setBackgroundResource(R.drawable.bg_pin_box_error)
        }

        layoutSlots.postDelayed({
            currentPin.clear()
            updateSlotsUI()
        }, 500)
    }

    private fun triggerBiometricPrompt() {
        if (!AppSecurityManager.isBiometricEnabled(this)) return

        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onUnlockSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                // Stay on passcode screen for fallback
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Biometric failed once, fallback passcode remains ready
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Hkode")
            .setSubtitle("Touch the fingerprint sensor to unlock")
            .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
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
        ViewCompat.setOnApplyWindowInsetsListener(layoutRoot) { _, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            layoutRoot.updatePadding(
                top = statusBarInsets.top + 16,
                bottom = navBarInsets.bottom + 16
            )
            insets
        }
    }
}
