package com.hkode.h3nrican3.app

import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator

object AppAnimationUtil {

    fun bouncePress(view: View, onEnd: (() -> Unit)? = null) {
        view.animate()
            .scaleX(0.93f)
            .scaleY(0.93f)
            .setDuration(70)
            .withEndAction {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(120)
                    .setInterpolator(OvershootInterpolator(2.0f))
                    .withEndAction {
                        onEnd?.invoke()
                    }
                    .start()
            }
            .start()
    }

    fun bounceInUp(view: View, delayMs: Long = 0, startYOffsetDp: Float = 60f) {
        val density = view.resources.displayMetrics.density
        view.visibility = View.VISIBLE
        view.alpha = 0f
        view.translationY = startYOffsetDp * density
        view.scaleX = 0.92f
        view.scaleY = 0.92f

        view.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setStartDelay(delayMs)
            .setDuration(450)
            .setInterpolator(OvershootInterpolator(1.35f))
            .start()
    }

    fun bounceInDown(view: View, delayMs: Long = 0, startYOffsetDp: Float = 40f) {
        val density = view.resources.displayMetrics.density
        view.visibility = View.VISIBLE
        view.alpha = 0f
        view.translationY = -startYOffsetDp * density

        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(delayMs)
            .setDuration(400)
            .setInterpolator(OvershootInterpolator(1.2f))
            .start()
    }

    fun slideInFromLeft(view: View, delayMs: Long = 0, startXOffsetDp: Float = 50f) {
        val density = view.resources.displayMetrics.density
        view.visibility = View.VISIBLE
        view.alpha = 0f
        view.translationX = -startXOffsetDp * density

        view.animate()
            .alpha(1f)
            .translationX(0f)
            .setStartDelay(delayMs)
            .setDuration(400)
            .setInterpolator(OvershootInterpolator(1.2f))
            .start()
    }

    fun popBounceOut(view: View) {
        view.visibility = View.VISIBLE
        view.alpha = 0f
        view.scaleX = 0.82f
        view.scaleY = 0.82f
        val density = view.resources.displayMetrics.density
        view.translationY = 24f * density

        view.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .translationY(0f)
            .setDuration(420)
            .setInterpolator(OvershootInterpolator(1.7f))
            .start()
    }

    fun elasticPinPop(view: View) {
        view.scaleX = 0.8f
        view.scaleY = 0.8f
        view.animate()
            .scaleX(1.18f)
            .scaleY(1.18f)
            .setDuration(120)
            .setInterpolator(OvershootInterpolator(2.5f))
            .withEndAction {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(100)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
            .start()
    }

    fun elasticCheckSuccess(views: List<View>, onComplete: () -> Unit) {
        for (i in views.indices) {
            val v = views[i]
            v.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setStartDelay(i * 45L)
                .setDuration(140)
                .setInterpolator(OvershootInterpolator(2.2f))
                .withEndAction {
                    v.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(100)
                        .start()
                    if (i == views.size - 1) {
                        v.postDelayed({ onComplete() }, 180)
                    }
                }
                .start()
        }
    }
}
