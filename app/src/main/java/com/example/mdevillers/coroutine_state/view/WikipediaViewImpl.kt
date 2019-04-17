package com.example.mdevillers.coroutine_state.view

import android.graphics.Bitmap
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.example.mdevillers.coroutine_state.R
import com.example.mdevillers.coroutine_state.WikipediaActivity
import com.example.mdevillers.coroutine_state.model.WikipediaArticle
import com.example.mdevillers.coroutine_state.view.WikipediaView.State
import com.example.mdevillers.coroutine_state.view.WikipediaView.State.*
import kotlin.properties.Delegates

@Suppress("FunctionName")
fun WikipediaView(activity: WikipediaActivity): WikipediaView =
    WikipediaViewImpl(activity.window.decorView)

class WikipediaViewImpl(private val view: View): WikipediaView {

    override var state by Delegates.observable<State>(Empty) { _, _, value ->
        when (value) {
            is Empty -> showEmpty()
            is ArticleProgress -> showProgress()
            is ArticleDownloaded -> showArticle(value.article)
            is ArticleError -> showError(value.error.toString())
            is ArticleImageProgress -> showImageProgress()
            is ArticleImageDownloaded -> showImage(value.image)
            is ArticleImageError -> showImageError(value.error.toString())
        }.exhaustive
    }

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
    private val image: ImageView
        get() = view.findViewById(R.id.image)
    private val imageProgress: View
        get() = view.findViewById(R.id.image_progress)
    private val imageError: TextView
        get() = view.findViewById(R.id.image_error)
    private val extract: TextView
        get() = view.findViewById(R.id.extract)
    private val error: TextView
        get() = view.findViewById(R.id.error)

    override fun onClickDownloadRandomPage(onClick: () -> Unit) {
        button.setOnClickListener { onClick() }
    }

    override fun onClickClear(onClick: () -> Unit) {
        clearButton.setOnClickListener { onClick() }
    }

    fun showEmpty() {
        progress.visibility = View.INVISIBLE
        title.visibility = View.INVISIBLE
        description.visibility = View.INVISIBLE
        extract.visibility = View.INVISIBLE
        error.visibility = View.INVISIBLE
        image.visibility = View.INVISIBLE
        imageProgress.visibility = View.INVISIBLE
        imageError.visibility = View.INVISIBLE
    }

    fun showProgress() {
        progress.visibility = View.VISIBLE
        title.visibility = View.INVISIBLE
        description.visibility = View.INVISIBLE
        extract.visibility = View.INVISIBLE
        error.visibility = View.INVISIBLE
        image.visibility = View.INVISIBLE
        imageProgress.visibility = View.INVISIBLE
        imageError.visibility = View.INVISIBLE
    }

    fun showArticle(article: WikipediaArticle) {
        progress.visibility = View.INVISIBLE
        title.text = article.title
        title.visibility = View.VISIBLE
        description.text = article.description
        description.visibility = View.VISIBLE
        extract.text = article.extract
        extract.visibility = View.VISIBLE
        error.visibility = View.INVISIBLE
        image.visibility = View.INVISIBLE
        imageProgress.visibility = View.INVISIBLE
        imageError.visibility = View.INVISIBLE
    }

    fun showError(message: String) {
        progress.visibility = View.INVISIBLE
        title.visibility = View.INVISIBLE
        description.visibility = View.INVISIBLE
        extract.visibility = View.INVISIBLE
        error.text = message
        error.visibility = View.VISIBLE
        image.visibility = View.INVISIBLE
        imageProgress.visibility = View.INVISIBLE
        imageError.visibility = View.INVISIBLE
    }

    fun showImageProgress() {
        imageProgress.visibility = View.VISIBLE
        image.visibility = View.INVISIBLE
        imageError.visibility = View.INVISIBLE
    }

    fun showImage(bitmap: Bitmap) {
        imageProgress.visibility = View.INVISIBLE
        imageError.visibility = View.INVISIBLE
        image.setImageBitmap(bitmap)
        image.visibility = View.VISIBLE
    }

    fun showImageError(message: String) {
        imageProgress.visibility = View.INVISIBLE
        image.visibility = View.INVISIBLE
        imageError.text = message
        imageError.visibility = View.VISIBLE
    }
}

val <T> T.exhaustive: T
    get() = this