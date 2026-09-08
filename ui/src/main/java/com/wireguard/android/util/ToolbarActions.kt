/*
 * Copyright © 2017-2025 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.util

import android.app.Activity
import android.content.Context
import android.view.animation.AnimationUtils
import android.view.View
import androidx.appcompat.widget.ActionMenuView
import androidx.appcompat.widget.Toolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonGroup
import com.wireguard.android.R

fun Activity.syncToolbarActions(actions: List<Pair<Int, Int>>) {
    val toolbar = findViewById<Toolbar>(R.id.app_toolbar) ?: return
    (toolbar.getTag(R.id.app_toolbar) as? Runnable)?.let { toolbar.removeCallbacks(it) }
    val sync = Runnable {
        for (i in 0 until toolbar.childCount)
            (toolbar.getChildAt(i) as? ActionMenuView)?.visibility = View.GONE
        val group = findViewById<MaterialButtonGroup>(R.id.toolbar_actions) ?: return@Runnable
        @Suppress("UNCHECKED_CAST")
        var buttons = group.getTag(R.id.toolbar_actions) as? List<Pair<MaterialButton, Int>>
        if (buttons == null) {
            buttons = actions.mapNotNull { (viewId, itemId) ->
                group.findViewById<MaterialButton>(viewId)?.also { it.visibility = View.VISIBLE }?.let { it to itemId }
            }
            group.setTag(R.id.toolbar_actions, buttons)
        }
        val wanted = buttons.filter { (_, itemId) -> toolbar.menu.findItem(itemId)?.isVisible == true }
        val current = (0 until group.childCount).map { group.getChildAt(it) }
        if (current == wanted.map { it.first }) return@Runnable

        fun apply() {
            group.removeAllViews()
            for ((button, itemId) in wanted) {
                button.setOnClickListener { toolbar.menu.performIdentifierAction(itemId, 0) }
                group.addView(button)
            }
            group.visibility = if (wanted.isEmpty()) View.GONE else View.VISIBLE
            group.animate().alpha(1f).setDuration(TOOLBAR_FADE_IN_MS)
                    .setInterpolator(toolbarFadeIn(group.context)).start()
        }
        if (current.isEmpty()) {
            group.alpha = 0f
            apply()
        } else {
            group.animate().alpha(0f).setDuration(TOOLBAR_FADE_OUT_MS)
                    .setInterpolator(toolbarFadeOut(group.context))
                    .withEndAction { apply() }.start()
        }
    }
    toolbar.setTag(R.id.app_toolbar, sync)
    toolbar.post(sync)
}

internal const val TOOLBAR_FADE_OUT_MS = 90L
internal const val TOOLBAR_FADE_IN_MS = 210L

internal fun toolbarFadeOut(context: Context) =
        AnimationUtils.loadInterpolator(context, android.R.interpolator.fast_out_linear_in)

internal fun toolbarFadeIn(context: Context) =
        AnimationUtils.loadInterpolator(context, android.R.interpolator.fast_out_slow_in)
