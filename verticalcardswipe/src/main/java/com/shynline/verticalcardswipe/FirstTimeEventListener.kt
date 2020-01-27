package com.shynline.verticalcardswipe

interface FirstTimeEventListener<T> {
    fun isSwipingTopForFirstTime(item: T?): Boolean

    fun isSwipingBottomForFirstTime(item: T?): Boolean

    fun swipingTopPaused(item: T?, callback: FirstTimeActions)

    fun swipingBottomPaused(item: T?, callback: FirstTimeActions)

}