/*
 * Copyright © 2017-2025 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.widget

import android.graphics.drawable.Animatable
import android.view.View
import androidx.core.view.isVisible
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.wireguard.android.R

class FabMenu(
    private val fab: FloatingActionButton,
    private val scrim: View,
    private val items: List<View>,
    private val onOpenChanged: (Boolean) -> Unit = {},
) {
    var isOpen = false
        private set

    init {
        scrim.setOnClickListener { close() }
    }

    fun toggle() = if (isOpen) close() else open()

    fun open() {
        if (isOpen) return
        isOpen = true
        onOpenChanged(true)
        scrim.alpha = 0f
        scrim.isVisible = true
        scrim.animate().alpha(SCRIM_ALPHA).setDuration(SCRIM_MS).start()
        items.asReversed().forEachIndexed { index, item ->
            item.alpha = 0f
            item.scaleX = ITEM_START_SCALE
            item.scaleY = ITEM_START_SCALE
            item.translationY = item.height / 2f
            item.isVisible = true
            item.animate()
                .alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
                .setStartDelay(index * STAGGER_MS)
                .setDuration(ITEM_MS)
                .start()
        }
        setFabIcon(R.drawable.avd_fab_plus_to_cross)
    }

    fun close() {
        if (!isOpen) return
        isOpen = false
        onOpenChanged(false)
        scrim.animate().alpha(0f).setDuration(SCRIM_MS)
            .withEndAction { scrim.isVisible = false }.start()
        items.forEachIndexed { index, item ->
            item.animate()
                .alpha(0f).scaleX(ITEM_START_SCALE).scaleY(ITEM_START_SCALE)
                .translationY(item.height / 2f)
                .setStartDelay(index * STAGGER_MS)
                .setDuration(ITEM_MS)
                .withEndAction { item.isVisible = false }
                .start()
        }
        setFabIcon(R.drawable.avd_fab_cross_to_plus)
    }

    private fun setFabIcon(resId: Int) {
        val tint = fab.imageTintList
        fab.setImageResource(resId)
        fab.imageTintList = tint
        (fab.drawable as? Animatable)?.start()
    }

    private companion object {
        const val SCRIM_ALPHA = 0.92f
        const val SCRIM_MS = 150L
        const val ITEM_MS = 180L
        const val STAGGER_MS = 35L
        const val ITEM_START_SCALE = 0.8f
    }
}
