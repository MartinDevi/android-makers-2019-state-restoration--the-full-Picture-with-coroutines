package mvpcoroutines.com.example.mdevillers.mvpcoroutines.mvp

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
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

    private val articleState: RetainedStateModel<Bundle, Article>
    private val thumbnailState: RetainedStateModel<ThumbnailUrl, Bitmap>

    init {
        articleState = retainedStateRepository[STATE_ARTICLE, ::downloadArticle]
        articleState.bind(
            onActive = ::showArticleProgress,
            onSuccess = ::showArticle,
            onError = ::showError
        )
        thumbnailState = retainedStateRepository[STATE_THUMBNAIL, ::downloadThumbnail]
        thumbnailState.bind(
            onActive = ::showThumbnailDownloadProgress,
            onSuccess = ::showThumbnail,
            onError = ::showThumbnailError
        )
    }

    private suspend fun downloadArticle(bundle: Bundle): Article =
        articleRepository.getArticle()

    private suspend fun downloadThumbnail(thumbnailUrl: ThumbnailUrl): Bitmap =
        thumbnailRepository.getThumbnail(thumbnailUrl.url)

    override fun onClickDownloadArticle() {
        thumbnailState.clear()
        val deferredArticle = articleState.start(Bundle())
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
        val deferredThumbnail = thumbnailState.start(ThumbnailUrl(thumbnailUrl))
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

    private data class ThumbnailUrl(val url: String) : Parcelable {
        constructor(parcel: Parcel) : this(parcel.readString()!!)

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(url)
        }

        override fun describeContents(): Int = 0

        companion object CREATOR : Parcelable.Creator<ThumbnailUrl> {
            override fun createFromParcel(parcel: Parcel): ThumbnailUrl = ThumbnailUrl(parcel)
            override fun newArray(size: Int): Array<ThumbnailUrl?> = arrayOfNulls(size)
        }
    }
}