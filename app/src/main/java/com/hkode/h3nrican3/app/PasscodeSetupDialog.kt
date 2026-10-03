package com.hkode.h3nrican3.app

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsetsController
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * Full-screen dialog for setting up or updating the 4-digit PIN passcode.
 * Eliminates bottom sheet gaps and provides rich elastic feedback.
 */
object PasscodeSetupDialog {

    fun show(
        context: Context,
        isChangingPin: Boolean = false,
        onSuccess: () -> Unit = {},
        onCancel: () -> Unit = {}
    ) {
        val dialog = Dialog(context, R.style.AppTheme_FullScreenDialog)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_passcode_setup_fullscreen, null)
        dialog.setContentView(view)

        dialog.window?.let { win ->
            win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            win.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            val bgColor = ContextCompat.getColor(context, R.color.app_background)
            win.statusBarColor = bgColor
            win.navigationBarColor = bgColor

            val isDarkMode = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                win.insetsController?.let { controller ->
                    val flags = if (!isDarkMode) {
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                    } else {
                        0
                    }
                    controller.setSystemBarsAppearance(flags, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)
                }
            } else {
                @Suppress("DEPRECATION")
                win.decorView.systemUiVisibility = if (!isDarkMode) {
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                } else {
                    0
                }
            }
        }

        val header = view.findViewById<LinearLayout>(R.id.header_setup_dialog)
        val origTopPadding = header.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            header.updatePadding(top = origTopPadding + statusBarInsets.top)
            view.updatePadding(bottom = navBarInsets.bottom)
            insets
        }

        val tvScreenTitle = view.findViewById<TextView>(R.id.tv_setup_screen_title)
        val tvTitle = view.findViewById<TextView>(R.id.tv_setup_title)
        val tvSubtitle = view.findViewById<TextView>(R.id.tv_setup_subtitle)
        val tvError = view.findViewById<TextView>(R.id.tv_setup_error)
        val btnClose = view.findViewById<FrameLayout>(R.id.btn_setup_close)
        val btnCancelStep = view.findViewById<TextView>(R.id.btn_setup_cancel_step)
        val layoutSlots = view.findViewById<LinearLayout>(R.id.layout_setup_pin_slots)

        if (isChangingPin) {
            tvScreenTitle.text = "Change Passcode"
            tvTitle.text = "Enter New Passcode"
        }

        val slots = arrayOf(
            view.findViewById<FrameLayout>(R.id.slot_setup_1),
            view.findViewById<FrameLayout>(R.id.slot_setup_2),
            view.findViewById<FrameLayout>(R.id.slot_setup_3),
            view.findViewById<FrameLayout>(R.id.slot_setup_4)
        )
        val dots = arrayOf(
            view.findViewById<View>(R.id.dot_setup_1),
            view.findViewById<View>(R.id.dot_setup_2),
            view.findViewById<View>(R.id.dot_setup_3),
            view.findViewById<View>(R.id.dot_setup_4)
        )

        var isConfirmMode = false
        var firstPin = ""
        val currentPin = StringBuilder()

        fun updateSlotsUI() {
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

        fun showMismatchError() {
            tvError.visibility = View.VISIBLE
            tvError.text = "Passcodes didn't match. Try again."
            val shake = AnimationUtils.loadAnimation(context, R.anim.shake_pin)
            layoutSlots.startAnimation(shake)

            for (slot in slots) {
                slot.setBackgroundResource(R.drawable.bg_pin_box_error)
            }

            layoutSlots.postDelayed({
                currentPin.clear()
                updateSlotsUI()
            }, 600)
        }

        fun onDigitEntered(digit: String) {
            if (currentPin.length >= 4) return
            currentPin.append(digit)
            val enteredIndex = currentPin.length - 1
            updateSlotsUI()
            AppAnimationUtil.elasticPinPop(slots[enteredIndex])

            if (currentPin.length == 4) {
                val entered = currentPin.toString()
                if (!isConfirmMode) {
                    // Transition to step 2: Confirm
                    firstPin = entered
                    currentPin.clear()
                    isConfirmMode = true

                    tvError.visibility = View.INVISIBLE
                    tvTitle.text = "Confirm Passcode"
                    tvSubtitle.text = "Re-enter your 4-digit passcode to confirm"
                    btnCancelStep.text = "Back"

                    val fadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out)
                    layoutSlots.startAnimation(fadeOut)
                    layoutSlots.postDelayed({
                        updateSlotsUI()
                        val fadeIn = AnimationUtils.loadAnimation(context, R.anim.fade_in)
                        layoutSlots.startAnimation(fadeIn)
                    }, 150)
                } else {
                    // Step 2 Verification
                    if (entered == firstPin) {
                        for (slot in slots) {
                            slot.setBackgroundResource(R.drawable.bg_pin_box_active)
                        }
                        dialog.dismiss()

                        val needsQuestions = !isChangingPin || !AppSecurityManager.hasSecurityQuestions(context)
                        if (needsQuestions) {
                            // User must mandatorily set up two security questions
                            SecurityQuestionsDialog.showSetup(
                                context = context,
                                mandatory = true,
                                onSuccess = {
                                    AppSecurityManager.setPasscode(context, entered)
                                    if (context is android.app.Activity) {
                                        val msg = if (isChangingPin) "Passcode & security questions updated" else "Passcode & recovery questions configured"
                                        CustomToast.showSuccess(context, "Setup Complete", msg)
                                    }
                                    onSuccess()
                                },
                                onCancel = {
                                    if (!isChangingPin) {
                                        // Passcode creation canceled because mandatory questions were dismissed
                                        AppSecurityManager.disablePasscode(context)
                                    }
                                    onCancel()
                                }
                            )
                        } else {
                            // Changing passcode with questions already set requires no extra steps
                            AppSecurityManager.setPasscode(context, entered)
                            if (context is android.app.Activity) {
                                CustomToast.showSuccess(context, "Passcode Updated", "Passcode updated successfully")
                            }
                            onSuccess()
                        }
                    } else {
                        showMismatchError()
                    }
                }
            }
        }

        fun onDeletePressed() {
            if (currentPin.isNotEmpty()) {
                val lastIdx = currentPin.length - 1
                currentPin.deleteCharAt(lastIdx)
                updateSlotsUI()
                tvError.visibility = View.INVISIBLE
            }
        }

        fun setupKeyButton(btn: View, digit: String) {
            btn.setOnClickListener {
                AppAnimationUtil.bouncePress(it)
                onDigitEntered(digit)
            }
        }

        setupKeyButton(view.findViewById(R.id.btn_setup_key_1), "1")
        setupKeyButton(view.findViewById(R.id.btn_setup_key_2), "2")
        setupKeyButton(view.findViewById(R.id.btn_setup_key_3), "3")
        setupKeyButton(view.findViewById(R.id.btn_setup_key_4), "4")
        setupKeyButton(view.findViewById(R.id.btn_setup_key_5), "5")
        setupKeyButton(view.findViewById(R.id.btn_setup_key_6), "6")
        setupKeyButton(view.findViewById(R.id.btn_setup_key_7), "7")
        setupKeyButton(view.findViewById(R.id.btn_setup_key_8), "8")
        setupKeyButton(view.findViewById(R.id.btn_setup_key_9), "9")
        setupKeyButton(view.findViewById(R.id.btn_setup_key_0), "0")

        view.findViewById<View>(R.id.btn_setup_key_delete).setOnClickListener {
            AppAnimationUtil.bouncePress(it)
            onDeletePressed()
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
            onCancel()
        }

        btnCancelStep.setOnClickListener {
            if (isConfirmMode) {
                // Back to step 1
                isConfirmMode = false
                currentPin.clear()
                firstPin = ""
                tvError.visibility = View.INVISIBLE
                tvTitle.text = if (isChangingPin) "Enter New Passcode" else "Create App Passcode"
                tvSubtitle.text = "Enter a 4-digit passcode to protect your data"
                btnCancelStep.text = "Cancel"
                updateSlotsUI()
            } else {
                dialog.dismiss()
                onCancel()
            }
        }

        dialog.setOnCancelListener {
            onCancel()
        }

        dialog.show()
    }
}
