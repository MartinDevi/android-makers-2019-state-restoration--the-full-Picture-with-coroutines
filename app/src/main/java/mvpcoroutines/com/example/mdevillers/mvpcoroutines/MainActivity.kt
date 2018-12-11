package mvpcoroutines.com.example.mdevillers.mvpcoroutines

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import kotlinx.coroutines.*
import mvpcoroutines.com.example.mdevillers.mvpcoroutines.framework.RetainedStateRepository
import mvpcoroutines.com.example.mdevillers.mvpcoroutines.model.ArticleRepository
import mvpcoroutines.com.example.mdevillers.mvpcoroutines.model.ArticleThumbnailRepository
import mvpcoroutines.com.example.mdevillers.mvpcoroutines.mvp.Presenter
import mvpcoroutines.com.example.mdevillers.mvpcoroutines.mvp.ViewProxy

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


