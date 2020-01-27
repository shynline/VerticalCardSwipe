package com.shynline.verticalcardswipe

import android.content.Context

internal object Utils {
    fun toPx(context: Context, dp: Float): Float {
        val scale = context.resources.displayMetrics.density
        return dp * scale + 0.5f
    }
}
