package eu.jaecktec.andrxs

import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor

internal data class SelectorContextHolder(
    val method: KCallable<*>,
    val receiver: Any,
    val selectorName: String,
    val modelClass: KClass<out Any>,
    var currentValue: Any
) {
    companion object {
        fun create(func: KFunction<*>, receiver: Any): SelectorContextHolder {
            val annotation = func.getAnnotation(Selector::class)!!
            return SelectorContextHolder(
                selectorName = annotation.name,
                receiver = receiver,
                method = func,
                modelClass = func.parameters.find { it.kind == KParameter.Kind.VALUE }!!.type.classifier as KClass<out Any>,
                currentValue = (func.returnType.classifier as KClass<*>).primaryConstructor!!.callBy(mapOf())
            )

        }
    }

    fun updateCurrentValue(stateContexts: Map<KClass<*>, StateContext<*>>) {
        val methodParams = this.method.parameters
        val instanceParam = methodParams.find { it -> it.kind == KParameter.Kind.INSTANCE }!!
        val modelParam = methodParams.find { it.type.classifier == this.modelClass }!!
        val m = mutableMapOf(
            instanceParam to this.receiver,
            modelParam to stateContexts[this.modelClass]!!.getState()
        )
        currentValue = this.method.callBy(m)!!
    }
}