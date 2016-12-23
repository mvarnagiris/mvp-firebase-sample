package com.mvcoding.mvpchat.feature.send

import com.mvcoding.mvpchat.DataWriter
import com.mvcoding.mvpchat.model.PostToSend
import com.mvcoding.mvpchat.model.aPostToSend
import com.mvcoding.mvpchat.model.anEmptyPostToSend
import com.mvcoding.mvpchat.model.withMessage
import com.mvcoding.mvpchat.rxSchedulers
import com.nhaarman.mockito_kotlin.*
import org.junit.Before
import org.junit.Test
import rx.lang.kotlin.PublishSubject

class SendPostPresenterTest {

    val sendSubject = PublishSubject<Unit>()
    val messageChangesSubject = PublishSubject<String>()

    val messageToSend = aPostToSend().withMessage("message")

    val postToSendWriter = mock<DataWriter<PostToSend>>()
    val view = mock<SendPostPresenter.View>()
    val inOrder = inOrder(view, postToSendWriter)
    val presenter = SendPostPresenter(postToSendWriter, rxSchedulers())

    @Before
    fun setUp() {
        whenever(view.sends()).thenReturn(sendSubject)
        whenever(view.messageChanges()).thenReturn(messageChangesSubject)
    }

    @Test
    fun `initially shows empty message`() {
        presenter.attach(view)

        verify(view).showMessageToSend(anEmptyPostToSend())
    }

    @Test
    fun `shows updated message only when it changes`() {
        presenter.attach(view)

        changeMessage(messageToSend.message)
        changeMessage(messageToSend.message)

        verify(view, times(1)).showMessageToSend(messageToSend)
    }

    @Test
    fun `shows last known message after reattach`() {
        presenter.attach(view)
        changeMessage(messageToSend.message)

        presenter.detach(view)
        presenter.attach(view)

        verify(view, times(2)).showMessageToSend(messageToSend)
    }

    @Test
    fun `disables send action when message is empty`() {
        presenter.attach(view)
        inOrder.verify(view).disableSend()

        changeMessage("message")
        inOrder.verify(view).enableSend()

        changeMessage("")
        inOrder.verify(view).disableSend()
    }

    @Test
    fun `sends message and shows empty message when send action is invoked`() {
        presenter.attach(view)
        changeMessage(messageToSend.message)

        send()

        inOrder.verify(view).showMessageToSend(anEmptyPostToSend())
        inOrder.verify(postToSendWriter).write(messageToSend)
    }

    fun changeMessage(message: String) = messageChangesSubject.onNext(message)
    fun send() = sendSubject.onNext(Unit)
}