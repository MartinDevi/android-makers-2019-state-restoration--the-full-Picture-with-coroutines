package mvpcoroutines.com.example.mdevillers.mvpcoroutines.mvp

import android.graphics.Bitmap
import mvpcoroutines.com.example.mdevillers.mvpcoroutines.model.Article

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