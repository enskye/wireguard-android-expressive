/*
 * Copyright © 2017-2025 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.preference

import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceGroupAdapter
import androidx.preference.PreferenceViewHolder
import com.wireguard.android.R

class ExpressivePreferenceGroupAdapter(group: PreferenceGroup) : PreferenceGroupAdapter(group) {

    override fun onBindViewHolder(holder: PreferenceViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)

        val item = getItem(position)
        if (item is PreferenceCategory || item?.layoutResource == R.layout.preference_donate_expressive) {
            holder.itemView.setBackgroundResource(0)
            return
        }

        val first = isGroupBoundary(position - 1)
        val last = isGroupBoundary(position + 1)
        holder.itemView.setBackgroundResource(
            when {
                first && last -> R.drawable.preference_item_background
                first -> R.drawable.preference_item_background_top
                last -> R.drawable.preference_item_background_bottom
                else -> R.drawable.preference_item_background_middle
            }
        )
    }

    private fun isGroupBoundary(position: Int): Boolean {
        if (position < 0 || position >= itemCount) return true
        val item = getItem(position)
        return item is PreferenceCategory || item?.layoutResource == R.layout.preference_donate_expressive
    }
}
