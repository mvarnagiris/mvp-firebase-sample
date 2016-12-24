package com.mvcoding.mvpchat.feature.send

import com.mvcoding.mvp.Presenter
import com.mvcoding.mvpchat.DataCache
import com.mvcoding.mvpchat.DataWriter
import com.mvcoding.mvpchat.RxSchedulers
import com.mvcoding.mvpchat.model.MessageToSend
import rx.Observable

class SendMessagePresenter(
        private val messageToSendCache: DataCache<MessageToSend>,
        private val remoteMessageToSendWriter: DataWriter<MessageToSend>,
        private val schedulers: RxSchedulers) : Presenter<SendMessagePresenter.View>() {

    override fun onViewAttached(view: View) {
        super.onViewAttached(view)

        view.messageChanges()
                .map(::MessageToSend)
                .subscribeUntilDetached { messageToSendCache.write(it) }

        val messageToSendChanges = messageToSendCache.data().share()

        messageToSendChanges.subscribeUntilDetached {
            view.showMessageToSend(it)
            enableOrDisableSend(view, it)
        }

        view.sends()
                .withLatestFrom(messageToSendChanges) { _, messageToSend -> messageToSend }
                .observeOn(schedulers.io)
                .doOnNext { remoteMessageToSendWriter.write(it) }
                .observeOn(schedulers.main)
                .subscribeUntilDetached { clearMessageToSend() }
    }

    private fun enableOrDisableSend(view: View, messageToSend: MessageToSend) {
        if (messageToSend.isEmpty()) view.disableSend() else view.enableSend()
    }

    private fun clearMessageToSend(): Unit = messageToSendCache.write(emptyMessageToSend())
    private fun emptyMessageToSend() = MessageToSend("")

    interface View : Presenter.View {
        fun sends(): Observable<Unit>
        fun messageChanges(): Observable<String>
        fun showMessageToSend(messageToSend: MessageToSend)
        fun enableSend()
        fun disableSend()
    }
}