package de.artcom.hsm

abstract class Action() {
    protected var mPreviousState: State<*>? = null
    protected var mNextState: State<*>? = null
    @JvmField
    protected var mPayload: Map<String?, Any?>? = null
    abstract fun run()
    fun setPreviousState(state: State<*>?) {
        mPreviousState = state
    }

    fun setNextState(state: State<*>?) {
        mNextState = state
    }

    fun setPayload(payload: Map<String?, Any?>?) {
        mPayload = payload
    }
}