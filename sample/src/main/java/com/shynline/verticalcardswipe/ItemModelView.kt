package com.shynline.verticalcardswipe

class ItemModelView(var text: String) : Ads {
    var id: Long = 0
    var ad = false

    init {
        this.id = idHolder
        idHolder++
    }

    override fun isCurrentItemAd(): Boolean {
        return ad
    }


    override fun equals(other: Any?): Boolean {
        return if (other is ItemModelView) {
            other.id == id
        } else false
    }

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + id.hashCode()
        return result
    }

    companion object {
        var idHolder: Long = 0
    }
}
