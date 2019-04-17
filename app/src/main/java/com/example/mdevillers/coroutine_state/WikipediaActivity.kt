package com.example.mdevillers.coroutine_state

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mdevillers.coroutine_state.model.Wikipedia
import com.example.mdevillers.coroutine_state.view.WikipediaView
import com.example.mdevillers.coroutine_state.view.exhaustive
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.actor

class WikipediaActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val view = WikipediaView(this)

        val actor = actor<ActorCommand>(start = CoroutineStart.UNDISPATCHED) {
            loop@ for (command in this) {
                when (command) {
                    ActorCommand.DOWNLOAD -> {
                        view.state = WikipediaView.State.ArticleProgress
                        val article = try {
                            Wikipedia.getRandomArticle()
                        } catch (e: Exception) {
                            view.state = WikipediaView.State.ArticleError(e)
                            continue@loop
                        }
                        view.state = WikipediaView.State.ArticleDownloaded(article)
                    }
                    ActorCommand.CLEAR -> {
                        view.state = WikipediaView.State.Empty
                    }
                }.exhaustive
            }
        }
        view.onClickDownloadRandomPage {
            actor.offer(ActorCommand.DOWNLOAD)
        }
        view.onClickClear {
            actor.offer(ActorCommand.CLEAR)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

    private enum class ActorCommand {
        DOWNLOAD,
        CLEAR
    }
}


