package com.mvcoding.mvpchat.model

import com.mvcoding.mvpchat.aString

fun aMessageToSend() = MessageToSend(aString("text"))
fun anEmptyMessageToSend() = MessageToSend("")
fun MessageToSend.withText(text: String) = copy(text = text)