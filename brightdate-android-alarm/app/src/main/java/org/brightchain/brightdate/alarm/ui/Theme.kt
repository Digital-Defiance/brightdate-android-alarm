package org.brightchain.brightdate.alarm.ui

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.materialswitch.MaterialSwitch
import org.brightchain.brightdate.alarm.R

/**
 * Applies the user's chosen accent color to the UI by post-processing the
 * inflated view tree.
 *
 * We deliberately do NOT use Material 3 [com.google.android.material.color.DynamicColors]:
 * its `setContentBasedSource` pipeline feeds the chosen color into an HCT tonal-palette
 * generator that desaturates it to produce "harmonized" tones — a vivid yellow ends up
 * as a sickly olive, the opposite of what the user picked. And mutating Material color
 * attributes (`colorPrimary`, `colorPrimaryContainer`, …) on a [android.content.res.Resources.Theme]
 * at runtime requires a style resource, which we can't generate dynamically.
 *
 * So instead the static theme paints accents as [R.color.bd_yellow], and we walk the
 * inflated view tree swapping any view whose accent matches the canonical theme color
 * for the user's pick. A view tag tracks the last-applied color per root so subsequent
 * passes (after the user changes the preference) swap from the *currently-displayed*
 * accent rather than the static theme color.
 */
object Theme {

    /** Canonical pure gold (#FFD700). */
    const val DEFAULT_ACCENT_HEX = "#FFFFD700"

    /**
     * Call from `Activity.onCreate` *after* `setContentView`. Installs a layout
     * listener on the content root so children inflated lazily (preference rows,
     * fragments, popup menus) are recolored as soon as they appear.
     */
    fun applyTo(activity: Activity) {
        val root: View = activity.findViewById(android.R.id.content) ?: return
        applyAccentTo(root)
        // Catch lazily-inflated children — e.g. PreferenceFragmentCompat's
        // RecyclerView rows aren't bound until after the first layout pass.
        root.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                applyAccentTo(root)
            }
        })
    }

    /**
     * Apply the user's accent to a view subtree. Tracks the last-applied
     * accent per root view (via a tag) so subsequent passes swap from the
     * actual currently-painted color rather than guessing. Newly-attached
     * children (RecyclerView item views, freshly-inflated fragments) have no
     * tag and fall back to swapping from the static theme accent — which is
     * what they're currently painted with.
     */
    fun applyAccentTo(view: View) {
        val accent = getAccentColor(view.context)
        val themeAccent = ContextCompat.getColor(view.context, R.color.bd_yellow)
        val lastApplied = view.getTag(R.id.tag_last_accent) as? Int
        val from = lastApplied ?: themeAccent
        if (from != accent) {
            recolor(view, from, accent)
        }
        view.setTag(R.id.tag_last_accent, accent)
    }

    /**
     * Recolor a dialog after it's shown. Dialogs have their own Window with a
     * separate decor view, so the activity-content walk in [applyTo] doesn't
     * reach them. Call this immediately after `dialog.show()`.
     */
    fun applyTo(dialog: android.app.Dialog) {
        val root = dialog.window?.decorView ?: return
        applyAccentTo(root)
        // Date/time pickers update their content (selected day, AM/PM toggle,
        // hour markers) after the initial layout — keep recoloring as the
        // tree changes.
        root.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                applyAccentTo(root)
            }
        })
    }

    fun getAccentColor(context: Context): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val key = context.getString(R.string.pref_key_accent_color)
        val hex = prefs.getString(key, DEFAULT_ACCENT_HEX) ?: DEFAULT_ACCENT_HEX
        return try {
            Color.parseColor(hex)
        } catch (_: IllegalArgumentException) {
            Color.parseColor(DEFAULT_ACCENT_HEX)
        }
    }

    /** Walks [view] and swaps every reference to [from] with [to]. */
    private fun recolor(view: View, from: Int, to: Int) {
        // SwitchCompat covers both AppCompat's SwitchCompat (used by
        // SwitchPreferenceCompat) and Material's MaterialSwitch which extends it.
        if (view is SwitchCompat) {
            view.thumbTintList?.let { csl ->
                if (matchesAny(csl, from)) view.thumbTintList = swapAll(csl, from, to)
            }
            view.trackTintList?.let { csl ->
                if (matchesAny(csl, from)) view.trackTintList = swapAll(csl, from, to)
            }
            if (view is MaterialSwitch) {
                view.trackDecorationTintList?.let { csl ->
                    if (matchesAny(csl, from)) view.trackDecorationTintList = swapAll(csl, from, to)
                }
            }
        }
        when (view) {
            is MaterialButton -> {
                if (matches(view.currentTextColor, from)) {
                    view.setTextColor(blend(to, view.currentTextColor))
                }
                view.iconTint?.let { csl ->
                    if (matchesAny(csl, from)) view.iconTint = swapAll(csl, from, to)
                }
                view.strokeColor?.let { csl ->
                    if (matchesAny(csl, from)) view.strokeColor = swapAll(csl, from, to)
                }
                view.backgroundTintList?.let { csl ->
                    if (matchesAny(csl, from)) view.backgroundTintList = swapAll(csl, from, to)
                }
                view.rippleColor?.let { csl ->
                    if (matchesAny(csl, from)) view.rippleColor = swapAll(csl, from, to)
                }
            }
            is FloatingActionButton -> {
                view.backgroundTintList?.let { csl ->
                    if (matchesAny(csl, from)) view.backgroundTintList = swapAll(csl, from, to)
                }
                view.imageTintList?.let { csl ->
                    if (matchesAny(csl, from)) view.imageTintList = swapAll(csl, from, to)
                }
                view.rippleColorStateList?.let { csl ->
                    if (matchesAny(csl, from)) view.setRippleColor(swapAll(csl, from, to))
                }
            }
            is TextView -> {
                if (matches(view.currentTextColor, from)) {
                    // Preserve any alpha the original had (preference category
                    // titles use colorPrimary with reduced alpha).
                    view.setTextColor(blend(to, view.currentTextColor))
                }
                view.compoundDrawableTintList?.let { csl ->
                    if (matches(csl.defaultColor, from)) {
                        view.compoundDrawableTintList = swap(csl, from, to)
                    }
                }
            }
            is ImageView -> {
                view.imageTintList?.let { csl ->
                    if (matches(csl.defaultColor, from)) view.imageTintList = swap(csl, from, to)
                }
            }
        }
        // Generic background tint (e.g. the color-picker preview swatch).
        if (view !is MaterialButton && view !is FloatingActionButton) {
            view.backgroundTintList?.let { csl ->
                if (matches(csl.defaultColor, from)) view.backgroundTintList = swap(csl, from, to)
            }
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) recolor(view.getChildAt(i), from, to)
        }
    }

    /** Match by RGB, ignoring alpha. */
    private fun matches(actual: Int, from: Int): Boolean =
        (actual and 0x00FFFFFF) == (from and 0x00FFFFFF)

    /** Combine [to]'s RGB with [original]'s alpha. */
    private fun blend(to: Int, original: Int): Int =
        (original and 0xFF000000.toInt()) or (to and 0x00FFFFFF)

    /** Swap a single-color CSL, preserving its alpha. */
    private fun swap(csl: ColorStateList, from: Int, to: Int): ColorStateList =
        ColorStateList.valueOf(blend(to, csl.defaultColor))

    /** True if any sampled state in [csl] uses [from]'s RGB. */
    private fun matchesAny(csl: ColorStateList, from: Int): Boolean {
        if (matches(csl.defaultColor, from)) return true
        for (states in SAMPLE_STATES) {
            if (matches(csl.getColorForState(states, csl.defaultColor), from)) return true
        }
        return false
    }

    /**
     * Build a new CSL that mirrors [csl]'s state set, swapping any state whose
     * color matches [from] for one based on [to].
     */
    private fun swapAll(csl: ColorStateList, from: Int, to: Int): ColorStateList {
        val states = SAMPLE_STATES
        val colors = IntArray(states.size + 1)
        for (i in states.indices) {
            val stateColor = csl.getColorForState(states[i], csl.defaultColor)
            colors[i] = if (matches(stateColor, from)) blend(to, stateColor) else stateColor
        }
        colors[states.size] = if (matches(csl.defaultColor, from))
            blend(to, csl.defaultColor) else csl.defaultColor
        return ColorStateList(
            states + arrayOf<IntArray>(intArrayOf()),
            colors
        )
    }

    private val SAMPLE_STATES: Array<IntArray> = arrayOf(
        intArrayOf(android.R.attr.state_checked, android.R.attr.state_enabled),
        intArrayOf(-android.R.attr.state_checked, android.R.attr.state_enabled),
        intArrayOf(-android.R.attr.state_enabled),
        intArrayOf(android.R.attr.state_pressed),
        intArrayOf(android.R.attr.state_focused)
    )
}
