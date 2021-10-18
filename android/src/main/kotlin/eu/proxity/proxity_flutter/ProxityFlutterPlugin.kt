package eu.proxity.proxity_flutter

import android.app.*
import androidx.annotation.NonNull
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import android.util.Log
import java.util.*

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.EventChannel.StreamHandler
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding


class ProxityFlutterPlugin: FlutterPlugin, MethodCallHandler, ActivityAware {
    private lateinit var channel: MethodChannel
    private var messages: EventChannel.EventSink? = null
    private var webhooks: EventChannel.EventSink? = null

    private lateinit var context: Context
    private lateinit var activity: Activity?

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "eu.proxity")
        channel.setMethodCallHandler(this)

        context = flutterPluginBinding.applicationContext

        EventChannel(flutterPluginBinding.binaryMessenger, "eu.proxity.messages")
            .setStreamHandler(object : StreamHandler {
                override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                    messages = events
                }
                override fun onCancel(arguments: Any?) {}
            })

        EventChannel(flutterPluginBinding.binaryMessenger, "eu.proxity.webhooks")
            .setStreamHandler(object : StreamHandler {
                override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                    webhooks = events
                }
                override fun onCancel(arguments: Any?) {}
            })
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: MethodChannel.Result) {
        when (call.method) {
            "initialize" -> {
                Log.d("ProxityService", "initialize")
                createNotificationChannel()
                val notificationIntent = Intent(context, activity!!::class.java)
                val pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent,0)
                val notification = NotificationCompat.Builder(context, "ProxityService")
                    .setContentTitle("Proximity service")
                    .setContentText("Finding beacons near you, dear")
                    //.setSmallIcon(R.drawable.icon_notification_center_wh)
                    .setContentIntent(pendingIntent)
                    .build()

                val apiKey = call.argument<String>("apiKey")
                if (apiKey == null) {
                    result.success(false)
                    return
                }

                val deviceId = UUID.fromString(call.argument<String>("deviceId"))

                ProxityService.start(context, notification, apiKey, deviceId)
                result.success(true)
            }
            "start" -> {
                Log.d("ProxityService", "start")
                // TODO weak ref?
                ProxityService.getInstance().proxityClient?.start {
                    Handler(Looper.getMainLooper()).post {
                        if (it.messages.isNotEmpty()) {
                            messages?.success(it.messages)
                        }
                        if (it.webhooks.isNotEmpty()) {
                            webhooks?.success(it.webhooks)
                        }
                    }
                }
            }
            "webhooks" -> {
                val ids = call.argument<List<String>>("ids")
                if (ids == null || ids.isEmpty()) {
                    result.error("Empty ids", null, null)
                    return
                }

                val data = call.argument<String>("data")
                ProxityService.getInstance().proxityClient?.runWebhooks(ids, data)
            }
            "location" -> {
                /*
                val location : Task<Location>? = Client.getInstance()?.location()
                if (location == null) {
                  result.success(null)
                } else {
                  result.success(
                    mapOf(
                      "latitude" to location.latitude,
                      "longitude" to location.longitude,
                      "accuracy" to location.accuracy,
                      "speed" to location.speed,
                      "timestamp" to location.time/1000,
                    )
                  )
                }
                 */
                result.success(null)
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
              "ProxityService",
              "Proxity Service Channel",
              NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = context.getSystemService(NotificationManager::class.java)
        manager!!.createNotificationChannel(serviceChannel)
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        messages = null
        webhooks = null
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {
        TODO("Not yet implemented")
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        TODO("Not yet implemented")
    }

    override fun onDetachedFromActivity() {
        activity = null
    }
}
