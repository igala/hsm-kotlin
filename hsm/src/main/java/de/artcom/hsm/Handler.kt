package de.artcom.hsm

internal class Handler {
    val targetState: State<*>
    val kind: TransitionKind
    private var mGuard: Guard? = null
    var action: Action? = null
        private set

    constructor(targetState: State<*>, kind: TransitionKind, action: Action?, guard: Guard?) {
        this.targetState = targetState
        this.kind = kind
        this.action = action
        mGuard = guard
    }

    constructor(targetState: State<*>, kind: TransitionKind, guard: Guard?) {
        this.targetState = targetState
        this.kind = kind
        mGuard = guard
    }

    constructor(targetState: State<*>, kind: TransitionKind, action: Action?) {
        this.targetState = targetState
        this.kind = kind
        this.action = action
    }

    constructor(targetState: State<*>, kind: TransitionKind) {
        this.targetState = targetState
        this.kind = kind
    }

    fun evaluate(event: Event): Boolean {
        return if (mGuard != null) {
            mGuard!!.evaluate(event.payload)
        } else true
    }

}