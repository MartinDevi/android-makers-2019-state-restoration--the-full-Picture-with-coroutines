package mvpcoroutines.com.example.mdevillers.mvpcoroutines.framework

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

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