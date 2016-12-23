package com.mvcoding.mvpchat.feature.message

import com.mvcoding.mvp.Presenter
import com.mvcoding.mvpchat.DataSource
import com.mvcoding.mvpchat.PageDataSource
import com.mvcoding.mvpchat.RxSchedulers
import com.mvcoding.mvpchat.model.Message
import rx.Observable
import rx.Observable.empty
import rx.lang.kotlin.toSingletonObservable

class MessagesPresenter(
        private val firstPagePostsSource: DataSource<List<Message>>,
        private val newPostsSource: DataSource<Message>,
        private val postsPageSource: PageDataSource<List<Message>>,
        private val schedulers: RxSchedulers) : Presenter<MessagesPresenter.View>() {

    private var isInitialDataLoaded = false
    private var isPageLoading = false

    override fun onViewAttached(view: View) {
        super.onViewAttached(view)

        view.refreshes()
                .startWith(Unit)
                .doOnNext { postsPageSource.resetPaging() }
                .doOnNext { view.showLoading() }
                .observeOn(schedulers.io)
                .switchMap { firstPagePostsSource.data().handleErrors(view) }
                .observeOn(schedulers.main)
                .doOnNext { view.hideLoading() }
                .subscribeUntilDetached { showFirstPage(view, it) }

        newPostsSource.data()
                .subscribeOn(schedulers.io)
                .observeOn(schedulers.main)
                .subscribeUntilDetached { view.addNewPost(it) }

        view.nextPageRequests()
                .filter { validStateForPaging() }
                .doOnNext { isPageLoading = true }
                .doOnNext { view.showLoadingNextPage() }
                .observeOn(schedulers.io)
                .switchMap { postsPageSource.data().handlePagingErrors(view) }
                .observeOn(schedulers.main)
                .doOnNext { view.hideLoadingNextPage() }
                .doOnNext { isPageLoading = false }
                .subscribeUntilDetached { view.addPostsPage(it) }
    }

    private fun showFirstPage(view: View, messages: List<Message>) {
        view.showPosts(messages)
        isInitialDataLoaded = true
    }

    private fun validStateForPaging() = !isPageLoading && isInitialDataLoaded && postsPageSource.hasNextPage()

    private fun <T> Observable<T>.handleErrors(view: View): Observable<T> = onErrorResumeNext {
        it.toSingletonObservable()
                .observeOn(schedulers.main)
                .doOnNext { view.hideLoading() }
                .subscribe { view.showError(it) }
        empty()
    }

    private fun <T> Observable<T>.handlePagingErrors(view: View): Observable<T> = onErrorResumeNext {
        it.toSingletonObservable()
                .observeOn(schedulers.main)
                .doOnNext { view.hideLoadingNextPage() }
                .doOnNext { isPageLoading = false }
                .subscribe { view.showNextPageError(it) }
        empty()
    }

    interface View : Presenter.View {
        fun refreshes(): Observable<Unit>
        fun nextPageRequests(): Observable<Unit>

        fun showPosts(messages: List<Message>)
        fun addNewPost(newMessage: Message)
        fun addPostsPage(data: List<Message>)
        fun showError(throwable: Throwable)
        fun showNextPageError(throwable: Throwable)
        fun showLoading()
        fun hideLoading()
        fun showLoadingNextPage()
        fun hideLoadingNextPage()
    }
}