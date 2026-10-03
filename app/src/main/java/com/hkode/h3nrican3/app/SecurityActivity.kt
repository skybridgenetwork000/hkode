package com.hkode.h3nrican3.app

import android.content.res.Configuration
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial

class SecurityActivity : AppCompatActivity() {

    private lateinit var layoutRoot: LinearLayout
    private lateinit var headerSecurity: LinearLayout
    private lateinit var btnBack: FrameLayout
    private lateinit var tvScreenTitle: TextView

    // Passcode controls
    private lateinit var switchPasscode: SwitchMaterial
    private lateinit var tvPasscodeSubtitle: TextView
    private lateinit var rowChangePasscode: LinearLayout
    private lateinit var dividerChangePasscode: View
    private lateinit var rowSecurityQuestions: LinearLayout
    private lateinit var dividerSecurityQuestions: View
    private lateinit var tvSecurityQuestionsSubtitle: TextView

    // Biometric controls
    private lateinit var switchBiometric: SwitchMaterial
    private lateinit var tvBiometricSubtitle: TextView
    private lateinit var rowBiometric: LinearLayout
    private lateinit var dividerBiometric: View

    // Auto-lock controls
    private lateinit var cardSecurityTimeout: MaterialCardView
    private lateinit var tvHeaderLockSetup: TextView
    private lateinit var tvHeaderLockSetupSub: TextView
    private lateinit var rowTimeoutImmediately: LinearLayout
    private lateinit var rowTimeout60Seconds: LinearLayout
    private lateinit var ivRadioImmediately: ImageView
    private lateinit var ivRadio60Seconds: ImageView
    private lateinit var tvTitleTimeoutImmediately: TextView
    private lateinit var tvTitleTimeout60: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_security)
            initializeViews()
            setupSystemBars()
            applyWindowInsets()
            applyFonts()
            setupListeners()
            updateSecurityUI()
            playEntranceAnimations()

            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    navigateBack()
                }
            })
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        if (AppSecurityManager.shouldEnforceLock(this)) {
            AppSecurityManager.launchLockScreen(this)
            return
        }
        updateSecurityUI()
    }

    private fun initializeViews() {
        layoutRoot = findViewById(R.id.layout_security_root)
        headerSecurity = findViewById(R.id.header_security)
        btnBack = findViewById(R.id.btn_back_security)
        tvScreenTitle = findViewById(R.id.tv_security_screen_title)

        switchPasscode = findViewById(R.id.switch_security_passcode)
        tvPasscodeSubtitle = findViewById(R.id.tv_security_passcode_subtitle)
        rowChangePasscode = findViewById(R.id.row_security_change_passcode)
        dividerChangePasscode = findViewById(R.id.divider_security_change_passcode)
        rowSecurityQuestions = findViewById(R.id.row_security_questions)
        dividerSecurityQuestions = findViewById(R.id.divider_security_questions)
        tvSecurityQuestionsSubtitle = findViewById(R.id.tv_security_questions_subtitle)

        switchBiometric = findViewById(R.id.switch_security_biometric)
        tvBiometricSubtitle = findViewById(R.id.tv_security_biometric_subtitle)
        rowBiometric = findViewById(R.id.row_security_biometric)
        dividerBiometric = findViewById(R.id.divider_security_biometric)

        cardSecurityTimeout = findViewById(R.id.card_security_timeout)
        tvHeaderLockSetup = findViewById(R.id.tv_header_lock_setup)
        tvHeaderLockSetupSub = findViewById(R.id.tv_header_lock_setup_sub)
        rowTimeoutImmediately = findViewById(R.id.row_timeout_immediately)
        rowTimeout60Seconds = findViewById(R.id.row_timeout_60_seconds)
        ivRadioImmediately = findViewById(R.id.iv_radio_immediately)
        ivRadio60Seconds = findViewById(R.id.iv_radio_60_seconds)
        tvTitleTimeoutImmediately = findViewById(R.id.tv_title_timeout_immediately)
        tvTitleTimeout60 = findViewById(R.id.tv_title_timeout_60)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { navigateBack() }

        rowChangePasscode.setOnClickListener {
            AppAnimationUtil.bouncePress(it)
            PasscodeSetupDialog.show(this, isChangingPin = true, onSuccess = {
                updateSecurityUI()
            })
        }

        rowSecurityQuestions.setOnClickListener {
            AppAnimationUtil.bouncePress(it)
            SecurityQuestionsDialog.showSetup(this, mandatory = false, onSuccess = {
                updateSecurityUI()
            })
        }

        rowTimeoutImmediately.setOnClickListener {
            AppAnimationUtil.bouncePress(it)
            AppSecurityManager.setLockTimeoutSeconds(this, AppSecurityManager.TIMEOUT_IMMEDIATELY)
            updateTimeoutUI()
            CustomToast.showInfo(this, "Auto-lock", "Auto-lock set to immediately")
        }

        rowTimeout60Seconds.setOnClickListener {
            AppAnimationUtil.bouncePress(it)
            AppSecurityManager.setLockTimeoutSeconds(this, AppSecurityManager.TIMEOUT_60_SECONDS)
            updateTimeoutUI()
            CustomToast.showInfo(this, "Auto-lock", "Auto-lock set to 60 seconds")
        }
    }

    private fun updateSecurityUI() {
        val passcodeEnabled = AppSecurityManager.isPasscodeEnabled(this)
        switchPasscode.setOnCheckedChangeListener(null)
        switchPasscode.isChecked = passcodeEnabled

        val biometricStatus = AppSecurityManager.checkBiometricStatus(this)
        val hasBiometricHardware = biometricStatus == AppSecurityManager.BiometricStatus.AVAILABLE

        if (passcodeEnabled) {
            tvPasscodeSubtitle.text = "Enabled (Protected with 4-digit PIN)"
            rowChangePasscode.visibility = View.VISIBLE
            dividerChangePasscode.visibility = View.VISIBLE

            rowSecurityQuestions.visibility = View.VISIBLE
            dividerSecurityQuestions.visibility = View.VISIBLE
            val hasQuestions = AppSecurityManager.hasSecurityQuestions(this)
            tvSecurityQuestionsSubtitle.text = if (hasQuestions) {
                "2 questions configured for recovery"
            } else {
                "Not set • Tap to configure recovery questions"
            }

            // Auto-lock section visible
            tvHeaderLockSetup.visibility = View.VISIBLE
            tvHeaderLockSetupSub.visibility = View.VISIBLE
            cardSecurityTimeout.visibility = View.VISIBLE
            updateTimeoutUI()

            if (!hasBiometricHardware) {
                rowBiometric.visibility = View.GONE
                dividerBiometric.visibility = View.GONE
            } else {
                rowBiometric.visibility = View.VISIBLE
                dividerBiometric.visibility = View.VISIBLE

                val biometricEnabled = AppSecurityManager.isBiometricEnabled(this)
                switchBiometric.setOnCheckedChangeListener(null)
                switchBiometric.isEnabled = true
                switchBiometric.isChecked = biometricEnabled
                tvBiometricSubtitle.text = if (biometricEnabled) "Enabled (Unlock with fingerprint)" else "Disabled"
                switchBiometric.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (!buttonView.isPressed) return@setOnCheckedChangeListener
                    AppSecurityManager.setBiometricEnabled(this, isChecked)
                    if (isChecked) {
                        CustomToast.showInfo(this, "Biometrics On", "Fingerprint unlock enabled")
                    } else {
                        CustomToast.showInfo(this, "Biometrics Off", "Fingerprint unlock disabled")
                    }
                    updateSecurityUI()
                }
            }
        } else {
            tvPasscodeSubtitle.text = "Disabled (No passcode set)"
            rowChangePasscode.visibility = View.GONE
            dividerChangePasscode.visibility = View.GONE

            rowSecurityQuestions.visibility = View.GONE
            dividerSecurityQuestions.visibility = View.GONE

            // Auto-lock section hidden when passcode disabled
            tvHeaderLockSetup.visibility = View.GONE
            tvHeaderLockSetupSub.visibility = View.GONE
            cardSecurityTimeout.visibility = View.GONE

            rowBiometric.visibility = View.GONE
            dividerBiometric.visibility = View.GONE

            switchBiometric.setOnCheckedChangeListener(null)
            switchBiometric.isChecked = false
            switchBiometric.isEnabled = false
        }

        switchPasscode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                PasscodeSetupDialog.show(this, isChangingPin = false, onSuccess = {
                    updateSecurityUI()
                }, onCancel = {
                    updateSecurityUI()
                })
            } else {
                confirmDisablePasscode()
            }
        }
    }

    private fun updateTimeoutUI() {
        val currentTimeout = AppSecurityManager.getLockTimeoutSeconds(this)
        val primaryColor = ContextCompat.getColor(this, R.color.colorPrimary)
        val mutedColor = ContextCompat.getColor(this, R.color.text_muted)
        val textPrimaryColor = ContextCompat.getColor(this, R.color.text_primary)
        val textSecondaryColor = ContextCompat.getColor(this, R.color.text_secondary)

        if (currentTimeout == AppSecurityManager.TIMEOUT_IMMEDIATELY) {
            ivRadioImmediately.setImageResource(R.drawable.ic_check)
            ivRadioImmediately.setColorFilter(primaryColor)
            tvTitleTimeoutImmediately.setTextColor(primaryColor)

            ivRadio60Seconds.setImageResource(R.drawable.ic_lock)
            ivRadio60Seconds.setColorFilter(mutedColor)
            tvTitleTimeout60.setTextColor(textPrimaryColor)
        } else {
            ivRadioImmediately.setImageResource(R.drawable.ic_lock)
            ivRadioImmediately.setColorFilter(mutedColor)
            tvTitleTimeoutImmediately.setTextColor(textPrimaryColor)

            ivRadio60Seconds.setImageResource(R.drawable.ic_check)
            ivRadio60Seconds.setColorFilter(primaryColor)
            tvTitleTimeout60.setTextColor(primaryColor)
        }
    }

    private fun confirmDisablePasscode() {
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Disable Passcode Protection?")
            .setMessage("Anyone with access to your device will be able to open Hkode and view your encrypted data.")
            .setPositiveButton("Disable") { _, _ ->
                AppSecurityManager.disablePasscode(this)
                CustomToast.showInfo(this, "Passcode Disabled", "Passcode protection disabled")
                updateSecurityUI()
            }
            .setNegativeButton("Cancel") { _, _ ->
                updateSecurityUI()
            }
            .setOnCancelListener {
                updateSecurityUI()
            }
            .create()
        dialog.show()
    }

    private fun playEntranceAnimations() {
        try {
            AppAnimationUtil.bounceInDown(headerSecurity, 30, 30f)
            findViewById<View>(R.id.card_security_auth)?.let { AppAnimationUtil.bounceInUp(it, 80, 40f) }
            cardSecurityTimeout.let { AppAnimationUtil.bounceInUp(it, 140, 45f) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun navigateBack() {
        finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    private fun setupSystemBars() {
        val bgColor = ContextCompat.getColor(this, R.color.app_background)
        val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

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
        val originalTop = headerSecurity.paddingTop

        ViewCompat.setOnApplyWindowInsetsListener(layoutRoot) { _, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            headerSecurity.updatePadding(
                top = originalTop + statusBarInsets.top
            )
            layoutRoot.updatePadding(
                bottom = navBarInsets.bottom
            )
            insets
        }
    }

    private fun applyFonts() {
        try {
            tvScreenTitle.typeface = Typeface.createFromAsset(assets, "fonts/outfit_semibold.ttf")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
