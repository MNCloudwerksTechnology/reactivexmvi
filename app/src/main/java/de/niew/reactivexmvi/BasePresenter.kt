package de.niew.reactivexmvi

import android.util.Log
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject

abstract class BasePresenter<M : IBaseModel<M>, V : IBaseView<M>>(private var state: M) :
    IBasePresenter<M, V> {
    private var compositeDisposable: CompositeDisposable = CompositeDisposable()

    private var modelUpdate: Subject<M> = PublishSubject.create()

    private val intentSubscriptions: ArrayList<Subject<*>> = ArrayList()
    private val intentDisposables: ArrayList<Disposable> = ArrayList()

    override fun attachView(view: V) {

        val viewStateChangeObservable: Observable<M> = this.bind(view)
        val viewStateSubscriber: Disposable = viewStateChangeObservable
            .map { this.state.merge(it) }
            .subscribe(
                {
                    this.state = it
                    this.modelUpdate.onNext(it)
                },
                {
                    Log.e(
                        this::class.simpleName,
                        "Failed to map event to view-state: $it",
                        it
                    )
                }
            )
        this.compositeDisposable.addAll(compositeDisposable, viewStateSubscriber)

        Log.d(this::class.simpleName, "Attached view: ${view.javaClass.simpleName}")
    }

    override fun getCurrentState(): M {
        return this.state
    }

    override fun <I> subscribeIntent(intent: Observable<I>): Observable<I> {
        val subject: Subject<I> = PublishSubject.create()
        val disposable = intent.subscribe(
            { subject.onNext(it) },
            { subject.onError(it) },
            { subject.onComplete() }
        )

        this.intentDisposables.add(disposable)
        this.intentSubscriptions.add(subject)

        return subject
    }

    override fun detachView() {
        this.compositeDisposable.dispose()
        this.modelUpdate.onComplete()

        this.intentSubscriptions.forEach {
            it.onComplete()
        }

        this.intentDisposables.forEach {
            it.dispose()
        }

        this.intentDisposables.clear()
        this.intentSubscriptions.clear()

        Log.d(this::class.simpleName, "Detached view.")
    }

    override fun onUpdateModel(): Observable<M> {
        if (this.compositeDisposable.isDisposed) {
            this.modelUpdate = PublishSubject.create()
            this.compositeDisposable = CompositeDisposable()
        }

        return this.modelUpdate
    }

    fun handleError(source: Class<Any>, msg: String, t: Throwable) {
        Log.e(source.simpleName, msg, t)
    }
}