package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SecurityDatabase
import com.example.data.SecurityLog
import com.example.data.SecurityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SecurityViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SecurityRepository
    
    // Reactive streams from database
    val securityLogs: StateFlow<List<SecurityLog>>
    private val _settingsMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val settingsMap = _settingsMap.asStateFlow()

    // Interactive simulator interface states
    private val _currentAuthMethod = MutableStateFlow(0) // 0: Fingerprint, 1: Face, 2: Iris, 3: Voice, 4: Pattern, 5: Password
    val currentAuthMethod = _currentAuthMethod.asStateFlow()

    private val _isSystemLocked = MutableStateFlow(true)
    val isSystemLocked = _isSystemLocked.asStateFlow()

    // Form inputs and feedback
    private val _passwordInput = MutableStateFlow("")
    val passwordInput = _passwordInput.asStateFlow()

    private val _patternInput = MutableStateFlow<List<Int>>(emptyList())
    val patternInput = _patternInput.asStateFlow()

    private val _verificationSuccess = MutableStateFlow(false)
    val verificationSuccess = _verificationSuccess.asStateFlow()

    private val _verificationError = MutableStateFlow(false)
    val verificationError = _verificationError.asStateFlow()

    private val _statusMessage = MutableStateFlow("الدرع الأمني متصل وجاهز للتحقق")
    val statusMessage = _statusMessage.asStateFlow()

    init {
        val database = SecurityDatabase.getDatabase(application)
        repository = SecurityRepository(database.securityDao())
        
        securityLogs = repository.allLogs.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
        // Load settings and seed defaults if empty
        viewModelScope.launch {
            repository.allSettings.collect { settings ->
                if (settings.isEmpty()) {
                    seedDefaultSettings()
                } else {
                    _settingsMap.value = settings.associate { it.key to it.value }
                }
            }
        }
    }

    private suspend fun seedDefaultSettings() {
        val defaults = mapOf(
            "fingerprint_enabled" to "true",
            "face_enabled" to "true",
            "iris_enabled" to "false",
            "voice_enabled" to "true",
            "pattern_enabled" to "true",
            "password_enabled" to "true",
            "saved_password" to "1234",
            "saved_pattern" to "0,1,2", // simple L-shape/bar
            "saved_voice_phrase" to "افتح تطبيقي الخاص الآمن بمحاكي التحقق البيومتري",
            "system_lock_activated" to "true"
        )
        defaults.forEach { (k, v) ->
            repository.saveSetting(k, v)
        }
    }

    // Toggle active authenticator
    fun toggleSetting(key: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.saveSetting(key, enabled.toString())
            repository.insertLog(
                methodName = "تنظيم الأمان",
                status = "تنبيه",
                details = "تم ${if (enabled) "تفعيل" else "تعطيل"} وسيلة الكشف: $key"
            )
        }
    }

    // Change registered credential text or code
    fun updateCredential(key: String, value: String) {
        viewModelScope.launch {
            repository.saveSetting(key, value)
            repository.insertLog(
                methodName = "تحديث الاعتماد",
                status = "تنبيه",
                details = "تم تحديث رمز/اعتماد الأمان للمفتاح: $key"
            )
        }
    }

    fun selectAuthMethod(index: Int) {
        _currentAuthMethod.value = index
        _verificationSuccess.value = false
        _verificationError.value = false
        _passwordInput.value = ""
        _patternInput.value = emptyList()
        _statusMessage.value = when(index) {
            0 -> "يرجى لمس المستشعر البيومتري لبصمة الإصبع أسفله..."
            1 -> "يرجى توجيه الوجه نحو إطار الكاميرا الهولوغرافي للمطابقة..."
            2 -> "يرجى تركيز الكينونة البصرية (بصمة العين) مع مستشعر القزحية..."
            3 -> "نظام الصوت نشط. تفضل بنطق الجملة الآمنية بوضوح..."
            4 -> "يرجى رسم نمط الحماية الذي قمت بتسجيله مسبقاً..."
            else -> "يرجى إدخال كلمة المرور الرقمية/النصية الآمنة لتأكيد الهوية..."
        }
    }

    // Triggered on successfully pressing biometrics mock sensor
    fun verifyBiometricScan(methodName: String) {
        viewModelScope.launch {
            // Check if enabled in settings
            val isEnabled = when (methodName) {
                "بصمة الإصبع" -> _settingsMap.value["fingerprint_enabled"] ?: "true"
                "التعرف على الوجه" -> _settingsMap.value["face_enabled"] ?: "true"
                "بصمة العين" -> _settingsMap.value["iris_enabled"] ?: "false"
                else -> "false"
            }.toBoolean()

            if (!isEnabled) {
                _verificationError.value = true
                _statusMessage.value = "فشل التحقق: تم تعطيل وسيلة الكشف البيومترية المعنية من الإعدادات!"
                repository.insertLog(
                    methodName = methodName,
                    status = "فشل",
                    details = "محاولة تحقق بيومترية معطلة في الإعدادات"
                )
                return@launch
            }

            _verificationSuccess.value = true
            _isSystemLocked.value = false
            _statusMessage.value = "تمت مطابقة $methodName بنجاح! تم إلغاء قفل النظام."
            repository.insertLog(
                methodName = methodName,
                status = "ناجح",
                details = "تم الكشف البيومتري بنجاح ومطابقة الهوية بنسبة 99.8%"
            )
        }
    }

    // Voice verification
    fun verifyVoiceScan() {
        viewModelScope.launch {
            val isEnabled = (_settingsMap.value["voice_enabled"] ?: "true").toBoolean()
            if (!isEnabled) {
                _verificationError.value = true
                _statusMessage.value = "فشل التحقق: وسيلة بصمة الصوت معطلة في الإعدادات!"
                repository.insertLog(
                    methodName = "بصمة الصوت",
                    status = "فشل",
                    details = "محاولة كشف صوتي والوسيلة معطلة"
                )
                return@launch
            }

            _verificationSuccess.value = true
            _isSystemLocked.value = false
            _statusMessage.value = "تمت مطابقة بصمة الصوت ومستويات التردد بنجاح! تم إلغاء القفل."
            repository.insertLog(
                methodName = "بصمة الصوت",
                status = "ناجح",
                details = "تطابق صوتي معالج بالخصائص الحيوية بنسبة 99.1%"
            )
        }
    }

    // Lock the system anytime
    fun lockSystem() {
        _isSystemLocked.value = true
        _verificationSuccess.value = false
        _verificationError.value = false
        _passwordInput.value = ""
        _patternInput.value = emptyList()
        _statusMessage.value = "تم تنشيط قفل الدرع الأمني يدوياً."
        viewModelScope.launch {
            repository.insertLog(
                methodName = "تأمين معزز",
                status = "تنبيه",
                details = "تم تفعيل القفل المركزي لتأمين الأصول والبيانات"
            )
        }
    }

    // Verify Password input
    fun verifyPasswordText(password: String) {
        _passwordInput.value = password
        val isEnabled = (_settingsMap.value["password_enabled"] ?: "true").toBoolean()
        if (!isEnabled) {
            _verificationError.value = true
            _statusMessage.value = "كلمة المرور معطلة حالياً في نظام الحماية التفضيلية!"
            return
        }

        val savedPassword = _settingsMap.value["saved_password"] ?: "1234"
        if (password == savedPassword) {
            _verificationSuccess.value = true
            _verificationError.value = false
            _isSystemLocked.value = false
            _statusMessage.value = "كلمة المرور صحيحة! تم فك قفل النظام."
            viewModelScope.launch {
                repository.insertLog(
                    methodName = "كلمة المرور",
                    status = "ناجح",
                    details = "تسجيل دخول ناجح بمطابقة التشفير والتحقق الآمن"
                )
            }
        } else {
            if (password.length >= savedPassword.length) {
                _verificationError.value = true
                _statusMessage.value = "فشل التحقق: كلمة المرور المدخلة غير صحيحة!"
                viewModelScope.launch {
                    repository.insertLog(
                        methodName = "كلمة المرور",
                        status = "فشل",
                        details = "محاولة حظر ناتجة عن كلمة مرور غير صحيحة"
                    )
                }
            }
        }
    }

    // Verify Pattern drawn list
    fun verifyPatternList(patternNodes: List<Int>) {
        _patternInput.value = patternNodes
        val isEnabled = (_settingsMap.value["pattern_enabled"] ?: "true").toBoolean()
        if (!isEnabled) {
            _verificationError.value = true
            _statusMessage.value = "قفل النمط معطل في إعدادات الحماية المركزية!"
            return
        }

        val savedPatternString = _settingsMap.value["saved_pattern"] ?: "0,1,2"
        val savedPatternNodes = savedPatternString.split(",").mapNotNull { it.trim().toIntOrNull() }
        
        if (patternNodes == savedPatternNodes) {
            _verificationSuccess.value = true
            _verificationError.value = false
            _isSystemLocked.value = false
            _statusMessage.value = "النمط مطابق تماماً! تم إلغاء القفل."
            viewModelScope.launch {
                repository.insertLog(
                    methodName = "قفل النمط",
                    status = "ناجح",
                    details = "رسم النمط طابق الكود الرقمي بنجاح"
                )
            }
        } else {
            _verificationError.value = true
            _statusMessage.value = "النمط المرسوم غير صحيح! حاول مرة أخرى."
            viewModelScope.launch {
                repository.insertLog(
                    methodName = "قفل النمط",
                    status = "فشل",
                    details = "محاولة فك حظر بالنمط باءت بالفشل (طول النمط: ${patternNodes.size})"
                )
            }
        }
    }

    // Clear logs completely from the Room Database
    fun clearLogs() {
        viewModelScope.launch {
            repository.clearAllLogs()
            repository.insertLog(
                methodName = "سجل الأمان",
                status = "تنبيه",
                details = "قام المستخدم بمسح سجل العمليات والتحقق بالكامل"
            )
        }
    }
}
