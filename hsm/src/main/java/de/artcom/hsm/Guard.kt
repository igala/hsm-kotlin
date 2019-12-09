package de.artcom.hsm

interface Guard {
    fun evaluate(payload: Map<String?, Any?>?): Boolean
}