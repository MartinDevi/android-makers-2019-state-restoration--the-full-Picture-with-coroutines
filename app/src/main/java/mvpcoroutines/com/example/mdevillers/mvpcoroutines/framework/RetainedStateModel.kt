package mvpcoroutines.com.example.mdevillers.mvpcoroutines.framework

import android.os.Bundle
import android.os.Parcelable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Handles retaining a state of a computed result, combining view models and saved instance states.
 */
class RetainedStateModel<T: Parcelable>(
    private val bundle: Bundle,
    private val deferredViewModel: DeferredViewModel<T>
) {
    val deferred: Deferred<Result<T>>?
        get() = deferredViewModel.deferred

    var success: T?
        get() = bundle.getParcelable(STATE_SUCCESS)
        private set(value) { bundle.putParcelable(STATE_SUCCESS, value) }

    var error: Throwable?
        get() = bundle.getSerializable(STATE_ERROR) as Throwable?
        private set(value) { bundle.putSerializable(STATE_ERROR, value) }

    var arguments: Bundle?
        get() = bundle.getBundle(STATE_ARGUMENTS)
        private set(value) { bundle.putBundle(STATE_ARGUMENTS, value) }

    /**
     * Fold the existing state of the value.
     *
     * @param onComplete
     *  Called if the result has been successfully obtained, with the computed value as parameter.
     * @param onError
     *  Called if the task failed to obtain the result, with the error thrown as parameter.
     * @param onProgress
     *  Called if the result is still being fetched, with the deferred result as parameter.
     *
     *  Note that this is often the default state of the view, in which case there's nothing to do.
     */
    inline fun getResult(
        onComplete: (T) -> Unit  = {},
        onError: (Throwable) -> Unit = {},
        onProgress: (Deferred<Result<T>>) -> Unit = {},
        onInterrupted: (Bundle) -> Unit = {}
    ) {
        val deferred = deferred
        if (deferred == null) {
            /*
             * See if values can be fetched from previously saved state.
             */
            val success = this.success
            val error = this.error
            val arguments = this.arguments
            when {
                success != null -> onComplete(success)
                error != null -> onError(error)
                arguments != null -> onInterrupted(arguments)
                // Else callback for "ready"/"idle" state?
            }
        } else {
            if (deferred.isCompleted) {
                val completed = deferred.getCompleted()
                completed.fold(
                    onSuccess = onComplete,
                    onFailure = onError
                )
            } else {
                onProgress(deferred)
            }
        }
    }

    /**
     * Start an async process.
     *
     * @param arguments
     *  An optional description of the process arguments which are used by the suspending block. If this is not-null,
     *  then it can be used later to restart the process.
     * @param block
     *  The suspending block which computes the result.
     *  This block should make sure to *not* capture any context-scoped instances, since it will be retained across
     *  configuration changes.
     */
    fun asyncCatching(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        arguments: Bundle? = null,
        block: suspend CoroutineScope.() -> T
    ): Deferred<Result<T>> {
        clearResult(arguments)
        return deferredViewModel.async(
            context,
            start
        ) {
            runCatching { block() }
        }.also {
            deferredViewModel.deferred = it
        }
    }

    fun clearResult(arguments: Bundle? = null) {
        deferredViewModel.deferred?.cancel()
        deferredViewModel.deferred = null
        success = null
        error = null
        this.arguments = arguments
    }

    fun savedInstanceState(): Bundle {
        getResult(
            onComplete = { success = it },
            onError = { error = it }
        )
        return bundle
    }

    companion object {
        const val STATE_SUCCESS = "SUCCESS"
        const val STATE_ERROR = "ERROR"
        const val STATE_ARGUMENTS = "ARGUMENTS"
    }
}