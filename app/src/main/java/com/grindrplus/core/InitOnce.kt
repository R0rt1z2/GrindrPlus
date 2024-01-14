package com.grindrplus.core

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * A property delegate that allows to initialize a property only once.
 * @see <a href="https://stackoverflow.com/a/48445081/14118812">StackOverflow Reference</a>
 */
class InitOnce<T> : ReadWriteProperty<Any, T> {
    private object EMPTY
    private var value: Any? = EMPTY

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        check(value != EMPTY) { "Value isn't initialized" }
        return value as T
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        check(this.value == EMPTY) { "Value is already initialized" }
        this.value = value
    }
}