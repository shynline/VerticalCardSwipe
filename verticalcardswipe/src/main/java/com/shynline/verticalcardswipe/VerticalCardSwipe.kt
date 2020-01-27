package com.shynline.verticalcardswipe

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Point
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import java.util.*

class VerticalCardSwipe<T, VH : BaseViewHolder> : FrameLayout {

    val config = Config()
    private val containers = LinkedList<CardContainer<T>>()
    private var adapter: VerticalCardAdapter<T, VH>? = null
    private var performSwipeAnimationCallbackUnLock = false
    private var oracleListener: OracleListener<T>? = null
    var firstTimeEventListener: FirstTimeEventListener<T>? = null
//    var firstTimeSwippingTop = true
//    var firstTimeSwippingBotton = true
//
//    private val firstTimeEventListener = object : OracleContainer.FirstTimeEventListener<T> {
//        override fun isSwipingBottomForFirstTime(): Boolean {
//            return firstTimeSwippingBotton
//        }
//
//        override fun isSwipingTopForFirstTime(): Boolean {
//            return firstTimeSwippingTop
//        }
//
//        override fun swipingBottomPaused(item: T?, callback: OracleContainer.FirstTimeActions) {
//            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//        }
//
//        override fun swipingTopPaused(item: T?, callback: OracleContainer.FirstTimeActions) {
//            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//        }
//    }

    private val containerEventListener = object : CardContainer.ContainerEventListener<T> {
        override fun onContainerDragging(percentY: Float, expired: Boolean) {
            update(percentY)
            if (oracleListener != null) {
                oracleListener!!.onDragging(percentY)
            }
        }

        override fun onContainerMovedToOrigin(fromDirection: SwipeDirection, expired: Boolean) {
            if (oracleListener != null) {
                oracleListener!!.onMovedToOrigin(fromDirection)
            }
        }

        override fun onContainerReleasedFromBottom(item: T?, expired: Boolean) {
            if (oracleListener != null) {
                oracleListener!!.onReleasedToAction(item)
            }
        }


        override fun onContainerSwipedTop(item: T?, point: Point, expired: Boolean) {
            swipe(point, item)
        }
    }

    val cloneCards: List<T>
        get() = ArrayList(adapter!!.getItems())

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun addCard(item: T) {
        adapter!!.getItems().add(item)
        if (adapter!!.count == 1) {
            loadTopView()
        } else if (adapter!!.count == 2) {
            loadBottomView()
        }
    }

    fun currentCardCount(): Int {
        return adapter!!.count
    }

    fun addCards(items: Collection<T>) {
        val lastCount = adapter!!.count
        adapter!!.getItems().addAll(items)
        if (lastCount >= 2) {
            return
        }
        if (lastCount == 0) {
            if (items.size > 1) {
                loadTopView()
                loadBottomView()
            } else if (items.isNotEmpty()) {
                loadTopView()
            }

        } else if (lastCount == 1) {
            if (items.isNotEmpty()) {
                loadBottomView()
            }
        }

    }

    fun removeCard(index: Int): Boolean {
        if (index >= adapter!!.count) {
            return false
        }
        when (index) {
            0 -> {
                adapter!!.onViewExpired(adapter!!.getHolder(containers.first.frameContent.getChildAt(0)), 0)
                containers.first.setExpired()
            }
            1 -> {
                adapter!!.onViewExpired(adapter!!.getHolder(containers.last.frameContent.getChildAt(0)), 1)
                containers.last.setExpired()
            }
            else -> adapter!!.getItems().removeAt(index)
        }
        return true
    }

    fun removeCard(item: T): Boolean {
        for (i in adapter!!.getItems()) {
            if (i == item) {
                return removeCard(adapter!!.getItems().indexOf(i))
            }
        }
        return false
    }

    fun updateCard(newItem: T, updater: Updater<T>, addIfNotExist: Boolean): Boolean {
        var index = -1
        val size = adapter!!.count
        for (i in 0 until size) {
            if (adapter!!.getItems()[i] == newItem) {
                index = i
                break
            }
        }
        if (index == -1) {
            if (addIfNotExist) {
                addCard(newItem)
            }
            return false
        }

        updater.update(adapter!!.getItem(index), newItem)

        if (index == 0) { // update top card
            adapter!!.onUpdateViewHolder(adapter!!.getHolder(containers.first.frameContent.getChildAt(0)), 0)
            containers.first.item = adapter!!.getItem(index)
        } else if (index == 1) { // update bottom card
            adapter!!.onUpdateViewHolder(adapter!!.getHolder(containers.last.frameContent.getChildAt(0)), 1)
            containers.last.item = adapter!!.getItem(index)
        }

        return true

    }

    private var topLayout: Int? = null
    private var bottomLayout: Int? = null
    private var expireLayout: Int? = null
    private lateinit var topLayoutInitializer: (View) -> Unit
    private lateinit var bottomLayoutInitializer: (View) -> Unit
    private lateinit var expireLayoutInitializer: (View) -> Unit
    fun initializeTheOrb(adapter: VerticalCardAdapter<T, VH>
                         , top_layout: Int? = null
                         , topLayoutInitializer: ((View) -> Unit)? = null
                         , bottom_layout: Int? = null
                         , bottomLayoutInitializer: ((View) -> Unit)? = null
                         , expireLayout: Int? = null
                         , expireLayoutInitializer: ((View) -> Unit)? = null) {
        this.adapter = adapter
        this.topLayout = top_layout
        this.bottomLayout = bottom_layout
        this.expireLayout = expireLayout
        if (topLayout != null) {
            if (topLayoutInitializer == null) {
                throw RuntimeException("overlay layout need initializer.")
            }
            this.topLayoutInitializer = topLayoutInitializer
        }
        if (bottomLayout != null) {
            if (bottomLayoutInitializer == null) {
                throw RuntimeException("overlay layout need initializer.")
            }
            this.bottomLayoutInitializer = bottomLayoutInitializer
        }
        if (expireLayout != null) {
            if (expireLayoutInitializer == null) {
                throw RuntimeException("overlay layout need initializer.")
            }
            this.expireLayoutInitializer = expireLayoutInitializer
        }
        init()
    }

    private fun createContainer(): CardContainer<T> {
        val oracleContainer = CardContainer<T>(context)
        val layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)


        layoutParams.setMargins(Utils.toPx(context, config.cardLeftMarginDP).toInt(), Utils.toPx(context, config.cardTopMarginDP).toInt(), Utils.toPx(context, config.cardRightMarginDP).toInt(), Utils.toPx(context, config.cardBottomMarginDP).toInt())
        oracleContainer.layoutParams = layoutParams
        oracleContainer.radius = Utils.toPx(context, config.cardRadiusDP)
        if (topLayout != null) {
            val tv = LayoutInflater.from(context)
                    .inflate(topLayout!!, oracleContainer.frameOverlayTop, false)
            topLayoutInitializer(tv)
            oracleContainer.frameOverlayTop.addView(tv)
        }
        if (bottomLayout != null) {
            val bv = LayoutInflater.from(context)
                    .inflate(bottomLayout!!, oracleContainer.frameOverlayBottom, false)
            bottomLayoutInitializer(bv)
            oracleContainer.frameOverlayBottom.addView(bv)
        }
        if (expireLayout != null) {
            val ev = LayoutInflater.from(context)
                    .inflate(expireLayout!!, oracleContainer.frameOverlayExpire, false)
            expireLayoutInitializer(ev)
            oracleContainer.frameOverlayExpire.addView(ev)
        }
        return oracleContainer
    }

    private fun init() {

        removeAllViews()
        containers.clear()
        // create top view
        containers.add(createContainer())
        // create bottom view
        containers.add(createContainer())

        addView(containers.last)
        addView(containers.first)

        containers.first.setViewOriginY()
        containers.last.setViewOriginY()

        containers.first.setConfig(config)
        containers.last.setConfig(config)

        containers.last.setContainerEventListener(null)
        containers.first.setContainerEventListener(null)

        containers.last.firstTimeEventListener = null
        containers.first.firstTimeEventListener = null

        containers.first.visibility = View.GONE
        containers.last.visibility = View.GONE
        update(0f)
    }
    //
    //    private void x() {
    //        OracleContainer<T> container = containers.getFirst();
    //        if (container.getChildCount() == 0) {
    //            container.addView(
    //                    adapter.getView(0, container.getChildAt(0), container));
    //        }
    //        container.setDraggable(true);
    //        container.setContainerEventListener(containerEventListener);
    //
    //        container = containers.getLast();
    //        if (container.getChildCount() == 0) {
    //            container.addView(
    //                    adapter.getView(1, container.getChildAt(0), container));
    //        }
    //        container.setDraggable(false);
    //        container.setContainerEventListener(null);
    //
    //    }

    private fun update(percentY: Float) {


        var view: CardContainer<*> = containers.last

        var currentScale = 1f - config.scaleDiff
        var nextScale = 1f
        var percent = currentScale + (nextScale - currentScale) * Math.abs(percentY)
        view.scaleX = percent
        view.scaleY = percent

        if (percentY <= 0) {
            view = containers.first
            currentScale = 1f
            nextScale = 1f - config.scaleDiff * 2
            percent = currentScale + (nextScale - currentScale) * Math.abs(percentY)
            view.scaleX = percent
            view.scaleY = percent
        }


    }

    fun swipe(point: Point, item: T?, silent: Boolean = false, artificial: Boolean = false) {
        executePreSwipeTask()
        performSwipe(point, artificial, object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animator: Animator) {
                executePostSwipeTask(point)
                if (!silent) {
                    oracleListener?.onSwipedTop(item)
                }

            }
        })
    }

    private fun executePreSwipeTask() {
        containers.first.setContainerEventListener(null)
        containers.first.firstTimeEventListener = null
        containers.first.setDraggable(false)
        containers.last.setContainerEventListener(null)
        containers.last.firstTimeEventListener = null
        containers.last.setDraggable(false)

    }

    fun swipeTop(silent: Boolean) {
        if (adapter!!.count == 0) {
            return
        }
        swipe(containers.first.getTopSwipeTargetPoint()
                , containers.first.item, silent, artificial = true)
    }

    private fun performSwipe(point: Point, artificial: Boolean, listener: Animator.AnimatorListener) {
        performSwipeAnimationCallbackUnLock = false

        containers.last.animate()
                .translationX(0f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(200L)
                .setUpdateListener(null)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (performSwipeAnimationCallbackUnLock) {
                            listener.onAnimationEnd(ObjectAnimator())
                        } else {
                            performSwipeAnimationCallbackUnLock = true
                        }
                    }
                })
                .start()

        val ani = containers.first.animate()
        if (artificial) {
            ani.scaleX(0.5f).scaleY(0.5f)
        }
        ani.translationY(point.y.toFloat())
                .setDuration(200L)
                .setUpdateListener(null)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (performSwipeAnimationCallbackUnLock) {
                            listener.onAnimationEnd(ObjectAnimator())
                        } else {
                            performSwipeAnimationCallbackUnLock = true
                        }
                    }
                })
                .start()


    }

    private fun moveToBottom(container: CardContainer<*>) {
        val parent = container.parent as VerticalCardSwipe<*, *>
        parent.removeView(container)
        parent.addView(container, 0)


    }

    private fun reorderForSwipe() {
        moveToBottom(containers.first)
        containers.addLast(containers.removeFirst())
    }

    private fun clear() {
        val view = containers.last
        view.translationX = 0f
        view.translationY = 0f
        view.scaleX = 1f
        view.scaleY = 1f

    }

    private fun loadTopView() {
        val container = containers.first
        container.reset()
        if (adapter!!.count > 0) {
            container.setDraggable(true)
            container.setContainerEventListener(containerEventListener)
            container.firstTimeEventListener = firstTimeEventListener
            val child = adapter!!.getView(0,
                    container.frameContent.getChildAt(0), container.frameContent)
            if (container.frameContent.childCount == 0) {
                container.frameContent.addView(child)
            }
            val item = adapter!!.getItem(0)
            if (item is Ads) {
                if (item.isCurrentItemAd()) {
                    container.setAsAds()
                }
            }
            container.item = item
            container.setVisibility(View.VISIBLE)
        } else {
            container.setDraggable(false)
            container.setContainerEventListener(null)
            container.firstTimeEventListener = null
            container.setVisibility(View.GONE)
        }
    }

    private fun loadBottomView() {
        val container = containers.last
        container.reset()
        if (adapter!!.count > 1) {
            container.setDraggable(false)
            container.setContainerEventListener(null)
            container.firstTimeEventListener = null
            val child = adapter!!.getView(1,
                    container.frameContent.getChildAt(0), container.frameContent)
            if (container.frameContent.childCount == 0) {
                container.frameContent.addView(child)
            }
            val item = adapter!!.getItem(1)
            if (item is Ads) {
                if (item.isCurrentItemAd()) {
                    container.setAsAds()
                }
            }
            container.item = item
            container.setVisibility(View.VISIBLE)
        } else {
            container.setDraggable(false)
            container.setContainerEventListener(null)
            container.firstTimeEventListener = null
            container.setVisibility(View.GONE)
        }
    }

    private fun loadNextView() {
        loadBottomView()

        if (adapter!!.count > 0) {
            containers.first.setDraggable(true)
            containers.first.setContainerEventListener(containerEventListener)
            containers.first.firstTimeEventListener = firstTimeEventListener
        }
    }

    private fun executePostSwipeTask(point: Point) {

        adapter!!.removeFirstItem()
        reorderForSwipe()
        clear()
        update(0f)
        loadNextView()
    }

    fun setOracleListener(oracleListener: OracleListener<T>) {
        this.oracleListener = oracleListener
    }

    interface Updater<T> {
        fun update(oldItem: T, newItem: T)
    }

    interface OracleListener<T> {
        fun onDragging(percent: Float)

        fun onMovedToOrigin(fromDirection: SwipeDirection)

        fun onReleasedToAction(item: T?)

        fun onSwipedTop(item: T?)
    }
}
