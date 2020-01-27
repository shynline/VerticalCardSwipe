package com.shynline.verticalcardswipe

/***
 * An interface for proceed or revert the animation
 * when first time swiping cause animation pause on views
 *
 */
interface FirstTimeActions {

    /***
     * Call this if you need to proceed swiping
     */
    fun proceed()

    /***
     * Call this if you want to revert to last state without swiping the card
     */
    fun cancel()
}