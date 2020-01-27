package com.shynline.verticalcardswipe

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import java.util.*

class ViewPagerAdaper(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {


    override fun getItem(position: Int): Fragment {
        return FragArray.newInstance()
    }


    override fun getCount(): Int {
        return 5
    }

    class FragArray : Fragment() {


        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val vv = inflater.inflate(R.layout.cell, container, false)

            val rnd = Random()
            val color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256))
            val textView = vv.findViewById<TextView>(R.id.text)
            textView.text = Date().toString()
            vv.findViewById<View>(R.id.root).setBackgroundColor(color)
            Log.d("touchsystem", color.toString())
            return vv
        }

        companion object {

            fun newInstance(): FragArray {

                val args = Bundle()

                val fragment = FragArray()
                fragment.arguments = args
                return fragment
            }
        }
    }
}
