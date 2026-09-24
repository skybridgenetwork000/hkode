package com.hkode.h3nrican3.app

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.util.SparseBooleanArray
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.Collections
import java.util.Random

object SketchwareUtil {

    const val TOP = 1
    const val CENTER = 2
    const val BOTTOM = 3

    @JvmStatic
    fun CustomToast(
        context: Context,
        message: String,
        textColor: Int,
        textSize: Int,
        bgColor: Int,
        radius: Int,
        gravity: Int
    ) {
        val toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
        val view = toast.view ?: return
        val textView = view.findViewById<TextView>(android.R.id.message) ?: return
        textView.textSize = textSize.toFloat()
        textView.setTextColor(textColor)
        textView.gravity = Gravity.CENTER

        val gradientDrawable = GradientDrawable().apply {
            setColor(bgColor)
            cornerRadius = radius.toFloat()
        }
        view.background = gradientDrawable
        view.setPadding(15, 10, 15, 10)
        view.elevation = 10f

        when (gravity) {
            1 -> toast.setGravity(Gravity.TOP, 0, 150)
            2 -> toast.setGravity(Gravity.CENTER, 0, 0)
            3 -> toast.setGravity(Gravity.BOTTOM, 0, 150)
        }
        toast.show()
    }

    @JvmStatic
    fun CustomToastWithIcon(
        context: Context,
        message: String,
        textColor: Int,
        textSize: Int,
        bgColor: Int,
        radius: Int,
        gravity: Int,
        icon: Int
    ) {
        val toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
        val view = toast.view ?: return
        val textView = view.findViewById<TextView>(android.R.id.message) ?: return
        textView.textSize = textSize.toFloat()
        textView.setTextColor(textColor)
        textView.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0)
        textView.gravity = Gravity.CENTER
        textView.compoundDrawablePadding = 10

        val gradientDrawable = GradientDrawable().apply {
            setColor(bgColor)
            cornerRadius = radius.toFloat()
        }
        view.background = gradientDrawable
        view.setPadding(10, 10, 10, 10)
        view.elevation = 10f

        when (gravity) {
            1 -> toast.setGravity(Gravity.TOP, 0, 150)
            2 -> toast.setGravity(Gravity.CENTER, 0, 0)
            3 -> toast.setGravity(Gravity.BOTTOM, 0, 150)
        }
        toast.show()
    }

    @JvmStatic
    fun sortListMap(
        listMap: ArrayList<HashMap<String, Any>>,
        key: String,
        isNumber: Boolean,
        ascending: Boolean
    ) {
        listMap.sortWith(Comparator { map1, map2 ->
            if (isNumber) {
                val count1 = map1[key]?.toString()?.toIntOrNull() ?: 0
                val count2 = map2[key]?.toString()?.toIntOrNull() ?: 0
                if (ascending) count1.compareTo(count2) else count2.compareTo(count1)
            } else {
                val str1 = map1[key]?.toString() ?: ""
                val str2 = map2[key]?.toString() ?: ""
                if (ascending) str1.compareTo(str2) else str2.compareTo(str1)
            }
        })
    }

    @JvmStatic
    fun CropImage(activity: Activity, path: String, requestCode: Int) {
        try {
            val intent = Intent("com.android.camera.action.CROP")
            val file = File(path)
            val contentUri = Uri.fromFile(file)
            intent.setDataAndType(contentUri, "image/*")
            intent.putExtra("crop", "true")
            intent.putExtra("aspectX", 1)
            intent.putExtra("aspectY", 1)
            intent.putExtra("outputX", 280)
            intent.putExtra("outputY", 280)
            intent.putExtra("return-data", false)
            activity.startActivityForResult(intent, requestCode)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(activity, "Your device doesn't support the crop action!", Toast.LENGTH_SHORT).show()
        }
    }

    @JvmStatic
    fun isConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
        }
    }

    @JvmStatic
    fun copyFromInputStream(inputStream: InputStream): String {
        val outputStream = ByteArrayOutputStream()
        val buf = ByteArray(1024)
        var i: Int
        try {
            while (inputStream.read(buf).also { i = it } != -1) {
                outputStream.write(buf, 0, i)
            }
            outputStream.close()
            inputStream.close()
        } catch (_: Exception) {
        }
        return outputStream.toString()
    }

    @JvmStatic
    fun hideKeyboard(context: Context) {
        val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        inputMethodManager?.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
    }

    @JvmStatic
    fun showKeyboard(context: Context) {
        val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        inputMethodManager?.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }

    @JvmStatic
    fun showMessage(context: Context, s: String) {
        Toast.makeText(context, s, Toast.LENGTH_SHORT).show()
    }

    @JvmStatic
    fun getLocationX(view: View): Int {
        val location = IntArray(2)
        view.getLocationInWindow(location)
        return location[0]
    }

    @JvmStatic
    fun getLocationY(view: View): Int {
        val location = IntArray(2)
        view.getLocationInWindow(location)
        return location[1]
    }

    @JvmStatic
    fun getRandom(min: Int, max: Int): Int {
        return Random().nextInt(max - min + 1) + min
    }

    @JvmStatic
    fun getCheckedItemPositionsToArray(list: ListView): ArrayList<Double> {
        val result = ArrayList<Double>()
        val arr: SparseBooleanArray = list.checkedItemPositions
        for (i in 0 until arr.size()) {
            if (arr.valueAt(i)) {
                result.add(arr.keyAt(i).toDouble())
            }
        }
        return result
    }

    @JvmStatic
    fun getDip(context: Context, input: Int): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            input.toFloat(),
            context.resources.displayMetrics
        )
    }

    @JvmStatic
    fun getDisplayWidthPixels(context: Context): Int {
        return context.resources.displayMetrics.widthPixels
    }

    @JvmStatic
    fun getDisplayHeightPixels(context: Context): Int {
        return context.resources.displayMetrics.heightPixels
    }

    @JvmStatic
    fun getAllKeysFromMap(map: Map<String, Any>?, output: ArrayList<String>?) {
        if (output == null) return
        output.clear()
        if (map.isNullOrEmpty()) return
        output.addAll(map.keys)
    }
}
