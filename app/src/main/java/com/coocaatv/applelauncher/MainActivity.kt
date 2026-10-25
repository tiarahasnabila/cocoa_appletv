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
import com.batoolapps.adhan.CalculationMethod
import com.batoolapps.adhan.Coordinates
import com.batoolapps.adhan.PrayerTimes
import com.batoolapps.adhan.data.DateComponents
import java.text.SimpleDateFormat
import java.util.*
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
            .background(Color(0xFF1C1C1E)) // Dark background khas Apple TV
    ) {
        Column(modifier = Modifier.padding(32.dp)) {
            // Widget Sholat Glassmorphism
            PrayerTimeWidget()

            Spacer(modifier = Modifier.height(48.dp))

            // Grid Aplikasi Bawah
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

@Composable
fun PrayerTimeWidget() {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    
    // Update setiap menit
    LaunchedEffect(Unit) {
        while (true) {
            delay(60000)
            currentTime = System.currentTimeMillis()
        }
    }

    // Koordinat Jogja
    val coordinates = Coordinates(-7.7956, 110.3695)
    val date = DateComponents.from(Date())
    // Parameter Muhammadiyah: Subuh 18, Isya 18
    val params = CalculationMethod.OTHER.parameters.apply {
        fajrAngle = 18.0
        ishaAngle = 18.0
    }
    val prayerTimes = PrayerTimes(coordinates, date, params)
    
    val nextPrayer = prayerTimes.nextPrayer()
    val prayerName = nextPrayer?.name ?: "Isya"
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val prayerTimeDate = prayerTimes.timeForPrayer(nextPrayer) ?: prayerTimes.isha
    val timeString = timeFormat.format(prayerTimeDate)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.15f)) // Efek Glassmorphism
            .padding(24.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Jadwal Sholat Yogyakarta (Muhammadiyah)", color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Menjelang $prayerName", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }
            Text(timeString, color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Light)
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
    val scale by animateFloatAsState(if (isFocused) 1.15f else 1.0f) // Efek Zoom ala Apple TV

    Box(
        modifier = Modifier
            .aspectRatio(1.5f) // Persegi panjang membulat
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isFocused) Color(0xFFE5E5EA) else Color.DarkGray.copy(alpha = 0.5f))
            .clickable {
                context.startActivity(appInfo.intent)
            },
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
