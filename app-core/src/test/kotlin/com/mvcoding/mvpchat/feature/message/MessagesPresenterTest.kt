package com.mvcoding.mvpchat.feature.message

import com.mvcoding.mvpchat.DataSource
import com.mvcoding.mvpchat.PageDataSource
import com.mvcoding.mvpchat.model.Message
import com.mvcoding.mvpchat.model.aMessage
import com.mvcoding.mvpchat.rxSchedulers
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.inOrder
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Test
import rx.Observable
import rx.Observable.error
import rx.Observable.just
import rx.lang.kotlin.PublishSubject

class MessagesPresenterTest {

    val newPostsSubject = PublishSubject<Message>()
    var postsPageSubject = PublishSubject<List<Message>>()
    val refreshesSubject = PublishSubject<Unit>()
    val nextPageRequestsSubject = PublishSubject<Unit>()

    val initialPosts = listOf(aMessage(), aMessage(), aMessage())

    val firstPagePostsSource = mock<DataSource<List<Message>>>()
    val newPostsSource = mock<DataSource<Message>>()
    val postsPageSource = mock<PageDataSource<List<Message>>>()
    val view = mock<MessagesPresenter.View>()
    val inOrder = inOrder(view, postsPageSource)
    val presenter = MessagesPresenter(
            firstPagePostsSource,
            newPostsSource,
            postsPageSource,
            rxSchedulers())

    @Before
    fun setUp() {
        whenever(firstPagePostsSource.data()).thenReturn(just(initialPosts))
        whenever(newPostsSource.data()).thenReturn(newPostsSubject)
        whenever(postsPageSource.data()).thenReturn(postsPageSubject)
        whenever(postsPageSource.hasNextPage()).thenReturn(true)
        whenever(view.refreshes()).thenReturn(refreshesSubject)
        whenever(view.nextPageRequests()).thenReturn(nextPageRequestsSubject)
    }

    @Test
    fun `initially loads all posts`() {
        presenter.attach(view)

        inOrder.verify(view).showLoading()
        inOrder.verify(view).hideLoading()
        inOrder.verify(view).showPosts(initialPosts)
    }

    @Test
    fun `adds new posts as they arrive`() {
        val newPost = aMessage()
        val anotherNewPost = aMessage()
        presenter.attach(view)

        receiveNewPost(newPost)
        receiveNewPost(anotherNewPost)

        inOrder.verify(view).addNewPost(newPost)
        inOrder.verify(view).addNewPost(anotherNewPost)
    }

    @Test
    fun `requests and adds new pages of posts when paging edge is reached`() {
        val postsPage = listOf(aMessage(), aMessage())
        presenter.attach(view)

        requestNextPage()
        receiveNewPostsPage(postsPage)

        inOrder.verify(view).showLoadingNextPage()
        inOrder.verify(view).hideLoadingNextPage()
        inOrder.verify(view).addPostsPage(postsPage)
    }

    @Test
    fun `doesn't try to load next page when next page does not exist`() {
        whenever(postsPageSource.hasNextPage()).thenReturn(false)
        presenter.attach(view)

        requestNextPage()

        verifyPagingWasNotTriggered()
    }

    @Test
    fun `doesn't try to load next page when initial data is not loaded`() {
        whenever(firstPagePostsSource.data()).thenReturn(Observable.never())
        presenter.attach(view)

        requestNextPage()

        verifyPagingWasNotTriggered()
    }

    @Test
    fun `doesn't try to load next page when it is already being loaded`() {
        val postsPage = listOf(aMessage(), aMessage())
        presenter.attach(view)

        requestNextPage()
        requestNextPage()
        verify(view, times(1)).showLoadingNextPage()
        verify(view, never()).hideLoadingNextPage()
        verify(view, never()).addPostsPage(any())

        receiveNewPostsPage(postsPage)
        verify(view, times(1)).hideLoadingNextPage()
        verify(view, times(1)).addPostsPage(postsPage)
    }

    @Test
    fun `can refresh data that will reset paging`() {
        presenter.attach(view)

        refresh()

        inOrder.verify(view).showPosts(initialPosts)
        inOrder.verify(postsPageSource).resetPaging()
        inOrder.verify(view).showLoading()
        inOrder.verify(view).hideLoading()
        inOrder.verify(view).showPosts(initialPosts)
    }

    @Test
    fun `handles and recovers from posts data source errors`() {
        val throwable = Throwable()
        whenever(firstPagePostsSource.data()).thenReturn(error(throwable), just(initialPosts))

        presenter.attach(view)

        refresh()

        inOrder.verify(view).showLoading()
        inOrder.verify(view).hideLoading()
        inOrder.verify(view).showError(throwable)
        inOrder.verify(view).showLoading()
        inOrder.verify(view).hideLoading()
        inOrder.verify(view).showPosts(initialPosts)
    }

    @Test
    fun `handles and recovers from paging errors`() {
        val throwable = Throwable()
        val postsPage = listOf(aMessage(), aMessage())
        presenter.attach(view)

        requestNextPage()
        failPaging(throwable)
        requestNextPage()
        receiveNewPostsPage(postsPage)

        inOrder.verify(view).showLoadingNextPage()
        inOrder.verify(view).hideLoadingNextPage()
        inOrder.verify(view).showNextPageError(throwable)
        inOrder.verify(view).showLoadingNextPage()
        inOrder.verify(view).hideLoadingNextPage()
        inOrder.verify(view).addPostsPage(postsPage)
    }

    private fun verifyPagingWasNotTriggered() {
        verify(view, never()).showLoadingNextPage()
        verify(view, never()).hideLoadingNextPage()
        verify(view, never()).addPostsPage(any())
        verify(postsPageSource, never()).data()
    }

    fun refresh() = refreshesSubject.onNext(Unit)
    fun requestNextPage() = nextPageRequestsSubject.onNext(Unit)
    fun receiveNewPost(message: Message) = newPostsSubject.onNext(message)
    fun receiveNewPostsPage(messages: List<Message>) = postsPageSubject.onNext(messages)
    fun failPaging(throwable: Throwable) {
        postsPageSubject.onError(throwable)
        postsPageSubject = PublishSubject()
        whenever(postsPageSource.data()).thenReturn(postsPageSubject)
    }
}