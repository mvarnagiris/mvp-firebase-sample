package com.mvcoding.mvpchat.model

data class MessageToSend(val text: String) {
    fun isEmpty(): Boolean = text.isEmpty()
}