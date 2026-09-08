/*
 * Copyright © 2017-2025 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.widget

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView
import com.wireguard.android.util.resolveAttribute

class ExpressiveScrollbar private constructor(private val list: RecyclerView) :
        RecyclerView.ItemDecoration(), RecyclerView.OnItemTouchListener {

    private val density = list.resources.displayMetrics.density
    private fun dp(value: Float) = value * density

    private val restWidth = dp(4f)
    private val pressedWidth = dp(10f)
    private val minLength = dp(48f)
    private val edgeMargin = dp(4f)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val restColour = list.context.resolveAttribute(
            com.google.android.material.R.attr.colorOutlineVariant)
    private val pressedColour = list.context.resolveAttribute(
            androidx.appcompat.R.attr.colorPrimary)

    private var width = restWidth
    private var dragging = false
    private var dragOffset = 0f
    private var widthAnimator: ValueAnimator? = null
    private val bounds = RectF()

    private fun thumb(): Pair<Float, Float>? {
        val range = list.computeVerticalScrollRange().toFloat()
        val extent = list.computeVerticalScrollExtent().toFloat()
        if (range <= extent || extent <= 0f) return null
        val length = (extent * extent / range).coerceAtLeast(minLength).coerceAtMost(extent)
        val travel = extent - length
        val progress = list.computeVerticalScrollOffset() / (range - extent)
        return (travel * progress.coerceIn(0f, 1f)) to length
    }

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val (top, length) = thumb() ?: return
        paint.color = if (dragging) pressedColour else restColour
        val right = parent.width - edgeMargin
        bounds.set(right - width, top, right, top + length)
        val radius = width / 2f
        canvas.drawRoundRect(bounds, radius, radius, paint)
    }

    private fun onThumb(event: MotionEvent): Boolean {
        val (top, length) = thumb() ?: return false
        val strip = dp(24f)
        return event.x >= list.width - strip && event.y >= top && event.y <= top + length
    }

    private fun animateWidth(to: Float) {
        widthAnimator?.cancel()
        widthAnimator = ValueAnimator.ofFloat(width, to).apply {
            duration = 120
            addUpdateListener {
                width = it.animatedValue as Float
                list.invalidate()
            }
            start()
        }
    }

    override fun onInterceptTouchEvent(rv: RecyclerView, event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN && onThumb(event)) {
            val (top, _) = thumb() ?: return false
            dragging = true
            dragOffset = event.y - top
            animateWidth(pressedWidth)
            return true
        }
        return dragging
    }

    override fun onTouchEvent(rv: RecyclerView, event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                val (_, length) = thumb() ?: return
                val range = list.computeVerticalScrollRange()
                val extent = list.computeVerticalScrollExtent()
                val travel = extent - length
                if (travel <= 0f) return
                val target = ((event.y - dragOffset) / travel).coerceIn(0f, 1f)
                list.scrollBy(0, ((range - extent) * target - list.computeVerticalScrollOffset()).toInt())
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragging = false
                animateWidth(restWidth)
                list.invalidate()
            }
        }
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) = Unit

    companion object {
        @JvmStatic
        fun attach(list: RecyclerView) {
            val scrollbar = ExpressiveScrollbar(list)
            list.addItemDecoration(scrollbar)
            list.addOnItemTouchListener(scrollbar)
        }
    }
}
