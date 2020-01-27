package com.shynline.verticalcardswipe

/***
 * This interface is used for inserting and controlling ads
 */
interface Ads {
    /***
     * With this method determine if the current
     * card is an Ad
     */
    fun isCurrentItemAd(): Boolean
}