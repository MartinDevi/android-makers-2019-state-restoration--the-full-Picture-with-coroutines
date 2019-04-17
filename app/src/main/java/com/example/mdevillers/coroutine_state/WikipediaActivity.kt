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

    private var article: WikipediaArticle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val view = WikipediaView(this)

        val articleDownloadViewModel = ViewModelProviders.of(this).get(ArticleDownloadViewModel::class.java)
        // Note: can use `SavedStateHandle` inside `ViewModel` once androidx.lifecycle:lifecycle-viewmodel-savedstate:1.0.0 becomes stable
        article = savedInstanceState?.getParcelable<WikipediaArticle>(STATE_ARTICLE)?.also {
            view.state = WikipediaView.State.ArticleDownloaded(it)
        }

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
                        clearArticle(articleDownloadViewModel)
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
        this.article = article

        view.state = WikipediaView.State.ArticleImageProgress(article)
        val image = try {
            Wikipedia.getImage(article)
        } catch (e: Exception) {
            view.state = WikipediaView.State.ArticleImageError(article, e)
            return
        }
        view.state = WikipediaView.State.ArticleImageDownloaded(article, image)
    }

    private fun clearArticle(articleDownloadViewModel: ArticleDownloadViewModel) {
        article = null
        articleDownloadViewModel.clear()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(STATE_ARTICLE, article)
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

    private enum class ActorCommand {
        DOWNLOAD,
        CLEAR
    }

    // Note: can use `viewModelScope` extension property once androidx.lifecycle:lifecycle-viewmodel-ktx:2.1.0 becomes stable
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

    companion object {
        private const val STATE_ARTICLE = "Article"
    }
}


