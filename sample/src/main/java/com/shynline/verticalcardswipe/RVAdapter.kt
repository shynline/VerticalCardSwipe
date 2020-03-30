package com.shynline.verticalcardswipe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class RVAdapter(private val count: Int, var text: String?) : RecyclerView.Adapter<RVAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.cell, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//        val rnd = Random()
//        val color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256))
//        holder.root.setBackgroundColor(color)
//        Glide.with(holder.image).load("https://picsum.photos/300/600?temp=${UUID.randomUUID()}")
//                .apply(RequestOptions().centerCrop())
//                .into(holder.image)
    }

    override fun getItemCount(): Int {
        return count
    }


    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var root: ViewGroup
        var image: ImageView

        init {
            root = itemView.findViewById(R.id.root)
            image = itemView.findViewById(R.id.image)
        }
    }
}
