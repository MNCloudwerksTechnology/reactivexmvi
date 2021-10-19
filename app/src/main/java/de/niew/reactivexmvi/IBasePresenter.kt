package de.niew.reactivexmvi

import io.reactivex.rxjava3.core.Observable

interface IBasePresenter<M: IBaseModel<M>, V: IBaseView<M>> {
    fun bind(view: V): Observable<M>
    fun <I>subscribeIntent(intent: Observable<I>): Observable<I>

    fun attachView(view: V)
    fun detachView()

    fun getCurrentState(): M

    fun onUpdateModel(): Observable<M>
}