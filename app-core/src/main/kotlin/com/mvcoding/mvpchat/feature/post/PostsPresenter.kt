package com.mvcoding.mvpchat.feature.post

import com.mvcoding.mvp.Presenter
import com.mvcoding.mvpchat.DataSource
import com.mvcoding.mvpchat.PageDataSource
import com.mvcoding.mvpchat.RxSchedulers
import com.mvcoding.mvpchat.model.Post
import rx.Observable
import rx.Observable.empty
import rx.lang.kotlin.toSingletonObservable

class PostsPresenter(
        private val firstPagePostsSource: DataSource<List<Post>>,
        private val newPostsSource: DataSource<Post>,
        private val postsPageSource: PageDataSource<List<Post>>,
        private val schedulers: RxSchedulers) : Presenter<PostsPresenter.View>() {

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

    private fun showFirstPage(view: View, posts: List<Post>) {
        view.showPosts(posts)
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

        fun showPosts(posts: List<Post>)
        fun addNewPost(newPost: Post)
        fun addPostsPage(data: List<Post>)
        fun showError(throwable: Throwable)
        fun showNextPageError(throwable: Throwable)
        fun showLoading()
        fun hideLoading()
        fun showLoadingNextPage()
        fun hideLoadingNextPage()
    }
}