package com.example.mdevillers.coroutine_state

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mdevillers.coroutine_state.model.Wikipedia
import com.example.mdevillers.coroutine_state.view.WikipediaView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class WikipediaActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val viewProxy = WikipediaView(this)

        viewProxy.onClickDownloadRandomPage {
            viewProxy.state = WikipediaView.State.ArticleProgress
            launch {
                val article = try {
                    Wikipedia.getRandomArticle()
                } catch (e: Exception) {
                    viewProxy.state = WikipediaView.State.ArticleError(e)
                    return@launch
                }
                viewProxy.state = WikipediaView.State.ArticleDownloaded(article)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }
}


