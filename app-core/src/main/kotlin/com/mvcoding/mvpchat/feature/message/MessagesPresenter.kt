package com.mvcoding.mvpchat.feature.message

import com.mvcoding.mvp.Presenter
import com.mvcoding.mvpchat.DataSource
import com.mvcoding.mvpchat.RxSchedulers
import com.mvcoding.mvpchat.model.Message
import rx.Observable
import rx.Observable.empty
import rx.Observable.just
import rx.lang.kotlin.BehaviorSubject
import rx.lang.kotlin.toSingletonObservable

class MessagesPresenter(
        private val messagesSource: DataSource<List<Message>>,
        private val newMessagesSource: DataSource<Message>,
        private val messagesPagesSource: DataSource<List<Message>>,
        private val schedulers: RxSchedulers) : Presenter<MessagesPresenter.View>() {

    private val allowPagingSubject = BehaviorSubject(false)

    override fun onViewAttached(view: View) {
        super.onViewAttached(view)

        just(Unit)
                .doOnNext { view.showLoading() }
                .observeOn(schedulers.io)
                .switchMap { messagesSource.data() }
                .observeOn(schedulers.main)
                .doOnNext { view.hideLoading() }
                .doOnError { view.hideLoading() }
                .retryWhen { handleErrors(view, it) }
                .subscribeUntilDetached {
                    view.showMessages(it)
                    startListeningToPageRequests()
                }

        newMessagesSource.data()
                .subscribeOn(schedulers.io)
                .observeOn(schedulers.main)
                .subscribeUntilDetached { view.addNewMessage(it) }

        view.nextPageRequests()
                .proceedOnlyWhenPagingIsAllowed()
                .doOnNext { stopListeningToPagingRequests() }
                .doOnNext { view.showLoadingNextPage() }
                .observeOn(schedulers.io)
                .switchMap { loadMessagesPage(view) }
                .observeOn(schedulers.main)
                .doOnNext { view.hideLoadingNextPage() }
                .subscribeUntilDetached { view.addMessagesPage(it) }
    }

    private fun handleErrors(view: View, errors: Observable<out Throwable>) = errors.switchMap { view.showErrorAndAllowToRetry(it) }
    private fun startListeningToPageRequests(): Unit = allowPagingSubject.onNext(true)
    private fun stopListeningToPagingRequests(): Unit = allowPagingSubject.onNext(false)
    private fun stopPagingCompletely() {
        allowPagingSubject.onNext(false)
        allowPagingSubject.onCompleted()
    }

    private fun Observable<Unit>.proceedOnlyWhenPagingIsAllowed() =
            withLatestFrom(allowPagingSubject) { _, allowPaging -> allowPaging }.filter { it }

    private fun loadMessagesPage(view: View) = messagesPagesSource.data()
            .doOnNext { startListeningToPageRequests() }
            .doOnCompleted { stopPagingCompletely() }
            .handlePagingErrors(view)

    private fun <T> Observable<T>.handlePagingErrors(view: View): Observable<T> = onErrorResumeNext {
        it.toSingletonObservable()
                .observeOn(schedulers.main)
                .doOnNext { view.hideLoadingNextPage() }
                .doOnNext { startListeningToPageRequests() }
                .subscribe { view.showNextPageError(it) }
        empty()
    }

    interface View : Presenter.View {
        fun nextPageRequests(): Observable<Unit>
        fun showMessages(messages: List<Message>)
        fun addNewMessage(newMessage: Message)
        fun addMessagesPage(messages: List<Message>)
        fun showErrorAndAllowToRetry(throwable: Throwable): Observable<Unit>
        fun showNextPageError(throwable: Throwable)
        fun showLoading()
        fun hideLoading()
        fun showLoadingNextPage()
        fun hideLoadingNextPage()
    }
}