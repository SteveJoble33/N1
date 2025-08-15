/**
 * تنظیمات پیش‌فرض NekoBox
 * این فایل برای کانفیگوریشن پیش‌فرض برنامه استفاده می‌شود
 */

const DEFAULT_CONFIG = {
    // تنظیمات Route
    route: {
        blockAds: true,           // فعال‌سازی پیش‌فرض Block ADs
        blockAnalysis: true,      // فعال‌سازی پیش‌فرض Block Analysis
        blockQuic: false,         // غیرفعال‌سازی پیش‌فرض Block QUIC
        iranDomainRule: true,     // فعال‌سازی پیش‌فرض Domain rule for Iran
        chinaDomainRule: false    // غیرفعال‌سازی پیش‌فرض Domain rule for China
    },
    
    // تنظیمات DNS
    dns: {
        remoteDns: "https://dns.google/dns-query",
        directDns: "https://223.5.5.5/dns-query",
        enableDnsRouting: true,
        enableFakeDns: false
    },
    
    // تنظیمات پینگ و تست اتصال
    ping: {
        urlTestUrl: "http://connectivitycheck.gstatic.com/generate_204",
        timeout: 5000,            // میلی‌ثانیه
        pingDisplayDecimals: 1,   // نمایش اعشار یک رقم برای پینگ
        autoRefreshInterval: 30   // ثانیه
    },
    
    // تنظیمات ایمپورت خودکار
    autoImport: {
        enabled: true,
        maxDailyRequests: 5,      // حداکثر 5 درخواست در روز (آنتی‌اسپم)
        minHealthyConfigs: 3,     // حداقل کانفیگ‌های سالم قبل از درخواست جدید
        repositories: [
            "https://raw.githubusercontent.com/barry-far/V2ray-Configs/main/Splitted-By-Protocol/vmess.txt",
            "https://raw.githubusercontent.com/free-server/vless/main/vless.txt",
            "https://raw.githubusercontent.com/IranianCypherpunks/sub/main/config/mix",
            "https://raw.githubusercontent.com/soroushmirzaei/telegram-configs-collector/main/protocols/vmess",
            "https://raw.githubusercontent.com/ALIILAPRO/v2rayNG-Config/main/server.txt"
        ],
        excludePermanent: [
            // کانفیگ‌هایی که هیچ‌وقت حذف نمی‌شوند
            "manual_config_",
            "user_defined_"
        ]
    },
    
    // تنظیمات Fragment
    fragment: {
        enabled: false,
        type: "none",            // none, random, sequence
        length: "100-200",       // اندازه فرگمنت
        interval: "10-20",       // فاصله بین فرگمنت‌ها (میلی‌ثانیه)
        packets: "tlshello"      // نوع packet
    },
    
    // تنظیمات Mux
    mux: {
        enabled: false,
        protocol: "smux",        // smux, yamux, h2mux
        maxConnections: 4,
        minStreams: 4,
        maxStreams: 0,
        padding: false
    },
    
    // تنظیمات Log
    log: {
        level: "warn",          // panic, fatal, error, warn, info, debug, trace
        disableColor: false,
        timestamp: true
    },
    
    // تنظیمات UI
    ui: {
        showBottomBar: true,
        nightTheme: "auto",     // auto, light, dark
        expertMode: false,
        confirmBeforeDelete: true,
        autoCheckUpdates: true
    }
};

// تابع برای دریافت تنظیمات
function getDefaultConfig() {
    return JSON.parse(JSON.stringify(DEFAULT_CONFIG));
}

// تابع برای merge کردن تنظیمات کاربر با پیش‌فرض
function mergeWithDefaults(userConfig) {
    return Object.assign({}, DEFAULT_CONFIG, userConfig);
}

// Export برای استفاده در Android
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        DEFAULT_CONFIG,
        getDefaultConfig,
        mergeWithDefaults
    };
}