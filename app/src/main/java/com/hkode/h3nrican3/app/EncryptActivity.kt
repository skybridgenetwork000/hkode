package com.hkode.h3nrican3.app

import android.animation.LayoutTransition
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class EncryptActivity : AppCompatActivity() {

    private lateinit var layoutRoot: LinearLayout
    private lateinit var headerEncrypt: LinearLayout
    private lateinit var btnBack: FrameLayout
    private lateinit var tvScreenTitle: TextView

    // Password controls
    private lateinit var tabSystem: TextView
    private lateinit var tabPassword: TextView
    private lateinit var layoutPasswordField: LinearLayout
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnSavedPasswords: MaterialButton
    private lateinit var btnSaveCurrentPassword: MaterialButton

    // Message input
    private lateinit var cardMessageInput: LinearLayout
    private lateinit var etMessage: EditText
    private lateinit var tvCharCount: TextView
    private lateinit var btnPasteMessage: MaterialButton
    private lateinit var btnClearMessage: MaterialButton

    // Primary action & progress
    private lateinit var btnEncryptAction: MaterialButton
    private lateinit var progressEncrypt: ProgressBar

    // Error & Result
    private lateinit var cardEncryptError: MaterialCardView
    private lateinit var tvEncryptError: TextView
    private lateinit var cardEncryptResult: MaterialCardView
    private lateinit var tvEncryptBadge: TextView
    private lateinit var tvCiphertextResult: TextView
    private lateinit var btnCopyCiphertext: MaterialButton
    private lateinit var btnShareCiphertext: MaterialButton
    private lateinit var btnNewEncrypt: MaterialButton

    private var activeMode = CryptoApiClient.Mode.SYSTEM
    private lateinit var historyManager: HistoryManager
    private val clipboardManager by lazy { getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_encrypt)

        historyManager = HistoryManager(this)

        initializeViews()
        setupSystemBars()
        applyWindowInsets()
        applyFonts()
        setupListeners()
        playEntranceAnimations()

        val startMode = intent.getStringExtra("EXTRA_START_MODE") ?: "SYSTEM"
        if (startMode == "PASSWORD") {
            selectMode(CryptoApiClient.Mode.PASSWORD, animate = false)
        } else {
            selectMode(CryptoApiClient.Mode.SYSTEM, animate = false)
        }

        val parent = layoutPasswordField.parent as? ViewGroup
        parent?.layoutTransition = LayoutTransition().apply {
            enableTransitionType(LayoutTransition.CHANGING)
            setDuration(180)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateBack()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (AppSecurityManager.shouldEnforceLock(this)) {
            AppSecurityManager.launchLockScreen(this)
        }
    }

    private fun initializeViews() {
        layoutRoot = findViewById(R.id.layout_encrypt_root)
        headerEncrypt = findViewById(R.id.header_encrypt)
        btnBack = findViewById(R.id.btn_back_encrypt)
        tvScreenTitle = findViewById(R.id.tv_encrypt_screen_title)

        tabSystem = findViewById(R.id.tab_mode_system)
        tabPassword = findViewById(R.id.tab_mode_password)
        layoutPasswordField = findViewById(R.id.layout_password_field)
        etPassword = findViewById(R.id.et_password)
        btnSavedPasswords = findViewById(R.id.btn_saved_passwords_encrypt)
        btnSaveCurrentPassword = findViewById(R.id.btn_save_current_password_encrypt)

        cardMessageInput = findViewById(R.id.card_message_input)
        etMessage = findViewById(R.id.et_message)
        tvCharCount = findViewById(R.id.tv_char_count)
        btnPasteMessage = findViewById(R.id.btn_paste_message)
        btnClearMessage = findViewById(R.id.btn_clear_message)

        btnEncryptAction = findViewById(R.id.btn_encrypt_action)
        progressEncrypt = findViewById(R.id.progress_encrypt)

        cardEncryptError = findViewById(R.id.card_encrypt_error)
        tvEncryptError = findViewById(R.id.tv_encrypt_error)
        cardEncryptResult = findViewById(R.id.card_encrypt_result)
        tvEncryptBadge = findViewById(R.id.tv_encrypt_badge)
        tvCiphertextResult = findViewById(R.id.tv_ciphertext_result)
        btnCopyCiphertext = findViewById(R.id.btn_copy_ciphertext)
        btnShareCiphertext = findViewById(R.id.btn_share_ciphertext)
        btnNewEncrypt = findViewById(R.id.btn_new_encrypt)

        GradientBorderDrawable.attachFocusAnimation(cardMessageInput, etMessage, cornerRadiusDp = 16f, strokeWidthDp = 2f)

        // Password focus visual feedback without horizontal scaling distortion
        val tilPassword = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.til_password)
        etPassword.setOnFocusChangeListener { _, hasFocus ->
            tilPassword?.boxStrokeWidth = if (hasFocus) (2 * resources.displayMetrics.density).toInt() else (1 * resources.displayMetrics.density).toInt()
        }
    }

    private fun playEntranceAnimations() {
        AppAnimationUtil.bounceInDown(headerEncrypt, 40)
        findViewById<View>(R.id.segmented_selector)?.let {
            AppAnimationUtil.bounceInUp(it, 100, 40f)
        }
        AppAnimationUtil.bounceInUp(cardMessageInput, 160, 60f)
        AppAnimationUtil.bounceInUp(btnEncryptAction, 220, 50f)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { navigateBack() }

        btnSavedPasswords.setOnClickListener {
            PasswordVaultHelper.showPasswordVaultBottomSheet(this) { selectedPassword ->
                etPassword.setText(selectedPassword)
                etPassword.setSelection(selectedPassword.length)
            }
        }

        btnSaveCurrentPassword.setOnClickListener {
            val currentPwd = etPassword.text.toString().trim()
            if (currentPwd.isEmpty()) {
                CustomToast.showWarning(this, "Empty Password", "Please enter a password first")
                etPassword.requestFocus()
            } else {
                PasswordVaultHelper.showSavePasswordDialog(this, currentPwd)
            }
        }

        tabSystem.setOnClickListener {
            selectMode(CryptoApiClient.Mode.SYSTEM, animate = true)
        }

        tabPassword.setOnClickListener {
            selectMode(CryptoApiClient.Mode.PASSWORD, animate = true)
        }

        etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val length = s?.length ?: 0
                tvCharCount.text = "$length characters"
                btnClearMessage.visibility = if (length > 0) View.VISIBLE else View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnPasteMessage.setOnClickListener {
            val clip = clipboardManager.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).coerceToText(this).toString()
                if (text.isNotBlank()) {
                    etMessage.setText(text)
                    etMessage.setSelection(etMessage.text.length)
                    CustomToast.showSuccess(this, "Pasted", "Message pasted from clipboard")
                    return@setOnClickListener
                }
            }
            CustomToast.showInfo(this, "Clipboard Empty", "No text available in clipboard")
        }

        btnClearMessage.setOnClickListener {
            etMessage.setText("")
            cardEncryptError.visibility = View.GONE
            cardEncryptResult.visibility = View.GONE
        }

        btnEncryptAction.setOnClickListener {
            performEncrypt()
        }

        // Copy Raw Encrypted Code
        btnCopyCiphertext.setOnClickListener {
            val ciphertext = tvCiphertextResult.text.toString()
            if (ciphertext.isNotBlank()) {
                val clip = ClipData.newPlainText("Encrypted Message", ciphertext)
                clipboardManager.setPrimaryClip(clip)

                btnCopyCiphertext.text = "Copied!"
                btnCopyCiphertext.setIconResource(R.drawable.ic_check)
                CustomToast.showSuccess(this, "Copied", "Encrypted message copied to clipboard")
                mainHandler.postDelayed({
                    btnCopyCiphertext.text = "Copy"
                    btnCopyCiphertext.setIconResource(R.drawable.ic_copy)
                }, 1600)
            }
        }

        // Share Raw Encrypted Code
        btnShareCiphertext.setOnClickListener {
            val ciphertext = tvCiphertextResult.text.toString()
            if (ciphertext.isNotBlank()) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Encrypted Message")
                    putExtra(Intent.EXTRA_TEXT, ciphertext)
                }
                startActivity(Intent.createChooser(intent, "Share Encrypted Message"))
            }
        }

        btnNewEncrypt.setOnClickListener {
            etMessage.setText("")
            etPassword.setText("")
            cardEncryptResult.visibility = View.GONE
            cardEncryptError.visibility = View.GONE
            etMessage.requestFocus()
        }
    }

    private fun selectMode(mode: CryptoApiClient.Mode, animate: Boolean) {
        activeMode = mode
        val primaryColor = ContextCompat.getColor(this, R.color.colorPrimary)
        val secondaryTextColor = ContextCompat.getColor(this, R.color.text_secondary)

        if (mode == CryptoApiClient.Mode.SYSTEM) {
            tabSystem.setBackgroundResource(R.drawable.bg_segmented_selected)
            tabSystem.setTextColor(primaryColor)
            tabSystem.setTypeface(null, Typeface.BOLD)

            tabPassword.setBackgroundResource(0)
            tabPassword.setTextColor(secondaryTextColor)
            tabPassword.setTypeface(null, Typeface.NORMAL)

            layoutPasswordField.visibility = View.GONE
        } else {
            tabPassword.setBackgroundResource(R.drawable.bg_segmented_selected)
            tabPassword.setTextColor(primaryColor)
            tabPassword.setTypeface(null, Typeface.BOLD)

            tabSystem.setBackgroundResource(0)
            tabSystem.setTextColor(secondaryTextColor)
            tabSystem.setTypeface(null, Typeface.NORMAL)

            layoutPasswordField.visibility = View.VISIBLE
            etPassword.requestFocus()
        }
    }

    private fun performEncrypt() {
        val message = etMessage.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (message.isEmpty()) {
            CustomToast.showWarning(this, "Empty Input", "Please enter a message to encrypt")
            etMessage.requestFocus()
            return
        }

        if (activeMode == CryptoApiClient.Mode.PASSWORD && password.isEmpty()) {
            CustomToast.showWarning(this, "Password Required", "Please enter a password for protection")
            etPassword.requestFocus()
            return
        }

        setLoading(true)
        cardEncryptError.visibility = View.GONE
        cardEncryptResult.visibility = View.GONE

        lifecycleScope.launch {
            val result = CryptoApiClient.encrypt(
                message = message,
                mode = activeMode,
                password = if (activeMode == CryptoApiClient.Mode.PASSWORD) password else null
            )

            setLoading(false)

            result.fold(
                onSuccess = { ciphertext ->
                    tvCiphertextResult.text = ciphertext
                    tvEncryptBadge.text = if (activeMode == CryptoApiClient.Mode.PASSWORD) "Password" else "System"
                    tvEncryptBadge.setBackgroundResource(if (activeMode == CryptoApiClient.Mode.PASSWORD) R.drawable.bg_badge_green else R.drawable.bg_badge_blue)
                    tvEncryptBadge.setTextColor(ContextCompat.getColor(this@EncryptActivity, if (activeMode == CryptoApiClient.Mode.PASSWORD) R.color.badge_pwd_text else R.color.badge_system_text))

                    cardEncryptResult.visibility = View.VISIBLE
                    cardEncryptError.visibility = View.GONE
                    AppAnimationUtil.popBounceOut(cardEncryptResult)
                    CustomToast.showSuccess(this@EncryptActivity, "Encrypted", "Message encrypted successfully")

                    historyManager.addEntry(
                        type = "ENCRYPT",
                        mode = activeMode.name,
                        content = ciphertext,
                        summary = if (message.length > 80) message.take(80) + "…" else message
                    )
                },
                onFailure = { error ->
                    CustomToast.showError(this@EncryptActivity, "Encryption Failed", error.message ?: "Failed to encrypt message")
                }
            )
        }
    }

    private fun showError(msg: String) {
        CustomToast.showError(this, "Encryption Error", msg)
    }

    private fun setLoading(loading: Boolean) {
        btnEncryptAction.isEnabled = !loading
        btnEncryptAction.text = if (loading) "" else "Encrypt Message"
        progressEncrypt.visibility = if (loading) View.VISIBLE else View.GONE
        cardMessageInput.alpha = if (loading) 0.6f else 1.0f
    }

    private fun navigateBack() {
        finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
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
        val originalTop = headerEncrypt.paddingTop

        ViewCompat.setOnApplyWindowInsetsListener(layoutRoot) { _, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            headerEncrypt.updatePadding(
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
            btnEncryptAction.typeface = Typeface.createFromAsset(assets, "fonts/outfit_semibold.ttf")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
