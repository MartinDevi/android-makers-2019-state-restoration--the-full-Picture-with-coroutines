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
import java.lang.Exception

class WikipediaActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    private var article: WikipediaArticle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val view = WikipediaView(this)

        article = savedInstanceState?.getParcelable<WikipediaArticle>(STATE_ARTICLE)?.also {
            view.state = WikipediaView.State.ArticleDownloaded(it)
        }

        val viewModel = ViewModelProviders.of(this).get(WikipediaViewModel::class.java)
        val actor = actor<Command>(start = CoroutineStart.UNDISPATCHED) {
            viewModel.deferredArticle?.let {
                bindDeferredArticle(view, it.first, it.second)
            }
            for (command in this) {
                when (command) {
                    Command.DOWNLOAD -> {
                        val (deferredArticle, deferredImage) = viewModel.getRandomArticleImageAsync()
                        bindDeferredArticle(view, deferredArticle, deferredImage)
                    }
                    Command.CLEAR -> {
                        viewModel.clear()
                        view.state = WikipediaView.State.Empty
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(STATE_ARTICLE, article)
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

    private suspend fun bindDeferredArticle(
        view: WikipediaView,
        deferredArticle: Deferred<WikipediaArticle>,
        deferredImage: Deferred<Bitmap>
    ) {
        view.state = WikipediaView.State.ArticleProgress
        val article = try {
            deferredArticle.await()
        } catch (e: Exception) {
            view.state = WikipediaView.State.ArticleError(e)
            return
        }
        this.article = article
        view.state = WikipediaView.State.ArticleImageProgress(article)
        val image = try {
            deferredImage.await()
        } catch (e: Exception) {
            view.state = WikipediaView.State.ArticleImageError(article, e)
            return
        }
        view.state = WikipediaView.State.ArticleImageDownloaded(article, image)
    }

    enum class Command {
        DOWNLOAD,
        CLEAR
    }

    class WikipediaViewModel : ViewModel(), CoroutineScope by MainScope() {

        private var _deferredArticleImage: Pair<Deferred<WikipediaArticle>, Deferred<Bitmap>>? = null
        val deferredArticle: Pair<Deferred<WikipediaArticle>, Deferred<Bitmap>>?
            get() = _deferredArticleImage

        fun getRandomArticleImageAsync(): Pair<Deferred<WikipediaArticle>, Deferred<Bitmap>> =
            async { Wikipedia.getRandomArticle() }.let {
                it to async {
                    Wikipedia.getImage(it.await())
                }
            }.also {
                _deferredArticleImage = it
            }

        fun clear() {
            _deferredArticleImage?.first?.cancel()
            _deferredArticleImage?.second?.cancel()
            _deferredArticleImage = null
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


