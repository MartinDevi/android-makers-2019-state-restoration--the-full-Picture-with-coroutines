package mvpcoroutines.com.example.mdevillers.mvpcoroutines.mvp

import android.graphics.Bitmap
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch
import mvpcoroutines.com.example.mdevillers.mvpcoroutines.model.Article
import mvpcoroutines.com.example.mdevillers.mvpcoroutines.model.ArticleRepository
import mvpcoroutines.com.example.mdevillers.mvpcoroutines.model.ArticleThumbnailRepository
import mvpcoroutines.com.example.mdevillers.mvpcoroutines.framework.RetainedStateModel
import mvpcoroutines.com.example.mdevillers.mvpcoroutines.framework.RetainedStateRepository

class Presenter(
    private val viewProxy: Contract.ViewProxy,
    private val coroutineScope: CoroutineScope,
    val retainedStateRepository: RetainedStateRepository,
    private val articleRepository: ArticleRepository,
    private val thumbnailRepository: ArticleThumbnailRepository
): Contract.Presenter, CoroutineScope by coroutineScope {

    private val articleState: RetainedStateModel<Article>
        get() = retainedStateRepository[STATE_ARTICLE]
    private val thumbnailState: RetainedStateModel<Bitmap>
        get() = retainedStateRepository[STATE_THUMBNAIL]

    init {
        articleState.getResult(
            onComplete = ::showArticle,
            onError = ::showError,
            onProgress = ::showArticleProgress
        )
        thumbnailState.getResult(
            onComplete = ::showThumbnail,
            onError = ::showThumbnailError,
            onProgress = ::showThumbnailDownloadProgress,
            onInterrupted = {
                downloadThumbnail(it.getString(ARGUMENT_URL)!!)
            }
        )
    }

    override fun onClickDownloadArticle() {
        thumbnailState.clearResult()
        val deferredArticle = articleState.asyncCatching {
            articleRepository.getArticle()
        }
        showArticleProgress(deferredArticle)
    }

    override fun onClickClear() {
        articleState.clearResult()
        thumbnailState.clearResult()
        viewProxy.showEmpty()
    }

    private fun showArticleProgress(deferredArticle: Deferred<Result<Article>>) {
        viewProxy.showProgress()
        launch {
            deferredArticle.await()
                .onSuccess { downloadThumbnail(it.thumbnailUrl) }
                .fold(
                    onSuccess = ::showArticle,
                    onFailure = ::showError
                )
        }
    }

    private fun downloadThumbnail(thumbnailUrl: String) {
        val arguments = Bundle().apply {
            putString(ARGUMENT_URL, thumbnailUrl)
        }
        val deferredThumbnail = thumbnailState.asyncCatching(arguments = arguments) {
            thumbnailRepository.getThumbnail(thumbnailUrl)
        }
        showThumbnailDownloadProgress(deferredThumbnail)
    }

    private fun showThumbnailDownloadProgress(deferredThumbnail: Deferred<Result<Bitmap>>) {
        launch {
            viewProxy.showThumbnailProgress()
            deferredThumbnail.await().fold(
                onSuccess = ::showThumbnail,
                onFailure = ::showThumbnailError
            )
        }
    }

    private fun showArticle(article: Article) {
        viewProxy.showArticle(article)
    }

    private fun showError(error: Throwable) {
        viewProxy.showError(error.message ?: error.javaClass.simpleName)
    }

    private fun showThumbnail(bitmap: Bitmap) {
        viewProxy.showThumbnail(bitmap)
    }

    private fun showThumbnailError(error: Throwable) {
        viewProxy.showThumbnailError(error.message ?: error.javaClass.simpleName)
    }

    companion object {
        const val STATE_ARTICLE = "ARTICLE"
        const val STATE_THUMBNAIL = "THUMBNAIL"

        const val ARGUMENT_URL = "URL"
    }
}