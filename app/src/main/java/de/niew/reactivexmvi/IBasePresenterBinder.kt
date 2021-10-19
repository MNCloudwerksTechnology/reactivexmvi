package de.niew.reactivexmvi


interface IBasePresenterBinder<V: IBaseView<M>, M: IBaseModel<M>, P: IBasePresenter<M, V>> {
    fun createPresenter(): P
    fun attachPresenter(presenter: P)
    fun detachPresenter(keepInstance: Boolean)
}