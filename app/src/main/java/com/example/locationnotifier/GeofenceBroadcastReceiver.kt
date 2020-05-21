package com.example.locationnotifier

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.widget.Toast
import com.example.locationnotifier.MapsActivity.Companion.ACTION_GEOFENCE_EVENT
import com.example.locationnotifier.MapsActivity.Companion.notificationManager
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
/*
A Geofence Broadcast Receiver to deal with Intent of ACTION_GEOFENCE_EVENT
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {
    private lateinit var notificationChannel: NotificationChannel
    private lateinit var builder: Notification.Builder
    private val description = "Geofencing Notification"
    companion object{
        const val CHANNEL_ID = "com.example.locationnotifier"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == ACTION_GEOFENCE_EVENT){
            val geofencingEvent = GeofencingEvent.fromIntent(intent)

            if (geofencingEvent.hasError()) {
                return
            }

            val geofenceTransition = geofencingEvent.geofenceTransition
            geofencingEvent.triggeringGeofences
            when(geofenceTransition){
                Geofence.GEOFENCE_TRANSITION_ENTER -> {
                    val title = "ENTERED"
                    val message = "You've entered the vicinity of the location."
                    showNotification(context, title, message)
                }
                Geofence.GEOFENCE_TRANSITION_EXIT -> {
                    val title= "EXITED"
                    val message = "You've exited from the vicinity of the location."
                    showNotification(context, title, message)
                }
            }
        }
    }
    /*
    A function to show Notification in case of Entrance and Exit
     */
    private fun showNotification(context : Context, title: String, message: String){
        Toast.makeText(context, "Entered in the desired Location", Toast.LENGTH_SHORT).show()
        val intent = Intent(context, MapsActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel = NotificationChannel(CHANNEL_ID, description, NotificationManager.IMPORTANCE_HIGH)
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.enableVibration(true)
            notificationManager.createNotificationChannel(notificationChannel)

            builder = Notification.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentIntent(pendingIntent)
        }
        else{
            builder = Notification.Builder(context)
                .setContentTitle("Android")
                .setContentText("New Message")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentIntent(pendingIntent)
        }
        notificationManager.notify(0, builder.build())
    }
}