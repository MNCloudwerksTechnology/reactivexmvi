package de.niew.reactivexmvi

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.Subject

abstract class BaseFragment<V : IBaseView<M>, M : IBaseModel<M>, P : IBasePresenter<M, V>> :
    IBasePresenterBinder<V, M, P>,
    IBaseView<M>, Fragment() {

    private var presenter: P? = null

    private var disposable: Disposable? = null

    private var restoreViewStateObserver = false

    private val intents: ArrayList<Subject<*>> = ArrayList()

    protected fun <I> intent(intent: Subject<I>): Subject<I> {
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
                {
                    Log.e(
                        BaseFragment::class.java.simpleName,
                        "Could not send state event to be rendered.",
                        it
                    )
                }
            )

        this.presenter = presenter
    }

    override fun detachPresenter(keepInstance: Boolean) {
        this.disposable?.dispose()

        this.presenter?.detachView()

        if(!keepInstance) {
            this.presenter = null
            this.restoreViewStateObserver = false
        } else {
            this.restoreViewStateObserver = true
        }

        Log.d(this::class.simpleName, "Detached presenter. Kept instance: $keepInstance")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(this::class.simpleName, "Lifecycle::onCreate")

        val presenter: P = this.createPresenter()
        this.attachPresenter(presenter)
        Log.d(
            this::class.simpleName,
            "Lifecycle::onViewCreated. Attached presenter: ${this.presenter}"
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(this::class.simpleName, "Lifecycle::onCreateView.")
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(this::class.simpleName, "Lifecycle::onViewStateRestored - restore: ${this.restoreViewStateObserver}")
        if(!this.restoreViewStateObserver) {
            this.presenter?.attachView(this as V)
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        Log.d(this::class.simpleName, "Lifecycle::onViewStateRestored")
    }

    override fun onStart() {
        super.onStart()
        Log.d(this::class.simpleName, "Lifecycle::onStart - restore: ${this.restoreViewStateObserver}")

        if (this.restoreViewStateObserver) {
            this.presenter?.let {
                this.attachPresenter(it)
                it.attachView(this as V)
            }
            this.restoreViewStateObserver = false
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(this::class.simpleName, "Lifecycle::onResume")

        //TODO("Not implemented, yet")//16.01.21 markus_n: maybe execute new event here
    }


    override fun onPause() {
        super.onPause()
        Log.d(this::class.simpleName, "Lifecycle::onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(this::class.simpleName, "Lifecycle::onStop")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d(this::class.simpleName, "Lifecycle::onSaveInstanceState")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(this::class.simpleName, "Lifecycle::onDestroyView")
        this.detachPresenter(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(this::class.simpleName, "Lifecycle::onDestroy")
        this.detachPresenter(false)

        this.intents.forEach {
            it.onComplete()
        }
        this.intents.clear()
    }
}