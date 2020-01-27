package com.shynline.verticalcardswipe

import android.view.View

/***
 * This is the base view holder for the cards
 */
open class BaseViewHolder(itemView: View) {
    var expired: Boolean = false

    init {
        expired = false
    }
}