package eu.jaecktec.andrxs

import kotlin.reflect.KCallable
import kotlin.reflect.KClass

internal fun <R, T : Annotation> KCallable<R>.getAnnotation(kClass: KClass<T>): T? {
    @Suppress("UNCHECKED_CAST")
    return this.annotations.find { it.annotationClass == kClass } as T?
}

internal fun <E : KCallable<*>, R : Annotation> Collection<E>.filterByAnnotation(kClass: KClass<R>): List<E> {
    return this.filter {
        it.getAnnotation(kClass) != null
    }
}


internal fun <T : Annotation> Any.getClassAnnotation(annotationClass: KClass<T>): T {
    @Suppress("UNCHECKED_CAST")
    return (this::class.annotations.find { it -> it.annotationClass == annotationClass } as T?
        ?: throw IllegalArgumentException("State needs ${annotationClass.simpleName} Annotation"))
}