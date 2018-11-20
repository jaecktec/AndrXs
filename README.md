[![](https://jitpack.io/v/jaecktec/AndrXs.svg)](https://jitpack.io/#jaecktec/AndrXs)

# Welcome to AndrXs!

## Introduction
This Library aims to decouple parts of your application that should work together.
AndrXs is an android implementation / library havily influenced on [ngxs/store](https://ngxs.gitbook.io/ngxs). However AndrXs only supports a subset of the functionality.

## Concept
There are 4 major concepts to AndrXs:
-   Store: Global state container, action dispatcher and selector
-   Actions: Class describing the action to take and its associated metadata
-   State: Class definition of the state
-   Selects: State slice selectors

![concept graph](https://github.com/jaecktec/AndrXs/raw/master/graph_concept.png)

# Installation
The library is currently available on **JitPack**

extend your root `build.gradle` with:
```
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
and add the dependencye to your app -> `build.gradle`
```
dependencies {
    implementation 'com.github.jaecktec:AndrXs:v0.0.1'
}
```

# API
Following will describe the API with some examples

## Creating a Store
A state management system should not be cleared when the activity switches. This is why we extend an Application class.

 `MainApplication.kt`

```
class MainApplication : Application() {

    companion object {
        private val TAG = MainApplication::class.java.simpleName
		private var sInstance: MainApplication? = null

		fun getAppContext(): MainApplication {
            return sInstance!!
        }
    }

    private lateinit var store: Store

    override fun onCreate() {
        super.onCreate()
        sInstance = this
		store = Store.createStore()
        store.addState(ZooState())  // add your states here
    }

    fun getStore(): Store {
        return store
  }

}
```
## Creating States

### State Model
Every State needs a model. Those can be primitives like `String::class`. However for most use cases a `data class` is way more suitable.

```
data class ZooStateModel(var zooName: String = "Borealis")
```

However note, that every Model needs a primary constructor without required arguments. Default parameters can be defined and will be used.

### State
States need to be annotated with the `@State` annotation. Lets create a State based on our `ZooStateModel`.

```
@State(
    model = ZooStateModel::class
)
class ZooState {}
```

Remember, every state has to be registered on the `Store`!!!

## Select
Selections are helpers to select slices of the store. A valid setup consists of a `@Selector` and zero to infinite `@Select` annotations. The linking is archived through the name attributes.
### Select Annotations
You can select slices of data from the store by using the `@Select` annotation.
The State will memoize the outcome of the function and notify your selected Observables in case the slice of the store gets changed.

The following example shows how to create a `@Selector`
```
@State(
    model = ZooStateModel::class
)
class UiState {

    @Selector(name = "ZooStateModel::currentZooCity")
    fun currentZooCity(state: ZooStateModel): String {
        return state.zooName
  }
}
```
 whenever you call `StateContext#setState` all `@Selector` annotations are evaluated. In case the result has changed, the respective `@Select` marked observables will be notified.
 
In your Application we can now define an observable of type String with our `@Select` annotation:

```
@Select(selector = "ZooStateModel::currentZooCity")
private lateinit var mCurrentZoo: Observable<String>
```

On calling `store.onCreate(this)` our observable gets initialized with the latest value from the store. 

## Actions
Actions can either be thought of as a command which should trigger something to happen, or as the resulting event of something that has already happened.

Each actions can be a generic `POJO` class or `data` class

### Simple Action
In the following example, lets assume we want to change the `zooLocation` in our state, because Old Athen is way more beautiful than the crowded Borealis.
```
class ChangeToZooToOldAthen()
```

Later in our state class, we will listen to this action and mutate our state, in this case changing toe `zooLocation` to "Old Athen".

### Actions with Metadata
Often we need metadata in our actions. Because we are lazy we don't want to create hundreds of classes for all the zooLocations. We can simply create a `data class` with a parameter:

```
data class ChangeZooLocation(val location: String)
```


### Dispatching actions
To dispatch actions, you need to get the store from the application

```
private val mMainApplication: MainApplication by lazy(LazyThreadSafetyMode.NONE) {
  MainApplication.getAppContext()
}
```
```
private val mStore: Store by lazy(LazyThreadSafetyMode.NONE) {
  mMainApplication.getStore()
}
```

afterwards you can dispatch actions with the simple api call
`this.store.dispatch(ChangeZooLocation("Old Athen"))`

In this case it would make sense to execute the dispatch inside a `OnClickListener` of a button.

### Handling actions
Action handlers are meant to manipulate the state. This can be done through some business logic we want to hide from the ui logic or through some API calls like a REST service.

To create an action handler just add a `@Action(YourAction::class)` annotated function within the `@State` annotated class.
Your handler will have access to the current Model, to your action object and to `this` of your state class.
```
@State(
    model = ZooStateModel::class
)
class UiState {

    @Action(ChangeZooLocation::class)
    fun currentZooCity(ctx: StateContext<ZooStateModel>, action: ChangeZooLocation) {
        ctx.setState(ctx.getState().copy(zooName = action.location))
  }
}
```
*** always use setState to persist your state, otherwise the Selectors are not notified!!!***


