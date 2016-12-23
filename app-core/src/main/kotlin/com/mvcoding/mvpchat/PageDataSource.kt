package com.mvcoding.mvpchat

interface PageDataSource<DATA> : DataSource<DATA> {
    fun hasNextPage(): Boolean
    fun resetPaging()
}