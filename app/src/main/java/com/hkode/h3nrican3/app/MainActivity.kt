package com.hkode.h3nrican3.app

import android.os.Bundle
import android.util.SparseBooleanArray
import android.util.TypedValue
import android.view.View
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Random

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        initialize(savedInstanceState)
        initializeLogic()
    }

    private fun initialize(savedInstanceState: Bundle?) {}

    private fun initializeLogic() {}

    @Deprecated("Sketchware helper method")
    fun showMessage(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    @Deprecated("Sketchware helper method")
    fun getLocationX(view: View): Int {
        val location = IntArray(2)
        view.getLocationInWindow(location)
        return location[0]
    }

    @Deprecated("Sketchware helper method")
    fun getLocationY(view: View): Int {
        val location = IntArray(2)
        view.getLocationInWindow(location)
        return location[1]
    }

    @Deprecated("Sketchware helper method")
    fun getRandom(min: Int, max: Int): Int {
        val random = Random()
        return random.nextInt(max - min + 1) + min
    }

    @Deprecated("Sketchware helper method")
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

    @Deprecated("Sketchware helper method")
    fun getDip(input: Int): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            input.toFloat(),
            resources.displayMetrics
        )
    }

    @Deprecated("Sketchware helper method")
    fun getDisplayWidthPixels(): Int {
        return resources.displayMetrics.widthPixels
    }

    @Deprecated("Sketchware helper method")
    fun getDisplayHeightPixels(): Int {
        return resources.displayMetrics.heightPixels
    }
}
