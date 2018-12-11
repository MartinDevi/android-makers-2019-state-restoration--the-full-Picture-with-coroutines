package mvpcoroutines.com.example.mdevillers.mvpcoroutines

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.view.View
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
        val viewModelProvider = ViewModelProviders.of(this)
        val viewProxy = ViewProxy(window.decorView)
        presenter = Presenter(
            viewProxy,
            CoroutineScope(Dispatchers.Main + job),
            viewModelProvider,
            savedInstanceState?.getBundle(STATE_PRESENTER) ?: Bundle(),
            ArticleRepository(Singleton.callFactory)
        ).also {
            viewProxy.presenter = it
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBundle(STATE_PRESENTER, presenter.savedInstanceState)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    companion object {
        const val STATE_PRESENTER = "PRESENTER"
    }
}


class DeferredViewModel<T>: ViewModel() {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    var deferred: Deferred<Result<T>>? = null

    /**
     * Warning: `block` should *NOT* capture any context-scoped instances.
     */
    fun asyncCatching(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> T
    ): Deferred<Result<T>> =
        scope.async(context, start) { runCatching { block() } }.also { deferred = it }

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
     *  Called if no result is being fetched.
     *  Only block with a default value because "ready" is generally the default state for the view, so there's nothing
     *  to do.
     */
    inline fun getResult(
        onComplete: (T) -> Unit,
        onError: (Throwable) -> Unit,
        onProgress: (Deferred<Result<T>>) -> Unit,
        onReady: () -> Unit = {}
    ) {
        val deferred = deferred
        if (deferred == null) {
            onReady()
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

    override fun onCleared() {
        super.onCleared()
        job.cancel()
    }
}

@Suppress("UNCHECKED_CAST")
operator fun <T> ViewModelProvider.get(key: String): DeferredViewModel<T> {
    return get(key, DeferredViewModel::class.java) as DeferredViewModel<T>
}

interface Contract {
    interface ViewProxy {
        fun showProgress()
        fun showArticle(article: Article)
        fun showError(message: String)
    }

    interface Presenter {
        fun onClickDownloadArticle()
    }
}

class Presenter(
    private val viewProxy: Contract.ViewProxy,
    private val coroutineScope: CoroutineScope,
    private val modelProviders: ViewModelProvider,
    val savedInstanceState: Bundle,
    private val articleRepository: ArticleRepository
): Contract.Presenter, CoroutineScope by coroutineScope {

    private var article: Article?
        get() = savedInstanceState.getParcelable(STATE_ARTICLE)
        set(value) { savedInstanceState.putParcelable(STATE_ARTICLE, value) }
    private var error: Throwable?
        get() = savedInstanceState.getSerializable(STATE_ERROR) as Throwable?
        set(value) { savedInstanceState.putSerializable(STATE_ERROR, value) }
    private val articleDownloadModel: DeferredViewModel<Article>
        get() = modelProviders[MODEL_ARTICLE]

    init {
        articleDownloadModel.getResult(
            onComplete = ::showArticle,
            onError = ::showError,
            onProgress = ::showArticleProgress,
            onReady = {
                article?.let(::showArticle)
            }
        )
    }

    override fun onClickDownloadArticle() {
        val deferredArticle = articleDownloadModel.asyncCatching { articleRepository.getArticle() }
        showArticleProgress(deferredArticle)
    }

    private fun showArticleProgress(deferredArticle: Deferred<Result<Article>>) {
        viewProxy.showProgress()
        launch {
            deferredArticle.await().fold(
                onSuccess = ::showArticle,
                onFailure = ::showError
            )
        }
    }

    private fun showArticle(article: Article) {
        articleDownloadModel.deferred = null
        this.article = article
        this.error = null
        viewProxy.showArticle(article)
    }

    private fun showError(error: Throwable) {
        articleDownloadModel.deferred = null
        this.article = null
        this.error = error
        viewProxy.showError(error.message ?: "")
    }

    companion object {
        const val STATE_ARTICLE = "ARTICLE"
        const val STATE_ERROR = "ERROR"
        const val MODEL_ARTICLE = "ARTICLE"
    }
}

class Article(
    val title: String,
    val description: String,
    val extract: String
): Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeString(description)
        parcel.writeString(extract)
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
    private val progress: View
        get() = view.findViewById(R.id.progress)
    private val title: TextView
        get() = view.findViewById(R.id.title)
    private val description: TextView
        get() = view.findViewById(R.id.description)
    private val extract: TextView
        get() = view.findViewById(R.id.extract)
    private val error: TextView
        get() = view.findViewById(R.id.error)

    init {
        button.setOnClickListener { presenter?.onClickDownloadArticle() }
    }

    override fun showProgress() {
        progress.visibility = View.VISIBLE
        title.visibility = View.GONE
        description.visibility = View.GONE
        extract.visibility = View.GONE
        error.visibility = View.GONE
    }

    override fun showArticle(article: Article) {
        progress.visibility = View.GONE
        title.text = article.title
        title.visibility = View.VISIBLE
        description.text = article.description
        description.visibility = View.VISIBLE
        extract.text = article.extract
        extract.visibility = View.VISIBLE
        error.visibility = View.GONE
    }

    override fun showError(message: String) {
        progress.visibility = View.GONE
        title.visibility = View.GONE
        description.visibility = View.GONE
        extract.visibility = View.GONE
        error.text = message
        error.visibility = View.VISIBLE
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
                getString("extract")
            )
        }
    }
}

object Singleton {
    val callFactory: Call.Factory = OkHttpClient()
}

