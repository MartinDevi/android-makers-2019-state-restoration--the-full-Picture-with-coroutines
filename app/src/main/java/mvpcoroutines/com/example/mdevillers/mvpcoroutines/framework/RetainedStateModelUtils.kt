package mvpcoroutines.com.example.mdevillers.mvpcoroutines.framework

import android.os.Bundle
import android.os.Parcelable
import kotlinx.coroutines.Deferred



const val KEY_ARG = "key"

inline operator fun <R: Parcelable> RetainedStateRepository.get(key: String, crossinline execute: () -> R): RetainedStateModel<Bundle, R> =
    get(key) { execute() }

inline operator fun <R: Parcelable> RetainedStateRepository.get(key: String, crossinline execute: (String) -> R): RetainedStateModel<Bundle, R> =
    get(key) { execute(it.getString(KEY_ARG)!!) }

fun <R: Parcelable> RetainedStateModel<Bundle, R>.start(): Deferred<Result<R>> =
    start(Bundle.EMPTY)

fun <R: Parcelable> RetainedStateModel<Bundle, R>.start(args: String): Deferred<Result<R>> =
    start(Bundle().apply { putString(KEY_ARG, args) })