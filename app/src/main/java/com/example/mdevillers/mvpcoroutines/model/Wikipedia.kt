package com.example.mdevillers.mvpcoroutines.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.lang.Exception
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object Wikipedia {

    private val httpCallFactory: Call.Factory = OkHttpClient()

    suspend fun getRandomArticle(): WikipediaArticle = withContext(Dispatchers.Default) {
        val request = Request.Builder()
            .url("https://en.wikipedia.org/api/rest_v1/page/random/summary")
            .header("Accept", "application/json")
            .build()
        val call = httpCallFactory.newCall(request)
        suspendCancellableCoroutine<WikipediaArticle> { continuation ->
            call.enqueue(object: Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    val article = try {
                        with(JSONObject(response.body()!!.string())) {
                            WikipediaArticle(
                                getString("title").apply { check(isNotEmpty()) { "Empty title" } },
                                getString("description"),
                                getString("extract"),
                                getJSONObject("originalimage").getString("source")
                            )
                        }
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                        return
                    }
                    continuation.resume(article)
                }
            })
        }
    }

    suspend fun getImage(article: WikipediaArticle): Bitmap = withContext(Dispatchers.Default) {
        val request = Request.Builder()
            .url(article.imageUrl)
            .build()
        val call = httpCallFactory.newCall(request)
        suspendCancellableCoroutine<Bitmap> { continuation ->
            call.enqueue(object: Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    val bitmap = try {
                        response.body()!!.byteStream().use {
                            BitmapFactory.decodeStream(it)
                        }
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                        return
                    }
                    continuation.resume(bitmap)
                }
            })
        }
    }
}