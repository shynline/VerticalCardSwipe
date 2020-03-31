package com.shynline.verticalcardswipe


/***
 * This default config of the VerticalCardSwipe View
 * it can be instantiated and modified
 */
class Config {
    var bottomDragLimit = 0.25f
    var bottomDragCallThreshold = 0.125f

    var topDragLimit = 0.25f
    var topDragCallThreshold = 0.125f

    var swipeThreshold = 0.25f

    var bottomCardScaleDiff = 0.15f
    var topCardScaleDiff = 0.3f

    var cardRadiusDP = 20f

    var topOverLaySoftener = 0.01f
    var bottomOverlaySoftener = 0.01f

    var cardTopMarginDP = 10f
    var cardBottomMarginDP = 10f
    var cardRightMarginDP = 10f
    var cardLeftMarginDP = 10f

    var preventSwipeTopIfExpired = false
    var preventSwipeBottomIfExpired = false
    var preventSwipeTopIfAd = false
    var preventSwipeBottomIfAd = false
}
