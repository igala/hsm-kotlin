package de.artcom.hsm

import java.util.ArrayList
import java.util.Arrays
import java.util.HashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.logging.Level
import java.util.logging.Logger

class StateMachine(initialState: State<*>?, vararg states: State<*>?) : EventHandler {
    internal var LOGGER: ILogger = object : ILogger {
        override fun debug(message: String?) {
            Logger.getAnonymousLogger().log(Level.INFO, message)
        }
    }
    private var mStateList = ArrayList<State<*>>()
    open var descendantStates: ArrayList<State<*>> = ArrayList<State<*>>()
    internal var name: String
    private var mInitialState: State<*>? = null
    private var mCurrentState: State<*>? = null
    private var mEventQueue = ConcurrentLinkedQueue<Event>()
    private var mEventQueueInProgress = false
    internal var path: ArrayList<StateMachine> = ArrayList<StateMachine>()
    internal lateinit var container: State<*>
    private lateinit var logger: ILogger
    val pathString: String
        get() {
            var sb = StringBuilder()
            var count = 0
            sb.append("\r\n")
            for (stateMachine in path) {
                sb.append(Integer.toString(++count))
                sb.append(' ')
                sb.append(stateMachine.toString())
                sb.append("\r\n")
            }
            return sb.toString()
        }
    val allActiveStates: List<State<*>>
        get() {
            val stateList = ArrayList<State<*>>()
            stateList.add(mCurrentState!!)
            stateList.addAll(mCurrentState!!.allActiveStates)
            return stateList
        }

    constructor(name: String, initialState: State<*>, vararg states: State<*>) : this(initialState, *states) {
        this.name = name
    }

    init {
        mStateList.addAll(Arrays.asList(*states) as Collection<State<*>>)
        mStateList.add(initialState!!)
        mInitialState = initialState
        setOwner()
        generatePath()
        generateDescendantStateList()
        name = ""
    }

    fun setLogger(log: ILogger) {
        LOGGER = log
    }

    private fun generateDescendantStateList() {
        descendantStates.addAll(mStateList)
        for (state in mStateList) {
            descendantStates.addAll(state.descendantStates)
        }
    }

    private fun generatePath() {
        path.add(0, this)
        for (state in mStateList) {
            state.addParent(this)
        }
    }

    @JvmOverloads
    fun init(payload: Map<String, Any>? = HashMap<String, Any>()) {
            LOGGER.debug(name + " init")
        if (mInitialState == null) {
            throw IllegalStateException(name + " Can't init without states defined.")
        } else {
            mEventQueueInProgress = true
            if (payload == null) {

                    enterState(null, mInitialState!!, HashMap<String?, Any?>())
            }
            else
                enterState(null, mInitialState!!, payload as HashMap<String?, Any?>)
            mEventQueueInProgress = false
            processEventQueue()
        }
    }

    internal fun teardown(payload: Map<String?, Any?>?) {
        LOGGER.debug(name + " teardown")
        if (payload == null) {
                exitState(mCurrentState, null, HashMap<String?, Any?>())
        }
        else
                exitState(mCurrentState, null, payload as HashMap<String?, Any?>)
        mCurrentState = null
    }

    fun teardown() {
        teardown(HashMap<String?, Any>())
    }


     override fun handleEvent(event: String?) {
        handleEvent(event, HashMap<String?, Any>())
    }

    override fun handleEvent(eventName: String?, payload: Map<String?, out Any?>?) {
        if (mCurrentState == null) {
            return // TODO: throw an exception here
        }
        // TODO: make a deep copy of the payload (also do this in Parallel)
        mEventQueue.add(Event(eventName, payload))
        processEventQueue()
    }

    private fun processEventQueue() {
        if (mEventQueueInProgress) {
            return
        }
        mEventQueueInProgress = true
        while (mEventQueue.peek() != null) {
            val event = mEventQueue.poll()
            if (!mCurrentState?.handleWithOverride(event)!!) {
                LOGGER.debug(name + " nobody handled event: " + event.name)
            }
        }
        mEventQueueInProgress = false
    }

    internal fun handleWithOverride(event: Event): Boolean {
        if (mCurrentState != null) {
            return mCurrentState!!.handleWithOverride(event)
        } else {
            return false
        }
    }

    internal fun executeHandler(handler: Handler, event: Event) {
        LOGGER.debug(name + " execute handler for event: " + event.name)
        val action = handler.action
        val targetState = handler.targetState
        if (targetState == null) {
            throw IllegalStateException(name + " cant find target state for transition " + event.name)
        }
        when (handler.kind) {
            TransitionKind.External -> doExternalTransition(mCurrentState, targetState, action, event)
            TransitionKind.Local -> doLocalTransition(mCurrentState, targetState, action, event)
            TransitionKind.Internal -> executeAction(action, mCurrentState, targetState, event.payload)
        }
    }

    private fun executeAction(action: Action?, previousState: State<*>?, targetState: State<*>?, payload: Map<String?, Any?>?) {
        if (action != null) {
            action.setPreviousState(previousState)
            action.setNextState(targetState)
            action.setPayload(payload)
            action.run()
        }
    }

    private fun doExternalTransition(previousState: State<*>?, targetState: State<*>?, action: Action?, event: Event) {
        val lca = findLowestCommonAncestor(targetState)
        lca.switchState(previousState, targetState, action, event.payload)
    }

    private fun doLocalTransition(previousState: State<*>?, targetState: State<*>?, action: Action?, event: Event) {
        if (previousState?.descendantStates!!.contains(targetState)) {
            val stateMachine = findNextStateMachineOnPathTo(targetState)
            stateMachine.switchState(previousState, targetState, action, event.payload)
        } else if (targetState?.descendantStates!!.contains(previousState)) {
            val targetLevel = targetState.owner?.path!!.size
            val stateMachine = path.get(targetLevel)
            stateMachine.switchState(previousState, targetState, action, event.payload)
        } else if (previousState.equals(targetState)) {
            //TODO: clarify desired behavior for local transition on self
            // currently behaves like an internal transition
        } else {
            doExternalTransition(previousState, targetState, action, event)
        }
    }

    private fun switchState(previousState: State<*>?, nextState: State<*>?, action: Action?, payload: Map<String?, Any?>?) {
        exitState(previousState, nextState, payload as HashMap<String?, Any?>?)
            if (action != null) {
                    executeAction(action, previousState, nextState, payload)
            }
        enterState(previousState, nextState, payload)
    }

    internal fun enterState(previousState: State<*>?, targetState: State<*>?, payload: HashMap<String?, Any?>?) {
        val targetLevel = targetState?.owner?.path?.size
        val localLevel = path.size!!
        var nextState: State<*>? = null
            if (targetLevel != null) {
                    if (targetLevel < localLevel) {
                            nextState = mInitialState
                    } else if (targetLevel == localLevel) {
                            nextState = targetState
                    } else { // if targetLevel > localLevel
                            nextState = findNextStateOnPathTo(targetState)
                    }
            }
        if (mStateList.contains(nextState)) {
            mCurrentState = nextState
        } else {
            mCurrentState = mInitialState
        }
        mCurrentState?.enter(previousState, targetState, payload)
    }

    private fun findNextStateOnPathTo(targetState: State<*>?): State<*> {
        return findNextStateMachineOnPathTo(targetState!!).container
    }

    private fun findNextStateMachineOnPathTo(targetState: State<*>?): StateMachine {
        val localLevel = path.size
        val targetOwner = targetState?.owner
        return targetOwner?.path!!.get(localLevel)
    }

    private fun exitState(previousState: State<*>?, nextState: State<*>?, payload: HashMap<String?, Any?>?) {
        mCurrentState?.exit(previousState, nextState, payload)
    }

    private fun setOwner() {
        for (state in mStateList) {
            state.owner = this
        }
    }

    public override fun toString(): String {
        if (mCurrentState == null) {
            return mInitialState.toString()
        }
        return mCurrentState.toString()
    }

    internal fun addParent(stateMachine: StateMachine) {
        path.add(0, stateMachine)
        for (state in mStateList) {
            state.addParent(stateMachine)
        }
    }

    private fun findLowestCommonAncestor(targetState: State<*>?): StateMachine {
        if (targetState?.owner == null) {
            throw IllegalStateException(name + " Target state '" + targetState?.id + "' is not contained in state machine model.")
        }
        val targetPath = targetState.owner?.path
        val size = path.size
        for (i in 1 until size) {
            try {
                val targetAncestor = targetPath?.get(i)
                val localAncestor = path.get(i)
                if (targetAncestor != localAncestor) {
                    return path.get(i - 1)
                }
            } catch (e: IndexOutOfBoundsException) {
                return path.get(i - 1)
            }
        }
        return this
    }
}