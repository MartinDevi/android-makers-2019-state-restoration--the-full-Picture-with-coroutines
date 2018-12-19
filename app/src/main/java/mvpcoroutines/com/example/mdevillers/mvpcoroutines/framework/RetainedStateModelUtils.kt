package mvpcoroutines.com.example.mdevillers.mvpcoroutines.framework

import android.os.Bundle
import android.os.Parcelable
import kotlinx.coroutines.Deferred

const val KEY_ARG = "key"

object StringStateHelper {

    inline operator fun <R: Parcelable> RetainedStateRepository.get(key: String, crossinline execute: (String) -> R): RetainedStateModel<Bundle, R> =
        get(key) { execute(it.getString(KEY_ARG)!!) }

    fun <R: Parcelable> RetainedStateModel<Bundle, R>.start(args: String): Deferred<Result<R>> =
        start(Bundle().apply { putString(KEY_ARG, args) })
}

object IntStateHelper {

    inline operator fun <R: Parcelable> RetainedStateRepository.get(key: String, crossinline execute: (Int) -> R): RetainedStateModel<Bundle, R> =
        get(key) { execute(it.getInt(KEY_ARG)) }

    fun <R: Parcelable> RetainedStateModel<Bundle, R>.start(args: Int): Deferred<Result<R>> =
        start(Bundle().apply { putInt(KEY_ARG, args) })
}

inline operator fun <R: Parcelable> RetainedStateRepository.get(key: String, crossinline execute: () -> R): RetainedStateModel<Bundle, R> =
    get(key) { execute() }

fun <R: Parcelable> RetainedStateModel<Bundle, R>.start(): Deferred<Result<R>> =
    start(Bundle.EMPTY)