/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.jelly.model

/**
 * An event holder.
 * Easiest way to use this is by inheriting directly from [Event] and using itself as T.
 * If you don't control the type directly then check [Event.Wrapper]
 */
abstract class Event<T> {
    /**
     * The thing to use for event handling.
     * Default implementation will return itself casted to T.
     */
    open val content = this as T

    /**
     * Whether this event has already been handled.
     */
    private var handled = false

    /**
     * Handle the event if not already handled.
     * @param unit The callback that will be executed if the event hasn't been handled already
     * @return Whether the event has been handled here
     */
    fun handle(unit: (content: T) -> Unit) = if (!handled) {
        handled = true
        unit(content)
        true
    } else {
        false
    }

    /**
     * An event which holds another object.
     */
    class Wrapper<T>(override val content: T) : Event<T>()
}
