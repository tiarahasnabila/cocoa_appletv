package com.coocaatv.applelauncher

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.Prayer
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppleTvLauncherScreen()
        }
    }
}

@Composable
fun AppleTvLauncherScreen() {
    val context = LocalContext.current
    val apps = remember { getInstalledApps(context.packageManager) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1C1E))
    ) {
        Column(modifier = Modifier.padding(32.dp)) {
            PrayerTimeWidget()

            Spacer(modifier = Modifier.height(48.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(apps) { app ->
                    AppIcon(app)
                }
            }
        }
    }
}

private val jakartaZone: TimeZone = TimeZone.getTimeZone("Asia/Jakarta")

private fun prayerLabel(prayer: Prayer): String = when (prayer) {
    Prayer.FAJR -> "Subuh"
    Prayer.SUNRISE -> "Terbit"
    Prayer.DHUHR -> "Dzuhur"
    Prayer.ASR -> "Ashar"
    Prayer.MAGHRIB -> "Maghrib"
    Prayer.ISHA -> "Isya"
    else -> "Subuh"
}

private fun prayerTimesFor(cal: Calendar): PrayerTimes {
    val coordinates = Coordinates(-7.7956, 110.3695) // Yogyakarta
    val date = DateComponents(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH)
    )
    // Muhammadiyah: Subuh 18, Isya 18
    val params = CalculationMethod.OTHER.parameters.apply {
        fajrAngle = 18.0
        ishaAngle = 18.0
    }
    return PrayerTimes(coordinates, date, params)
}

@Composable
fun PrayerTimeWidget() {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }

    // Update tiap 30 detik
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = System.currentTimeMillis()
        }
    }

    val (prayerName, timeString) = remember(now) {
        val cal = Calendar.getInstance(jakartaZone).apply { timeInMillis = now }
        val today = prayerTimesFor(cal)
        val next = today.nextPrayer(Date(now))

        val targetPrayer: Prayer
        val targetTime: Date
        if (next == Prayer.NONE) {
            // Sudah lewat Isya, tampilkan Subuh besok
            val tomorrow = Calendar.getInstance(jakartaZone).apply {
                timeInMillis = now
                add(Calendar.DAY_OF_MONTH, 1)
            }
            targetPrayer = Prayer.FAJR
            targetTime = prayerTimesFor(tomorrow).fajr
        } else {
            targetPrayer = next
            targetTime = today.timeForPrayer(next)
        }

        val fmt = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
            timeZone = jakartaZone
        }
        Pair(prayerLabel(targetPrayer), fmt.format(targetTime))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .padding(24.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Jadwal Sholat Yogyakarta (Muhammadiyah)",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Menjelang $prayerName",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                timeString,
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Light
            )
        }
    }
}

data class AppInfo(val name: String, val packageName: String, val intent: Intent)

fun getInstalledApps(pm: PackageManager): List<AppInfo> {
    val intent = Intent(Intent.ACTION_MAIN, null).apply {
        addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
    }
    val resolveInfos = pm.queryIntentActivities(intent, 0)
    return resolveInfos.map {
        val appIntent = Intent(intent).apply {
            setClassName(it.activityInfo.packageName, it.activityInfo.name)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }
        AppInfo(
            name = it.loadLabel(pm).toString(),
            packageName = it.activityInfo.packageName,
            intent = appIntent
        )
    }
}

@Composable
fun AppIcon(appInfo: AppInfo) {
    val context = LocalContext.current
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFocused) 1.15f else 1.0f, label = "focusScale")

    Box(
        modifier = Modifier
            .aspectRatio(1.5f)
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isFocused) Color(0xFFE5E5EA) else Color.DarkGray.copy(alpha = 0.5f))
            .clickable { context.startActivity(appInfo.intent) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = appInfo.name,
            color = if (isFocused) Color.Black else Color.White,
            fontWeight = FontWeight.Medium,
            fontSize = 18.sp,
            modifier = Modifier.padding(16.dp)
        )
    }
}
