package de.artcom.hsm

interface EventHandler {
    fun handleEvent(event: String?)
    fun handleEvent(event: String?, payload: Map<String?, Any?>?)
}