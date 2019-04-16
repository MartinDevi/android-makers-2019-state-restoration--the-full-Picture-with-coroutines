package com.example.mdevillers.mvpcoroutines

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mdevillers.mvpcoroutines.model.Wikipedia
import com.example.mdevillers.mvpcoroutines.mvp.ViewProxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class MainActivity(
    private val scope: CoroutineScope = MainScope()
) : AppCompatActivity(), CoroutineScope by scope {

    override val coroutineContext: CoroutineContext
        get() = scope.coroutineContext

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val viewProxy = ViewProxy(this)

        viewProxy.onClickDownloadRandomPage {
            viewProxy.showProgress()
            launch {
                val article = try {
                    Wikipedia.getRandomArticle()
                } catch (e: Exception) {
                    viewProxy.showError(e.message ?: "Unknown error")
                    return@launch
                }
                viewProxy.showArticle(article)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}


