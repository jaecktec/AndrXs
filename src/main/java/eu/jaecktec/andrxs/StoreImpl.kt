package eu.jaecktec.andrxs

import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.functions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

internal class StoreImpl : Store, StateValueChangedCallback {
    private val stateHandler = mutableListOf<Any>()

    private val stateContexts = mutableMapOf<KClass<*>, StateContext<*>>()
    private var selects = listOf<SelectContextHolder>()
    private var selectors = listOf<SelectorContextHolder>()

    override fun addState(state: Any) {
        initialiseStore(state)
        selectors = state::class.functions.filterByAnnotation(Selector::class)
            .map { func ->
                val annotation = func.getAnnotation(Selector::class)!!
                verifySelectorDoesNotExist(annotation)
                val result = SelectorContextHolder.create(func, state)
                result.updateCurrentValue(stateContexts)
                result
            }.plus(selectors)
    }

    override fun <T : Any> getState(stateClass: KClass<T>): T {
        return findStateContext(stateClass).getState()
    }

    // select stuff
    override fun onCreate(obj: Any) {
        val objSelects = obj::class.memberProperties
            .filterByAnnotation(Select::class)
            .map { SelectContextHolder.create(it, obj) }

        objSelects.forEach { select ->
            val selector = selectors.find { selector ->
                selector.selectorName == select.selectorName
            } ?: throw IllegalArgumentException("could not find a @Selector for name=${select.selectorName}")
            select.subject.onNext(selector.currentValue)
        }

        selects += objSelects
    }

    override fun onDestroy(obj: Any) {
        selects -= selects.filter { it.receiver == obj }
    }

    override fun dispatch(obj: Any) {
        val methods = stateHandler.flatMap { state ->
            state::class.members
                .filterByAnnotation(Action::class)
                .filter { it.getAnnotation(Action::class)!!.type == obj::class }
                .map { DispatchContextHolder.create(it, state) }
        }

        methods.forEach { holder ->
            val parameters = holder.method.parameters

            val instanceParam = holder.method.parameters.find { it.kind == KParameter.Kind.INSTANCE }!!

            val m = mutableMapOf(instanceParam to holder.receiver)
            parameters.find { it.type.classifier == StateContext::class }?.run {
                m[this] = findStateContext(holder.modelClass)
            }
            parameters.find { it.type.classifier == obj::class }?.run {
                m[this] = obj
            }

            val a = mutableListOf(*holder.method.parameters.toTypedArray())
            m.keys.forEach { a.remove(it) }
            a.find { !it.isOptional }?.run {
                throw IllegalArgumentException("Action method can\'t have required parameters except StateContext and Action Type")
            }

            holder.method.callBy(m)
        }

    }

    private fun initialiseStore(state: Any) {
        val stateAnnotation = state.getClassAnnotation(State::class)

        val primaryConstructor = stateAnnotation.model.primaryConstructor
            ?: throw IllegalArgumentException("State.model needs a default constructor")

        primaryConstructor.parameters.find { !it.isOptional }?.run {
            throw IllegalArgumentException("State.model constructor can't have required parameters")
        }

        val stateContext =
            StateContext(primaryConstructor.callBy(mapOf()), this)

        stateContexts[stateAnnotation.model] = stateContext
        stateHandler.add(state)
    }

    override fun onStateValueChange(state: Any) {
        val stateClass = state::class
        val affectedSelectors = this.selectors.filter { it.modelClass == stateClass }

        affectedSelectors.forEach { context ->
            val lastValue = context.currentValue
            context.updateCurrentValue(stateContexts)
            if (context.currentValue != lastValue) {
                selects.filter { select ->
                    select.selectorName == context.selectorName
                }.map { select ->
                    select.subject
                }.forEach { subj ->
                    subj.onNext(context.currentValue)
                }
            }
        }
    }

    private fun computeContextValue(context: SelectorContextHolder): Any {
        val methodParams = context.method.parameters
        val instanceParam = methodParams.find { it -> it.kind == KParameter.Kind.INSTANCE }!!
        val modelParam = methodParams.find { it.type.classifier == context.modelClass }!!
        val m = mutableMapOf(
            instanceParam to context.receiver,
            modelParam to stateContexts[context.modelClass]!!.getState()
        )
        return context.method.callBy(m)!!
    }

    private fun verifySelectorDoesNotExist(annotation: Selector) {
        selectors.find { it.selectorName == annotation.name }?.run {
            throw IllegalArgumentException("Selector ${annotation.name} needs to have an unique name")
        }
    }

    private fun <T : Any> findStateContext(stateClass: KClass<T>): StateContext<T> {
        @Suppress("UNCHECKED_CAST")
        return stateContexts[stateClass] as StateContext<T>?
            ?: throw IllegalArgumentException("requested type has no state")
    }
}