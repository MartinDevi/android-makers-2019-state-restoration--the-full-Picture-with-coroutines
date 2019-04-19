package com.example.mdevillers.coroutine_state

import android.graphics.Bitmap
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import com.example.mdevillers.coroutine_state.model.Wikipedia
import com.example.mdevillers.coroutine_state.model.WikipediaArticle
import com.example.mdevillers.coroutine_state.view.WikipediaView
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
                bindDeferredArticle(view, it, articleDownloadViewModel)
            }
            for (command in this) {
                when (command) {
                    ActorCommand.DOWNLOAD -> {
                        val deferred = articleDownloadViewModel.getRandomArticleAsync()
                        bindDeferredArticle(view, deferred, articleDownloadViewModel)
                    }
                    ActorCommand.CLEAR -> {
                        clearArticle(articleDownloadViewModel)
                        view.state = WikipediaView.State.Empty
                    }
                }
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
        deferred: Deferred<WikipediaArticle>,
        articleDownloadViewModel: ArticleDownloadViewModel
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

        val deferredImage = articleDownloadViewModel.deferredImage ?: articleDownloadViewModel.getImageAsync(article)
        bindDeferredArticleImage(view, article, deferredImage)
    }

    private suspend fun bindDeferredArticleImage(
        view: WikipediaView,
        article: WikipediaArticle,
        deferred: Deferred<Bitmap>
    ) {
        view.state = WikipediaView.State.ArticleImageProgress(article)
        val image = try {
            deferred.await()
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
    class ArticleDownloadViewModel : ViewModel(), CoroutineScope by MainScope() {

        private var _deferred: Deferred<WikipediaArticle>? = null
        val deferred: Deferred<WikipediaArticle>?
            get() = _deferred
        private var _deferredImage: Deferred<Bitmap>? = null
        val deferredImage: Deferred<Bitmap>?
            get() = _deferredImage

        fun getRandomArticleAsync(): Deferred<WikipediaArticle> =
            async { Wikipedia.getRandomArticle() }.also { _deferred = it }

        fun getImageAsync(article: WikipediaArticle): Deferred<Bitmap> =
            async { Wikipedia.getImage(article) }.also { _deferredImage = it }

        fun clear() {
            _deferred?.cancel()
            _deferred = null
            _deferredImage?.cancel()
            _deferredImage = null
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


