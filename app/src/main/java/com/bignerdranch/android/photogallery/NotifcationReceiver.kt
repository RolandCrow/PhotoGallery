package com.bignerdranch.android.photogallery

import android.app.Activity
import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat

private const val TAG = "NotificationReceiver"
class NotificationReceiver: BroadcastReceiver() { // широковещательный приемник
    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "receiver result: $resultCode" )
        if(resultCode != Activity.RESULT_OK) {
            // отмена возврата трансляции
        }

        val requestCode = intent.getIntExtra(PollWorker.REQUEST_CODE, 0)
        val notification: Notification =
                intent.getParcelableExtra(PollWorker.NOTIFICATION)

        val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(requestCode, notification)
    }
}
// 645