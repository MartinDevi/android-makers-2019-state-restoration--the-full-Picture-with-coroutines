package com.example.mdevillers.mvpcoroutines.framework

import android.os.Bundle
import android.os.Parcelable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

/**
 * Handles retaining a state of a computed result, combining view models and saved instance states.
 */
class RetainedStateModel<T: Parcelable, R: Parcelable>(
    private val bundle: Bundle,
    private val deferredViewModel: DeferredViewModel<R>,
    private val execute: suspend (T) -> R
) {
    val deferred: Deferred<Result<R>>?
        get() = deferredViewModel.deferred

    var success: R?
        get() = bundle.getParcelable(STATE_SUCCESS)
        private set(value) { bundle.putParcelable(STATE_SUCCESS, value) }

    var error: Throwable?
        get() = bundle.getSerializable(STATE_ERROR) as Throwable?
        private set(value) { bundle.putSerializable(STATE_ERROR, value) }

    var arguments: T?
        get() = bundle.getParcelable(STATE_ARGUMENTS)
        private set(value) { bundle.putParcelable(STATE_ARGUMENTS, value) }

    init {
        persistResult()

        val arguments = this.arguments
        if (arguments != null) {
            if (deferred == null && success == null) {
                @Suppress("DeferredResultUnused") // Should obtained deferred during `bind` call right after
                start(arguments)
            }
        }
    }

    /**
     * Bind to the existing state of the value.
     *
     * Note that since "idle" should be the default state of the view, there's no callback for it because there should
     * be nothing to do.
     *
     * @param onActive
     *  Called if the result is still being fetched, with the deferred result as parameter.
     * @param onSuccess
     *  Called if the result has been successfully obtained, with the computed value as parameter.
     * @param onError
     *  Called if the task failed to obtain the result, with the error thrown as parameter.
     */
    inline fun bind(
        onActive: (Deferred<Result<R>>) -> Unit = {},
        onSuccess: (R) -> Unit = {},
        onError: (Throwable) -> Unit ={}
    ) {
        val success = this.success
        val error = this.error
        val deferred = deferred
        when {
            success != null -> onSuccess(success)
            error != null -> onError(error)
            deferred != null -> onActive(deferred)
        }
    }

    fun save(): Bundle {
        persistResult()
        return bundle
    }

    /**
     * Start an async process with the provided arguments.
     */
    fun start(arguments: T): Deferred<Result<R>> {
        clearResult(arguments)
        return deferredViewModel.async {
            runCatching { execute(arguments) }
        }.also {
            deferredViewModel.deferred = it
        }
    }

    fun clear() {
        clearResult()
    }

    private fun clearResult(arguments: T? = null) {
        deferredViewModel.deferred?.cancel()
        deferredViewModel.deferred = null
        success = null
        error = null
        this.arguments = arguments
    }

    private fun persistResult() {
        val deferred = this.deferred ?: return
        if (deferred.isCompleted) {
            val completed = deferred.getCompleted()
            completed.fold(
                onSuccess = { success = it },
                onFailure = { error = it.takeUnless { throwable -> throwable is CancellationException } }
            )
        }
    }

    companion object {
        const val STATE_SUCCESS = "SUCCESS"
        const val STATE_ERROR = "ERROR"
        const val STATE_ARGUMENTS = "ARGUMENTS"
    }
}