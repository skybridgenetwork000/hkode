package com.hkode.h3nrican3.app

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.ChangeImageTransform
import android.transition.ChangeTransform
import android.transition.Transition
import android.transition.TransitionSet
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsetsController
import android.view.animation.DecelerateInterpolator
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
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class DashboardActivity : AppCompatActivity() {

    // Preserved shared transition elements
    private lateinit var linear1: LinearLayout
    private lateinit var linear2: LinearLayout
    private lateinit var logo: ImageView
    private lateinit var textview1: TextView
    private lateinit var tvHomeSubtitle: TextView
    private lateinit var btnHomeHistory: FrameLayout
    private lateinit var btnHomeSettings: FrameLayout

    // Launcher cards
    private lateinit var cardHomeEncrypt: MaterialCardView
    private lateinit var cardHomeDecrypt: MaterialCardView
    private lateinit var cardHomeCustom: MaterialCardView
    private lateinit var tvEncryptCardTitle: TextView
    private lateinit var tvDecryptCardTitle: TextView
    private lateinit var tvCustomCardTitle: TextView
    private lateinit var tvHomeSecuritySpec: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dashboard)

        setupSharedElementTransitions()
        initializeViews()
        initializeLogic()
        setupListeners()
    }

    private fun setupSharedElementTransitions() {
        val transitionSet = TransitionSet().apply {
            addTransition(ChangeBounds())
            addTransition(ChangeTransform())
            addTransition(ChangeImageTransform())
            duration = 350
            interpolator = DecelerateInterpolator(1.5f)
        }
        window.sharedElementEnterTransition = transitionSet

        window.sharedElementReturnTransition = null
        window.sharedElementExitTransition = null
        window.returnTransition = null
        window.exitTransition = null

        transitionSet.addListener(object : Transition.TransitionListener {
            override fun onTransitionStart(transition: Transition) {}
            override fun onTransitionEnd(transition: Transition) {
                ViewCompat.setTransitionName(logo, null)
                window.sharedElementEnterTransition = null
                window.sharedElementReturnTransition = null
                window.sharedElementExitTransition = null
                window.sharedElementReenterTransition = null
            }
            override fun onTransitionCancel(transition: Transition) {
                ViewCompat.setTransitionName(logo, null)
                window.sharedElementReturnTransition = null
                window.sharedElementExitTransition = null
            }
            override fun onTransitionPause(transition: Transition) {}
            override fun onTransitionResume(transition: Transition) {}
        })
    }

    private fun initializeViews() {
        linear1 = findViewById(R.id.linear1)
        linear2 = findViewById(R.id.linear2)
        logo = findViewById(R.id.logo)
        textview1 = findViewById(R.id.textview1)
        tvHomeSubtitle = findViewById(R.id.tv_home_subtitle)
        btnHomeHistory = findViewById(R.id.btn_home_history)
        btnHomeSettings = findViewById(R.id.btn_home_settings)

        cardHomeEncrypt = findViewById(R.id.card_home_encrypt)
        cardHomeDecrypt = findViewById(R.id.card_home_decrypt)
        cardHomeCustom = findViewById(R.id.card_home_custom)
        tvEncryptCardTitle = findViewById(R.id.tv_encrypt_card_title)
        tvDecryptCardTitle = findViewById(R.id.tv_decrypt_card_title)
        tvCustomCardTitle = findViewById(R.id.tv_custom_card_title)
        tvHomeSecuritySpec = findViewById(R.id.tv_home_security_spec)
    }

    private fun initializeLogic() {
        ViewCompat.setTransitionName(logo, "app_logo")
        setupSystemBars()
        applyWindowInsets()
        applyFonts()
        playDashboardAnimations()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitConfirmationDialog()
            }
        })
    }

    private fun playDashboardAnimations() {
        AppAnimationUtil.bounceInDown(linear2, delayMs = 40, startYOffsetDp = 40f)
        AppAnimationUtil.bounceInDown(btnHomeHistory, delayMs = 90, startYOffsetDp = 30f)
        AppAnimationUtil.bounceInDown(btnHomeSettings, delayMs = 130, startYOffsetDp = 30f)

        // All three cards come up together from the bottom, joining in with sweet cascading spring bounce
        AppAnimationUtil.bounceInUp(cardHomeEncrypt, delayMs = 120, startYOffsetDp = 180f)
        AppAnimationUtil.bounceInUp(cardHomeDecrypt, delayMs = 180, startYOffsetDp = 200f)
        AppAnimationUtil.bounceInUp(cardHomeCustom, delayMs = 240, startYOffsetDp = 220f)

        AppAnimationUtil.bounceInUp(tvHomeSecuritySpec, delayMs = 320, startYOffsetDp = 50f)
    }

    private fun setupListeners() {
        applyTouchBounce(cardHomeEncrypt) {
            val intent = Intent(this, EncryptActivity::class.java).apply {
                putExtra("EXTRA_START_MODE", "SYSTEM")
            }
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }

        applyTouchBounce(cardHomeDecrypt) {
            val intent = Intent(this, DecryptActivity::class.java).apply {
                putExtra("EXTRA_START_MODE", "SYSTEM")
            }
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }

        applyTouchBounce(cardHomeCustom) {
            val intent = Intent(this, CustomActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }

        applyTouchBounce(btnHomeHistory) {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }

        applyTouchBounce(btnHomeSettings) {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
    }

    private fun applyTouchBounce(view: View, onClick: () -> Unit) {
        view.setOnClickListener {
            view.animate().scaleX(0.94f).scaleY(0.94f).setDuration(80).withEndAction {
                view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120)
                    .setInterpolator(android.view.animation.OvershootInterpolator(2.0f))
                    .withEndAction { onClick() }
                    .start()
            }.start()
        }
    }

    override fun onResume() {
        super.onResume()
        if (AppSecurityManager.shouldEnforceLock(this)) {
            AppSecurityManager.launchLockScreen(this)
            return
        }
        checkFirstTimePasscodePrompt()
    }

    private fun checkFirstTimePasscodePrompt() {
        if (!AppSecurityManager.hasUserDecidedFirstTime(this) && !AppSecurityManager.isPasscodeSet(this)) {
            showFirstTimePasscodePrompt()
        }
    }

    private fun showFirstTimePasscodePrompt() {
        val dialog = Dialog(this, R.style.AppTheme_TransparentDialog)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogView = layoutInflater.inflate(R.layout.dialog_passcode_prompt, null)
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)

        val btnSetup = dialogView.findViewById<MaterialButton>(R.id.btn_prompt_setup)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_prompt_cancel)

        btnSetup.setOnClickListener {
            dialog.dismiss()
            AppSecurityManager.setUserDecidedFirstTime(this, true)
            PasscodeSetupBottomSheet.show(this, onSuccess = {
                CustomToast.showSuccess(this, "Security Setup", "Security setup complete")
            })
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
            AppSecurityManager.setUserDecidedFirstTime(this, true)
        }

        dialog.setOnCancelListener {
            AppSecurityManager.setUserDecidedFirstTime(this, true)
        }

        dialog.show()
    }

    private fun showExitConfirmationDialog() {
        val dialog = Dialog(this, R.style.AppTheme_TransparentDialog)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_exit)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val width = (resources.displayMetrics.widthPixels * 0.88).toInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)

        val btnCancel = dialog.findViewById<MaterialButton>(R.id.btn_exit_cancel)
        val btnExit = dialog.findViewById<MaterialButton>(R.id.btn_exit_confirm)

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnExit.setOnClickListener {
            dialog.dismiss()
            ViewCompat.setTransitionName(logo, null)
            window.sharedElementReturnTransition = null
            window.sharedElementExitTransition = null
            finishAffinity()
        }

        dialog.show()
    }

    override fun finish() {
        ViewCompat.setTransitionName(logo, null)
        window.sharedElementReturnTransition = null
        window.sharedElementExitTransition = null
        super.finish()
        overridePendingTransition(0, 0)
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
            textview1.typeface = Typeface.createFromAsset(assets, "fonts/outfit_bold.ttf")
            tvHomeSubtitle.typeface = Typeface.createFromAsset(assets, "fonts/outfit_nomal.ttf")
            tvEncryptCardTitle.typeface = Typeface.createFromAsset(assets, "fonts/outfit_semibold.ttf")
            tvDecryptCardTitle.typeface = Typeface.createFromAsset(assets, "fonts/outfit_semibold.ttf")
            tvCustomCardTitle.typeface = Typeface.createFromAsset(assets, "fonts/outfit_semibold.ttf")
            tvHomeSecuritySpec.typeface = Typeface.createFromAsset(assets, "fonts/outfit_nomal.ttf")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
