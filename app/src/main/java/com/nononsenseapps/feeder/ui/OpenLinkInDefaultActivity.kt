package com.nononsenseapps.feeder.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Browser.EXTRA_CREATE_NEW_TAB
import android.util.Log
import android.widget.Toast
import com.nononsenseapps.feeder.R
import com.nononsenseapps.feeder.base.CoroutineScopedKodeinAwareActivity
import com.nononsenseapps.feeder.db.COL_LINK
import com.nononsenseapps.feeder.db.room.FeedItemDao
import com.nononsenseapps.feeder.db.room.ID_UNSET
import com.nononsenseapps.feeder.model.cancelNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kodein.di.generic.instance

/**
 * Proxy activity to mark item as read and notified in database as well as cancelling the
 * notification before performing a notification action such as opening in the browser.
 *
 * If link is null, then item is only marked as read and notified.
 */
class OpenLinkInDefaultActivity : CoroutineScopedKodeinAwareActivity() {
    val feedItemDao: FeedItemDao by instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent?.let { intent ->
            val id: Long = intent.data?.lastPathSegment?.toLong() ?: ID_UNSET
            val link: String? = intent.data?.getQueryParameter(COL_LINK)

            launch(Dispatchers.Default) {
                feedItemDao.markAsReadAndNotified(id)
                cancelNotification(this@OpenLinkInDefaultActivity, id)
            }

            if (link != null) {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)).also {
                        intent.putExtra(EXTRA_CREATE_NEW_TAB, true)
                    })
                } catch (e: Throwable) {
                    e.printStackTrace()
                    Toast.makeText(this, R.string.no_activity_for_link, Toast.LENGTH_SHORT).show()
                    Log.e("FeederOpenInWebBrowser", "Failed to start browser", e)
                }
            }
        }

        // Terminate activity immediately
        finish()
    }
}
