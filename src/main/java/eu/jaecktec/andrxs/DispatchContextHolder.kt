package eu.jaecktec.andrxs

import kotlin.reflect.KCallable
import kotlin.reflect.KClass

internal data class DispatchContextHolder(
    val method: KCallable<*>,
    val receiver: Any,
    val modelClass: KClass<out Any>
) {
    companion object {
        fun create(method: KCallable<*>, receiver: Any): DispatchContextHolder {
            return DispatchContextHolder(
                method,
                receiver,
                receiver.getClassAnnotation(State::class).model
            )
        }
    }
}