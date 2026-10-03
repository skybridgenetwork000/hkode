package com.hkode.h3nrican3.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
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
import androidx.core.widget.NestedScrollView
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private lateinit var layoutRoot: LinearLayout
    private lateinit var headerHistory: LinearLayout
    private lateinit var btnBack: FrameLayout
    private lateinit var tvScreenTitle: TextView
    private lateinit var btnClearHistory: MaterialButton

    private lateinit var layoutEmptyHistory: LinearLayout
    private lateinit var scrollHistory: NestedScrollView
    private lateinit var containerHistoryItems: LinearLayout

    private lateinit var historyManager: HistoryManager
    private val clipboardManager by lazy { getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        historyManager = HistoryManager(this)

        initializeViews()
        setupSystemBars()
        applyWindowInsets()
        applyFonts()
        setupListeners()
        playEntranceAnimations()
        loadHistoryList()

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
        layoutRoot = findViewById(R.id.layout_history_root)
        headerHistory = findViewById(R.id.header_history)
        btnBack = findViewById(R.id.btn_back_history)
        tvScreenTitle = findViewById(R.id.tv_history_screen_title)
        btnClearHistory = findViewById(R.id.btn_clear_history)

        layoutEmptyHistory = findViewById(R.id.layout_empty_history)
        scrollHistory = findViewById(R.id.scroll_history)
        containerHistoryItems = findViewById(R.id.container_history_items)
    }

    private fun playEntranceAnimations() {
        AppAnimationUtil.bounceInDown(headerHistory, 40)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { navigateBack() }

        btnClearHistory.setOnClickListener {
            historyManager.clearAll()
            loadHistoryList()
            CustomToast.showInfo(this, "History Cleared", "All history entries removed")
        }
    }

    private fun loadHistoryList() {
        val items = historyManager.getHistory()
        containerHistoryItems.removeAllViews()

        if (items.isEmpty()) {
            layoutEmptyHistory.visibility = View.VISIBLE
            scrollHistory.visibility = View.GONE
            btnClearHistory.visibility = View.GONE
        } else {
            layoutEmptyHistory.visibility = View.GONE
            scrollHistory.visibility = View.VISIBLE
            btnClearHistory.visibility = View.VISIBLE

            val inflater = LayoutInflater.from(this)
            items.forEachIndexed { index, item ->
                val itemView = inflater.inflate(R.layout.item_history, containerHistoryItems, false)

                val tvBadge = itemView.findViewById<TextView>(R.id.tv_history_badge)
                val tvTime = itemView.findViewById<TextView>(R.id.tv_history_time)
                val tvSummary = itemView.findViewById<TextView>(R.id.tv_history_summary)
                val btnOpen = itemView.findViewById<MaterialButton>(R.id.btn_history_open)
                val btnCopy = itemView.findViewById<MaterialButton>(R.id.btn_history_copy)
                val btnDeleteContainer = itemView.findViewById<FrameLayout>(R.id.btn_history_delete_container)

                val isEncrypt = item.type == "ENCRYPT"
                val modeLabel = if (item.mode.equals("PASSWORD", ignoreCase = true)) "Password" else "System"
                tvBadge.text = if (isEncrypt) "Encrypted • $modeLabel" else "Decrypted • $modeLabel"
                tvBadge.setBackgroundResource(if (isEncrypt) R.drawable.bg_badge_blue else R.drawable.bg_badge_green)
                tvBadge.setTextColor(ContextCompat.getColor(this, if (isEncrypt) R.color.badge_system_text else R.color.badge_pwd_text))

                tvTime.text = formatTimestamp(item.timestamp)
                tvSummary.text = item.summary

                btnOpen.text = if (isEncrypt) "Decrypt" else "Encrypt"
                btnOpen.setOnClickListener {
                    if (isEncrypt) {
                        val intent = Intent(this, DecryptActivity::class.java).apply {
                            putExtra("EXTRA_CIPHERTEXT", item.content)
                            putExtra("EXTRA_START_MODE", item.mode)
                        }
                        startActivity(intent)
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    } else {
                        val intent = Intent(this, EncryptActivity::class.java).apply {
                            putExtra("EXTRA_START_MODE", item.mode)
                        }
                        startActivity(intent)
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    }
                }

                btnCopy.setOnClickListener {
                    val clip = ClipData.newPlainText("Hkode Content", item.content)
                    clipboardManager.setPrimaryClip(clip)
                    CustomToast.showSuccess(this, "Copied", "Copied to clipboard")
                }

                btnDeleteContainer.setOnClickListener {
                    historyManager.deleteEntry(item.id)
                    loadHistoryList()
                }

                containerHistoryItems.addView(itemView)
                AppAnimationUtil.bounceInUp(itemView, delayMs = index * 35L, startYOffsetDp = 30f)
            }
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val now = Calendar.getInstance()
        val time = Calendar.getInstance().apply { timeInMillis = timestamp }

        val isToday = now.get(Calendar.YEAR) == time.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == time.get(Calendar.DAY_OF_YEAR)

        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault()).format(timestamp)

        return if (isToday) {
            "Today · $timeFormat"
        } else {
            val isYesterday = now.get(Calendar.YEAR) == time.get(Calendar.YEAR) &&
                    now.get(Calendar.DAY_OF_YEAR) - time.get(Calendar.DAY_OF_YEAR) == 1
            if (isYesterday) {
                "Yesterday · $timeFormat"
            } else {
                SimpleDateFormat("MMM d · h:mm a", Locale.getDefault()).format(timestamp)
            }
        }
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
        val originalTop = headerHistory.paddingTop

        ViewCompat.setOnApplyWindowInsetsListener(layoutRoot) { _, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            headerHistory.updatePadding(
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
            findViewById<TextView>(R.id.tv_empty_title)?.typeface = Typeface.createFromAsset(assets, "fonts/outfit_semibold.ttf")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
