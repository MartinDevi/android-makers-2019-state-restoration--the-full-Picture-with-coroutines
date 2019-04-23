package com.example.mdevillers.coroutine_state

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

        val viewModel = ViewModelProviders.of(this).get(WikipediaViewModel::class.java)
        val actor = actor<Command> {
            savedInstanceState?.getParcelable<WikipediaArticle>(STATE_ARTICLE)?.let { article ->
                bindDeferredArticle(view, CompletableDeferred(article))
            }
            viewModel.deferredArticle?.let {
                bindDeferredArticle(view, it)
            }
            loop@ for (command in this) {
                when (command) {
                    Command.DONWLOAD -> {
                        val articleDeferred = viewModel.getArticleDeferred()
                        article = null
                        bindDeferredArticle(view, articleDeferred)
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
            actor.offer(Command.DONWLOAD)
        }
        view.onClickClear {
            actor.offer(Command.CLEAR)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(STATE_ARTICLE, article)
    }

    private suspend fun bindDeferredArticle(
        view: WikipediaView,
        articleDeferred: Deferred<WikipediaArticle>
    ) {
        view.state = WikipediaView.State.ArticleProgress
        val article = try {
            articleDeferred.await()
        } catch (e: Exception) {
            view.state = WikipediaView.State.ArticleError(e)
            return
        }
        view.state = WikipediaView.State.ArticleDownloaded(article)
        this.article = article

        val image = try {
            Wikipedia.getImage(article)
        } catch (e: Exception) {
            view.state = WikipediaView.State.ArticleImageError(article, e)
            return
        }
        view.state = WikipediaView.State.ArticleImageDownloaded(article, image)
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

    enum class Command {
        DONWLOAD,
        CLEAR
    }

    class WikipediaViewModel : ViewModel(), CoroutineScope by MainScope() {

        private var _deferredArticle: Deferred<WikipediaArticle>? = null
        val deferredArticle: Deferred<WikipediaArticle>?
            get() = _deferredArticle

        fun getArticleDeferred() =
            async { Wikipedia.getRandomArticle() }.also { _deferredArticle = it }

        fun clear() {
            _deferredArticle?.cancel()
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


