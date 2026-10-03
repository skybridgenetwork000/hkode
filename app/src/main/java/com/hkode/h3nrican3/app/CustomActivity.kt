package com.hkode.h3nrican3.app

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

class CustomActivity : AppCompatActivity() {

    private lateinit var layoutRoot: LinearLayout
    private lateinit var headerCustom: LinearLayout
    private lateinit var btnBack: FrameLayout
    private lateinit var tvScreenTitle: TextView

    // Segmented selector [ Encrypt ] [ Decrypt ]
    private lateinit var tabEncrypt: TextView
    private lateinit var tabDecrypt: TextView

    // Permanent Password Field & Vault
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnSavedPasswords: MaterialButton
    private lateinit var btnSaveCurrentPassword: MaterialButton

    // Encrypt view bindings
    private lateinit var viewEncrypt: LinearLayout
    private lateinit var cardMessageInput: LinearLayout
    private lateinit var etMessage: EditText
    private lateinit var tvCharCount: TextView
    private lateinit var btnPasteMsg: MaterialButton
    private lateinit var btnClearMsg: MaterialButton
    private lateinit var btnEncryptAction: MaterialButton
    private lateinit var progressEncrypt: ProgressBar
    private lateinit var cardEncryptError: MaterialCardView
    private lateinit var tvEncryptError: TextView
    private lateinit var cardEncryptResult: MaterialCardView
    private lateinit var tvCiphertextResult: TextView
    private lateinit var btnCopyCiphertext: MaterialButton
    private lateinit var btnShareCiphertext: MaterialButton
    private lateinit var btnNewEncrypt: MaterialButton

    // Decrypt view bindings
    private lateinit var viewDecrypt: LinearLayout
    private lateinit var cardCiphertextInput: LinearLayout
    private lateinit var etCiphertext: EditText
    private lateinit var tvDecCharCount: TextView
    private lateinit var btnPasteCiphertext: MaterialButton
    private lateinit var btnClearCiphertext: MaterialButton
    private lateinit var btnDecryptAction: MaterialButton
    private lateinit var progressDecrypt: ProgressBar
    private lateinit var cardDecryptError: MaterialCardView
    private lateinit var tvDecryptError: TextView
    private lateinit var cardDecryptResult: MaterialCardView
    private lateinit var tvPlaintextResult: TextView
    private lateinit var btnCopyPlaintext: MaterialButton
    private lateinit var btnSharePlaintext: MaterialButton
    private lateinit var btnNewDecrypt: MaterialButton

    private var isEncryptSelected = true
    private lateinit var historyManager: HistoryManager
    private val clipboardManager by lazy { getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom)

        historyManager = HistoryManager(this)

        initializeViews()
        setupSystemBars()
        applyWindowInsets()
        applyFonts()
        setupListeners()
        playEntranceAnimations()

        selectTab(encrypt = true, animate = false)

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
        layoutRoot = findViewById(R.id.layout_custom_root)
        headerCustom = findViewById(R.id.header_custom)
        btnBack = findViewById(R.id.btn_back_custom)
        tvScreenTitle = findViewById(R.id.tv_custom_screen_title)

        tabEncrypt = findViewById(R.id.tab_custom_encrypt)
        tabDecrypt = findViewById(R.id.tab_custom_decrypt)
        etPassword = findViewById(R.id.et_custom_password)
        btnSavedPasswords = findViewById(R.id.btn_saved_passwords_custom)
        btnSaveCurrentPassword = findViewById(R.id.btn_save_current_password_custom)

        // Encrypt view
        viewEncrypt = findViewById(R.id.view_custom_encrypt)
        cardMessageInput = findViewById(R.id.card_custom_message_input)
        etMessage = findViewById(R.id.et_custom_message)
        tvCharCount = findViewById(R.id.tv_custom_char_count)
        btnPasteMsg = findViewById(R.id.btn_custom_paste_msg)
        btnClearMsg = findViewById(R.id.btn_custom_clear_msg)
        btnEncryptAction = findViewById(R.id.btn_custom_encrypt_action)
        progressEncrypt = findViewById(R.id.progress_custom_encrypt)
        cardEncryptError = findViewById(R.id.card_custom_encrypt_error)
        tvEncryptError = findViewById(R.id.tv_custom_encrypt_error)
        cardEncryptResult = findViewById(R.id.card_custom_encrypt_result)
        tvCiphertextResult = findViewById(R.id.tv_custom_ciphertext_result)
        btnCopyCiphertext = findViewById(R.id.btn_custom_copy_ciphertext)
        btnShareCiphertext = findViewById(R.id.btn_custom_share_ciphertext)
        btnNewEncrypt = findViewById(R.id.btn_custom_new_encrypt)

        // Decrypt view
        viewDecrypt = findViewById(R.id.view_custom_decrypt)
        cardCiphertextInput = findViewById(R.id.card_custom_ciphertext_input)
        etCiphertext = findViewById(R.id.et_custom_ciphertext)
        tvDecCharCount = findViewById(R.id.tv_custom_dec_char_count)
        btnPasteCiphertext = findViewById(R.id.btn_custom_paste_ciphertext)
        btnClearCiphertext = findViewById(R.id.btn_custom_clear_ciphertext)
        btnDecryptAction = findViewById(R.id.btn_custom_decrypt_action)
        progressDecrypt = findViewById(R.id.progress_custom_decrypt)
        cardDecryptError = findViewById(R.id.card_custom_decrypt_error)
        tvDecryptError = findViewById(R.id.tv_custom_decrypt_error)
        cardDecryptResult = findViewById(R.id.card_custom_decrypt_result)
        tvPlaintextResult = findViewById(R.id.tv_custom_plaintext_result)
        btnCopyPlaintext = findViewById(R.id.btn_custom_copy_plaintext)
        btnSharePlaintext = findViewById(R.id.btn_custom_share_plaintext)
        btnNewDecrypt = findViewById(R.id.btn_custom_new_decrypt)

        GradientBorderDrawable.attachFocusAnimation(cardMessageInput, etMessage, cornerRadiusDp = 16f, strokeWidthDp = 2f)
        GradientBorderDrawable.attachFocusAnimation(cardCiphertextInput, etCiphertext, cornerRadiusDp = 16f, strokeWidthDp = 2f)

        // Password focus visual feedback without horizontal scaling distortion
        val tilPassword = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.til_custom_password)
        etPassword.setOnFocusChangeListener { _, hasFocus ->
            tilPassword?.boxStrokeWidth = if (hasFocus) (2 * resources.displayMetrics.density).toInt() else (1 * resources.displayMetrics.density).toInt()
        }
    }

    private fun playEntranceAnimations() {
        AppAnimationUtil.bounceInDown(headerCustom, 40)
        findViewById<View>(R.id.segmented_selector_custom)?.let {
            AppAnimationUtil.bounceInUp(it, 100, 40f)
        }
        findViewById<View>(R.id.til_custom_password)?.let {
            AppAnimationUtil.bounceInUp(it, 150, 45f)
        }
        AppAnimationUtil.bounceInUp(cardMessageInput, 200, 50f)
        AppAnimationUtil.bounceInUp(btnEncryptAction, 250, 50f)
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

        tabEncrypt.setOnClickListener {
            if (!isEncryptSelected) selectTab(encrypt = true, animate = true)
        }

        tabDecrypt.setOnClickListener {
            if (isEncryptSelected) selectTab(encrypt = false, animate = true)
        }

        // Encrypt input listeners
        etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val len = s?.length ?: 0
                tvCharCount.text = "$len characters"
                btnClearMsg.visibility = if (len > 0) View.VISIBLE else View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnPasteMsg.setOnClickListener {
            pasteToInput(etMessage)
        }

        btnClearMsg.setOnClickListener {
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
            cardEncryptResult.visibility = View.GONE
            cardEncryptError.visibility = View.GONE
            etMessage.requestFocus()
        }

        // Decrypt input listeners
        etCiphertext.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val len = s?.length ?: 0
                tvDecCharCount.text = "$len characters"
                btnClearCiphertext.visibility = if (len > 0) View.VISIBLE else View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnPasteCiphertext.setOnClickListener {
            pasteToInput(etCiphertext)
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
            val plaintext = tvPlaintextResult.text.toString()
            if (plaintext.isNotBlank()) {
                val clip = ClipData.newPlainText("Decrypted Plaintext", plaintext)
                clipboardManager.setPrimaryClip(clip)

                btnCopyPlaintext.text = "Copied!"
                btnCopyPlaintext.setIconResource(R.drawable.ic_check)
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
            cardDecryptResult.visibility = View.GONE
            cardDecryptError.visibility = View.GONE
            etCiphertext.requestFocus()
        }
    }

    private fun selectTab(encrypt: Boolean, animate: Boolean) {
        isEncryptSelected = encrypt
        val primaryColor = ContextCompat.getColor(this, R.color.colorPrimary)
        val secondaryTextColor = ContextCompat.getColor(this, R.color.text_secondary)

        if (encrypt) {
            tabEncrypt.setBackgroundResource(R.drawable.bg_segmented_selected)
            tabEncrypt.setTextColor(primaryColor)
            tabEncrypt.setTypeface(null, Typeface.BOLD)

            tabDecrypt.setBackgroundResource(0)
            tabDecrypt.setTextColor(secondaryTextColor)
            tabDecrypt.setTypeface(null, Typeface.NORMAL)

            if (animate) {
                viewDecrypt.visibility = View.GONE
                viewEncrypt.visibility = View.VISIBLE
                val anim = AnimationUtils.loadAnimation(this, R.anim.fade_in)
                viewEncrypt.startAnimation(anim)
            } else {
                viewDecrypt.visibility = View.GONE
                viewEncrypt.visibility = View.VISIBLE
            }
        } else {
            tabDecrypt.setBackgroundResource(R.drawable.bg_segmented_selected)
            tabDecrypt.setTextColor(primaryColor)
            tabDecrypt.setTypeface(null, Typeface.BOLD)

            tabEncrypt.setBackgroundResource(0)
            tabEncrypt.setTextColor(secondaryTextColor)
            tabEncrypt.setTypeface(null, Typeface.NORMAL)

            if (animate) {
                viewEncrypt.visibility = View.GONE
                viewDecrypt.visibility = View.VISIBLE
                val anim = AnimationUtils.loadAnimation(this, R.anim.fade_in)
                viewDecrypt.startAnimation(anim)
            } else {
                viewEncrypt.visibility = View.GONE
                viewDecrypt.visibility = View.VISIBLE
            }
        }
    }

    private fun performEncrypt() {
        val password = etPassword.text.toString().trim()
        val message = etMessage.text.toString().trim()

        if (password.isEmpty()) {
            CustomToast.showWarning(this, "Password Required", "Please enter a password for protection")
            etPassword.requestFocus()
            return
        }

        if (message.isEmpty()) {
            CustomToast.showWarning(this, "Empty Input", "Please enter a message to encrypt")
            etMessage.requestFocus()
            return
        }

        setEncryptLoading(true)
        cardEncryptError.visibility = View.GONE
        cardEncryptResult.visibility = View.GONE

        lifecycleScope.launch {
            val result = CryptoApiClient.encrypt(
                message = message,
                mode = CryptoApiClient.Mode.PASSWORD,
                password = password
            )

            setEncryptLoading(false)

            result.fold(
                onSuccess = { ciphertext ->
                    tvCiphertextResult.text = ciphertext
                    cardEncryptResult.visibility = View.VISIBLE
                    cardEncryptError.visibility = View.GONE
                    AppAnimationUtil.popBounceOut(cardEncryptResult)
                    CustomToast.showSuccess(this@CustomActivity, "Encrypted", "Message encrypted successfully")

                    historyManager.addEntry(
                        type = "ENCRYPT",
                        mode = "PASSWORD",
                        content = ciphertext,
                        summary = if (message.length > 80) message.take(80) + "…" else message
                    )
                },
                onFailure = { error ->
                    CustomToast.showError(this@CustomActivity, "Encryption Failed", error.message ?: "Failed to encrypt message")
                }
            )
        }
    }

    private fun performDecrypt() {
        val password = etPassword.text.toString().trim()
        val ciphertext = etCiphertext.text.toString().trim()

        if (password.isEmpty()) {
            CustomToast.showWarning(this, "Password Required", "Please enter the password used to encrypt")
            etPassword.requestFocus()
            return
        }

        if (ciphertext.isEmpty()) {
            CustomToast.showWarning(this, "Empty Input", "Please enter or paste the encrypted message")
            etCiphertext.requestFocus()
            return
        }

        setDecryptLoading(true)
        cardDecryptError.visibility = View.GONE
        cardDecryptResult.visibility = View.GONE

        lifecycleScope.launch {
            val result = CryptoApiClient.decrypt(
                ciphertext = ciphertext,
                mode = CryptoApiClient.Mode.PASSWORD,
                password = password
            )

            setDecryptLoading(false)

            result.fold(
                onSuccess = { plaintext ->
                    tvPlaintextResult.text = plaintext
                    cardDecryptResult.visibility = View.VISIBLE
                    cardDecryptError.visibility = View.GONE
                    AppAnimationUtil.popBounceOut(cardDecryptResult)
                    CustomToast.showSuccess(this@CustomActivity, "Decrypted", "Message decrypted successfully")

                    historyManager.addEntry(
                        type = "DECRYPT",
                        mode = "PASSWORD",
                        content = plaintext,
                        summary = if (plaintext.length > 80) plaintext.take(80) + "…" else plaintext
                    )
                },
                onFailure = { error ->
                    CustomToast.showError(this@CustomActivity, "Decryption Failed", error.message ?: "Failed to decrypt message")
                }
            )
        }
    }

    private fun showEncryptError(msg: String) {
        CustomToast.showError(this, "Encryption Error", msg)
    }

    private fun showDecryptError(msg: String) {
        CustomToast.showError(this, "Decryption Error", msg)
    }

    private fun setEncryptLoading(loading: Boolean) {
        btnEncryptAction.isEnabled = !loading
        btnEncryptAction.text = if (loading) "" else "Encrypt Message"
        progressEncrypt.visibility = if (loading) View.VISIBLE else View.GONE
        cardMessageInput.alpha = if (loading) 0.6f else 1.0f
    }

    private fun setDecryptLoading(loading: Boolean) {
        btnDecryptAction.isEnabled = !loading
        btnDecryptAction.text = if (loading) "" else "Decrypt Message"
        progressDecrypt.visibility = if (loading) View.VISIBLE else View.GONE
        cardCiphertextInput.alpha = if (loading) 0.6f else 1.0f
    }

    private fun pasteToInput(target: EditText) {
        val clip = clipboardManager.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).coerceToText(this).toString()
            if (text.isNotBlank()) {
                target.setText(text)
                target.setSelection(target.text.length)
                CustomToast.showSuccess(this, "Pasted", "Pasted from clipboard")
                return
            }
        }
        CustomToast.showInfo(this, "Clipboard Empty", "Clipboard is empty")
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
        val originalTop = headerCustom.paddingTop

        ViewCompat.setOnApplyWindowInsetsListener(layoutRoot) { _, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            headerCustom.updatePadding(
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
            btnDecryptAction.typeface = Typeface.createFromAsset(assets, "fonts/outfit_semibold.ttf")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
