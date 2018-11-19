package eu.jaecktec.andrxs

class StateContext<T : Any> internal constructor(
    private var state: T,
    private val valueChangedCallback: StateValueChangedCallback
) {

    fun getState(): T {
        return this.state
    }

    fun setState(state: T) {
        this.state = state
        valueChangedCallback.onStateValueChange(state)
    }
}