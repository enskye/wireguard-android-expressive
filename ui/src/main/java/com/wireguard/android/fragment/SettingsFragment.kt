/*
 * Copyright © 2017-2025 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.fragment

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.wireguard.android.Application
import com.wireguard.android.QuickTileService
import com.wireguard.android.R
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import com.wireguard.android.backend.WgQuickBackend
import com.wireguard.android.preference.ExpressivePreferenceGroupAdapter
import com.wireguard.android.preference.PreferencesPreferenceDataStore
import com.wireguard.android.util.AdminKnobs
import com.wireguard.android.util.padForNavigationBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView.padForNavigationBar(resources.getDimensionPixelSize(R.dimen.bottom_nav_height))
        setDivider(null)
        setDividerHeight(0)
    }

    override fun onCreateAdapter(preferenceScreen: PreferenceScreen) =
        ExpressivePreferenceGroupAdapter(preferenceScreen)

    override fun onCreatePreferences(savedInstanceState: Bundle?, key: String?) {
        preferenceManager.preferenceDataStore = PreferencesPreferenceDataStore(lifecycleScope, Application.getPreferencesDataStore())
        addPreferencesFromResource(R.xml.preferences)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || QuickTileService.isAdded) {
            val quickTile = preferenceManager.findPreference<Preference>("quick_tile")
            quickTile?.parent?.removePreference(quickTile)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val darkTheme = preferenceManager.findPreference<Preference>("dark_theme")
            darkTheme?.parent?.removePreference(darkTheme)
        }
        if (AdminKnobs.disableConfigExport) {
            val zipExporter = preferenceManager.findPreference<Preference>("zip_exporter")
            zipExporter?.parent?.removePreference(zipExporter)
        }
        val wgQuickOnlyPrefs = arrayOf(
            preferenceManager.findPreference("tools_installer"),
            preferenceManager.findPreference("restore_on_boot"),
            preferenceManager.findPreference<Preference>("multiple_tunnels")
        ).filterNotNull()
        wgQuickOnlyPrefs.forEach { it.isVisible = false }
        lifecycleScope.launch {
            if (Application.getBackend() is WgQuickBackend) {
                wgQuickOnlyPrefs.forEach { it.isVisible = true }
            } else {
                wgQuickOnlyPrefs.forEach { it.parent?.removePreference(it) }
            }
        }
        preferenceManager.findPreference<Preference>("log_viewer")?.setOnPreferenceClickListener {
            requireActivity().supportFragmentManager.commit {
                setReorderingAllowed(true)
                setCustomAnimations(R.animator.screen_enter, R.animator.screen_exit, R.animator.screen_enter, R.animator.screen_exit)
                replace(R.id.list_detail_container, LogViewerFragment())
                addToBackStack(getString(R.string.log_viewer_screen_title))
            }
            true
        }
        val kernelModuleEnabler = preferenceManager.findPreference<Preference>("kernel_module_enabler")
        if (WgQuickBackend.hasKernelSupport()) {
            lifecycleScope.launch {
                if (Application.getBackend() !is WgQuickBackend) {
                    try {
                        withContext(Dispatchers.IO) { Application.getRootShell().start() }
                    } catch (_: Throwable) {
                        kernelModuleEnabler?.parent?.removePreference(kernelModuleEnabler)
                    }
                }
            }
        } else {
            kernelModuleEnabler?.parent?.removePreference(kernelModuleEnabler)
        }
    }
}
