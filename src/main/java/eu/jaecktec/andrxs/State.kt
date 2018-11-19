package eu.jaecktec.andrxs

import kotlin.reflect.KClass

annotation class State(val model: KClass<out Any>)