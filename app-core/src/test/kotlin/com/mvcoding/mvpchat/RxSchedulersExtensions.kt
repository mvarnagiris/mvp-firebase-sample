package com.mvcoding.mvpchat

import rx.schedulers.Schedulers.immediate

fun rxSchedulers() = RxSchedulers(immediate(), immediate(), immediate())