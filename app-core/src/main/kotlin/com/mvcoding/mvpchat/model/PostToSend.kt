package com.mvcoding.mvpchat.model

data class PostToSend(val message: String) {
    fun isEmpty(): Boolean = message.isEmpty()
}