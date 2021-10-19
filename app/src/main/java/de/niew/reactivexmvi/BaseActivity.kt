package de.niew.reactivexmvi

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.Subject

abstract class BaseActivity<V : IBaseView<M>, M : IBaseModel<M>, P : IBasePresenter<M, V>> :
    IBasePresenterBinder<V, M, P>,
    IBaseView<M>,
    AppCompatActivity() {

    private var presenter: P? = null

    private var disposable: Disposable? = null

    private val intents: ArrayList<Subject<*>> = ArrayList()

    protected fun  <I> intent(intent: Subject<I>): Subject<I> {
        this.intents.add(intent)

        return intent
    }

    override fun attachPresenter(presenter: P) {
        this.disposable = presenter.onUpdateModel()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    Log.d(this::class.simpleName, "Rendering ui: $it")
                    this.render(it)
                },
                { Log.e(BaseFragment::class.java.simpleName, it.message, it) }
            )

        this.presenter = presenter
    }

    override fun detachPresenter(keepInstance: Boolean) {
        this.disposable?.dispose()
        this.presenter?.detachView()

        if (!keepInstance) {
            this.presenter = null
        }

        Log.d(this::class.simpleName, "Detached presenter. Kept instance: $keepInstance")
    }

    override fun onStart() {
        super.onStart()

        if (this.presenter == null) {
            this.presenter = this.createPresenter()
        }

        this.presenter?.let {
            this.attachPresenter(it)
            it.attachView(this as V)
        }

        val currentState = (this.presenter as P).getCurrentState()

        try {
            this.render(currentState)
        } catch (ex: Exception) {
            Log.e(this::class.simpleName, "Error during first rendering: $currentState", ex)
        }

        //TODO("Not implemented, yet")//16.01.21 markus_n: maybe add onStart event here
    }

    override fun onStop() {
        super.onStop()

        this.detachPresenter(true)
    }

    override fun onDestroy() {
        super.onDestroy()

        this.detachPresenter(false)

        this.intents.forEach {
            it.onComplete()
        }
    }
}