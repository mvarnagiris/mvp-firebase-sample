package com.mvcoding.mvpchat

interface DataWriter<in DATA> {
    fun write(data: DATA)
}