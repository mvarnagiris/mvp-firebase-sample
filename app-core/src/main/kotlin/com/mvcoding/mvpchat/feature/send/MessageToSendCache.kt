package com.mvcoding.mvpchat.feature.send

import com.mvcoding.mvpchat.DataCache
import com.mvcoding.mvpchat.model.MessageToSend
import rx.Observable
import rx.lang.kotlin.BehaviorSubject

class MessageToSendCache : DataCache<MessageToSend> {
    private val messageToSendSubject = BehaviorSubject<MessageToSend>()

    override fun write(data: MessageToSend): Unit = messageToSendSubject.onNext(data)
    override fun data(): Observable<MessageToSend> = messageToSendSubject
}