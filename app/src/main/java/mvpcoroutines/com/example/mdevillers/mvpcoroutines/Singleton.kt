package mvpcoroutines.com.example.mdevillers.mvpcoroutines

import okhttp3.Call
import okhttp3.OkHttpClient

object Singleton {
    val callFactory: Call.Factory = OkHttpClient()
}