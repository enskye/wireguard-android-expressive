/*
 * Copyright © 2017-2025 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.activity

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.appcompat.app.ActionBar
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import androidx.core.animation.doOnEnd
import androidx.core.view.ViewCompat
import androidx.core.view.marginBottom
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.wireguard.android.R
import com.wireguard.android.util.hideFullyBelowNavigationBar
import com.wireguard.android.util.TOOLBAR_FADE_IN_MS
import com.wireguard.android.util.TOOLBAR_FADE_OUT_MS
import com.wireguard.android.util.setAppBarTitle
import com.wireguard.android.util.toolbarFadeIn
import com.wireguard.android.util.toolbarFadeOut
import com.wireguard.android.util.syncToolbarActions
import com.wireguard.android.fragment.TunnelDetailFragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.wireguard.android.fragment.SettingsFragment
import com.wireguard.android.fragment.TunnelEditorFragment
import com.wireguard.android.fragment.TunnelListFragment
import com.wireguard.android.model.ObservableTunnel

/**
 * CRUD interface for WireGuard tunnels. This activity serves as the main entry point to the
 * WireGuard application, and contains several fragments for listing, viewing details of, and
 * editing the configuration and interface state of WireGuard tunnels.
 */
class MainActivity : BaseActivity(), FragmentManager.OnBackStackChangedListener {
    private companion object {
        val ACTION_BUTTONS = listOf(
            R.id.action_edit to R.id.menu_action_edit,
            R.id.action_save to R.id.menu_action_save,
            R.id.action_save_log to R.id.save_log,
        )
        const val PAGE_TUNNELS = 0
        const val PAGE_SETTINGS = 1
    }

    private class PagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount() = 2
        override fun createFragment(position: Int) =
            if (position == PAGE_SETTINGS) SettingsFragment() else TunnelListFragment()
    }

    private var actionBar: ActionBar? = null
    private var isTwoPaneLayout = false
    private var backPressedCallback: OnBackPressedCallback? = null
    private var leadingShowsLogo: Boolean? = null
    private var leadingAnimator: ValueAnimator? = null

    private fun handleBackPressed() {
        val backStackEntries = supportFragmentManager.backStackEntryCount
        findViewById<View>(R.id.fab_menu_scrim)?.let { scrim ->
            ViewCompat.setOnApplyWindowInsetsListener(scrim) { v, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                (v.layoutParams as? ViewGroup.MarginLayoutParams)?.let {
                    it.topMargin = -bars.top
                    it.bottomMargin = -bars.bottom
                    v.layoutParams = it
                }
                insets
            }
        }
        val pager = findViewById<ViewPager2>(R.id.pager)
        if (backStackEntries == 0 && pager != null && pager.currentItem != PAGE_TUNNELS) {
            pager.currentItem = PAGE_TUNNELS
            return
        }
        // If the two-pane layout does not have an editor open, going back should exit the app.
        if (isTwoPaneLayout && backStackEntries <= 1) {
            finish()
            return
        }

        if (backStackEntries >= 1)
            supportFragmentManager.popBackStack()

        // Deselect the current tunnel on navigating back from the detail pane to the one-pane list.
        if (backStackEntries == 1)
            selectedTunnel = null
    }

    private fun onPageChanged(position: Int) {
        findViewById<BottomNavigationView>(R.id.bottom_nav)?.menu
                ?.findItem(if (position == PAGE_SETTINGS) R.id.nav_settings else R.id.nav_tunnels)
                ?.isChecked = true
        setAppBarTitle(getString(if (position == PAGE_SETTINGS) R.string.settings else R.string.app_name))
        onBackStackChanged()
    }

    override fun onBackStackChanged() {
        val backStackEntries = supportFragmentManager.backStackEntryCount
        backPressedCallback?.isEnabled = backStackEntries >= 1 ||
                findViewById<ViewPager2>(R.id.pager)?.currentItem == PAGE_SETTINGS
        if (actionBar == null) return
        // Do not show the home menu when the two-pane layout is at the detail view (see above).
        val minBackStackEntries = if (isTwoPaneLayout) 2 else 1
        val atTunnelList = backStackEntries < minBackStackEntries
        val onTunnelsPage = findViewById<ViewPager2>(R.id.pager)?.currentItem != PAGE_SETTINGS
        val topScreen = if (backStackEntries > 0)
            supportFragmentManager.getBackStackEntryAt(backStackEntries - 1).name else null

        setAppBarTitle(topScreen
                ?: getString(if (!onTunnelsPage) R.string.settings else R.string.app_name))

        val logViewer = topScreen == getString(R.string.log_viewer_screen_title)
        findViewById<CollapsingToolbarLayout>(R.id.app_collapsing_toolbar)?.let { bar ->
            bar.isTitleEnabled = !logViewer
            val params = bar.layoutParams as AppBarLayout.LayoutParams
            params.height = if (logViewer) ViewGroup.LayoutParams.WRAP_CONTENT else
                resources.getDimensionPixelSize(R.dimen.expanded_bar_height)
            params.scrollFlags = if (logViewer) 0 else
                AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or
                        AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED or
                        AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP
            bar.layoutParams = params
        }
        if (!logViewer) findViewById<AppBarLayout>(R.id.app_appbar)?.setExpanded(true, true)
        val covered = backStackEntries > 0
        findViewById<View>(R.id.list_detail_container)?.let { container ->
            if (covered) container.visibility = View.VISIBLE
            else container.postDelayed({
                if (supportFragmentManager.backStackEntryCount == 0)
                    container.visibility = View.GONE
            }, TOOLBAR_FADE_OUT_MS + TOOLBAR_FADE_IN_MS)
        }
        findViewById<View>(R.id.pager)?.let { pager ->
            if (covered) {
                if (pager.alpha != 0f)
                    pager.animate().alpha(0f).setStartDelay(0)
                            .setDuration(TOOLBAR_FADE_OUT_MS)
                            .setInterpolator(toolbarFadeOut(pager.context)).start()
            } else if (pager.alpha != 1f) {
                pager.animate().alpha(1f).setStartDelay(TOOLBAR_FADE_OUT_MS)
                        .setDuration(TOOLBAR_FADE_IN_MS)
                        .setInterpolator(toolbarFadeIn(pager.context)).start()
            }
        }
        findViewById<View>(R.id.bottom_nav)?.let { bar ->
            val hidden = (bar.height + bar.marginBottom).toFloat()
            if (covered) {
                if (bar.translationY == 0f)
                    bar.animate().translationY(hidden).setStartDelay(0)
                            .setDuration(TOOLBAR_FADE_OUT_MS)
                            .setInterpolator(toolbarFadeOut(bar.context)).start()
            } else if (bar.translationY != 0f) {
                bar.animate().translationY(0f).setStartDelay(TOOLBAR_FADE_OUT_MS)
                        .setDuration(TOOLBAR_FADE_IN_MS)
                        .setInterpolator(toolbarFadeIn(bar.context)).start()
            }
        }
        setLeadingIcon(showLogo = atTunnelList && onTunnelsPage)
        findViewById<FloatingActionButton>(R.id.create_fab)?.apply {
            if (atTunnelList && onTunnelsPage) show() else hide()
        }
        findViewById<FloatingActionButton>(R.id.share_fab)?.apply {
            if (logViewer) show() else hide()
        }
        syncActionButtons()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        setSupportActionBar(findViewById(R.id.app_toolbar))
        actionBar = supportActionBar
        findViewById<FloatingActionButton?>(R.id.create_fab)?.hideFullyBelowNavigationBar()
        findViewById<View>(R.id.fab_menu_scrim)?.let { scrim ->
            ViewCompat.setOnApplyWindowInsetsListener(scrim) { v, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                (v.layoutParams as? ViewGroup.MarginLayoutParams)?.let {
                    it.topMargin = -bars.top
                    it.bottomMargin = -bars.bottom
                    v.layoutParams = it
                }
                insets
            }
        }
        val pager = findViewById<ViewPager2>(R.id.pager)
        pager?.adapter = PagerAdapter(this)
        pager?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = onPageChanged(position)
        })
        findViewById<BottomNavigationView>(R.id.bottom_nav)?.setOnItemSelectedListener { item ->
            pager?.currentItem = if (item.itemId == R.id.nav_settings) PAGE_SETTINGS else PAGE_TUNNELS
            true
        }

        isTwoPaneLayout = findViewById<View?>(R.id.master_detail_wrapper) != null
        supportFragmentManager.addOnBackStackChangedListener(this)
        backPressedCallback = onBackPressedDispatcher.addCallback(this) { handleBackPressed() }
        onBackStackChanged()
    }

    private fun setLeadingIcon(showLogo: Boolean) {
        val toolbar = findViewById<Toolbar>(R.id.app_toolbar) ?: return
        val logo = findViewById<View>(R.id.toolbar_logo) ?: return
        if (leadingShowsLogo == showLogo) return
        leadingShowsLogo = showLogo
        leadingAnimator?.cancel()
        val arrow = toolbar.navigationIcon
        arrow?.alpha = 255

        fun swap() {
            actionBar?.setDisplayHomeAsUpEnabled(!showLogo)
            logo.visibility = if (showLogo) View.VISIBLE else View.GONE
            if (showLogo) {
                logo.alpha = 0f
                logo.animate().alpha(1f).setDuration(TOOLBAR_FADE_IN_MS)
                        .setInterpolator(toolbarFadeIn(this)).start()
            } else {
                val incoming = toolbar.navigationIcon ?: return
                leadingAnimator = ValueAnimator.ofInt(0, 255).apply {
                    duration = TOOLBAR_FADE_IN_MS
                    interpolator = toolbarFadeIn(this@MainActivity)
                    addUpdateListener { incoming.alpha = it.animatedValue as Int }
                    doOnEnd { incoming.alpha = 255 }
                    start()
                }
            }
        }

        if (showLogo) {
            if (arrow == null) {
                swap()
                return
            }
            leadingAnimator = ValueAnimator.ofInt(255, 0).apply {
                duration = TOOLBAR_FADE_OUT_MS
                interpolator = toolbarFadeOut(this@MainActivity)
                addUpdateListener { arrow.alpha = it.animatedValue as Int }
                doOnEnd {
                    arrow.alpha = 255
                    swap()
                }
                start()
            }
        } else {
            logo.animate().alpha(0f).setDuration(TOOLBAR_FADE_OUT_MS)
                    .setInterpolator(toolbarFadeOut(this))
                    .withEndAction { swap() }.start()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_activity, menu)
        syncActionButtons()
        return true
    }

    private fun syncActionButtons() = syncToolbarActions(ACTION_BUTTONS)

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }

            R.id.menu_action_edit -> {
                supportFragmentManager.commit {
                    setReorderingAllowed(true)
                    setCustomAnimations(R.animator.screen_enter, R.animator.screen_exit, R.animator.screen_enter, R.animator.screen_exit)
                    replace(if (isTwoPaneLayout) R.id.detail_container else R.id.list_detail_container, TunnelEditorFragment())
                    addToBackStack(getString(R.string.tunnel_editor_screen_title))
                }
                true
            }
            // This menu item is handled by the editor fragment.
            R.id.menu_action_save -> false
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSelectedTunnelChanged(
        oldTunnel: ObservableTunnel?,
        newTunnel: ObservableTunnel?
    ): Boolean {
        val fragmentManager = supportFragmentManager
        if (fragmentManager.isStateSaved) {
            return false
        }

        val backStackEntries = fragmentManager.backStackEntryCount
        if (newTunnel == null) {
            // Clear everything off the back stack (all editors and detail fragments).
            fragmentManager.popBackStackImmediate(0, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            return true
        }
        if (backStackEntries == 2) {
            // Pop the editor off the back stack to reveal the detail fragment. Use the immediate
            // method to avoid the editor picking up the new tunnel while it is still visible.
            fragmentManager.popBackStackImmediate()
        } else if (backStackEntries == 0) {
            // Create and show a new detail fragment.
            fragmentManager.commit {
                setReorderingAllowed(true)
                setCustomAnimations(R.animator.screen_enter, R.animator.screen_exit, R.animator.screen_enter, R.animator.screen_exit)
                add(if (isTwoPaneLayout) R.id.detail_container else R.id.list_detail_container, TunnelDetailFragment())
                addToBackStack(getString(R.string.tunnel_detail_screen_title))
            }
        }
        return true
    }
}
