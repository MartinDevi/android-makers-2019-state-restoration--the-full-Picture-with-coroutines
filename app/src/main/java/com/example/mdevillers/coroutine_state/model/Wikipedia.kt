package com.example.mdevillers.coroutine_state.model

import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object Wikipedia {

    private val httpCallFactory: Call.Factory = OkHttpClient()

    suspend fun getRandomArticle(): WikipediaArticle = withContext(Dispatchers.Default) {
        delay(3000)
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
                                getLong("pageid"),
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
            continuation.invokeOnCancellation {
                call.cancel()
            }
        }
    }

    suspend fun getImage(article: WikipediaArticle): WikipediaImage = withContext(Dispatchers.Default) {
        delay(3000)
        val request = Request.Builder()
            .url(article.imageUrl)
            .build()
        val call = httpCallFactory.newCall(request)
        suspendCancellableCoroutine<WikipediaImage> { continuation ->
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
                    continuation.resume(WikipediaImage(bitmap))
                }
            })
            continuation.invokeOnCancellation {
                call.cancel()
            }
        }
    }
}