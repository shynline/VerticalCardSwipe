package com.shynline.verticalcardswipe

class Config {
    var bottomDragLimit = 0.25f
    var bottomDragCallThreshold = 0.125f
    var swipeThreshold = 0.25f
    var scaleDiff = 0.15f
    var cardRadiusDP = 20f
    var topOverLayHardener = 1.5f
    var bottomOverlaySoftener = 0.75f

    var cardTopMarginDP = 10f
    var cardBottomMarginDP = 10f
    var cardRightMarginDP = 10f
    var cardLeftMarginDP = 10f


    fun setMarginDP(left: Float, top: Float, right: Float, bottom: Float): Config {
        this.cardTopMarginDP = top
        this.cardBottomMarginDP = bottom
        this.cardRightMarginDP = right
        this.cardLeftMarginDP = left
        return this
    }

    fun setCardRadiusDP(cardRadiusDP: Float): Config {
        this.cardRadiusDP = cardRadiusDP
        return this
    }


    fun setScaleDiff(scaleDiff: Float): Config {
        this.scaleDiff = scaleDiff
        return this
    }


    fun setBottomDragCallThreshold(bottomDragCallThreshold: Float): Config {
        this.bottomDragCallThreshold = bottomDragCallThreshold
        return this
    }

    fun setBottomDragLimit(bottomDragLimit: Float): Config {
        this.bottomDragLimit = bottomDragLimit
        return this
    }


    fun setSwipeThreshold(swipeThreshold: Float): Config {
        this.swipeThreshold = swipeThreshold
        return this
    }
}
