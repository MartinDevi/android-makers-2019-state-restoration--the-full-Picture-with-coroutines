package com.example.mdevillers.mvpcoroutines.framework

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class DeferredViewModel<T: Parcelable>: ViewModel(), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    var deferred: Deferred<Result<T>>? = null

    private val job = SupervisorJob()

    override fun onCleared() {
        super.onCleared()
        job.cancel()
    }
}