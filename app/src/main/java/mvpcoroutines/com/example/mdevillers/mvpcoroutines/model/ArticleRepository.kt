package mvpcoroutines.com.example.mdevillers.mvpcoroutines.model

import okhttp3.Call
import okhttp3.Request
import org.json.JSONObject

class ArticleRepository(private val callFactory: Call.Factory) {
    fun getArticle(): Article {
        val request = Request.Builder()
            .url("https://en.wikipedia.org/api/rest_v1/page/random/summary")
            .header("Accept", "application/json")
            .build()
        val call = callFactory.newCall(request)
        val response = call.execute()
        val responseBody = response.body()!!.string()
        val jsonObject = JSONObject(responseBody)
        return with(jsonObject) {
            Article(
                getString("title").apply { check(isNotEmpty()) { "Empty title" } },
                getString("description"),
                getString("extract"),
                getJSONObject("thumbnail").getString("source")
            )
        }
    }
}