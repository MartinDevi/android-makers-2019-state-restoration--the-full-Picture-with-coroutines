package com.example.mdevillers.mvpcoroutines.framework

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
    private val retainedStates = mutableMapOf<String, RetainedStateModel<*, *>>()

    operator fun <T: Parcelable, R: Parcelable> get(key: String, execute: suspend (T) -> R): RetainedStateModel<T, R> =
        RetainedStateModel(
            bundle.getBundle(key) ?: Bundle(),
            getDeferredViewModel(key),
            execute
        ).also {
            retainedStates[key] = it
        }

    @Suppress("UNCHECKED_CAST")
    private fun <R : Parcelable> getDeferredViewModel(key: String) =
        viewModelProvider.get(key, DeferredViewModel::class.java) as DeferredViewModel<R>

    fun savedInstanceState(): Bundle =
        bundle.apply {
            retainedStates.forEach {
                putBundle(it.key, it.value.save())
            }
        }
}