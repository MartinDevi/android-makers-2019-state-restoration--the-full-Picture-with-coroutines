package com.example.mdevillers.mvpcoroutines.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import okhttp3.Call
import okhttp3.Request

class ArticleThumbnailRepository(
    private val callFactory: Call.Factory
) {
    fun getThumbnail(url: String): Bitmap {
        val request = Request.Builder()
            .url(url)
            .build()
        val call = callFactory.newCall(request)
        val response = call.execute()
        return response.body()!!.byteStream().use {
            BitmapFactory.decodeStream(it)
        }
    }
}