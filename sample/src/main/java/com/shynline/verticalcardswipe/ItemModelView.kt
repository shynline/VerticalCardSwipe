package com.shynline.verticalcardswipe

data class ItemModelView(var text: String, var id: Long = 0, var ad : Boolean = false) : Ads {

    init {
        this.id = idHolder
        idHolder++
    }

    override fun isCurrentItemAd(): Boolean {
        return ad
    }

    companion object {
        var idHolder: Long = 0
    }
}
