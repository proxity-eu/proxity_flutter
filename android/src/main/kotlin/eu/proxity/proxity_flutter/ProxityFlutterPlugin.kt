package eu.proxity.proxity_flutter

import android.app.Activity
import androidx.annotation.NonNull
import android.content.Context
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import java.util.*

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.EventChannel.StreamHandler
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.embedding.engine.plugins.service.ServiceAware
import io.flutter.embedding.engine.plugins.service.ServicePluginBinding

import eu.proxity.proxitykit.Client

class ProxityFlutterPlugin: FlutterPlugin, MethodCallHandler, ActivityAware, ServiceAware {
  private lateinit var channel: MethodChannel
  private var messages: EventChannel.EventSink? = null
  private var webhooks: EventChannel.EventSink? = null

  private lateinit var context: Context
  private lateinit var activity: Activity

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
        createNotificationChannel()
        val notificationIntent = Intent(context, activity::class.java)
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

        Client.start(context, notification, apiKey, deviceId)
        result.success(true)
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
      "start" -> {
        val client = Client.getInstance()
        client?.setMessagesCallback { ids ->
          Handler(Looper.getMainLooper()).post {
            messages?.success(ids)
          }
        }
        client?.setWebhooksCallback { ids ->
          Handler(Looper.getMainLooper()).post {
            webhooks?.success(ids)
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
        Client.getInstance()?.runWebhooks(ids, data)
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
    TODO("Not yet implemented")
  }

  override fun onAttachedToService(binding: ServicePluginBinding) {
    TODO("Not yet implemented")
  }

  override fun onDetachedFromService() {
    TODO("Not yet implemented")
  }
}
