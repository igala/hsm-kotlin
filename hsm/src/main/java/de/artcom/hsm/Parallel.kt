package de.artcom.hsm

import java.util.*

class Parallel(id: String?, vararg stateMachines: StateMachine?) : State<Parallel>(id!!) {
    private var mStateMachineList: List<StateMachine>? = null
    fun setStateMachineList(stateMachineList: List<StateMachine>?) {
        mStateMachineList = stateMachineList
        for (stateMachine in mStateMachineList!!) {
            stateMachine.container = this
        }
    }

    override fun getThis(): Parallel {
        return this
    }

    override fun enter(prev: State<*>?, next: State<*>?, payload: Map<String?, Any?>?) {
        super.enter(prev, next, payload)
        for (stateMachine in mStateMachineList!!) {
            stateMachine.enterState(prev, next, payload as HashMap<String?, Any?>?)
        }
    }

    override fun exit(prev: State<*>?, next: State<*>?, payload: Map<String?, Any?>?) {
        super.exit(prev, next, payload)
        for (stateMachine in mStateMachineList!!) {
            stateMachine.teardown(payload)
        }
    }

    override fun handleWithOverride(event: Event): Boolean {
        var isHandled = false
        for (stateMachine in mStateMachineList!!) {
            if (stateMachine.handleWithOverride(event)) {
                isHandled = true
            }
        }
        return if (!isHandled) {
            super.handleWithOverride(event)
        } else true
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(id)
        sb.append("/(")
        for (stateMachine in mStateMachineList!!) {
            sb.append(stateMachine.toString())
            sb.append('|')
        }
        sb.deleteCharAt(sb.length - 1)
        sb.append(')')
        return sb.toString()
    }

    override fun addParent(stateMachine: StateMachine) {
        for (machine in mStateMachineList!!) {
            machine.addParent(stateMachine)
        }
    }

      override val descendantStates: Collection<out State<*>>
          get() {
        val descendantStates: MutableList<State<*>> = ArrayList()
        for (stateMachine in mStateMachineList!!) {
            descendantStates.addAll(stateMachine.descendantStates)
        }
        return descendantStates
    }

   override val allActiveStates: List<State<*>>
        get(){
        val stateList: MutableList<State<*>> = ArrayList()
        for (stateMachine in mStateMachineList!!) {
            stateList.addAll(stateMachine.allActiveStates)
        }
        return stateList
    }

    init {
        setStateMachineList(listOf(*stateMachines) as List<StateMachine>?)
    }
}