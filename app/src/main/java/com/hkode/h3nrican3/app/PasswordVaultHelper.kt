package com.hkode.h3nrican3.app

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

object PasswordVaultHelper {

    fun showPasswordVaultBottomSheet(
        context: Context,
        onPasswordSelected: (String) -> Unit
    ) {
        val passwordManager = PasswordManager(context)
        val bottomSheet = BottomSheetDialog(context, R.style.AppTheme_BottomSheetDialog)
        val sheetView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_saved_passwords, null)
        bottomSheet.setContentView(sheetView)

        val rv = sheetView.findViewById<RecyclerView>(R.id.rv_saved_passwords)
        val emptyLayout = sheetView.findViewById<View>(R.id.layout_passwords_empty)
        val btnAdd = sheetView.findViewById<MaterialButton>(R.id.btn_bottom_sheet_add_pwd)
        val btnDone = sheetView.findViewById<MaterialButton>(R.id.btn_close_passwords_sheet)

        rv.layoutManager = LinearLayoutManager(context)

        fun refreshList(adapter: SavedPasswordsAdapter) {
            val list = passwordManager.getSavedPasswords()
            if (list.isEmpty()) {
                rv.visibility = View.GONE
                emptyLayout.visibility = View.VISIBLE
            } else {
                rv.visibility = View.VISIBLE
                emptyLayout.visibility = View.GONE
            }
            adapter.updateList(list)
        }

        val initialList = passwordManager.getSavedPasswords().toMutableList()
        lateinit var adapter: SavedPasswordsAdapter
        adapter = SavedPasswordsAdapter(
            context = context,
            passwords = initialList,
            onSelect = { saved ->
                onPasswordSelected(saved.password)
                if (context is android.app.Activity) {
                    CustomToast.showSuccess(context, "Autofilled", "Autofilled '${saved.label}'")
                }
                bottomSheet.dismiss()
            },
            onDelete = { saved ->
                passwordManager.deletePassword(saved.id)
                refreshList(adapter)
                if (context is android.app.Activity) {
                    CustomToast.showInfo(context, "Deleted", "Deleted '${saved.label}'")
                }
            }
        )

        rv.adapter = adapter

        if (initialList.isEmpty()) {
            rv.visibility = View.GONE
            emptyLayout.visibility = View.VISIBLE
        } else {
            rv.visibility = View.VISIBLE
            emptyLayout.visibility = View.GONE
        }

        btnAdd.setOnClickListener {
            showSavePasswordDialog(context, "") {
                refreshList(adapter)
            }
        }

        btnDone.setOnClickListener {
            bottomSheet.dismiss()
        }

        bottomSheet.setOnShowListener { dialog ->
            val d = dialog as BottomSheetDialog
            val bottomSheetInternal = d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheetInternal?.let { sheet ->
                sheet.background = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bg_bottom_sheet)
                val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(sheet)
                behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(sheet) { _, insets -> insets }
            }
        }
        bottomSheet.window?.let { win ->
            win.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            win.navigationBarColor = androidx.core.content.ContextCompat.getColor(context, R.color.card_background)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                win.isNavigationBarContrastEnforced = false
            }
        }

        bottomSheet.show()
    }

    fun showSavePasswordDialog(
        context: Context,
        prefilledPassword: String = "",
        onSaved: () -> Unit = {}
    ) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_save_password, null)
        dialog.setContentView(dialogView)

        dialog.window?.let { window ->
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.setLayout(
                (context.resources.displayMetrics.widthPixels * 0.90).toInt(),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val etLabel = dialogView.findViewById<TextInputEditText>(R.id.et_dialog_pwd_label)
        val etPassword = dialogView.findViewById<TextInputEditText>(R.id.et_dialog_pwd_value)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_dialog_save_pwd_cancel)
        val btnConfirm = dialogView.findViewById<MaterialButton>(R.id.btn_dialog_save_pwd_confirm)

        if (prefilledPassword.isNotBlank()) {
            etPassword.setText(prefilledPassword)
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnConfirm.setOnClickListener {
            val label = etLabel.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (password.isEmpty()) {
                if (context is android.app.Activity) {
                    CustomToast.showWarning(context, "Empty Password", "Password cannot be empty")
                }
                return@setOnClickListener
            }

            val finalLabel = if (label.isNotEmpty()) label else "Key ${System.currentTimeMillis() % 1000}"
            PasswordManager(context).savePassword(finalLabel, password)
            if (context is android.app.Activity) {
                CustomToast.showSuccess(context, "Saved", "Saved to password vault!")
            }
            onSaved()
            dialog.dismiss()
        }

        dialog.show()
    }
}
