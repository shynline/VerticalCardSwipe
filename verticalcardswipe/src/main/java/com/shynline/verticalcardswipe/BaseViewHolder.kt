package com.shynline.verticalcardswipe

import android.view.View

open class BaseViewHolder(itemView: View) {
    var expired: Boolean = false

    init {
        expired = false
    }
}