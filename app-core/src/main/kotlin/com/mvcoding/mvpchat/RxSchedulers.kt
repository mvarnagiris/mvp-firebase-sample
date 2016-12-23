package com.mvcoding.mvpchat

import rx.Scheduler

data class RxSchedulers(val main: Scheduler, val io: Scheduler, val computation: Scheduler)