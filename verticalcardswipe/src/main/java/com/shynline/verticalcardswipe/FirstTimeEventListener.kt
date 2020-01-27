package com.shynline.verticalcardswipe

/***
 * This interface helps to interact and notify on first time swiping
 */
interface FirstTimeEventListener<T> {

    /***
     * Ask user if the current card is swiping top for the first time
     * @param item is the card which is going to be swiped
     */
    fun isSwipingTopForFirstTime(item: T?): Boolean

    /***
     * Ask user if the current card is swiping bottom for the first time
     * @param item is the card which is going to be swiped
     */
    fun isSwipingBottomForFirstTime(item: T?): Boolean

    /***
     * This will be called if it's first time that the card is swiping top
     * @param callback helps to control the flow
     * @param item is the card which is being swiped
     */
    fun swipingTopPaused(item: T?, callback: FirstTimeActions)

    /***
     * This will be called if it's first time that the card is swiping bottom
     * @param callback helps to control the flow
     * @param item is the card which is being swiped
     */
    fun swipingBottomPaused(item: T?, callback: FirstTimeActions)
}