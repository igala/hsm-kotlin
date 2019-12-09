package de.artcom.hsm

import java.util.*

class Sub : State<Sub> {
    private val mSubMachine: StateMachine
    override fun getThis(): Sub {
        return this
    }

    constructor(id: String?, subMachine: StateMachine) : super(id!!) {
        mSubMachine = subMachine
        mSubMachine.container = this
    }

    constructor(id: String?, initialState: State<*>?, vararg states: State<*>?) : super(id!!) {
        mSubMachine = StateMachine(initialState, *states)
        mSubMachine.container = this
    }

    override fun enter(prev: State<*>?, next: State<*>?, payload: Map<String?, Any?>?) {
        super.enter(prev, next, payload)
        mSubMachine.enterState(prev, next, payload as HashMap<String?, Any?>?)
    }

    override fun exit(prev: State<*>?, next: State<*>?, payload: Map<String?, Any?>?) {
        mSubMachine.teardown(payload)
        super.exit(prev, next, payload)
    }

    override fun handleWithOverride(event: Event): Boolean {
        return if (mSubMachine.handleWithOverride(event)) {
            true
        } else {
            super.handleWithOverride(event)
        }
    }

    override fun toString(): String {
        return "$id/($mSubMachine)"
    }

    override fun addParent(stateMachine: StateMachine) {
        mSubMachine.addParent(stateMachine)
    }

    override fun setOwner(ownerMachine: StateMachine) {
        super.owner = ownerMachine
        mSubMachine.name = owner!!.name
    }

    override val descendantStates: Collection<out State<*>>
        get() {
            return mSubMachine.descendantStates
        }

    override val allActiveStates: List<State<*>>
        get() {
            return mSubMachine.allActiveStates
        }
}