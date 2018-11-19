package eu.jaecktec.andrxs

import io.reactivex.Observable
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.After
import org.junit.Before
import org.junit.Test


class StoreTest {

    internal data class SampleAction(val value: String)

    internal data class SampleStateModel(
        val value: String = "test",
        val secondValue: String = "second"
    )

    @State(
        model = SampleStateModel::class
    )
    internal class SampleState {

        @Selector(name = "example")
        fun viewName(state: SampleStateModel): String {
            return state.value
        }

        @Selector(name = "example-full")
        fun everything(state: SampleStateModel): SampleStateModel {
            return state
        }

        @Action(SampleAction::class)
        fun sampleAction(ctx: StateContext<SampleStateModel>, action: SampleAction) {
            ctx.setState(ctx.getState().copy(value = action.value))
        }
    }

    internal class SampleActivity {
        @Select("example")
        lateinit var stringObservable: Observable<String>

        @Select("example-full")
        lateinit var fullModelObservable: Observable<SampleStateModel>
    }


    private lateinit var store: Store
    private lateinit var activity: SampleActivity

    @Before
    fun setUp() {
        activity = SampleActivity()
        store = Store.createStore()
        store.addState(SampleState())
        store.onCreate(activity)
    }

    @After
    fun tearDown() {
        store.onDestroy(activity)
    }

    @Test
    fun `😎 - should call observable on state change`() {
        // given
        var observedValue = ""
        var numCalled = 0
        activity.stringObservable.subscribe {
            observedValue = it
            numCalled++
        }

        // when
        store.dispatch(SampleAction("someValue"))
        // then
        assertThat(observedValue).isEqualTo("someValue")
        assertThat(numCalled).isEqualTo(2)
    }

    @Test
    fun `😎 - should not call observable on state change - when selector didn't change`() {
        // given
        var observedValue = ""
        var numCalled = 0
        activity.stringObservable.subscribe {
            observedValue = it
            numCalled++
        }

        // when
        store.dispatch(SampleAction("test"))
        // then
        assertThat(observedValue).isEqualTo("test")
        assertThat(numCalled).isEqualTo(1)
    }

    @Test
    fun `🤯 - should throw if selector does not exist`() {
        // given
        val activity = object {
            @Select("I-DO-NOT-EXIST")
            lateinit var stringObservable: Observable<String>
        }

        // when
        val assertThatThrownBy = assertThatThrownBy { store.onCreate(activity) }

        // then
        assertThatThrownBy.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy.hasMessage("could not find a @Selector for name=I-DO-NOT-EXIST")
    }

    @Test
    fun `🤯 - should throw if selector is double`() {
        // given
        @State(
            model = String::class
        )
        class AnotherState {

            @Selector(name = "example")
            fun viewName(state: String): String {
                return state
            }

        }

        // when
        val assertThatThrownBy = assertThatThrownBy { store.addState(AnotherState()) }

        // then
        assertThatThrownBy.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy.hasMessage("Selector example needs to have an unique name")
    }

    internal class SomeStupidModel {
        constructor(a: Int) {
            println(a)
        }

        constructor(b: String) {
            println(b)
        }
    }

    @Test
    fun `🤯 - should throw if model class has no default constructor`() {
        // given


        @State(
            model = SomeStupidModel::class
        )
        class AnotherState {
            @Selector(name = "example")
            fun viewName(state: SomeStupidModel): SomeStupidModel {
                return state
            }
        }

        // when
        val assertThatThrownBy = assertThatThrownBy { store.addState(AnotherState()) }

        // then
        assertThatThrownBy.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy.hasMessage("State.model needs a default constructor")
    }

    @Test
    fun `🤯 - should throw if state is not annotated`() {
        // given
        class AnotherState {
            @Selector(name = "example")
            fun viewName(state: SomeStupidModel): SomeStupidModel {
                return state
            }
        }

        // when
        val assertThatThrownBy = assertThatThrownBy { store.addState(AnotherState()) }

        // then
        assertThatThrownBy.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy.hasMessage("State needs State Annotation")
    }

    internal data class SomeModelWithRequiredParams(val a: String)

    @Test
    fun `🤯 - should throw if model class required params in constructor`() {
        // given


        @State(
            model = SomeModelWithRequiredParams::class
        )
        class AnotherState {
            @Selector(name = "example")
            fun viewName(state: SomeModelWithRequiredParams): SomeModelWithRequiredParams {
                return state
            }
        }

        // when
        val assertThatThrownBy = assertThatThrownBy { store.addState(AnotherState()) }

        // then
        assertThatThrownBy.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy.hasMessage("State.model constructor can't have required parameters")
    }


}