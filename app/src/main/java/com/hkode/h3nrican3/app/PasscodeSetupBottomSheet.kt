package com.hkode.h3nrican3.app

import android.content.Context

/**
 * Backward-compatible bridge forwarding setup requests to the new full-screen PasscodeSetupDialog.
 */
object PasscodeSetupBottomSheet {

    fun show(
        context: Context,
        onSuccess: () -> Unit = {},
        onCancel: () -> Unit = {}
    ) {
        PasscodeSetupDialog.show(
            context = context,
            isChangingPin = false,
            onSuccess = onSuccess,
            onCancel = onCancel
        )
    }
}
