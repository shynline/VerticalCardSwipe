package com.shynline.verticalcardswipe

import android.view.View
import android.view.ViewGroup

/***
 * VerticalCardSwipe base adapter
 */
@Suppress("UNCHECKED_CAST")
abstract class VerticalCardAdapter<T, VH : BaseViewHolder> {

    private val items: MutableList<T> = arrayListOf()

    internal val count: Int
        get() = items.size

    fun getItem(position: Int): T {
        return items[position]
    }

    fun getItems(): MutableList<T> {
        return items
    }

    /***
     * This will be called when a card has been expired
     * the view will handle layout automatically
     *
     */
    open fun onViewExpired(holder: VH, position: Int) {
        holder.expired = true
    }

    internal fun removeFirstItem(): Boolean {
        return if (count > 0) {
            items.removeAt(0)
            true
        } else {
            false
        }
    }

    /***
     * inflate your card view here
     */
    abstract fun createItemView(parent: ViewGroup): View

    /***
     * Create your view holder here
     */
    abstract fun createViewHolder(itemView: View): VH

    /***
     * This will be called when a card updates
     *
     */
    abstract fun onUpdateViewHolder(holder: VH, position: Int)

    /***
     * This will be called when a card binds
     */
    abstract fun onBindViewHolder(holder: VH, position: Int)

    internal fun getView(position: Int, convert_view: View?, parent: ViewGroup): View {
        var convertView = convert_view
        val holder: VH
        if (convertView == null) {
            convertView = createItemView(parent)
            holder = createViewHolder(convertView)
            convertView.tag = holder
        } else {
            holder = convertView.tag as VH
        }
        onBindViewHolder(holder, position)
        return convertView
    }

    internal fun getHolder(convertView: View): VH {
        return convertView.tag as VH
    }

}

