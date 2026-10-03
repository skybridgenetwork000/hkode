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

class DecryptActivity : AppCompatActivity() {

    private lateinit var layoutRoot: LinearLayout
    private lateinit var headerDecrypt: LinearLayout
    private lateinit var btnBack: FrameLayout
    private lateinit var tvScreenTitle: TextView

    // Method selector
    private lateinit var tabSystem: TextView
    private lateinit var tabPassword: TextView
    private lateinit var layoutPasswordField: LinearLayout
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnSavedPasswords: MaterialButton
    private lateinit var btnSaveCurrentPassword: MaterialButton

    // Ciphertext input
    private lateinit var cardCiphertextInput: LinearLayout
    private lateinit var etCiphertext: EditText
    private lateinit var tvCharCount: TextView
    private lateinit var btnPasteCiphertext: MaterialButton
    private lateinit var btnClearCiphertext: MaterialButton

    // Primary action & progress
    private lateinit var btnDecryptAction: MaterialButton
    private lateinit var progressDecrypt: ProgressBar

    // Error & Result
    private lateinit var cardDecryptError: MaterialCardView
    private lateinit var tvDecryptError: TextView
    private lateinit var cardDecryptResult: MaterialCardView
    private lateinit var tvPlaintextResult: TextView
    private lateinit var btnCopyPlaintext: MaterialButton
    private lateinit var btnSharePlaintext: MaterialButton
    private lateinit var btnNewDecrypt: MaterialButton

    private var activeMode = CryptoApiClient.Mode.SYSTEM
    private lateinit var historyManager: HistoryManager
    private val clipboardManager by lazy { getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_decrypt)

        historyManager = HistoryManager(this)

        initializeViews()
        setupSystemBars()
        applyWindowInsets()
        applyFonts()
        setupListeners()
        playEntranceAnimations()

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

        handleIncomingIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (AppSecurityManager.shouldEnforceLock(this)) {
            AppSecurityManager.launchLockScreen(this)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(inIntent: Intent) {
        val rawDataToParse = inIntent.getStringExtra("EXTRA_CIPHERTEXT")

        if (!rawDataToParse.isNullOrBlank()) {
            val startMode = inIntent.getStringExtra("EXTRA_START_MODE") ?: "SYSTEM"
            val finalMode = if (startMode == "PASSWORD") CryptoApiClient.Mode.PASSWORD else CryptoApiClient.Mode.SYSTEM

            selectMode(finalMode, animate = false)
            etCiphertext.setText(rawDataToParse)
            etCiphertext.setSelection(rawDataToParse.length)

            if (finalMode == CryptoApiClient.Mode.SYSTEM) {
                mainHandler.postDelayed({
                    performDecrypt()
                }, 200)
            } else {
                etPassword.requestFocus()
            }
            return
        }

        val startMode = inIntent.getStringExtra("EXTRA_START_MODE") ?: "SYSTEM"
        selectMode(if (startMode == "PASSWORD") CryptoApiClient.Mode.PASSWORD else CryptoApiClient.Mode.SYSTEM, animate = false)
    }

    private fun initializeViews() {
        layoutRoot = findViewById(R.id.layout_decrypt_root)
        headerDecrypt = findViewById(R.id.header_decrypt)
        btnBack = findViewById(R.id.btn_back_decrypt)
        tvScreenTitle = findViewById(R.id.tv_decrypt_screen_title)

        tabSystem = findViewById(R.id.tab_dec_mode_system)
        tabPassword = findViewById(R.id.tab_dec_mode_password)
        layoutPasswordField = findViewById(R.id.layout_dec_password_field)
        etPassword = findViewById(R.id.et_dec_password)
        btnSavedPasswords = findViewById(R.id.btn_saved_passwords_decrypt)
        btnSaveCurrentPassword = findViewById(R.id.btn_save_current_password_decrypt)

        cardCiphertextInput = findViewById(R.id.card_ciphertext_input)
        etCiphertext = findViewById(R.id.et_ciphertext)
        tvCharCount = findViewById(R.id.tv_dec_char_count)
        btnPasteCiphertext = findViewById(R.id.btn_paste_ciphertext)
        btnClearCiphertext = findViewById(R.id.btn_clear_ciphertext)

        btnDecryptAction = findViewById(R.id.btn_decrypt_action)
        progressDecrypt = findViewById(R.id.progress_decrypt)

        cardDecryptError = findViewById(R.id.card_decrypt_error)
        tvDecryptError = findViewById(R.id.tv_decrypt_error)
        cardDecryptResult = findViewById(R.id.card_decrypt_result)
        tvPlaintextResult = findViewById(R.id.tv_plaintext_result)
        btnCopyPlaintext = findViewById(R.id.btn_copy_plaintext)
        btnSharePlaintext = findViewById(R.id.btn_share_plaintext)
        btnNewDecrypt = findViewById(R.id.btn_new_decrypt)

        GradientBorderDrawable.attachFocusAnimation(cardCiphertextInput, etCiphertext, cornerRadiusDp = 16f, strokeWidthDp = 2f)

        // Password focus visual feedback without horizontal scaling distortion
        val tilPassword = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.til_dec_password)
        etPassword.setOnFocusChangeListener { _, hasFocus ->
            tilPassword?.boxStrokeWidth = if (hasFocus) (2 * resources.displayMetrics.density).toInt() else (1 * resources.displayMetrics.density).toInt()
        }
    }

    private fun playEntranceAnimations() {
        AppAnimationUtil.bounceInDown(headerDecrypt, 40)
        findViewById<View>(R.id.segmented_selector_decrypt)?.let {
            AppAnimationUtil.bounceInUp(it, 100, 40f)
        }
        AppAnimationUtil.bounceInUp(cardCiphertextInput, 160, 60f)
        AppAnimationUtil.bounceInUp(btnDecryptAction, 220, 50f)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { navigateBack() }

        btnSavedPasswords.setOnClickListener {
            PasswordVaultHelper.showPasswordVaultBottomSheet(this) { selectedPassword ->
                etPassword.setText(selectedPassword)
                etPassword.setSelection(selectedPassword.length)
                // If ciphertext is present, decrypt immediately with selected password!
                if (etCiphertext.text.toString().trim().isNotEmpty()) {
                    performDecrypt()
                }
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

        etCiphertext.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val length = s?.length ?: 0
                tvCharCount.text = "$length characters"
                btnClearCiphertext.visibility = if (length > 0) View.VISIBLE else View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnPasteCiphertext.setOnClickListener {
            val clip = clipboardManager.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).coerceToText(this).toString()
                if (text.isNotBlank()) {
                    etCiphertext.setText(text)
                    etCiphertext.setSelection(etCiphertext.text.length)
                    CustomToast.showSuccess(this, "Pasted", "Encrypted message pasted from clipboard")
                    return@setOnClickListener
                }
            }
            CustomToast.showInfo(this, "Clipboard Empty", "No text available in clipboard")
        }

        btnClearCiphertext.setOnClickListener {
            etCiphertext.setText("")
            cardDecryptError.visibility = View.GONE
            cardDecryptResult.visibility = View.GONE
        }

        btnDecryptAction.setOnClickListener {
            performDecrypt()
        }

        btnCopyPlaintext.setOnClickListener {
            val text = tvPlaintextResult.text.toString()
            if (text.isNotBlank()) {
                val clip = ClipData.newPlainText("Decrypted Plaintext", text)
                clipboardManager.setPrimaryClip(clip)

                btnCopyPlaintext.text = "Copied"
                btnCopyPlaintext.setIconResource(R.drawable.ic_check)
                CustomToast.showSuccess(this, "Copied", "Plaintext message copied to clipboard")
                mainHandler.postDelayed({
                    btnCopyPlaintext.text = "Copy"
                    btnCopyPlaintext.setIconResource(R.drawable.ic_copy)
                }, 1600)
            }
        }

        btnSharePlaintext.setOnClickListener {
            val text = tvPlaintextResult.text.toString()
            if (text.isNotBlank()) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Decrypted Message")
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                startActivity(Intent.createChooser(intent, "Share via"))
            }
        }

        btnNewDecrypt.setOnClickListener {
            etCiphertext.setText("")
            etPassword.setText("")
            cardDecryptResult.visibility = View.GONE
            cardDecryptError.visibility = View.GONE
            etCiphertext.requestFocus()
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

    private fun performDecrypt() {
        val ciphertext = etCiphertext.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (ciphertext.isEmpty()) {
            CustomToast.showWarning(this, "Empty Input", "Please enter or paste the encrypted message")
            etCiphertext.requestFocus()
            return
        }

        if (activeMode == CryptoApiClient.Mode.PASSWORD && password.isEmpty()) {
            CustomToast.showWarning(this, "Password Required", "Please enter the password used to encrypt")
            etPassword.requestFocus()
            return
        }

        setLoading(true)
        cardDecryptError.visibility = View.GONE
        cardDecryptResult.visibility = View.GONE

        lifecycleScope.launch {
            val result = CryptoApiClient.decrypt(
                ciphertext = ciphertext,
                mode = activeMode,
                password = if (activeMode == CryptoApiClient.Mode.PASSWORD) password else null
            )

            setLoading(false)

            result.fold(
                onSuccess = { plaintext ->
                    tvPlaintextResult.text = plaintext
                    cardDecryptResult.visibility = View.VISIBLE
                    cardDecryptError.visibility = View.GONE
                    AppAnimationUtil.popBounceOut(cardDecryptResult)
                    CustomToast.showSuccess(this@DecryptActivity, "Decrypted", "Message decrypted successfully")

                    historyManager.addEntry(
                        type = "DECRYPT",
                        mode = activeMode.name,
                        content = plaintext,
                        summary = if (plaintext.length > 80) plaintext.take(80) + "…" else plaintext
                    )
                },
                onFailure = { error ->
                    CustomToast.showError(this@DecryptActivity, "Decryption Failed", error.message ?: "Failed to decrypt message")
                }
            )
        }
    }

    private fun showError(msg: String) {
        CustomToast.showError(this, "Decryption Error", msg)
    }

    private fun setLoading(loading: Boolean) {
        btnDecryptAction.isEnabled = !loading
        btnDecryptAction.text = if (loading) "" else "Decrypt Message"
        progressDecrypt.visibility = if (loading) View.VISIBLE else View.GONE
        cardCiphertextInput.alpha = if (loading) 0.6f else 1.0f
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
        val originalTop = headerDecrypt.paddingTop

        ViewCompat.setOnApplyWindowInsetsListener(layoutRoot) { _, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            headerDecrypt.updatePadding(
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
            btnDecryptAction.typeface = Typeface.createFromAsset(assets, "fonts/outfit_semibold.ttf")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
