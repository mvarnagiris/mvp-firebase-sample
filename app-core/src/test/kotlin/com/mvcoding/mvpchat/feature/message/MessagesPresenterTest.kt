package com.mvcoding.mvpchat.feature.message

import com.mvcoding.mvpchat.DataSource
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

    val newMessagesSubject = PublishSubject<Message>()
    var postsPageSubject = PublishSubject<List<Message>>()
    val nextPageRequestsSubject = PublishSubject<Unit>()
    val errorSubject = PublishSubject<Unit>()

    val messages = listOf(aMessage(), aMessage(), aMessage())

    val messagesSource = mock<DataSource<List<Message>>>()
    val newMessagesSource = mock<DataSource<Message>>()
    val messagesPagesSource = mock<DataSource<List<Message>>>()
    val view = mock<MessagesPresenter.View>()
    val inOrder = inOrder(view, messagesPagesSource)
    val presenter = MessagesPresenter(messagesSource, newMessagesSource, messagesPagesSource, rxSchedulers())

    @Before
    fun setUp() {
        whenever(messagesSource.data()).thenReturn(just(messages))
        whenever(newMessagesSource.data()).thenReturn(newMessagesSubject)
        whenever(messagesPagesSource.data()).thenReturn(postsPageSubject)
        whenever(view.nextPageRequests()).thenReturn(nextPageRequestsSubject)
        whenever(view.showErrorAndAllowToRetry(any())).thenReturn(errorSubject)
    }

    @Test
    fun `initially loads all messages`() {
        presenter.attach(view)

        inOrder.verify(view).showLoading()
        inOrder.verify(view).hideLoading()
        inOrder.verify(view).showMessages(messages)
    }

    @Test
    fun `handles errors and recovers from errors while loading all messages`() {
        val throwable = Throwable()
        whenever(messagesSource.data()).thenReturn(error(throwable), just(messages))

        presenter.attach(view)
        retry()

        inOrder.verify(view).showLoading()
        inOrder.verify(view).hideLoading()
        inOrder.verify(view).showErrorAndAllowToRetry(throwable)
        inOrder.verify(view).showLoading()
        inOrder.verify(view).hideLoading()
        inOrder.verify(view).showMessages(messages)
    }

    @Test
    fun `adds new messages as they arrive`() {
        val newMessage = aMessage()
        val anotherNewMessage = aMessage()
        presenter.attach(view)

        receiveNewMessage(newMessage)
        receiveNewMessage(anotherNewMessage)

        inOrder.verify(view).addNewMessage(newMessage)
        inOrder.verify(view).addNewMessage(anotherNewMessage)
    }

    @Test
    fun `requests and adds new pages of posts when paging edge is reached`() {
        val messagesPage = listOf(aMessage(), aMessage())
        presenter.attach(view)

        requestNextPage()
        receiveNewPostsPage(messagesPage)

        inOrder.verify(view).showLoadingNextPage()
        inOrder.verify(view).hideLoadingNextPage()
        inOrder.verify(view).addMessagesPage(messagesPage)
    }

    @Test
    fun `stops requesting new pages after end was reached`() {
        presenter.attach(view)

        requestNextPage()
        receiveEndOfPaging()
        requestNextPage()
        presenter.detach(view)
        presenter.attach(view)
        requestNextPage()

        verify(messagesPagesSource, times(1)).data()
    }

    @Test
    fun `doesn't try to load next page when initial data is not loaded`() {
        whenever(messagesSource.data()).thenReturn(Observable.never())
        presenter.attach(view)

        requestNextPage()

        verifyPagingWasNotTriggered()
    }

    @Test
    fun `doesn't try to load next page when it is already being loaded`() {
        val messagesPage = listOf(aMessage(), aMessage())
        presenter.attach(view)

        requestNextPage()
        requestNextPage()
        verify(view, times(1)).showLoadingNextPage()
        verify(view, never()).hideLoadingNextPage()
        verify(view, never()).addMessagesPage(any())

        receiveNewPostsPage(messagesPage)
        verify(view, times(1)).hideLoadingNextPage()
        verify(view, times(1)).addMessagesPage(messagesPage)
    }

    @Test
    fun `handles and recovers from paging errors`() {
        val throwable = Throwable()
        val messagesPage = listOf(aMessage(), aMessage())
        whenever(messagesPagesSource.data()).thenReturn(error(throwable), just(messagesPage))
        presenter.attach(view)

        requestNextPage()
        requestNextPage()

        inOrder.verify(view).showLoadingNextPage()
        inOrder.verify(view).hideLoadingNextPage()
        inOrder.verify(view).showNextPageError(throwable)
        inOrder.verify(view).showLoadingNextPage()
        inOrder.verify(view).hideLoadingNextPage()
        inOrder.verify(view).addMessagesPage(messagesPage)
    }

    private fun verifyPagingWasNotTriggered() {
        verify(view, never()).showLoadingNextPage()
        verify(view, never()).hideLoadingNextPage()
        verify(view, never()).addMessagesPage(any())
        verify(messagesPagesSource, never()).data()
    }

    fun requestNextPage() = nextPageRequestsSubject.onNext(Unit)
    fun receiveNewMessage(message: Message) = newMessagesSubject.onNext(message)
    fun receiveNewPostsPage(messages: List<Message>) = postsPageSubject.onNext(messages)
    fun receiveEndOfPaging() = postsPageSubject.onCompleted()
    fun retry() = errorSubject.onNext(Unit)
}