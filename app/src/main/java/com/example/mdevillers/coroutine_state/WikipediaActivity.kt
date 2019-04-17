package com.example.mdevillers.coroutine_state

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import com.example.mdevillers.coroutine_state.model.Wikipedia
import com.example.mdevillers.coroutine_state.model.WikipediaArticle
import com.example.mdevillers.coroutine_state.view.WikipediaView
import com.example.mdevillers.coroutine_state.view.exhaustive
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.actor

class WikipediaActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val view = WikipediaView(this)

        val articleDownloadViewModel = ViewModelProviders.of(this).get(ArticleDownloadViewModel::class.java)
        val actor = actor<ActorCommand>(start = CoroutineStart.UNDISPATCHED) {
            articleDownloadViewModel.deferred?.let {
                bindDeferredArticle(view, it)
            }
            for (command in this) {
                when (command) {
                    ActorCommand.DOWNLOAD -> {
                        val deferred = articleDownloadViewModel.getRandomArticleAsync()
                        bindDeferredArticle(view, deferred)
                    }
                    ActorCommand.CLEAR -> {
                        articleDownloadViewModel.clear()
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

    private suspend fun bindDeferredArticle(
        view: WikipediaView,
        deferred: Deferred<WikipediaArticle>
    ) {
        view.state = WikipediaView.State.ArticleProgress
        val article = try {
            deferred.await()
        } catch (e: Exception) {
            view.state = WikipediaView.State.ArticleError(e)
            return
        }
        view.state = WikipediaView.State.ArticleDownloaded(article)
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

    private enum class ActorCommand {
        DOWNLOAD,
        CLEAR
    }

    // Note: can use `viewModelScope` extension property once androidx lifecycle 2.1.0 becomes stable
    class ArticleDownloadViewModel: ViewModel(), CoroutineScope by MainScope() {

        private var _deferred: Deferred<WikipediaArticle>? = null
        val deferred: Deferred<WikipediaArticle>?
            get() = _deferred

        fun getRandomArticleAsync(): Deferred<WikipediaArticle> =
            async { Wikipedia.getRandomArticle() }.also { _deferred = it }

        fun clear() {
            _deferred?.cancel()
            _deferred = null
        }

        override fun onCleared() {
            super.onCleared()
            cancel()
        }
    }
}


