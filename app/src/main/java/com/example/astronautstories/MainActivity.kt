package com.example.astronautstories // نام پکیج خودت رو جایگزین کن

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val webView: WebView = findViewById(R.id.webView)

        // فعال کردن جاوا اسکریپت برای اجرای انیمیشن‌ها و منطق بازی
        webView.settings.javaScriptEnabled = true

        // اجازه دسترسی به فایل‌های محلی و منابع محتوایی
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true

        // فعال کردن DOM storage و database برای localStorage و sessionStorage
        webView.settings.domStorageEnabled = true
        webView.settings.databaseEnabled = true

        // اجازه دسترسی یونیورسال از فایل:// به منابع دیگر (نیاز به API 16+)
        try {
            webView.settings.javaClass.getMethod("setAllowFileAccessFromFileURLs", java.lang.Boolean::class.java)
            webView.settings.javaClass.getMethod("setAllowUniversalAccessFromFileURLs", java.lang.Boolean::class.java)
            // اگر متدها وجود داشته باشند، با بازتاب صدا بزن
            webView.settings.javaClass.getMethod("setAllowFileAccessFromFileURLs", java.lang.Boolean::class.java)
            webView.settings.allowFileAccessFromFileURLs = true
            webView.settings.allowUniversalAccessFromFileURLs = true
        } catch (ignored: Exception) {
            // برخی ورژن‌ها این متدها را ندارند؛ نادیده می‌گیریم
        }

        // برای محتوای ترکیبی (http داخل file://) اجازه بده (API 21+)
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                webView.settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }
        } catch (ignored: Exception) {}

        // بهبود پشتیبانی: WebViewClient و WebChromeClient برای بارگذاری صحیح منابع
        webView.webViewClient = android.webkit.WebViewClient()
        webView.webChromeClient = android.webkit.WebChromeClient()

        // برخی دستگاه‌ها با سخت‌افزار گرافیکی مشکل دارند. اگر خطای GPUAUX ادامه داشت،
        // می‌توان WebView را به صورت نرم‌افزاری رسم کرد (خط بعدی را از کامنت خارج کنید).
        // webView.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)

        // این دو خط برای تنظیم اندازه صفحه وب به اندازه گوشی اضافه شده است
        webView.settings.loadWithOverviewMode = true
        webView.settings.useWideViewPort = true

        // ابتدا فایل ویدیو را بارگذاری کن
        webView.loadUrl("file:///android_asset/video.html")

        // یک Handler برای ایجاد تاخیر ایجاد کن
        Handler(Looper.getMainLooper()).postDelayed({
            // بعد از 8 ثانیه، فایل index.html را بارگذاری کن
            webView.loadUrl("file:///android_asset/index.html")
        }, 8000) // 8000 میلی‌ثانیه معادل 8 ثانیه است
    }
}
