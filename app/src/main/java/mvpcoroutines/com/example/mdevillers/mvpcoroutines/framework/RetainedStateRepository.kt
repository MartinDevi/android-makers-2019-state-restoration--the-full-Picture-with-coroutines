package mvpcoroutines.com.example.mdevillers.mvpcoroutines.framework

import android.os.Bundle
import android.os.Parcelable
import androidx.lifecycle.ViewModelProvider

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
            RetainedStateModel(
                bundle.getBundle(key) ?: Bundle(),
                viewModelProvider.get(key, DeferredViewModel::class.java) as DeferredViewModel<T>
            )
        } as RetainedStateModel<T>

    fun savedInstanceState(): Bundle =
        bundle.apply {
            retainedStates.forEach {
                putBundle(it.key, it.value.savedInstanceState())
            }
        }
}