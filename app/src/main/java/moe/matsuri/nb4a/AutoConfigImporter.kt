package moe.matsuri.nb4a

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.SagerNet
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.database.ProxyEntity
import io.nekohasekai.sagernet.database.SagerDatabase
import io.nekohasekai.sagernet.fmt.UniversalFmt
import io.nekohasekai.sagernet.ktx.Logs
import io.nekohasekai.sagernet.ktx.runOnDefaultDispatcher
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.random.Random

/**
 * کلاس ایمپورت خودکار کانفیگ از ریپازیتوری‌های رندوم
 * شامل آنتی‌اسپم، مدیریت کانفیگ‌های دائمی و تست پینگ
 */
class AutoConfigImporter(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "auto_config_importer"
        private const val KEY_DAILY_REQUESTS = "daily_requests"
        private const val KEY_LAST_REQUEST_DATE = "last_request_date"
        private const val MAX_DAILY_REQUESTS = 5
        private const val MIN_HEALTHY_CONFIGS = 3
        private const val REQUEST_TIMEOUT = 30L // seconds
        
        // لیست ریپازیتوری‌های پیش‌فرض
        private val DEFAULT_REPOSITORIES = listOf(
            "https://raw.githubusercontent.com/barry-far/V2ray-Configs/main/Splitted-By-Protocol/vmess.txt",
            "https://raw.githubusercontent.com/free-server/vless/main/vless.txt",
            "https://raw.githubusercontent.com/IranianCypherpunks/sub/main/config/mix",
            "https://raw.githubusercontent.com/soroushmirzaei/telegram-configs-collector/main/protocols/vmess",
            "https://raw.githubusercontent.com/ALIILAPRO/v2rayNG-Config/main/server.txt"
        )
        
        // پیشوندهای کانفیگ‌های دائمی
        private val PERMANENT_CONFIG_PREFIXES = listOf(
            "manual_config_",
            "user_defined_",
            "custom_",
            "permanent_"
        )
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
        .build()
    
    /**
     * بررسی و اجرای ایمپورت خودکار در صورت نیاز
     */
    suspend fun checkAndImportIfNeeded(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // بررسی آنتی‌اسپم
                if (!checkAntiSpam()) {
                    Logs.w("AutoConfigImporter: Daily request limit exceeded")
                    return@withContext false
                }
                
                // بررسی تعداد کانفیگ‌های سالم
                val healthyCount = getHealthyConfigsCount()
                if (healthyCount >= MIN_HEALTHY_CONFIGS) {
                    Logs.d("AutoConfigImporter: Sufficient healthy configs ($healthyCount)")
                    return@withContext false
                }
                
                // نمایش پیام "در حال دریافت..."
                showLoadingDialog()
                
                // ایمپورت کانفیگ‌های جدید
                val success = importConfigsFromRepositories()
                
                // ثبت درخواست
                recordRequest()
                
                return@withContext success
                
            } catch (e: Exception) {
                Logs.e("AutoConfigImporter error", e)
                return@withContext false
            }
        }
    }
    
    /**
     * ایمپورت دستی کانفیگ‌ها
     */
    suspend fun manualImport(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // بررسی آنتی‌اسپم
                if (!checkAntiSpam()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context, 
                            "حداکثر ${MAX_DAILY_REQUESTS} درخواست در روز مجاز است", 
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@withContext false
                }
                
                // نمایش پیام "در حال دریافت..."
                showLoadingDialog()
                
                val success = importConfigsFromRepositories()
                recordRequest()
                
                withContext(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(context, "کانفیگ‌های جدید با موفقیت دریافت شدند", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "خطا در دریافت کانفیگ‌ها", Toast.LENGTH_SHORT).show()
                    }
                }
                
                return@withContext success
                
            } catch (e: Exception) {
                Logs.e("AutoConfigImporter manual import error", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "خطا در دریافت کانفیگ‌ها: ${e.message}", Toast.LENGTH_LONG).show()
                }
                return@withContext false
            }
        }
    }
    
    /**
     * بررسی آنتی‌اسپم (حداکثر 5 درخواست در روز)
     */
    private fun checkAntiSpam(): Boolean {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastRequestDate = prefs.getString(KEY_LAST_REQUEST_DATE, "")
        
        if (today != lastRequestDate) {
            // روز جدید - ریست شمارنده
            prefs.edit()
                .putString(KEY_LAST_REQUEST_DATE, today)
                .putInt(KEY_DAILY_REQUESTS, 0)
                .apply()
            return true
        }
        
        val dailyRequests = prefs.getInt(KEY_DAILY_REQUESTS, 0)
        return dailyRequests < MAX_DAILY_REQUESTS
    }
    
    /**
     * ثبت درخواست برای آنتی‌اسپم
     */
    private fun recordRequest() {
        val currentRequests = prefs.getInt(KEY_DAILY_REQUESTS, 0)
        prefs.edit()
            .putInt(KEY_DAILY_REQUESTS, currentRequests + 1)
            .apply()
    }
    
    /**
     * شمارش کانفیگ‌های سالم (ping < 1000ms)
     */
    private fun getHealthyConfigsCount(): Int {
        return try {
            SagerDatabase.proxyDao.allProxies().count { proxy ->
                proxy.status == 1 && proxy.ping > 0 && proxy.ping < 1000
            }
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * نمایش دیالوگ "در حال دریافت..."
     */
    private fun showLoadingDialog() {
        runOnDefaultDispatcher {
            withContext(Dispatchers.Main) {
                val dialog = AlertDialog.Builder(context)
                    .setTitle("دریافت کانفیگ")
                    .setMessage("در حال دریافت کانفیگ‌های جدید...")
                    .setCancelable(false)
                    .create()
                
                dialog.show()
                
                // بستن دیالوگ بعد از 5 ثانیه
                delay(5000)
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
            }
        }
    }
    
    /**
     * ایمپورت کانفیگ‌ها از ریپازیتوری‌ها
     */
    private suspend fun importConfigsFromRepositories(): Boolean {
        val repositories = DEFAULT_REPOSITORIES.shuffled() // رندوم کردن ترتیب
        var successCount = 0
        val importedConfigs = mutableListOf<ProxyEntity>()
        
        for (repo in repositories) {
            try {
                val configs = fetchConfigsFromRepository(repo)
                if (configs.isNotEmpty()) {
                    // حذف تکراری‌ها
                    val uniqueConfigs = removeDuplicateConfigs(configs)
                    importedConfigs.addAll(uniqueConfigs)
                    successCount++
                    
                    // حداکثر 20 کانفیگ از هر ریپازیتوری
                    if (uniqueConfigs.size >= 20) break
                }
            } catch (e: Exception) {
                Logs.w("Failed to fetch from $repo: ${e.message}")
            }
            
            // وقفه کوتاه بین درخواست‌ها
            delay(1000)
        }
        
        if (importedConfigs.isNotEmpty()) {
            // ذخیره کانفیگ‌ها در دیتابیس
            saveConfigsToDatabase(importedConfigs)
            
            // شروع تست پینگ برای کانفیگ‌های جدید
            startPingTestForNewConfigs(importedConfigs)
            
            return true
        }
        
        return false
    }
    
    /**
     * دریافت کانفیگ‌ها از یک ریپازیتوری
     */
    private suspend fun fetchConfigsFromRepository(repoUrl: String): List<ProxyEntity> {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(repoUrl)
                .get()
                .build()
            
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}")
            }
            
            val content = response.body?.string() ?: ""
            val configs = mutableListOf<ProxyEntity>()
            
            // پردازش محتوا و تبدیل به ProxyEntity
            content.lines().forEach { line ->
                val trimmedLine = line.trim()
                if (trimmedLine.isNotEmpty() && !trimmedLine.startsWith("#")) {
                    try {
                        val proxy = UniversalFmt.parseLinksNB(trimmedLine).firstOrNull()
                        if (proxy != null) {
                            proxy.name = "Auto_${Random.nextInt(1000, 9999)}_${proxy.name}"
                            configs.add(proxy)
                        }
                    } catch (e: Exception) {
                        // Skip invalid configs
                    }
                }
            }
            
            configs
        }
    }
    
    /**
     * حذف کانفیگ‌های تکراری
     */
    private fun removeDuplicateConfigs(newConfigs: List<ProxyEntity>): List<ProxyEntity> {
        val existingConfigs = SagerDatabase.proxyDao.allProxies()
        val existingHashes = existingConfigs.map { it.toStandardLink() }.toSet()
        
        return newConfigs.filter { config ->
            config.toStandardLink() !in existingHashes
        }
    }
    
    /**
     * ذخیره کانفیگ‌ها در دیتابیس
     */
    private suspend fun saveConfigsToDatabase(configs: List<ProxyEntity>) {
        withContext(Dispatchers.IO) {
            configs.forEach { config ->
                config.groupId = DataStore.selectedGroupForImport()
                config.id = SagerDatabase.proxyDao.addProxy(config)
            }
        }
    }
    
    /**
     * شروع تست پینگ برای کانفیگ‌های جدید
     */
    private fun startPingTestForNewConfigs(configs: List<ProxyEntity>) {
        runOnDefaultDispatcher {
            delay(2000) // وقفه برای تثبیت
            
            configs.forEach { config ->
                try {
                    // TODO: پیاده‌سازی تست پینگ
                    // در اینجا باید از URL test استفاده کرد
                } catch (e: Exception) {
                    Logs.w("Ping test failed for ${config.name}: ${e.message}")
                }
            }
        }
    }
    
    /**
     * حذف کانفیگ‌های خراب (بعد از 3 بار refresh)
     */
    fun cleanupBadConfigs() {
        runOnDefaultDispatcher {
            try {
                val allProxies = SagerDatabase.proxyDao.allProxies()
                val badProxies = allProxies.filter { proxy ->
                    // کانفیگ‌های با ping < 0 یا status = 2 (خطا)
                    (proxy.ping < 0 || proxy.status == 2) && 
                    !isPermanentConfig(proxy.name) // حذف نکردن کانفیگ‌های دائمی
                }
                
                if (badProxies.isNotEmpty()) {
                    SagerDatabase.proxyDao.deleteProxies(badProxies)
                    Logs.d("Cleaned up ${badProxies.size} bad configs")
                }
            } catch (e: Exception) {
                Logs.e("Error cleaning up bad configs", e)
            }
        }
    }
    
    /**
     * بررسی اینکه آیا کانفیگ دائمی است
     */
    private fun isPermanentConfig(name: String): Boolean {
        return PERMANENT_CONFIG_PREFIXES.any { prefix ->
            name.startsWith(prefix, ignoreCase = true)
        }
    }
}