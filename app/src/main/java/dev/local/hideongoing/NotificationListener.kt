package dev.local.hideongoing

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NotificationListener : NotificationListenerService() {

    private lateinit var store: RuleStore

    override fun onCreate() {
        super.onCreate()
        store = RuleStore.get(this)
        live = this
    }

    override fun onDestroy() {
        live = null
        _connected.value = false
        super.onDestroy()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        _connected.value = true
        activeNotifications?.forEach(::enforce)
        publish()
    }

    override fun onListenerDisconnected() {
        _connected.value = false
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        enforce(sbn)
        publish()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        publish()
    }

    fun enforceNow(pkg: String, channelId: String?) {
        activeNotifications?.filter {
            it.packageName == pkg &&
                (channelId == null || it.notification?.channelId == channelId)
        }?.forEach(::enforce)
        publish()
    }

    fun liftRule(pkg: String, channelId: String?) {
        snoozedNotifications?.filter {
            it.packageName == pkg &&
                (channelId == null || it.notification?.channelId == channelId)
        }?.forEach { snoozeNotification(it.key, 1L) }
        publish()
    }

    private fun enforce(sbn: StatusBarNotification) {
        if (!store.matches(sbn.packageName, sbn.notification?.channelId)) return
        if (sbn.isClearable) {
            cancelNotification(sbn.key)
        } else {
            snoozeNotification(sbn.key, ONE_YEAR_MS)
        }
    }

    private fun publish() {
        _state.value = activeNotifications?.toList().orEmpty()
    }

    companion object {
        private const val ONE_YEAR_MS = 365L * 24 * 3600 * 1000

        @Volatile
        var live: NotificationListener? = null
            private set

        private val _state = MutableStateFlow<List<StatusBarNotification>>(emptyList())
        val state: StateFlow<List<StatusBarNotification>> = _state.asStateFlow()

        // 监听服务是否真正连接（绑定）。系统在应用更新/恢复备份后会解绑监听器，
        // 此时权限仍显示已授予，但服务并未连接，导致列表为空。UI 据此自愈重连。
        private val _connected = MutableStateFlow(false)
        val connected: StateFlow<Boolean> = _connected.asStateFlow()
    }
}
