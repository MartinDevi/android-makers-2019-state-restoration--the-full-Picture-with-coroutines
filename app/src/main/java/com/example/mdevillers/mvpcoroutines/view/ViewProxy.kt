package com.example.mdevillers.mvpcoroutines.view

import android.app.Activity
import android.graphics.Bitmap
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.example.mdevillers.mvpcoroutines.R
import com.example.mdevillers.mvpcoroutines.model.Article

class ViewProxy(private val view: View) {

    constructor(activity: Activity): this(activity.window.decorView)

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

    fun onClickDownloadRandomPage(onClick: () -> Unit) {
        button.setOnClickListener { onClick() }
    }

    fun onClickClear(onClick: () -> Unit) {
        clearButton.setOnClickListener { onClick() }
    }

    fun showEmpty() {
        progress.visibility = View.INVISIBLE
        title.visibility = View.INVISIBLE
        description.visibility = View.INVISIBLE
        extract.visibility = View.INVISIBLE
        error.visibility = View.INVISIBLE
        thumbnail.visibility = View.INVISIBLE
        thumbnailProgress.visibility = View.INVISIBLE
        thumbnailError.visibility = View.INVISIBLE
    }

    fun showProgress() {
        progress.visibility = View.VISIBLE
        title.visibility = View.INVISIBLE
        description.visibility = View.INVISIBLE
        extract.visibility = View.INVISIBLE
        error.visibility = View.INVISIBLE
        thumbnail.visibility = View.INVISIBLE
        thumbnailProgress.visibility = View.INVISIBLE
        thumbnailError.visibility = View.INVISIBLE
    }

    fun showArticle(article: Article) {
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

    fun showError(message: String) {
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

    fun showThumbnailProgress() {
        thumbnailProgress.visibility = View.VISIBLE
        thumbnail.visibility = View.INVISIBLE
        thumbnailError.visibility = View.INVISIBLE
    }

    fun showThumbnail(bitmap: Bitmap) {
        thumbnailProgress.visibility = View.INVISIBLE
        thumbnailError.visibility = View.INVISIBLE
        thumbnail.setImageBitmap(bitmap)
        thumbnail.visibility = View.VISIBLE
    }

    fun showThumbnailError(message: String) {
        thumbnailProgress.visibility = View.INVISIBLE
        thumbnail.visibility = View.INVISIBLE
        thumbnailError.text = message
        thumbnailError.visibility = View.VISIBLE
    }
}