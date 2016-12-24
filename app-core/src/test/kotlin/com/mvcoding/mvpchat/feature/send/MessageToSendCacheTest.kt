package com.mvcoding.mvpchat.feature.send

import com.mvcoding.mvpchat.model.MessageToSend
import com.mvcoding.mvpchat.model.aMessageToSend
import org.junit.Test
import rx.observers.TestSubscriber

class MessageToSendCacheTest {

    val messageToSend = aMessageToSend()
    val subscriber = TestSubscriber<MessageToSend>()
    val messageToSendCache = MessageToSendCache()

    @Test
    fun `emits written value`() {
        messageToSendCache.data().subscribe(subscriber)

        messageToSendCache.write(messageToSend)

        subscriber.assertValue(messageToSend)
    }

    @Test
    fun `subscribers will get cached value`() {
        messageToSendCache.write(messageToSend)

        messageToSendCache.data().subscribe(subscriber)

        subscriber.assertValue(messageToSend)
    }
}