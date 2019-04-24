package com.example.mdevillers.coroutine_state

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.get
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


        val viewModel = ViewModelProviders.of(this).get<WikipediaViewModel>()
        val actor = actor<Command>(start = CoroutineStart.UNDISPATCHED) {
            savedInstanceState?.getParcelable<WikipediaArticle>(STATE_ARTICLE)?.let {
                view.state = WikipediaView.State.ArticleDownloaded(it)
                article = it
            } ?: viewModel.deferredArticle?.let {
                bindDeferredArticle(view, it)
            }
            loop@ for (command in this) {
                when (command) {
                    Command.DOWNLOAD -> {
                        val deferred = viewModel.getRandomArticleAsync()
                        bindDeferredArticle(view, deferred)
                    }
                    Command.CLEAR -> {
                        view.state = WikipediaView.State.Empty
                        article = null
                        viewModel.clear()
                    }
                }
            }
        }
        view.onClickDownloadRandomPage {
            actor.offer(Command.DOWNLOAD)
        }
        view.onClickClear {
            actor.offer(Command.CLEAR)
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
            view.state = WikipediaView.State.ArticleProgress
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(STATE_ARTICLE, article)
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

    enum class Command {
        DOWNLOAD,
        CLEAR
    }

    class WikipediaViewModel : ViewModel(), CoroutineScope by MainScope() {

        private var _deferredArticle: Deferred<WikipediaArticle>? = null
        val deferredArticle: Deferred<WikipediaArticle>?
            get() = _deferredArticle

        fun getRandomArticleAsync()  =
            async { Wikipedia.getRandomArticle() }.also { _deferredArticle = it }

        fun clear() {
            _deferredArticle = null
        }

        override fun onCleared() {
            super.onCleared()
            cancel()
        }
    }

    companion object {
        const val STATE_ARTICLE = "Article"
    }
}


