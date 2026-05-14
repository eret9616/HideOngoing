package dev.local.hideongoing

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Screen()
                }
            }
        }
    }
}

@Composable
private fun Screen() {
    val ctx = LocalContext.current
    val store = remember { RuleStore.get(ctx) }
    val rules by store.rules.collectAsState()
    val actives by NotificationListener.state.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    var enabled by remember {
        mutableStateOf(
            NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)
        )
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                enabled = NotificationManagerCompat.getEnabledListenerPackages(ctx)
                    .contains(ctx.packageName)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        if (!enabled) {
            Text("先去授权:")
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }) { Text("打开通知使用权设置") }
            return@Column
        }

        Text("活跃常驻通知", style = MaterialTheme.typography.titleMedium)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(actives) { sbn ->
                val ch = sbn.notification?.channelId
                val title = sbn.notification?.extras
                    ?.getCharSequence("android.title")?.toString().orEmpty()
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        "${sbn.packageName} [${ch ?: "-"}]",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (title.isNotEmpty()) {
                        Text(title, style = MaterialTheme.typography.bodySmall)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = {
                            store.add(Rule(sbn.packageName, ch))
                            NotificationListener.live?.enforceNow(sbn.packageName, ch)
                        }) { Text("屏蔽 [channel]") }
                        TextButton(onClick = {
                            store.add(Rule(sbn.packageName, null))
                            NotificationListener.live?.enforceNow(sbn.packageName, null)
                        }) { Text("屏蔽 [app]") }
                    }
                    HorizontalDivider()
                }
            }
        }

        HorizontalDivider()
        Text("已屏蔽规则", style = MaterialTheme.typography.titleMedium)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(rules.toList()) { r ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        "${r.pkg} [${r.channelId ?: "整个 app"}]",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(onClick = {
                        store.remove(r)
                        NotificationListener.live?.liftRule(r.pkg, r.channelId)
                    }) { Text("解除") }
                }
            }
        }
    }
}
