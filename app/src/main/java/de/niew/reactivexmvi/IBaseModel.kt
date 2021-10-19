package de.niew.reactivexmvi

interface IBaseModel<M> {
    /**
     * Merges all incoming changes into the current state and returns a new immutable state.
     *
     * Should ignore all null values of the new changes.
     */
    fun merge(changes: M?): M
}