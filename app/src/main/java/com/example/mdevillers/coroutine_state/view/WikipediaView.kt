package com.example.mdevillers.coroutine_state.view

import android.graphics.Bitmap
import com.example.mdevillers.coroutine_state.model.WikipediaArticle

interface WikipediaView {

    var state: State

    fun onClickDownloadRandomPage(onClick: () -> Unit)
    fun onClickClear(onClick: () -> Unit)

    sealed class State {
        object Empty : State()
        object ArticleProgress : State()
        data class ArticleDownloaded(val article: WikipediaArticle) : State()
        data class ArticleError(val error: Throwable) : State()
        data class ArticleImageProgress(val article: WikipediaArticle) : State()
        data class ArticleImageDownloaded(val article: WikipediaArticle, val image: Bitmap) : State()
        data class ArticleImageError(val article: WikipediaArticle, val error: Throwable) : State()
    }
}