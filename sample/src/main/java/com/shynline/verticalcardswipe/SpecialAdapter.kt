package com.shynline.verticalcardswipe


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView

class SpecialAdapter(private val context: Context) : VerticalCardAdapter<ItemModelView, ViewHolder>() {

    override fun createItemView(parent: ViewGroup): View {
        return LayoutInflater.from(parent.context).inflate(R.layout.pagercell, parent, false)
    }

    override fun createViewHolder(itemView: View): ViewHolder {
        return ViewHolder(itemView, context)
    }

    override fun onUpdateViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.adapter?.text = item.text
        holder.adapter?.notifyDataSetChanged()
    }

    override fun onViewExpired(holder: ViewHolder, position: Int) {
        super.onViewExpired(holder, position)
        holder.adapter?.notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        if (!item.isCurrentItemAd()) {
            holder.recyclerView.visibility = View.VISIBLE
            holder.adapter = RVAdapter(5, item.text)
            holder.recyclerView.adapter = holder.adapter
            holder.ad.visibility = View.GONE
            holder.text.text = "${item.id}"
        } else {
            holder.recyclerView.visibility = View.GONE
            holder.ad.visibility = View.VISIBLE
            holder.ad.text = "This is a fancy ad"
        }


    }

}

class ViewHolder(itemView: View, context: Context) : BaseViewHolder(itemView) {
    var recyclerView: RecyclerView = itemView.findViewById(R.id.rv)
    var llm: LinearLayoutManager = LinearLayoutManager(
            context, LinearLayoutManager.HORIZONTAL, false
    )
    var ad = itemView.findViewById<TextView>(R.id.ad)
    var text = itemView.findViewById<TextView>(R.id.text)
    var adapter: RVAdapter? = null

    init {

        recyclerView.layoutManager = llm
        recyclerView.addItemDecoration(LinePagerIndicatorDecoration())
        llm.scrollToPosition(1) // best for initiation
        // problem when it merge with natural user flings
        val snapHelper =
                object : PagerSnapHelper() {
                    override fun findTargetSnapPosition(layoutManager: RecyclerView.LayoutManager, velocityX: Int, velocityY: Int): Int {
                        val i = super.findTargetSnapPosition(layoutManager, velocityX, velocityY)
                        // not reliable - wont call in smooth user manual page without fling
                        return i

                    }
                }

        snapHelper.attachToRecyclerView(recyclerView)
    }
}
