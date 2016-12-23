package com.mvcoding.mvpchat

import rx.Observable

interface DataSource<DATA> {
    fun data(): Observable<DATA>
}