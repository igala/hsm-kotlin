package de.artcom.hsm

import com.google.common.collect.LinkedListMultimap
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger


open class State<T : State<T>>(id: String) {
    internal var LOGGER: ILogger = object : ILogger {
            override fun debug(message: String?) {
            Logger.getAnonymousLogger().log(Level.INFO, message)
        }

    }
    val id: String
    private var mOnEnterAction: Action? = null
    private var mOnExitAction: Action? = null
    private  var mHandlers: LinkedListMultimap<String, Handler>
    internal var owner: StateMachine? = null
    protected open fun getThis(): T
    {
            return this as T
    }

    open fun setOwner(ownerMachine: StateMachine) {
        owner = ownerMachine
    }

    open fun getOwner(): StateMachine? {
        return owner
    }
    open val descendantStates: Collection<out State<*>>
        get() {
            return ArrayList<State<*>>()
        }
    val eventHandler: EventHandler
        get() {
            return owner?.path!!.get(0)
        }
    open val allActiveStates: List<State<*>>
        get() {
            return ArrayList<State<*>>()
        }

    init {
        mHandlers = LinkedListMultimap.create()
        this.id = id
    }

    fun setLogger(log: ILogger) {
        LOGGER = log
    }

    fun onEnter(onEnterAction: Action): T {
        mOnEnterAction = onEnterAction
        return getThis()
    }

    fun onExit(onExitAction: Action): T {
        mOnExitAction = onExitAction
        return getThis()
    }

    fun addHandler(eventName: String, target: State<*>, kind: TransitionKind, guard: Guard): T {
        mHandlers.put(eventName, Handler(target, kind, guard))
        return getThis()
    }

    fun addHandler(eventName: String, target: State<*>, kind: TransitionKind, action: Action): T {
        mHandlers.put(eventName, Handler(target, kind, action))
        return getThis()
    }

    fun addHandler(eventName: String, target: State<*>, kind: TransitionKind, action: Action, guard: Guard): T {
        mHandlers.put(eventName, Handler(target, kind, action, guard))
        return getThis()
    }

    fun addHandler(eventName: String, target: State<*>, kind: TransitionKind): T {
        mHandlers.put(eventName, Handler(target, kind))
        return getThis()
    }

    open fun enter(prev: State<*>?, next: State<*>?, payload: Map<String?, Any?>?) {
        LOGGER.debug("[" + owner?.name + "] " + id + " - enter")
        if (mOnEnterAction != null) {
            mOnEnterAction?.setPreviousState(prev)
            mOnEnterAction?.setNextState(next)
            mOnEnterAction?.setPayload(payload)
            mOnEnterAction?.run()
        }
    }

    open fun exit(prev: State<*>?, next: State<*>?, payload: Map<String?, Any?>?) {
        LOGGER.debug("[" + owner?.name + "] " + id + " - exit")
        if (mOnExitAction != null) {
            mOnExitAction?.setPreviousState(prev)
            mOnExitAction?.setNextState(next)
            mOnExitAction?.setPayload(payload)
            mOnExitAction?.run()
        }
    }

    internal fun findHandler(event: Event): Handler? {
        for (handler in mHandlers.get(event.name)) {
            if (handler.evaluate(event)) {
                return handler
            }
        }
        return null
    }

    open fun handleWithOverride(event: Event): Boolean {
        val handler = findHandler(event)
        if (handler != null) {
            LOGGER.debug("[" + owner?.name + "] " + id + " - handle Event: " + event.name)
            owner?.executeHandler(handler, event)
            return true
        }
        return false
    }

     override fun toString(): String {
        return id
    }

    open fun addParent(stateMachine: StateMachine) {
        // do nothing
    }


}