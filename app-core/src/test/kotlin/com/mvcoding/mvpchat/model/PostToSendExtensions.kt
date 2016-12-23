package com.mvcoding.mvpchat.model

import com.mvcoding.mvpchat.aString

fun aPostToSend() = PostToSend(aString("message"))
fun anEmptyPostToSend() = PostToSend("")
fun PostToSend.withMessage(message: String) = copy(message = message)