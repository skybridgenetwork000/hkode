package com.hkode.h3nrican3.app

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsetsController
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsActivity : AppCompatActivity() {

    private lateinit var layoutRoot: LinearLayout
    private lateinit var headerSettings: LinearLayout
    private lateinit var btnBack: FrameLayout
    private lateinit var tvScreenTitle: TextView

    // Theme selector
    private lateinit var tabThemeSystem: TextView
    private lateinit var tabThemeLight: TextView
    private lateinit var tabThemeDark: TextView

    // Notification switch
    private lateinit var switchNotifications: SwitchMaterial

    // Settings cards
    private lateinit var cardSettingsSecurity: MaterialCardView
    private lateinit var tvSettingsSecuritySubtitle: TextView
    private lateinit var cardSettingsSavedPasswords: MaterialCardView
    private lateinit var cardSettingsAbout: MaterialCardView
    private lateinit var cardSettingsTerms: MaterialCardView
    private lateinit var cardSettingsClearData: MaterialCardView

    private lateinit var historyManager: HistoryManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_settings)
            historyManager = HistoryManager(this)

            initializeViews()
            setupSystemBars()
            applyWindowInsets()
            applyFonts()

            try {
                switchNotifications.setOnCheckedChangeListener(null)
                switchNotifications.isChecked = NotificationScheduler.areNotificationsEnabled(this)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            setupListeners()
            updateThemeSelectionUI(ThemeManager.getSavedTheme(this))
            playSettingsEntranceAnimations()

            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    navigateBack()
                }
            })
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    private fun playSettingsEntranceAnimations() {
        try {
            AppAnimationUtil.bounceInDown(headerSettings, 30, 30f)
            findViewById<View>(R.id.card_settings_theme)?.let { AppAnimationUtil.bounceInUp(it, 70, 40f) }
            cardSettingsSecurity.let { AppAnimationUtil.bounceInUp(it, 110, 40f) }
            cardSettingsSavedPasswords.let { AppAnimationUtil.bounceInUp(it, 150, 45f) }
            findViewById<View>(R.id.card_settings_notifications)?.let { AppAnimationUtil.bounceInUp(it, 190, 45f) }
            cardSettingsAbout.let { AppAnimationUtil.bounceInUp(it, 230, 45f) }
            cardSettingsTerms.let { AppAnimationUtil.bounceInUp(it, 270, 45f) }
            cardSettingsClearData.let { AppAnimationUtil.bounceInUp(it, 310, 45f) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        if (AppSecurityManager.shouldEnforceLock(this)) {
            AppSecurityManager.launchLockScreen(this)
            return
        }
        updateSecurityStatusSubtitle()
    }

    private fun initializeViews() {
        layoutRoot = findViewById(R.id.layout_settings_root)
        headerSettings = findViewById(R.id.header_settings)
        btnBack = findViewById(R.id.btn_back_settings)
        tvScreenTitle = findViewById(R.id.tv_settings_screen_title)

        tabThemeSystem = findViewById(R.id.tab_theme_system)
        tabThemeLight = findViewById(R.id.tab_theme_light)
        tabThemeDark = findViewById(R.id.tab_theme_dark)

        switchNotifications = findViewById(R.id.switch_notifications)

        cardSettingsSecurity = findViewById(R.id.card_settings_security)
        tvSettingsSecuritySubtitle = findViewById(R.id.tv_settings_security_subtitle)
        cardSettingsSavedPasswords = findViewById(R.id.card_settings_saved_passwords)
        cardSettingsAbout = findViewById(R.id.card_settings_about)
        cardSettingsTerms = findViewById(R.id.card_settings_terms)
        cardSettingsClearData = findViewById(R.id.card_settings_clear_data)
    }

    private fun updateSecurityStatusSubtitle() {
        val passcodeEnabled = AppSecurityManager.isPasscodeEnabled(this)
        if (passcodeEnabled) {
            val biometricEnabled = AppSecurityManager.isBiometricEnabled(this)
            val timeoutSec = AppSecurityManager.getLockTimeoutSeconds(this)
            val timeoutLabel = if (timeoutSec == AppSecurityManager.TIMEOUT_IMMEDIATELY) "Immediate lock" else "60s lock"
            val bioLabel = if (biometricEnabled) " • Fingerprint on" else ""
            tvSettingsSecuritySubtitle.text = "Passcode active • $timeoutLabel$bioLabel"
        } else {
            tvSettingsSecuritySubtitle.text = "Disabled • Tap to set up 4-digit PIN & biometrics"
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { navigateBack() }

        cardSettingsSecurity.setOnClickListener {
            AppAnimationUtil.bouncePress(it)
            val intent = Intent(this, SecurityActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }

        cardSettingsSavedPasswords.setOnClickListener {
            AppAnimationUtil.bouncePress(it)
            PasswordVaultHelper.showPasswordVaultBottomSheet(this) { selectedPassword ->
                // Vault interaction
            }
        }

        tabThemeSystem.setOnClickListener {
            ThemeManager.setSavedTheme(this, ThemeManager.THEME_SYSTEM)
            updateThemeSelectionUI(ThemeManager.THEME_SYSTEM)
        }

        tabThemeLight.setOnClickListener {
            ThemeManager.setSavedTheme(this, ThemeManager.THEME_LIGHT)
            updateThemeSelectionUI(ThemeManager.THEME_LIGHT)
        }

        tabThemeDark.setOnClickListener {
            ThemeManager.setSavedTheme(this, ThemeManager.THEME_DARK)
            updateThemeSelectionUI(ThemeManager.THEME_DARK)
        }

        switchNotifications.setOnCheckedChangeListener { buttonView, isChecked ->
            if (!buttonView.isPressed) return@setOnCheckedChangeListener
            NotificationScheduler.setNotificationsEnabled(this, isChecked)
            if (isChecked) {
                CustomToast.showInfo(this, "Notifications On", "Security reminders enabled")
            } else {
                CustomToast.showInfo(this, "Notifications Off", "Security reminders disabled")
            }
        }

        cardSettingsAbout.setOnClickListener {
            AppAnimationUtil.bouncePress(it)
            showAboutBottomSheet()
        }

        cardSettingsTerms.setOnClickListener {
            AppAnimationUtil.bouncePress(it)
            showTermsBottomSheet()
        }

        cardSettingsClearData.setOnClickListener {
            AppAnimationUtil.bouncePress(it)
            showClearDataConfirmationDialog()
        }
    }

    private fun updateThemeSelectionUI(theme: String) {
        val primaryColor = ContextCompat.getColor(this, R.color.colorPrimary)
        val secondaryColor = ContextCompat.getColor(this, R.color.text_secondary)

        tabThemeSystem.setBackgroundResource(if (theme == ThemeManager.THEME_SYSTEM) R.drawable.bg_segmented_selected else 0)
        tabThemeSystem.setTextColor(if (theme == ThemeManager.THEME_SYSTEM) primaryColor else secondaryColor)
        tabThemeSystem.setTypeface(null, if (theme == ThemeManager.THEME_SYSTEM) Typeface.BOLD else Typeface.NORMAL)

        tabThemeLight.setBackgroundResource(if (theme == ThemeManager.THEME_LIGHT) R.drawable.bg_segmented_selected else 0)
        tabThemeLight.setTextColor(if (theme == ThemeManager.THEME_LIGHT) primaryColor else secondaryColor)
        tabThemeLight.setTypeface(null, if (theme == ThemeManager.THEME_LIGHT) Typeface.BOLD else Typeface.NORMAL)

        tabThemeDark.setBackgroundResource(if (theme == ThemeManager.THEME_DARK) R.drawable.bg_segmented_selected else 0)
        tabThemeDark.setTextColor(if (theme == ThemeManager.THEME_DARK) primaryColor else secondaryColor)
        tabThemeDark.setTypeface(null, if (theme == ThemeManager.THEME_DARK) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun showAboutBottomSheet() {
        val dialog = BottomSheetDialog(this, R.style.AppTheme_BottomSheetDialog)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_about, null)
        dialog.setContentView(sheetView)

        dialog.setOnShowListener { d ->
            val bsDialog = d as BottomSheetDialog
            val bottomSheetInternal = bsDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheetInternal?.let { sheet ->
                sheet.background = ContextCompat.getDrawable(this, R.drawable.bg_bottom_sheet)
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                behavior.isFitToContents = false
                behavior.expandedOffset = 0
                sheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            }
        }

        dialog.window?.let { win ->
            win.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            win.navigationBarColor = ContextCompat.getColor(this, R.color.card_background)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                win.isNavigationBarContrastEnforced = false
            }
        }

        sheetView.findViewById<View>(R.id.btn_about_close)?.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showTermsBottomSheet() {
        val dialog = BottomSheetDialog(this, R.style.AppTheme_BottomSheetDialog)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_terms, null)
        dialog.setContentView(sheetView)

        dialog.setOnShowListener { d ->
            val bsDialog = d as BottomSheetDialog
            val bottomSheetInternal = bsDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheetInternal?.let { sheet ->
                sheet.background = ContextCompat.getDrawable(this, R.drawable.bg_bottom_sheet)
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                behavior.isFitToContents = false
                behavior.expandedOffset = 0
                sheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            }
        }

        dialog.window?.let { win ->
            win.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            win.navigationBarColor = ContextCompat.getColor(this, R.color.card_background)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                win.isNavigationBarContrastEnforced = false
            }
        }

        sheetView.findViewById<View>(R.id.btn_terms_close)?.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showClearDataConfirmationDialog() {
        val dialog = Dialog(this, R.style.AppTheme_TransparentDialog)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_confirm_clear)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val width = (resources.displayMetrics.widthPixels * 0.88).toInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)

        val btnCancel = dialog.findViewById<MaterialButton>(R.id.btn_clear_cancel)
        val btnConfirm = dialog.findViewById<MaterialButton>(R.id.btn_clear_confirm)

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnConfirm.setOnClickListener {
            dialog.dismiss()
            historyManager.clearAll()
            ThemeManager.resetToDefault(this)

            getSharedPreferences("hkode_app_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("is_first_launch", true)
                .apply()

            CustomToast.showInfo(this, "Data Cleared", "Application data cleared")

            val intent = Intent(this, DashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            finish()
        }

        dialog.show()
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
        val originalTop = headerSettings.paddingTop

        ViewCompat.setOnApplyWindowInsetsListener(layoutRoot) { _, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            headerSettings.updatePadding(
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
