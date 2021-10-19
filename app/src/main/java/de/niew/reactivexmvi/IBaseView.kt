package de.niew.reactivexmvi

interface IBaseView<M: IBaseModel<M>> {
    //Map incoming model to valid ui state
    fun createInitialState(): M
    fun render(model: M)
}