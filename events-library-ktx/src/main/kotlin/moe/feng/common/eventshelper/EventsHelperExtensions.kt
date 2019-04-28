package moe.feng.common.eventshelper

inline fun <reified T : Any> EventsHelper.of(tag: String? = null): T {
    return this.of(T::class.java, tag)
}

fun EventsHelper.registerListener(vararg listenerPairs: Pair<Any, String?>) {
    for ((listener, tag) in listenerPairs) {
        this.registerListener(listener, tag)
    }
}

val EventsHelper.listeners get() = ListenersCollection(this)

class ListenersCollection internal constructor(private val eventsHelper: EventsHelper) {

    operator fun plusAssign(list: List<Any>) {
        for (obj in list) {
            if (obj is Pair<*, *>) {
                eventsHelper.registerListener(obj.first!!, obj.second as String)
            } else {
                eventsHelper.registerListener(obj)
            }
        }
    }

    operator fun plusAssign(pair: Pair<Any, String>) {
        eventsHelper.registerListener(pair.first, pair.second)
    }

    operator fun plusAssign(listener: Any) {
        eventsHelper.registerListener(listener)
    }

    operator fun minusAssign(listeners: List<Any>) {
        eventsHelper.unregisterListeners(*listeners.toTypedArray())
    }

    operator fun minusAssign(listener: Any) {
        eventsHelper.unregisterListener(listener)
    }

    fun clear() {
        eventsHelper.clearAllListeners()
    }

}