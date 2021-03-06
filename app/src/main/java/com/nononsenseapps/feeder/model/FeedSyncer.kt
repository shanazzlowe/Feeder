package com.nononsenseapps.feeder.model

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.*
import com.nononsenseapps.feeder.db.room.ID_UNSET
import com.nononsenseapps.feeder.ui.ARG_FEED_ID
import com.nononsenseapps.feeder.ui.ARG_FEED_TAG
import com.nononsenseapps.feeder.util.Prefs
import com.nononsenseapps.feeder.util.currentlyCharging
import com.nononsenseapps.feeder.util.currentlyConnected
import com.nononsenseapps.feeder.util.currentlyUnmetered
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import java.util.concurrent.TimeUnit

const val ARG_FORCE_NETWORK = "force_network"

const val UNIQUE_PERIODIC_NAME = "feeder_periodic"
const val PARALLEL_SYNC = "parallel_sync"
const val MIN_FEED_AGE_MINUTES = "min_feed_age_minutes"
const val IGNORE_CONNECTIVITY_SETTINGS = "ignore_connectivity_settings"

const val FEED_ADDED_BROADCAST = "feeder.nononsenseapps.RSS_FEED_ADDED_BROADCAST"
const val SYNC_BROADCAST = "feeder.nononsenseapps.RSS_SYNC_BROADCAST"
const val SYNC_BROADCAST_IS_ACTIVE = "IS_ACTIVE"


fun isOkToSyncAutomatically(context: Context): Boolean {
    val kodein: Kodein by closestKodein(context)
    val prefs: Prefs by kodein.instance()
    return (currentlyConnected(context)
            && (!prefs.onlySyncWhileCharging || currentlyCharging(context))
            && (!prefs.onlySyncOnWIfi || currentlyUnmetered(context))
            )
}

class FeedSyncer(val context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams), KodeinAware {
    override val kodein: Kodein by closestKodein(context)
    private val localBroadcastManager: LocalBroadcastManager by instance()

    override suspend fun doWork(): Result {
        val goParallel = inputData.getBoolean(PARALLEL_SYNC, false)
        val ignoreConnectivitySettings = inputData.getBoolean(IGNORE_CONNECTIVITY_SETTINGS, false)

        val bcast = Intent(SYNC_BROADCAST)
                .putExtra(SYNC_BROADCAST_IS_ACTIVE, true)

        var success = false

        if (ignoreConnectivitySettings || isOkToSyncAutomatically(context)) {
            localBroadcastManager.sendBroadcast(bcast)

            val feedId = inputData.getLong(ARG_FEED_ID, ID_UNSET)
            val feedTag = inputData.getString(ARG_FEED_TAG) ?: ""
            val forceNetwork = inputData.getBoolean(ARG_FORCE_NETWORK, false)
            val minFeedAgeMinutes = inputData.getInt(MIN_FEED_AGE_MINUTES, 15)

            success = syncFeeds(
                    context = applicationContext,
                    feedId = feedId,
                    feedTag = feedTag,
                    forceNetwork = forceNetwork,
                    parallel = goParallel,
                    minFeedAgeMinutes = minFeedAgeMinutes
            )
            // Send notifications for configured feeds
            notify(applicationContext)
        }

        localBroadcastManager.sendBroadcast(bcast.putExtra(SYNC_BROADCAST_IS_ACTIVE, false))

        return when (success) {
            true -> Result.success()
            false -> Result.failure()
        }
    }
}

fun requestFeedSync(kodein: Kodein,
                    feedId: Long = ID_UNSET,
                    feedTag: String = "",
                    ignoreConnectivitySettings: Boolean = false,
                    forceNetwork: Boolean = false,
                    parallell: Boolean = false) {
    val workRequest = OneTimeWorkRequestBuilder<FeedSyncer>()

    val data = workDataOf(ARG_FEED_ID to feedId,
            ARG_FEED_TAG to feedTag,
            PARALLEL_SYNC to parallell,
            IGNORE_CONNECTIVITY_SETTINGS to ignoreConnectivitySettings,
            ARG_FORCE_NETWORK to forceNetwork)

    workRequest.setInputData(data)
    val workManager by kodein.instance<WorkManager>()
    workManager.enqueue(workRequest.build())
}

fun configurePeriodicSync(context: Context, forceReplace: Boolean = false) {
    val kodein by closestKodein(context)
    val workManager: WorkManager by kodein.instance()
    val prefs: Prefs by kodein.instance()
    val shouldSync = prefs.shouldSync()

    if (shouldSync) {
        val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiresCharging(prefs.onlySyncWhileCharging)

        if (prefs.onlySyncOnWIfi) {
            constraints.setRequiredNetworkType(NetworkType.UNMETERED)
        } else {
            constraints.setRequiredNetworkType(NetworkType.CONNECTED)
        }

        var timeInterval = prefs.synchronizationFrequency

        if (timeInterval in 1..12 || timeInterval == 24L) {
            // Old value for periodic sync was in hours, convert it to minutes
            timeInterval *= 60
            prefs.synchronizationFrequency = timeInterval
        }

        val workRequestBuilder = PeriodicWorkRequestBuilder<FeedSyncer>(
                timeInterval, TimeUnit.MINUTES,
                timeInterval / 2, TimeUnit.MINUTES)

        val syncWork = workRequestBuilder
                .setConstraints(constraints.build())
                .addTag("periodic_sync")
                .build()

        val existingWorkPolicy = if (forceReplace) {
            ExistingPeriodicWorkPolicy.REPLACE
        } else {
            ExistingPeriodicWorkPolicy.KEEP
        }

        workManager.enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_NAME,
                existingWorkPolicy,
                syncWork
        )
    } else {
        workManager.cancelUniqueWork(UNIQUE_PERIODIC_NAME)
    }
}
