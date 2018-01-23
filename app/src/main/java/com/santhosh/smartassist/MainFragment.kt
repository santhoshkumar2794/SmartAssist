package com.santhosh.smartassist

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.app.NotificationCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.gms.awareness.Awareness
import com.google.android.gms.awareness.fence.*
import com.google.android.gms.awareness.state.HeadphoneState
import com.orhanobut.logger.Logger
import io.reactivex.disposables.Disposable
import java.util.*
import com.google.android.gms.awareness.state.Weather


/**
 * Created by santhosh-3366 on 22/01/18.
 */
public class MainFragment : Fragment() {

    val FENCE_RXR_ACTION = "FENCE_RECEIVER_ACTION"

    companion object {
        const val HEADPHONE = "HEADPHONE"
        const val WALKING = "WALKING"
        const val DRIVING = "DRIVING"
    }

    lateinit var mPendingIntent: PendingIntent
    var fenceRxr: FenceRxr? = null
    var disposable: Disposable? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.main_fragment, container, false)
        return view
    }

    override fun onStart() {
        super.onStart()
        checkPermission()
        val intent = Intent(FENCE_RXR_ACTION)
        mPendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
        setupFences()

        fenceRxr = FenceRxr()
        activity.registerReceiver(fenceRxr, IntentFilter(FENCE_RXR_ACTION))

        disposable = fenceRxr!!.fenceStateStream.subscribe({ queryFence() })

        queryFence()
    }

    override fun onStop() {
        super.onStop()
        removeAllFence()

        disposable?.dispose()
        activity.unregisterReceiver(fenceRxr!!)
        fenceRxr = null
    }

    private fun setupFences() {
        setupHeadphoneFence()
        setupWalkingFence()
        setupDrivingFence()
    }

    fun setupHeadphoneFence() {
        val fenceClient = Awareness.getFenceClient(activity)
        val headPhoneFence = HeadphoneFence.during(HeadphoneState.PLUGGED_IN)
        fenceClient.updateFences(FenceUpdateRequest.Builder()
                .addFence(HEADPHONE, headPhoneFence, mPendingIntent)
                .build())
                .addOnSuccessListener { Logger.i("Headphone Fence was successfully registered.") }
                .addOnFailureListener { e -> Logger.e("Headphone Fence could not be registered:  $e") }
    }

    fun setupWalkingFence() {
        val fenceClient = Awareness.getFenceClient(activity)
        val walkingFence = DetectedActivityFence.during(DetectedActivityFence.WALKING)
        fenceClient.updateFences(FenceUpdateRequest.Builder()
                .addFence(WALKING, walkingFence, mPendingIntent)
                .build())
                .addOnSuccessListener { Logger.i("Walking Fence was successfully registered.") }
                .addOnFailureListener { e -> Logger.e("Walking Fence could not be registered:  $e") }
    }

    fun setupDrivingFence() {
        val fenceClient = Awareness.getFenceClient(activity)
        val walkingFence = DetectedActivityFence.during(DetectedActivityFence.WALKING)
        fenceClient.updateFences(FenceUpdateRequest.Builder()
                .addFence(DRIVING, walkingFence, mPendingIntent)
                .build())
                .addOnSuccessListener { Logger.i("Driving Fence was successfully registered.") }
                .addOnFailureListener { e -> Logger.e("Driving Fence could not be registered:  $e") }
    }

    fun removeAllFence() {
        val fenceClient = Awareness.getFenceClient(activity)
        fenceClient.updateFences(FenceUpdateRequest.Builder()
                .removeFence(DRIVING)
                .build())
                .addOnSuccessListener { Logger.i("Driving Fence was successfully unregistered.") }
                .addOnFailureListener { e -> Logger.e("Driving Fence could not be unregistered:  $e") }

        fenceClient.updateFences(FenceUpdateRequest.Builder()
                .removeFence(WALKING)
                .build())
                .addOnSuccessListener { Logger.i("Walking Fence was successfully unregistered.") }
                .addOnFailureListener { e -> Logger.e("Walking Fence could not be unregistered:  $e") }

        fenceClient.updateFences(FenceUpdateRequest.Builder()
                .removeFence(HEADPHONE)
                .build())
                .addOnSuccessListener { Logger.i("Headphone Fence was successfully unregistered.") }
                .addOnFailureListener { e -> Logger.e("Headphone Fence could not be unregistered:  $e") }
    }

    private fun checkPermission() {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION), 1)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            1 -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    Logger.e("Location permission denied.  Weather snapshot skipped.")
                }
            }
        }
    }

    fun queryFence() {
        Awareness.getFenceClient(activity)
                .queryFences(FenceQueryRequest.forFences(arrayListOf(HEADPHONE, DRIVING, WALKING)))
                .addOnSuccessListener { fenceQueryResponse ->
                    Logger.e("Fence was successfully fetched.")
                    val fenceStateMap = fenceQueryResponse.fenceStateMap

                    val linearLayout = view?.findViewById<LinearLayout>(R.id.fence_data_list)
                    linearLayout?.removeAllViews()
                    for (fenceKey in fenceStateMap.fenceKeys) {
                        val fenceState = fenceStateMap.getFenceState(fenceKey)
                        val view = LayoutInflater.from(context).inflate(R.layout.fence_data_display, linearLayout, false)
                        view.findViewById<TextView>(R.id.fence_title).text = fenceKey.toUpperCase()
                        when (fenceKey) {
                            WALKING -> {
                                val state = if (fenceState.currentState == FenceState.TRUE) "User is walking." else "User is not walking"
                                view.findViewById<TextView>(R.id.fence_data).text = "Current State : $state\nLast Update Time : ${Date(fenceState.lastFenceUpdateTimeMillis)}"
                            }
                            DRIVING -> {
                                val state = if (fenceState.currentState == FenceState.TRUE) "User is driving." else "User is not driving"
                                view.findViewById<TextView>(R.id.fence_data).text = "Current State : $state\nLast Update Time : ${Date(fenceState.lastFenceUpdateTimeMillis)}"
                            }
                            HEADPHONE -> {
                                val state = if (fenceState.currentState == FenceState.TRUE) "Headphones plugged in." else "Headphones unplugged"
                                view.findViewById<TextView>(R.id.fence_data).text = "Current State : $state\nLast Update Time : ${Date(fenceState.lastFenceUpdateTimeMillis)}"

                                if (fenceState.currentState == FenceState.TRUE) {
                                    triggerMediaStartNotification()
                                }
                            }
                        }
                        linearLayout?.addView(view)
                    }

                }
                .addOnFailureListener { e -> Logger.e("Fence could not be fetched:  $e") }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Awareness.getSnapshotClient(activity).weather
                    .addOnSuccessListener { weatherResponse ->
                        val weather = weatherResponse.weather
                        val linearLayout = view?.findViewById<LinearLayout>(R.id.weather_data)
                        linearLayout?.findViewById<TextView>(R.id.fence_title)?.text = "WEATHER"
                        linearLayout?.findViewById<TextView>(R.id.fence_data)?.text = "Temperature : ${weather.getTemperature(Weather.CELSIUS)}\nHumidity : ${weather.humidity}"
                    }
                    .addOnFailureListener { e -> Logger.e("Could not get weather: " + e) }
        }
    }

    fun triggerMediaStartNotification() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
        intent.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "")
        val pendingIntent = PendingIntent.getActivity(context, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setStyle(NotificationCompat.BigTextStyle())
                .setContentTitle("Headphones Plugged in.")
                .setContentText("Want to play music?")
                .setChannelId("com.santhosh.smartassist")
                .setAutoCancel(true)
                .addAction(R.mipmap.ic_launcher, "START MUSIC PLAYER", pendingIntent)

        notificationManager.notify(1, notification.build())
    }
}