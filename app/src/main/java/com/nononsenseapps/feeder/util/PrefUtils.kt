package com.nononsenseapps.feeder.util

import android.app.Application
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.nononsenseapps.feeder.R
import com.nononsenseapps.feeder.db.room.ID_UNSET
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance

/**
 * Boolean indicating whether we performed the (one-time) welcome flow.
 */
const val PREF_WELCOME_DONE = "pref_welcome_done"

/**
 * Boolean indicating if only unread items should be shown
 */
const val PREF_SHOW_ONLY_UNREAD = "pref_show_only_unread"

/**
 * These indicate which fragment to open by default
 */
const val PREF_LAST_FEED_TAG = "pref_last_feed_tag"
const val PREF_LAST_FEED_ID = "pref_last_feed_id"

/**
 * Theme settings
 */
const val PREF_THEME = "pref_theme"

/**
 * Sync settings
 */
const val PREF_SYNC_ONLY_CHARGING = "pref_sync_only_charging"
const val PREF_SYNC_ONLY_WIFI = "pref_sync_only_wifi"
const val PREF_SYNC_FREQ = "pref_sync_freq"
const val PREF_SYNC_ON_RESUME = "pref_sync_on_resume"

/**
 * Image settings
 */
const val PREF_IMG_ONLY_WIFI = "pref_img_only_wifi"

/**
 * Reader settings
 */
const val PREF_DEFAULT_OPEN_ITEM_WITH = "pref_default_open_item_with"
const val PREF_OPEN_LINKS_WITH = "pref_open_links_with"

const val PREF_VAL_OPEN_WITH_READER = "0"
const val PREF_VAL_OPEN_WITH_WEBVIEW = "1"
const val PREF_VAL_OPEN_WITH_BROWSER = "2"

const val PREF_JAVASCRIPT_ENABLED = "pref_javascript_enabled"

/**
 * Database settings
 */
const val PREF_MAX_ITEM_COUNT_PER_FEED = "pref_max_item_count_per_feed"

object PreferenceSummaryUpdater : Preference.OnPreferenceChangeListener {
    override fun onPreferenceChange(preference: Preference?, value: Any?): Boolean {
        val stringValue = value.toString()
        if (preference is ListPreference) {
            // For list preferences, look up the correct display value in
            // the preference's 'entries' list.
            val index = preference.findIndexOfValue(stringValue)
            // Set the summary to reflect the new value.
            preference.setSummary(if (index >= 0) preference.entries[index] else null)
        } else {
            // For all other preferences, set the summary to the value's
            // simple string representation.
            preference?.summary = stringValue
        }
        return true
    }
}

fun SharedPreferences.getStringNonNull(key: String, defaultValue: String): String =
        getString(key, defaultValue) ?: defaultValue

class Prefs(override val kodein: Kodein) : KodeinAware {
    private val sp: SharedPreferences by instance()
    private val app: Application by instance()

    var javascriptEnabled: Boolean
        get() = sp.getBoolean(PREF_JAVASCRIPT_ENABLED, true)
        set(value) = sp.edit().putBoolean(PREF_JAVASCRIPT_ENABLED, value).apply()

    var onlyLoadImagesOnWIfi: Boolean
        get() = sp.getBoolean(PREF_IMG_ONLY_WIFI, false)
        set(value) = sp.edit().putBoolean(PREF_IMG_ONLY_WIFI, value).apply()

    var onlySyncOnWIfi: Boolean
        get() = sp.getBoolean(PREF_SYNC_ONLY_WIFI, false)
        set(value) = sp.edit().putBoolean(PREF_SYNC_ONLY_WIFI, value).apply()

    var syncOnResume: Boolean
        get() = sp.getBoolean(PREF_SYNC_ON_RESUME, false)
        set(value) = sp.edit().putBoolean(PREF_SYNC_ON_RESUME, value).apply()

    var onlySyncWhileCharging: Boolean
        get() = sp.getBoolean(PREF_SYNC_ONLY_CHARGING, false)
        set(value) = sp.edit().putBoolean(PREF_SYNC_ONLY_CHARGING, value).apply()

    var maximumCountPerFeed: Int
        get() = sp.getStringNonNull(PREF_MAX_ITEM_COUNT_PER_FEED, "100").toInt()
        set(value) = sp.edit().putString(PREF_MAX_ITEM_COUNT_PER_FEED, "$value").apply()

    /**
     * @return true if automatic syncing is enabled, false otherwise
     */
    fun shouldSync(): Boolean = synchronizationFrequency > 0

    /**
     * @return number of minutes between syncs, or zero if none should be performed
     */
    var synchronizationFrequency: Long
        get() = sp.getStringNonNull(PREF_SYNC_FREQ, "60").toLong()
        set(value) = sp.edit().putString(PREF_SYNC_FREQ, "$value").apply()

    var openItemsWith: String
        get() = sp.getStringNonNull(PREF_DEFAULT_OPEN_ITEM_WITH, PREF_VAL_OPEN_WITH_READER)
        set(value) = sp.edit().putString(PREF_DEFAULT_OPEN_ITEM_WITH, value).apply()

    var openLinksWith: String
        get() = sp.getStringNonNull(PREF_OPEN_LINKS_WITH, PREF_VAL_OPEN_WITH_BROWSER)
        set(value) = sp.edit().putString(PREF_OPEN_LINKS_WITH, value).apply()

    var welcomeDone: Boolean
        get() = sp.getBoolean(PREF_WELCOME_DONE, false)
        set(value) = sp.edit().putBoolean(PREF_WELCOME_DONE, value).apply()

    var nightMode: Int
        get() = when (sp.getString(PREF_THEME, app.getString(R.string.pref_theme_value_default))) {
            app.getString(R.string.pref_theme_value_day) -> AppCompatDelegate.MODE_NIGHT_NO
            app.getString(R.string.pref_theme_value_night) -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        set(value) = sp.edit().putString(
                PREF_THEME,
                when (value) {
                    AppCompatDelegate.MODE_NIGHT_YES -> app.getString(R.string.pref_theme_value_night)
                    AppCompatDelegate.MODE_NIGHT_NO -> app.getString(R.string.pref_theme_value_day)
                    else -> app.getString(R.string.pref_theme_value_system)
                }
        ).apply()

    var isNightMode: Boolean
        get() = when (nightMode) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            else -> false
        }
        set(value) {
            nightMode = when (value) {
                true -> AppCompatDelegate.MODE_NIGHT_YES
                false -> AppCompatDelegate.MODE_NIGHT_NO
            }
        }

    var showOnlyUnread: Boolean
        get() = sp.getBoolean(PREF_SHOW_ONLY_UNREAD, true)
        set(value) = sp.edit().putBoolean(PREF_SHOW_ONLY_UNREAD, value).apply()

    /**
     * Remember the open fragment, either by id, or tag if id < 1.
     * Clears the other one.
     *
     * @param id
     * @param tag
     */
    fun setLastOpenFeed(id: Long, tag: String?) =
            sp.edit().putLong(PREF_LAST_FEED_ID, id)
                    .putString(PREF_LAST_FEED_TAG, tag)
                    .apply()

    /**
     * Get which feed tag was last open. Check id first.
     */
    var lastOpenFeedTag: String?
        get() = sp.getString(PREF_LAST_FEED_TAG, null)
        set(value) = sp.edit().putString(PREF_LAST_FEED_TAG, value).apply()

    /**
     * Get which feed id was last open. If -1, check tag.
     */
    var lastOpenFeedId: Long
        get() = sp.getLong(PREF_LAST_FEED_ID, ID_UNSET)
        set(value) = sp.edit().putLong(PREF_LAST_FEED_ID, value).apply()

    fun shouldLoadImages(): Boolean {
        return when {
            onlyLoadImagesOnWIfi -> currentlyUnmetered(app)
            else -> true
        }
    }
}
