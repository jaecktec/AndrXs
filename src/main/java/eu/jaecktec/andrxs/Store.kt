package eu.jaecktec.andrxs

import kotlin.reflect.KClass

interface Store {
    companion object {
        fun createStore(): Store {
            return StoreImpl()
        }
    }

    fun addState(state: Any)
    fun <T : Any> getState(stateClass: KClass<T>): T
    fun onCreate(obj: Any)
    fun onDestroy(obj: Any)
    fun dispatch(obj: Any)
}



