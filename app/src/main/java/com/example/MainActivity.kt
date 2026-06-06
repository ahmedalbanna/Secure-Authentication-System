package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.SecurityViewModel
import com.example.ui.components.CameraScannerView
import com.example.ui.components.FingerprintSensorView
import com.example.ui.components.PatternLockView
import com.example.ui.components.VoiceAuthenticatorView
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Force Right-to-Left (RTL) layout direction to support perfect Arabic designs natively
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        containerColor = MaterialTheme.colorScheme.background
                    ) { innerPadding ->
                        SecurityHubDashboard(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityHubDashboard(
    modifier: Modifier = Modifier,
    viewModel: SecurityViewModel = viewModel()
) {
    val isLocked by viewModel.isSystemLocked.collectAsState()
    val logs by viewModel.securityLogs.collectAsState()
    val settings by viewModel.settingsMap.collectAsState()
    val activeTabIdx = remember { mutableStateOf(0) } // 0: Scanners, 1: History Logs, 2: Control Hub

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // HEADER SUMMARY BLOCK (Premium light blue/slate gray custom header)
        HeaderSecurityStatus(
            isLocked = isLocked,
            securityRating = calculateSecurityScore(settings),
            onLockTriggered = { viewModel.lockSystem() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // NAVIGATION TABS DESIGNED WITH THE PROFESSIONAL POLISH TAB STYLE (White card with outline, active state is #DDE2F1 pill and blue text/icon)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tabTitles = listOf("محاكاة الكشف", "سجلات الأمان", "إعدادات الأمان")
                val tabIcons = listOf(Icons.Default.SettingsInputAntenna, Icons.Default.ListAlt, Icons.Default.AdminPanelSettings)
                
                tabTitles.forEachIndexed { index, title ->
                    val isActive = activeTabIdx.value == index
                    val activeBg = Color(0xFFDDE2F1)        // custom light blue accent
                    val activeContent = Color(0xFF005AC1)   // royal brand blue
                    val inactiveContent = Color(0xFF74777F) // slate gray

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isActive) activeBg else Color.Transparent)
                            .clickable { activeTabIdx.value = index }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = tabIcons[index],
                                contentDescription = title,
                                tint = if (isActive) activeContent else inactiveContent,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = title,
                                color = if (isActive) activeContent else inactiveContent,
                                fontSize = 11.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // CENTRAL DYNAMIC WRAPPER
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (activeTabIdx.value) {
                0 -> AuthenticationWorkspace(viewModel = viewModel, settings = settings)
                1 -> SecurityLogsHistory(logs = logs, onClearAll = { viewModel.clearLogs() })
                else -> SecurityControlSettings(viewModel = viewModel, settings = settings)
            }
        }
    }
}

@Composable
fun HeaderSecurityStatus(
    isLocked: Boolean,
    securityRating: Int,
    onLockTriggered: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE1E2EC)),
        border = BorderStroke(1.dp, Color(0xFFC3C6CF).copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isLocked) Color(0xFFEF4444) else Color(0xFF10B981))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isLocked) "حالة النظام: مقفل ومؤمن" else "حالة النظام: آمن وتم فك القفل",
                        color = Color(0xFF1B1B1F),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "مستوى التقييم الأمني للحماية: $securityRating%",
                    color = if (securityRating >= 80) Color(0xFF00875A) else if (securityRating >= 50) Color(0xFFD07000) else Color(0xFFEF4444),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "التوقيت الأمني: 2026-06-06 (UTC)",
                    color = Color(0xFF74777F),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            IconButton(
                onClick = onLockTriggered,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(if (isLocked) Color(0xFFEF4444).copy(alpha = 0.15f) else Color(0xFF005AC1).copy(alpha = 0.12f))
            ) {
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = "قفل فوري",
                    tint = if (isLocked) Color(0xFFEF4444) else Color(0xFF005AC1),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun AuthenticationWorkspace(
    viewModel: SecurityViewModel,
    settings: Map<String, String>
) {
    val activeMethod by viewModel.currentAuthMethod.collectAsState()
    val isLocked by viewModel.isSystemLocked.collectAsState()
    val successState by viewModel.verificationSuccess.collectAsState()
    val errorState by viewModel.verificationError.collectAsState()
    val statusMsg by viewModel.statusMessage.collectAsState()
    val pwdInput by viewModel.passwordInput.collectAsState()

    val sensorTitles = listOf("بصمة الإصبع", "بصمة الوجه", "بصمة العين", "بصمة الصوت", "قفل النمط", "كلمة المرور")
    val sensorIcons = listOf(
        Icons.Default.Fingerprint,
        Icons.Default.Face,
        Icons.Default.RemoveRedEye,
        Icons.Default.Mic,
        Icons.Default.Pattern,
        Icons.Default.Keyboard
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Horizontal Selector for Authenticators
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            verticalArrangement = Arrangement.Center
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sensorTitles.forEachIndexed { index, title ->
                        val isSelected = activeMethod == index
                        val isEnabled = when (index) {
                            0 -> settings["fingerprint_enabled"] ?: "true"
                            1 -> settings["face_enabled"] ?: "true"
                            2 -> settings["iris_enabled"] ?: "false"
                            3 -> settings["voice_enabled"] ?: "true"
                            4 -> settings["pattern_enabled"] ?: "true"
                            else -> settings["password_enabled"] ?: "true"
                        }.toBoolean()

                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectAuthMethod(index) },
                            label = {
                                Text(
                                    text = title,
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color.White else if (isEnabled) Color(0xFF1B1B1F) else Color(0xFF74777F)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF005AC1),
                                containerColor = if (isEnabled) Color(0xFFDDE2F1) else Color(0xFFE1E2EC).copy(alpha = 0.6f)
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = sensorIcons[index],
                                    contentDescription = title,
                                    tint = if (isSelected) Color.White else if (isEnabled) Color(0xFF005AC1) else Color(0xFF74777F),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // FEEDBACK STATUS CONSOLE
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (successState) Color(0xFF10B981).copy(alpha = 0.1f)
                                 else if (errorState) Color(0xFFEF4444).copy(alpha = 0.1f)
                                 else Color.White
            ),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(
                1.dp,
                if (successState) Color(0xFF10B981)
                else if (errorState) Color(0xFFEF4444)
                else Color(0xFFC3C6CF).copy(alpha = 0.5f)
            )
        ) {
            Text(
                text = statusMsg,
                color = if (successState) Color(0xFF00875A) else if (errorState) Color(0xFFEF4444) else Color(0xFF1B1B1F),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // DISPLAY CORRESPONDING SCANNER
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            when (activeMethod) {
                0 -> {
                    // Fingerprint Scan Simulator
                    FingerprintSensorView(
                        isScanningActive = isLocked && !successState,
                        successState = successState,
                        onFingerprintScan = { viewModel.verifyBiometricScan("بصمة الإصبع") }
                    )
                }
                1 -> {
                    // Face Scan Simulator
                    CameraScannerView(
                        scannerType = "FACE",
                        isSuccess = successState,
                        onScanFinished = { viewModel.verifyBiometricScan("التعرف على الوجه") }
                    )
                }
                2 -> {
                    // Iris Scan Simulator
                    CameraScannerView(
                        scannerType = "IRIS",
                        isSuccess = successState,
                        onScanFinished = { viewModel.verifyBiometricScan("بصمة العين") }
                    )
                }
                3 -> {
                    // Voice Biometrics Simulator
                    val voiceWord = settings["saved_voice_phrase"] ?: "افتح تطبيقي الخاص الآمن بمحاكي التحقق البيومتري"
                    VoiceAuthenticatorView(
                        isSuccess = successState,
                        passphrase = voiceWord,
                        onVoiceMatch = { viewModel.verifyVoiceScan() }
                    )
                }
                4 -> {
                    // Custom Pattern Lock view (Using fully interactive drag nodes)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "قم برسم نمط التوصيل (النمط المسجل الافتراضي: شريط أفقي علوي)",
                            color = Color(0xFF74777F),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        PatternLockView(
                            modifier = Modifier
                                .size(240.dp)
                                .clip(RoundedCornerShape(24.dp)),
                            isError = errorState,
                            onPatternComplete = { patternList ->
                                viewModel.verifyPatternList(patternList)
                            }
                        )
                    }
                }
                5 -> {
                    // Password input simulator
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "الرقم السري الافتراضي: 1234",
                            color = Color(0xFF74777F),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = pwdInput,
                            onValueChange = { viewModel.verifyPasswordText(it) },
                            label = { Text("رمز المرور الآمن") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF005AC1),
                                unfocusedBorderColor = Color(0xFFC3C6CF),
                                focusedLabelColor = Color(0xFF005AC1),
                                unfocusedLabelColor = Color(0xFF74777F),
                                focusedTextColor = Color(0xFF1B1B1F),
                                unfocusedTextColor = Color(0xFF1B1B1F)
                            ),
                            modifier = Modifier.fillMaxWidth(0.85f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityLogsHistory(
    logs: List<com.example.data.SecurityLog>,
    onClearAll: () -> Unit
) {
    val fmt = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "سجل الأنشطة والولوج الآمن",
                color = Color(0xFF1B1B1F),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            TextButton(
                onClick = onClearAll,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("مسح السجلات", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.Inbox, contentDescription = "", modifier = Modifier.size(52.dp), tint = Color(0xFF74777F))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("السجل فارغ تماماً حالياً", color = Color(0xFF74777F), fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color(0xFFC3C6CF).copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Method Icon
                            val icon = when (log.methodName) {
                                "بصمة الإصبع" -> Icons.Default.Fingerprint
                                "التعرف على الوجه" -> Icons.Default.Face
                                "بصمة العين" -> Icons.Default.RemoveRedEye
                                "بصمة الصوت" -> Icons.Default.Mic
                                "قفل النمط" -> Icons.Default.Pattern
                                "كلمة المرور" -> Icons.Default.Keyboard
                                "تنظيم الأمان" -> Icons.Default.Settings
                                else -> Icons.Default.Shield
                            }
                            
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDDE2F1)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = "",
                                    tint = Color(0xFF005AC1),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = log.methodName,
                                    color = Color(0xFF1B1B1F),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = log.details,
                                    color = Color(0xFF44474E),
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = fmt.format(Date(log.timestamp)),
                                    color = Color(0xFF74777F),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Status badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when (log.status) {
                                            "ناجح" -> Color(0xFF10B981).copy(alpha = 0.15f)
                                            "فشل" -> Color(0xFFEF4444).copy(alpha = 0.15f)
                                            else -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                        }
                                    )
                                    .padding(vertical = 6.dp, horizontal = 12.dp)
                            ) {
                                Text(
                                    text = log.status,
                                    color = when (log.status) {
                                        "ناجح" -> Color(0xFF00875A)
                                        "فشل" -> Color(0xFFEF4444)
                                        else -> Color(0xFFD07000)
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityControlSettings(
    viewModel: SecurityViewModel,
    settings: Map<String, String>
) {
    var editPwdOpen by remember { mutableStateOf(false) }
    var newPwdStr by remember { mutableStateOf("") }

    var editPtnOpen by remember { mutableStateOf(false) }
    var newPtnStr by remember { mutableStateOf("") }

    var editVoiceOpen by remember { mutableStateOf(false) }
    var newVoiceStr by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "التحكم في وسائل التحقق البيومترية النشطة",
                color = Color(0xFF1B1B1F),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        // Toggles mapping
        val keys = listOf(
            "fingerprint_enabled" to "تفعيل مستشعر بصمة الإصبع",
            "face_enabled" to "تفعيل فحص الوجه ثلاثي الأبعاد",
            "iris_enabled" to "تفعيل كاشف قزحية العين المستقل",
            "voice_enabled" to "تفعيل مستشعر مطابقة التردد الصوتي",
            "pattern_enabled" to "تمكين قفل رسم النمط",
            "password_enabled" to "تمكين كلمة المرور التنافسية الدقيقة"
        )

        items(keys) { (key, label) ->
            val isChecked = settings[key]?.toBoolean() ?: (key != "iris_enabled")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFC3C6CF).copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = label, color = Color(0xFF1B1B1F), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = isChecked,
                        onCheckedChange = { viewModel.toggleSetting(key, !isChecked) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF005AC1),
                            uncheckedThumbColor = Color(0xFF74777F),
                            uncheckedTrackColor = Color(0xFFE1E2EC)
                        )
                    )
                }
            }
        }

        // Custom Credentials Admin Section
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "تحديث البيانات المرجعية الحساسة",
                color = Color(0xFF1B1B1F),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        // 1. Password change
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFC3C6CF).copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("تعديل كلمة مرور النظام", color = Color(0xFF1B1B1F), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("الرمز الحالي: ${settings["saved_password"] ?: "1234"}", color = Color(0xFF74777F), fontSize = 12.sp)
                        }
                        Button(
                            onClick = { editPwdOpen = !editPwdOpen },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005AC1)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("تغيير", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (editPwdOpen) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newPwdStr,
                                onValueChange = { newPwdStr = it },
                                label = { Text("الرمز الجديد", fontSize = 11.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF005AC1),
                                    unfocusedBorderColor = Color(0xFFC3C6CF),
                                    focusedLabelColor = Color(0xFF005AC1)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newPwdStr.isNotEmpty()) {
                                        viewModel.updateCredential("saved_password", newPwdStr)
                                        newPwdStr = ""
                                        editPwdOpen = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("حفظ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 2. Pattern Nodes change
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFC3C6CF).copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("تعديل النمط الرقمي المرجعي", color = Color(0xFF1B1B1F), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("القيم الممثلة: ${settings["saved_pattern"] ?: "0,1,2"}", color = Color(0xFF74777F), fontSize = 12.sp)
                        }
                        Button(
                            onClick = { editPtnOpen = !editPtnOpen },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005AC1)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("تغيير", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (editPtnOpen) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newPtnStr,
                                onValueChange = { newPtnStr = it },
                                label = { Text("نموذج: 0,1,2,5 (من 0 إلى 8)", fontSize = 11.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF005AC1),
                                    unfocusedBorderColor = Color(0xFFC3C6CF),
                                    focusedLabelColor = Color(0xFF005AC1)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newPtnStr.isNotEmpty()) {
                                        viewModel.updateCredential("saved_pattern", newPtnStr)
                                        newPtnStr = ""
                                        editPtnOpen = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("حفظ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 3. Voice Phrase change
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFC3C6CF).copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تعديل العبارة الصوتية الآمنة", color = Color(0xFF1B1B1F), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "العبارة: “${settings["saved_voice_phrase"] ?: "افتح تطبيقي الخاص" }”",
                                color = Color(0xFF74777F),
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { editVoiceOpen = !editVoiceOpen },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005AC1)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("تغيير", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (editVoiceOpen) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newVoiceStr,
                                onValueChange = { newVoiceStr = it },
                                label = { Text("العبارة الصوتية الجديدة", fontSize = 11.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF005AC1),
                                    unfocusedBorderColor = Color(0xFFC3C6CF),
                                    focusedLabelColor = Color(0xFF005AC1)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newVoiceStr.isNotEmpty()) {
                                        viewModel.updateCredential("saved_voice_phrase", newVoiceStr)
                                        newVoiceStr = ""
                                        editVoiceOpen = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("حفظ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Calculate secure index based on toggles
private fun calculateSecurityScore(settings: Map<String, String>): Int {
    var score = 10
    if (settings["fingerprint_enabled"]?.toBoolean() == true) score += 15
    if (settings["face_enabled"]?.toBoolean() == true) score += 15
    if (settings["iris_enabled"]?.toBoolean() == true) score += 20
    if (settings["voice_enabled"]?.toBoolean() == true) score += 15
    if (settings["pattern_enabled"]?.toBoolean() == true) score += 10
    if (settings["password_enabled"]?.toBoolean() == true) score += 15
    return score.coerceAtMost(100)
}
