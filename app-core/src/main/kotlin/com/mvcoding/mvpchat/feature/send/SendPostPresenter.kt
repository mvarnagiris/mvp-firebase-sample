package com.mvcoding.mvpchat.feature.send

import com.mvcoding.mvp.Presenter
import com.mvcoding.mvpchat.DataWriter
import com.mvcoding.mvpchat.RxSchedulers
import com.mvcoding.mvpchat.model.PostToSend
import rx.Observable

class SendPostPresenter(
        private val postToSendWriter: DataWriter<PostToSend>,
        private val schedulers: RxSchedulers) : Presenter<SendPostPresenter.View>() {

    private var messageToSend = PostToSend("")

    override fun onViewAttached(view: View) {
        super.onViewAttached(view)

        val messageChanges = view.messageChanges()
                .map(::PostToSend)
                .startWith(messageToSend)
                .distinctUntilChanged()
                .doOnNext { messageToSend = it }
                .doOnNext { view.showMessageToSend(it) }
                .doOnNext { enableOrDisableSend(view, it) }

        view.sends().withLatestFrom(messageChanges) { _, messageToSend -> messageToSend }
                .doOnNext { clearMessageToSend(view) }
                .observeOn(schedulers.io)
                .subscribeUntilDetached { postToSendWriter.write(it) }
    }

    private fun enableOrDisableSend(view: View, postToSend: PostToSend) {
        if (postToSend.isEmpty()) view.disableSend() else view.enableSend()
    }

    private fun clearMessageToSend(view: View) {
        messageToSend = PostToSend("")
        view.showMessageToSend(messageToSend)
    }

    interface View : Presenter.View {
        fun sends(): Observable<Unit>
        fun messageChanges(): Observable<String>

        fun showMessageToSend(postToSend: PostToSend)
        fun enableSend()
        fun disableSend()
    }
}