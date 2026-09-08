/*
 * Copyright © 2017-2025 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.util

import android.animation.ValueAnimator
import android.app.Activity
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import androidx.core.animation.doOnEnd
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.wireguard.android.R

fun View.padForNavigationBar(extraBottom: Int = 0) {
    (this as? ViewGroup)?.clipToPadding = false
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        v.updatePadding(bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom + extraBottom)
        insets
    }
}

fun View.hideFullyBelowNavigationBar() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val behavior = (v.layoutParams as? CoordinatorLayout.LayoutParams)?.behavior
        @Suppress("UNCHECKED_CAST")
        (behavior as? HideBottomViewOnScrollBehavior<View>)
                ?.setAdditionalHiddenOffsetY(v, insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom)
        insets
    }
    ViewCompat.requestApplyInsets(this)
}

@Suppress("DEPRECATION")
fun Activity.slideTransition(forward: Boolean) {
    val enter = if (forward) R.anim.slide_in_right else R.anim.slide_in_left
    val exit = if (forward) R.anim.slide_out_left else R.anim.slide_out_right
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        overrideActivityTransition(
                if (forward) Activity.OVERRIDE_TRANSITION_OPEN else Activity.OVERRIDE_TRANSITION_CLOSE,
                enter, exit)
    else
        overridePendingTransition(enter, exit)
}

fun Activity.setAppBarTitle(title: CharSequence) {
    setTitle(title)
    val bar = findViewById<CollapsingToolbarLayout>(R.id.app_collapsing_toolbar) ?: return
    bar.post {
        if (bar.title == title) return@post
        val colour = resolveAttribute(com.google.android.material.R.attr.colorOnSurface)
        fun setTitleAlpha(fraction: Float) {
            val c = ColorUtils.setAlphaComponent(colour, (fraction * 255).toInt())
            bar.setCollapsedTitleTextColor(c)
            bar.setExpandedTitleColor(c)
        }
        ValueAnimator.ofFloat(1f, 0f).apply {
            duration = TOOLBAR_FADE_OUT_MS
            interpolator = toolbarFadeOut(bar.context)
            addUpdateListener { setTitleAlpha(it.animatedValue as Float) }
            doOnEnd {
                bar.title = title
                ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = TOOLBAR_FADE_IN_MS
                    interpolator = toolbarFadeIn(bar.context)
                    addUpdateListener { setTitleAlpha(it.animatedValue as Float) }
                    start()
                }
            }
            start()
        }
    }
}

