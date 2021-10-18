package eu.proxity.proxity_flutter

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import java.util.*

import eu.proxity.proxitykit.ProxityClient

class ProxityService: Service() {
    var proxityClient : ProxityClient? = null

    override fun onCreate() {
        instance = this
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val apiKey = intent?.getStringExtra("api_key") ?: ""
        val deviceId = intent?.getStringExtra("device_id") ?: UUID.randomUUID().toString()
        val id = intent?.getIntExtra("id", 123) ?: 123
        val notification = intent?.getParcelableExtra<Notification>("notification")
        startForeground(id, notification)

        proxityClient = ProxityClient.getInstance(this)
        proxityClient!!.initialize(apiKey, deviceId)

        return START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    companion object {
        private lateinit var instance: ProxityService
        fun getInstance() : ProxityService = instance

        fun start(context: Context, notification: Notification, apiKey: String, deviceId: UUID) {
            val startIntent = Intent(context, ProxityService::class.java)
            startIntent.putExtra("notification", notification)
            startIntent.putExtra("api_key", apiKey)
            startIntent.putExtra("device_id", deviceId.toString())
            context.startForegroundService(startIntent)
        }

        fun stop(context: Context) {
            val stopIntent = Intent(context, ProxityService::class.java)
            context.stopService(stopIntent)
        }
    }
}