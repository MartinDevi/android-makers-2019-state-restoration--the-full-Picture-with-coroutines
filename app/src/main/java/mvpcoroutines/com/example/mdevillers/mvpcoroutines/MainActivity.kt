package mvpcoroutines.com.example.mdevillers.mvpcoroutines

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import kotlinx.coroutines.*
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class MainActivity : AppCompatActivity() {

    private val job = Job()

    private lateinit var presenter: Presenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val viewProxy = ViewProxy(window.decorView)
        presenter = Presenter(
            viewProxy,
            CoroutineScope(Dispatchers.Main + job),
            RetainedStateRepository(
                savedInstanceState?.getBundle(STATE_PRESENTER) ?: Bundle(),
                ViewModelProviders.of(this)
            ),
            ArticleRepository(Singleton.callFactory),
            ArticleThumbnailRepository(Singleton.callFactory)
        ).also {
            viewProxy.presenter = it
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBundle(STATE_PRESENTER, presenter.retainedStateRepository.savedInstanceState())
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    companion object {
        const val STATE_PRESENTER = "PRESENTER"
    }
}


/**
 * Aggregates retained states.
 */
class RetainedStateRepository(
    private val bundle: Bundle,
    private val viewModelProvider: ViewModelProvider
) {
    private val retainedStates = mutableMapOf<String, RetainedStateModel<*>>()

    @Suppress("UNCHECKED_CAST")
    operator fun <T: Parcelable> get(key: String): RetainedStateModel<T> =
        retainedStates.getOrPut(key) {
            RetainedStateModel<T>(
                bundle.getBundle(key) ?: Bundle(),
                viewModelProvider[key]
            )
        } as RetainedStateModel<T>

    fun savedInstanceState(): Bundle =
        bundle.apply {
            retainedStates.forEach {
                putBundle(it.key, it.value.savedInstanceState())
            }
        }
}

/**
 * Handles retaining a state of a computed result, combining view models and saved instance states.
 */
class RetainedStateModel<T: Parcelable>(
    private val bundle: Bundle,
    private val deferredViewModel: DeferredViewModel<T>
) {
    val deferred: Deferred<Result<T>>?
        get() = deferredViewModel.deferred

    var success: T?
        get() = bundle.getParcelable(STATE_SUCCESS)
        private set(value) { bundle.putParcelable(STATE_SUCCESS, value) }

    var error: Throwable?
        get() = bundle.getSerializable(STATE_ERROR) as Throwable?
        private set(value) { bundle.putSerializable(STATE_ERROR, value) }

    /**
     * Fold the existing state of the value.
     *
     * @param onComplete
     *  Called if the result has been successfully obtained, with the computed value as parameter.
     * @param onError
     *  Called if the task failed to obtain the result, with the error thrown as parameter.
     * @param onProgress
     *  Called if the result is still being fetched, with the deferred result as parameter.
     * @param onReady
     *  Called if no result computing has been started.
     *
     *  Note that this is often the default state of the view, in which case there's nothing to do.
     */
    inline fun getResult(
        onComplete: (T) -> Unit  = {},
        onError: (Throwable) -> Unit = {},
        onProgress: (Deferred<Result<T>>) -> Unit = {},
        onReady: () -> Unit = {}
    ) {
        val deferred = deferred
        if (deferred == null) {
            val success = this.success
            val error = this.error
            when {
                success != null -> onComplete(success)
                error != null -> onError(error)
                else -> onReady()
            }
        } else {
            if (deferred.isCompleted) {
                val completed = deferred.getCompleted()
                completed.fold(
                    onSuccess = onComplete,
                    onFailure = onError
                )
            } else {
                onProgress(deferred)
            }
        }
    }

    /**
     * Warning: `block` should *NOT* capture any context-scoped instances.
     */
    fun asyncCatching(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> T
    ): Deferred<Result<T>> {
        clearResult()
        return deferredViewModel.async(
            context,
            start
        ) {
            runCatching { block() }
        }.also {
            deferredViewModel.deferred = it
        }
    }

    fun clearResult() {
        deferredViewModel.deferred?.cancel()
        deferredViewModel.deferred = null
        success = null
        error = null
    }

    fun savedInstanceState(): Bundle {
        getResult(
            onComplete = { success = it },
            onError = { error = it }
        )
        return bundle
    }

    companion object {
        const val STATE_SUCCESS = "SUCCESS"
        const val STATE_ERROR = "ERROR"
    }
}


class DeferredViewModel<T: Parcelable>: ViewModel(), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    var deferred: Deferred<Result<T>>? = null

    private val job = Job()

    override fun onCleared() {
        super.onCleared()
        job.cancel()
    }
}

@Suppress("UNCHECKED_CAST")
operator fun <T: Parcelable> ViewModelProvider.get(key: String): DeferredViewModel<T> {
    return get(key, DeferredViewModel::class.java) as DeferredViewModel<T>
}

interface Contract {
    interface ViewProxy {
        fun showEmpty()
        fun showProgress()
        fun showArticle(article: Article)
        fun showError(message: String)
        fun showThumbnailProgress()
        fun showThumbnail(bitmap: Bitmap)
        fun showThumbnailError(message: String)
    }

    interface Presenter {
        fun onClickDownloadArticle()
        fun onClickClear()
    }
}

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
            // TODO: Might need to restart thumbnail if activity was killed during loading
            // In general, the state should probably include information on how to restart the process
            onComplete = ::showArticle,
            onError = ::showError,
            onProgress = ::showArticleProgress
        )
        thumbnailState.getResult(
            onComplete = ::showThumbnail,
            onError = ::showThumbnailError,
            onProgress = ::showThumbnailDownloadProgress
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
                .onSuccess { downloadThumbnail(it) }
                .fold(
                    onSuccess = ::showArticle,
                    onFailure = ::showError
                )
        }
    }

    private fun downloadThumbnail(article: Article) {
        val deferredThumbnail = thumbnailState.asyncCatching {
            thumbnailRepository.getThumbnail(article.thumbnailUrl)
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
    }
}

class Article(
    val title: String,
    val description: String,
    val extract: String,
    val thumbnailUrl: String
): Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeString(description)
        parcel.writeString(extract)
        parcel.writeString(thumbnailUrl)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Article> {
        override fun createFromParcel(parcel: Parcel): Article = Article(parcel)
        override fun newArray(size: Int): Array<Article?> = arrayOfNulls(size)
    }
}

class ViewProxy(private val view: View): Contract.ViewProxy {

    var presenter: Contract.Presenter? = null

    private val button: View
        get() = view.findViewById(R.id.button)
    private val clearButton: View
        get() = view.findViewById(R.id.button_clear)
    private val progress: View
        get() = view.findViewById(R.id.progress)
    private val title: TextView
        get() = view.findViewById(R.id.title)
    private val description: TextView
        get() = view.findViewById(R.id.description)
    private val thumbnail: ImageView
        get() = view.findViewById(R.id.thumbnail)
    private val thumbnailProgress: View
        get() = view.findViewById(R.id.thumbnail_progress)
    private val thumbnailError: TextView
        get() = view.findViewById(R.id.thumbnail_error)
    private val extract: TextView
        get() = view.findViewById(R.id.extract)
    private val error: TextView
        get() = view.findViewById(R.id.error)

    init {
        button.setOnClickListener { presenter?.onClickDownloadArticle() }
        clearButton.setOnClickListener { presenter?.onClickClear() }
    }

    override fun showEmpty() {
        progress.visibility = View.INVISIBLE
        title.visibility = View.INVISIBLE
        description.visibility = View.INVISIBLE
        extract.visibility = View.INVISIBLE
        error.visibility = View.INVISIBLE
        thumbnail.visibility = View.INVISIBLE
        thumbnailProgress.visibility = View.INVISIBLE
        thumbnailError.visibility = View.INVISIBLE
    }

    override fun showProgress() {
        progress.visibility = View.VISIBLE
        title.visibility = View.INVISIBLE
        description.visibility = View.INVISIBLE
        extract.visibility = View.INVISIBLE
        error.visibility = View.INVISIBLE
        thumbnail.visibility = View.INVISIBLE
        thumbnailProgress.visibility = View.INVISIBLE
        thumbnailError.visibility = View.INVISIBLE
    }

    override fun showArticle(article: Article) {
        progress.visibility = View.INVISIBLE
        title.text = article.title
        title.visibility = View.VISIBLE
        description.text = article.description
        description.visibility = View.VISIBLE
        extract.text = article.extract
        extract.visibility = View.VISIBLE
        error.visibility = View.INVISIBLE
        thumbnail.visibility = View.INVISIBLE
        thumbnailProgress.visibility = View.INVISIBLE
        thumbnailError.visibility = View.INVISIBLE
    }

    override fun showError(message: String) {
        progress.visibility = View.INVISIBLE
        title.visibility = View.INVISIBLE
        description.visibility = View.INVISIBLE
        extract.visibility = View.INVISIBLE
        error.text = message
        error.visibility = View.VISIBLE
        thumbnail.visibility = View.INVISIBLE
        thumbnailProgress.visibility = View.INVISIBLE
        thumbnailError.visibility = View.INVISIBLE
    }

    override fun showThumbnailProgress() {
        thumbnailProgress.visibility = View.VISIBLE
        thumbnail.visibility = View.INVISIBLE
        thumbnailError.visibility = View.INVISIBLE
    }

    override fun showThumbnail(bitmap: Bitmap) {
        thumbnailProgress.visibility = View.INVISIBLE
        thumbnailError.visibility = View.INVISIBLE
        thumbnail.setImageBitmap(bitmap)
        thumbnail.visibility = View.VISIBLE
    }

    override fun showThumbnailError(message: String) {
        thumbnailProgress.visibility = View.INVISIBLE
        thumbnail.visibility = View.INVISIBLE
        thumbnailError.text = message
        thumbnailError.visibility = View.VISIBLE
    }
}

class ArticleRepository(private val callFactory: Call.Factory) {
    fun getArticle(): Article {
        val request = Request.Builder()
            .url("https://en.wikipedia.org/api/rest_v1/page/random/summary")
            .header("Accept", "application/json")
            .build()
        val call = callFactory.newCall(request)
        val response = call.execute()
        val responseBody = response.body()!!.string()
        val jsonObject = JSONObject(responseBody)
        return with(jsonObject) {
            Article(
                getString("title").apply { check(isNotEmpty()) { "Empty title" } },
                getString("description"),
                getString("extract"),
                getJSONObject("thumbnail").getString("source")
            )
        }
    }
}

class ArticleThumbnailRepository(
    private val callFactory: Call.Factory
) {
    fun getThumbnail(url: String): Bitmap {
        val request = Request.Builder()
            .url(url)
            .build()
        val call = callFactory.newCall(request)
        val response = call.execute()
        return response.body()!!.byteStream().use {
            BitmapFactory.decodeStream(it)
        }
    }
}

object Singleton {
    val callFactory: Call.Factory = OkHttpClient()
}

