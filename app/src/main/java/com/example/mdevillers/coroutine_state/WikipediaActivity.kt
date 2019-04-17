package com.example.mdevillers.coroutine_state

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mdevillers.coroutine_state.model.Wikipedia
import com.example.mdevillers.coroutine_state.view.WikipediaView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.actor

class WikipediaActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val view = WikipediaView(this)

        val clickDownloadPageActor = actor<Unit>(start = CoroutineStart.UNDISPATCHED) {
            for (command in this) {
                view.state = WikipediaView.State.ArticleProgress
                val article = try {
                    Wikipedia.getRandomArticle()
                } catch (e: Exception) {
                    view.state = WikipediaView.State.ArticleError(e)
                    continue
                }
                view.state = WikipediaView.State.ArticleDownloaded(article)
            }
        }
        view.onClickDownloadRandomPage {
            clickDownloadPageActor.offer(Unit)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }
}


