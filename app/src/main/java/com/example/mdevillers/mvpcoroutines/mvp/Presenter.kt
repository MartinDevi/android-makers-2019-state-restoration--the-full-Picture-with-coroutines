package com.example.mdevillers.mvpcoroutines.mvp

import android.graphics.Bitmap
import android.os.Bundle
import com.example.mdevillers.mvpcoroutines.framework.RetainedStateModel
import com.example.mdevillers.mvpcoroutines.framework.RetainedStateRepository
import com.example.mdevillers.mvpcoroutines.framework.StringStateHelper
import com.example.mdevillers.mvpcoroutines.framework.start
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch
import com.example.mdevillers.mvpcoroutines.model.Article
import com.example.mdevillers.mvpcoroutines.model.ArticleRepository
import com.example.mdevillers.mvpcoroutines.model.ArticleThumbnailRepository

class Presenter private constructor(
    private val viewProxy: Contract.ViewProxy,
    private val coroutineScope: CoroutineScope,
    private val articleState: RetainedStateModel<Bundle, Article>,
    private val thumbnailState: RetainedStateModel<Bundle, Bitmap>
): Contract.Presenter, CoroutineScope by coroutineScope {

    constructor(
        viewProxy: Contract.ViewProxy,
        coroutineScope: CoroutineScope,
        retainedStateRepository: RetainedStateRepository,
        articleRepository: ArticleRepository,
        thumbnailRepository: ArticleThumbnailRepository
    ): this(
        viewProxy,
        coroutineScope,
        retainedStateRepository[STATE_ARTICLE, { it: Bundle -> articleRepository.getArticle() } ],
        with(StringStateHelper) { retainedStateRepository[STATE_THUMBNAIL, thumbnailRepository::getThumbnail] }
    )

    init {
        articleState.bind(
            onActive = ::showArticleProgress,
            onSuccess = ::showArticle,
            onError = ::showError
        )
        thumbnailState.bind(
            onActive = ::showThumbnailDownloadProgress,
            onSuccess = ::showThumbnail,
            onError = ::showThumbnailError
        )
    }

    override fun onClickDownloadArticle() {
        thumbnailState.clear()
        val deferredArticle = articleState.start()
        showArticleProgress(deferredArticle)
    }

    override fun onClickClear() {
        articleState.clear()
        thumbnailState.clear()
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
        val deferredThumbnail = with(StringStateHelper) { thumbnailState.start(thumbnailUrl) }
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
    }
}