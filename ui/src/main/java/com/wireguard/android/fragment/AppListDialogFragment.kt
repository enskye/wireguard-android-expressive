/*
 * Copyright © 2017-2025 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.fragment

import android.Manifest
import android.app.Dialog
import android.content.DialogInterface
import android.view.WindowManager
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PackageInfoFlags
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.databinding.Observable
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.wireguard.android.BR
import com.wireguard.android.R
import com.wireguard.android.widget.ExpressiveScrollbar
import com.wireguard.android.databinding.AppListDialogFragmentBinding
import com.wireguard.android.databinding.ObservableKeyedArrayList
import com.wireguard.android.model.ApplicationData
import com.wireguard.android.util.ErrorMessages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppListDialogFragment : DialogFragment() {
    private val appData = ObservableKeyedArrayList<String, ApplicationData>()
    private var currentlySelectedApps = emptyList<String>()
    private var initiallyExcluded = false
    private var tabs: TabLayout? = null
    private var binding: AppListDialogFragmentBinding? = null

    private fun loadData() {
        val activity = activity ?: return
        val pm = activity.packageManager
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val applicationData: MutableList<ApplicationData> = ArrayList()
                withContext(Dispatchers.IO) {
                    val packageInfos = getPackagesHoldingPermissions(pm, arrayOf(Manifest.permission.INTERNET))
                    packageInfos.forEach {
                        val packageName = it.packageName
                        val appInfo = it.applicationInfo ?: return@forEach
                        val appData =
                            ApplicationData(appInfo.loadIcon(pm), appInfo.loadLabel(pm).toString(), packageName, currentlySelectedApps.contains(packageName))
                        applicationData.add(appData)
                        appData.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
                            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                                if (propertyId == BR.selected)
                                    setButtonText()
                            }
                        })
                    }
                }
                applicationData.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
                withContext(Dispatchers.Main.immediate) {
                    appData.clear()
                    appData.addAll(applicationData)
                    setButtonText()
                }
            } catch (e: Throwable) {
                withContext(Dispatchers.Main.immediate) {
                    val error = ErrorMessages[e]
                    val message = activity.getString(R.string.error_fetching_apps, error)
                    Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
                    dismissAllowingStateLoss()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentlySelectedApps = (arguments?.getStringArrayList(KEY_SELECTED_APPS) ?: emptyList())
        initiallyExcluded = arguments?.getBoolean(KEY_IS_EXCLUDED) ?: true
    }

    private fun getPackagesHoldingPermissions(pm: PackageManager, permissions: Array<String>): List<PackageInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackagesHoldingPermissions(permissions, PackageInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackagesHoldingPermissions(permissions, 0)
        }
    }

    private fun setButtonText() {
        val numSelected = appData.count { it.isSelected }
        binding?.confirm?.text = if (numSelected == 0)
            getString(R.string.all_applications)
        else when (tabs?.selectedTabPosition) {
            0 -> resources.getQuantityString(R.plurals.exclude_n_applications, numSelected, numSelected)
            1 -> resources.getQuantityString(R.plurals.include_n_applications, numSelected, numSelected)
            else -> getString(R.string.all_applications)
        }
        binding?.selectAll?.isChecked = appData.isNotEmpty() && numSelected == appData.size
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val alertDialogBuilder = MaterialAlertDialogBuilder(requireActivity())
        val binding = AppListDialogFragmentBinding.inflate(requireActivity().layoutInflater, null, false)
        this.binding = binding
        binding.executePendingBindings()
        alertDialogBuilder.setTitle(R.string.applications)
        alertDialogBuilder.setView(binding.root)
        tabs = binding.tabs
        tabs?.apply {
            selectTab(binding.tabs.getTabAt(if (initiallyExcluded) 0 else 1))
            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabReselected(tab: TabLayout.Tab?) = Unit
                override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
                override fun onTabSelected(tab: TabLayout.Tab?) = setButtonText()
            })
        }
        binding.confirm.setOnClickListener { setSelectionAndDismiss() }
        binding.cancel.setOnClickListener { dismiss() }
        binding.selectAll.setOnClickListener {
            val selectAll = appData.none { it.isSelected }
            appData.forEach { it.isSelected = selectAll }
        }
        binding.fragment = this
        binding.appData = appData
        loadData()
        val dialog = alertDialogBuilder.create()
        dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        ExpressiveScrollbar.attach(binding.appList)
        dialog.setOnShowListener { setButtonText() }
        return dialog
    }

    private fun setSelectionAndDismiss() {
        val selectedApps: MutableList<String> = ArrayList()
        for (data in appData) {
            if (data.isSelected) {
                selectedApps.add(data.packageName)
            }
        }
        setFragmentResult(
            REQUEST_SELECTION,
            Bundle().apply {
                putStringArray(KEY_SELECTED_APPS, selectedApps.toTypedArray())
                putBoolean(KEY_IS_EXCLUDED, tabs?.selectedTabPosition == 0)
            }
        )
        dismiss()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
    }

    companion object {
        const val KEY_SELECTED_APPS = "selected_apps"
        const val KEY_IS_EXCLUDED = "is_excluded"
        const val REQUEST_SELECTION = "request_selection"

        fun newInstance(selectedApps: ArrayList<String?>?, isExcluded: Boolean): AppListDialogFragment {
            val extras = Bundle()
            extras.putStringArrayList(KEY_SELECTED_APPS, selectedApps)
            extras.putBoolean(KEY_IS_EXCLUDED, isExcluded)
            val fragment = AppListDialogFragment()
            fragment.arguments = extras
            return fragment
        }
    }
}
