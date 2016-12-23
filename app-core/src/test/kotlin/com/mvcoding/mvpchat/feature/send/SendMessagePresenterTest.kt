package com.mvcoding.mvpchat.feature.send

import com.mvcoding.mvpchat.DataCache
import com.mvcoding.mvpchat.DataWriter
import com.mvcoding.mvpchat.model.MessageToSend
import com.mvcoding.mvpchat.model.aMessageToSend
import com.mvcoding.mvpchat.model.anEmptyMessageToSend
import com.mvcoding.mvpchat.model.withText
import com.mvcoding.mvpchat.rxSchedulers
import com.nhaarman.mockito_kotlin.inOrder
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Test
import rx.lang.kotlin.PublishSubject

class SendMessagePresenterTest {

    val sendSubject = PublishSubject<Unit>()
    val messageChangesSubject = PublishSubject<String>()
    val messageToSendCacheSubject = PublishSubject<MessageToSend>()

    val messageToSendCache = mock<DataCache<MessageToSend>>()
    val remoteMessageToSendWriter = mock<DataWriter<MessageToSend>>()
    val view = mock<SendMessagePresenter.View>()
    val inOrder = inOrder(view, messageToSendCache, remoteMessageToSendWriter)
    val presenter = SendMessagePresenter(messageToSendCache, remoteMessageToSendWriter, rxSchedulers())

    @Before
    fun setUp() {
        whenever(view.sends()).thenReturn(sendSubject)
        whenever(view.messageChanges()).thenReturn(messageChangesSubject)
        whenever(messageToSendCache.data()).thenReturn(messageToSendCacheSubject)
    }

    @Test
    fun `writes message changes to cache`() {
        presenter.attach(view)

        changeMessage("message")

        verify(messageToSendCache).write(aMessageToSend().withText("message"))
    }

    @Test
    fun `shows messages to send from cache`() {
        val messageToSend = aMessageToSend()
        val anotherMessageToSend = aMessageToSend()
        presenter.attach(view)

        receiveMessageToSendFromCache(messageToSend)
        receiveMessageToSendFromCache(anotherMessageToSend)

        inOrder.verify(view).showMessageToSend(messageToSend)
        inOrder.verify(view).showMessageToSend(anotherMessageToSend)
    }

    @Test
    fun `disables send action when message to send is empty`() {
        val notEmptyMessageToSend = aMessageToSend()
        val emptyMessageToSend = anEmptyMessageToSend()
        presenter.attach(view)

        receiveMessageToSendFromCache(notEmptyMessageToSend)
        receiveMessageToSendFromCache(emptyMessageToSend)

        inOrder.verify(view).enableSend()
        inOrder.verify(view).disableSend()
    }

    @Test
    fun `clears message to send after it was sent`() {
        val messageToSend = aMessageToSend()
        presenter.attach(view)
        receiveMessageToSendFromCache(messageToSend)

        send()

        inOrder.verify(remoteMessageToSendWriter).write(messageToSend)
        inOrder.verify(messageToSendCache).write(anEmptyMessageToSend())
    }

    fun changeMessage(message: String) = messageChangesSubject.onNext(message)
    fun receiveMessageToSendFromCache(messageToSend: MessageToSend) = messageToSendCacheSubject.onNext(messageToSend)
    fun send() = sendSubject.onNext(Unit)
}