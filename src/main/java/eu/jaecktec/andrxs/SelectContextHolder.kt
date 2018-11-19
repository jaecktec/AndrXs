package eu.jaecktec.andrxs

import io.reactivex.subjects.BehaviorSubject
import kotlin.reflect.KCallable
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.isAccessible

internal data class SelectContextHolder(
    val method: KCallable<*>,
    val receiver: Any,
    val selectorName: String,
    val subject: BehaviorSubject<Any>
) {
    companion object {
        fun create(property: KProperty<*>, receiver: Any): SelectContextHolder {
            val initialAccessibility = property.isAccessible
            property.isAccessible = true
            val mutableProperty = property as KMutableProperty<*>

            val selectorName = mutableProperty.getAnnotation(Select::class)!!.selector
            val subject = BehaviorSubject.create<Any>()
            mutableProperty.setter.call(receiver, subject.toSerialized())
            val result = SelectContextHolder(
                method = mutableProperty,
                receiver = receiver,
                selectorName = selectorName,
                subject = subject
            )
            property.isAccessible = initialAccessibility
            return result
        }
    }
}