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

/***
 * VerticalCardSwipe
 * a container for having 2 cards with ability of swiping vertically
 */
class VerticalCardSwipe<T, VH : BaseViewHolder> : FrameLayout {

    val config = Config()
    private val containers = LinkedList<CardContainer<T>>()
    private lateinit var adapter: VerticalCardAdapter<T, VH>
    private var performSwipeAnimationCallbackUnLock = false
    var verticalCardsStateListener: VerticalCardsStateListener<T>? = null
    var verticalCardsActionListener: VerticalCardsActionListener<T>? = null
    var verticalCardsSwipeListener: VerticalCardsSwipeListener<T>? = null

    private var topLayout: Int? = null
    private var bottomLayout: Int? = null
    private var expireLayout: Int? = null

    private var topLayoutInitializer: ((View) -> Unit)? = null
    private var bottomLayoutInitializer: ((View) -> Unit)? = null
    private var expireLayoutInitializer: ((View) -> Unit)? = null

    private val itemConfig = ItemConfig()

    private val containerEventListener = object : CardContainer.ContainerEventListener<T> {
        override fun onContainerDragging(percentY: Float, expired: Boolean) {
            update(percentY)
            verticalCardsStateListener?.onDragging(percentY)
        }

        override fun onContainerMovedToOrigin(fromDirection: SwipeDirection, expired: Boolean) {
            verticalCardsStateListener?.onMovedToOrigin(fromDirection)
        }

        override fun onContainerReleasedFromBottom(item: T?, expired: Boolean) {
            verticalCardsActionListener?.onReleasedToActionBottom(item, expired)
        }

        override fun onContainerReleasedFromTop(item: T?, expired: Boolean) {
            verticalCardsActionListener?.onReleasedToActionTop(item, expired)
        }

        override fun onContainerSwipedTop(item: T?, point: Point, expired: Boolean) {
            swipeTop(point, item, silent = false, artificial = false, expired = expired)
        }

        override fun onContainerSwipedBottom(item: T?, point: Point, expired: Boolean) {
            swipeBottom(point, item, silent = false, artificial = false, expired = expired)
        }
    }

    /***
     * This is a listener to handle first time swiping bottom or top
     * and additional method to control if it's first time or not
     */
    var firstTimeEventListener: FirstTimeEventListener<T>? = null

    /***
     * This will return a copy of cards
     */
    val cloneCards: List<T>
        get() = ArrayList(adapter.getItems())


    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    /***
     * Adds a single card to the stack
     * It will automatically handle empty or half stack
     */
    fun addCard(item: T) {
        ensureInitialized()
        adapter.getItems().add(item)
        if (adapter.count == 1) {
            loadTopView()
        } else if (adapter.count == 2) {
            loadBottomView()
        }
    }


    /***
     * Returns the total number of available
     * cards in stack
     */
    fun currentCardCount(): Int {
        ensureInitialized()
        return adapter.count
    }

    /***
     * Adds a collection of cards to the stack
     * It will automatically handle empty or half stack
     */
    fun addCards(items: Collection<T>) {
        ensureInitialized()
        val lastCount = adapter.count
        adapter.getItems().addAll(items)
        // If the stack already had more than visible cards
        // nothing needs to be done
        if (lastCount >= 2) {
            return
        }
        // In case the stack was completely empty
        if (lastCount == 0) {
            // Load top view if the list is not empty
            if (items.isNotEmpty()) {
                loadTopView()
            }
            // Load bottom view if more than 1 card has been added
            if (items.size > 1) {
                loadBottomView()
            }
        } else
        // If there has been already a card
            if (lastCount == 1) {
                // Load bottom view if new collection has atleast one card
                if (items.isNotEmpty()) {
                    loadBottomView()
                }
            }

    }

    /***
     * This method will remove a card with specified index
     * It will set visible card as EXPIRED which make the card to show Expire layout
     *
     * returns true if it was a successful attempt
     * and false if index is out of bound
     * @return Boolean
     */
    fun removeCard(index: Int): Boolean {
        ensureInitialized()
        if (index >= adapter.count) {
            return false
        }
        when (index) {
            0 -> {
                adapter.onViewExpired(adapter.getHolder(containers.first.frameContent.getChildAt(0)), 0)
                containers.first.setExpired()
            }
            1 -> {
                adapter.onViewExpired(adapter.getHolder(containers.last.frameContent.getChildAt(0)), 1)
                containers.last.setExpired()
            }
            else -> adapter.getItems().removeAt(index)
        }
        return true
    }

    /***
     * This method will remove a card
     * It will set visible card as EXPIRED which make the card to show Expire layout
     *
     * The item needs to implement proper equals method in its class
     *
     * returns true if it was a successful attempt
     * and false if index is out of bound
     * @return Boolean
     */
    fun removeCard(item: T): Boolean {
        ensureInitialized()
        adapter.getItems().find {
            it == item
        }?.let {
            return removeCard(adapter.getItems().indexOf(it))
        }
        return false
    }

    /***
     * This method will update a card
     * update process is handled by user with Updater class
     *
     * returns true if the card has already been existed and false if it wasn't
     * it doesn't matter if it has been created or not
     *
     * @param addIfNotExist determines if the card needs to be added if it doesn't exist
     * @param newItem target card
     * @param updater this method will be called if there is already a card which needs to be updated
     * @return Boolean
     */
    fun updateCard(newItem: T, updater: Updater<T>, addIfNotExist: Boolean): Boolean {
        ensureInitialized()
        var index = -1
        val size = adapter.count

        for (i in 0 until size) {
            if (adapter.getItems()[i] == newItem) {
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

        updater.update(adapter.getItem(index), newItem)

        if (index == 0) { // update top card
            adapter.onUpdateViewHolder(adapter.getHolder(containers.first.frameContent.getChildAt(0)), 0)
            containers.first.item = adapter.getItem(index)
        } else if (index == 1) { // update bottom card
            adapter.onUpdateViewHolder(adapter.getHolder(containers.last.frameContent.getChildAt(0)), 1)
            containers.last.item = adapter.getItem(index)
        }

        return true

    }

    private var initialized = false

    private fun ensureInitialized() {
        if (!initialized) {
            throw java.lang.RuntimeException("The method initializeTheOrb should be called first")
        }
    }

    /***
     * This method needs to be called one time
     * it requires an adapter and 3 additional layouts and callbacks
     * top layout is the card at top and bottom layout is the one at bottom
     * expire layout is shown when a card mark as EXPIRED
     * the corresponding callback will be called after inflating the View
     */
    fun initialize(adapter: VerticalCardAdapter<T, VH>
                   , top_layout: Int? = null
                   , topLayoutInitializer: ((View) -> Unit)? = null
                   , bottom_layout: Int? = null
                   , bottomLayoutInitializer: ((View) -> Unit)? = null
                   , expireLayout: Int? = null
                   , expireLayoutInitializer: ((View) -> Unit)? = null): VerticalCardSwipe<T, VH> {
        if (initialized) {
            throw IllegalStateException("You can't call initializeTheOrb more than once")
        }
        initialized = true
        this.adapter = adapter
        this.topLayout = top_layout
        this.bottomLayout = bottom_layout
        this.expireLayout = expireLayout
        this.topLayoutInitializer = topLayoutInitializer
        this.bottomLayoutInitializer = bottomLayoutInitializer
        this.expireLayoutInitializer = expireLayoutInitializer

        init()
        return this
    }

    private fun createContainer(): CardContainer<T> {
        val vcsContainer = CardContainer<T>(context)
        val layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)


        layoutParams.setMargins(Utils.toPx(context, config.cardLeftMarginDP).toInt(), Utils.toPx(context, config.cardTopMarginDP).toInt(), Utils.toPx(context, config.cardRightMarginDP).toInt(), Utils.toPx(context, config.cardBottomMarginDP).toInt())
        vcsContainer.layoutParams = layoutParams
        vcsContainer.radius = Utils.toPx(context, config.cardRadiusDP)
        if (topLayout != null) {
            val tv = LayoutInflater.from(context)
                    .inflate(topLayout!!, vcsContainer.frameOverlayTop, false)
            topLayoutInitializer?.invoke(tv)
            vcsContainer.frameOverlayTop.addView(tv)
        }
        if (bottomLayout != null) {
            val bv = LayoutInflater.from(context)
                    .inflate(bottomLayout!!, vcsContainer.frameOverlayBottom, false)
            bottomLayoutInitializer?.invoke(bv)
            vcsContainer.frameOverlayBottom.addView(bv)
        }
        if (expireLayout != null) {
            val ev = LayoutInflater.from(context)
                    .inflate(expireLayout!!, vcsContainer.frameOverlayExpire, false)
            expireLayoutInitializer?.invoke(ev)
            vcsContainer.frameOverlayExpire.addView(ev)
        }
        return vcsContainer
    }

    private fun init() {
        removeAllViews()
        containers.clear()
        // create top view
        containers.add(createContainer())
        // create bottom view
        containers.add(createContainer())

        // Add cards to layout
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


    private fun update(percentY: Float) {
        var view: CardContainer<*> = containers.last

        var currentScale = 1f - config.scaleDiff
        var nextScale = 1f
        var percent = currentScale + (nextScale - currentScale) * Math.abs(percentY)
        view.scaleX = percent
        view.scaleY = percent

        if (percentY <= 0 && !containers.first.itemConfig.actionTop) {
            view = containers.first
            currentScale = 1f
            nextScale = 1f - config.scaleDiff * 2
            percent = currentScale + (nextScale - currentScale) * Math.abs(percentY)
            view.scaleX = percent
            view.scaleY = percent
        }
        if (percentY >= 0 && !containers.first.itemConfig.actionBottom) {
            view = containers.first
            currentScale = 1f
            nextScale = 1f - config.scaleDiff * 2
            percent = currentScale + (nextScale - currentScale) * Math.abs(percentY)
            view.scaleX = percent
            view.scaleY = percent
        }


    }

    private fun swipeBottom(point: Point, item: T?, silent: Boolean = false, artificial: Boolean = false, expired: Boolean) {
        executePreSwipeTask()
        performSwipeBottom(point, artificial, object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animator: Animator) {
                executePostSwipeTask(point)
                if (!silent) {
                    verticalCardsSwipeListener?.onSwipedBottom(item, expired)
                }

            }
        })
    }

    private fun swipeTop(point: Point, item: T?, silent: Boolean = false, artificial: Boolean = false, expired: Boolean) {
        executePreSwipeTask()
        performSwipeTop(point, artificial, object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animator: Animator) {
                executePostSwipeTask(point)
                if (!silent) {
                    verticalCardsSwipeListener?.onSwipedTop(item, expired)
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

    /***
     * It will swipe the top card to top
     * safe to call if there's no card existed
     * @param silent If silent is true it won't call listener
     */
    fun swipeTop(silent: Boolean) {
        ensureInitialized()
        if (adapter.count == 0) {
            return
        }
        swipeTop(containers.first.getTopSwipeTargetPoint()
                , containers.first.item, silent, artificial = true, expired = containers.first.isExpired())
    }


    /***
     * It will swipe the top card to bottom
     * safe to call if there's no card existed
     * @param silent If silent is true it won't call listener
     */
    fun swipeBottom(silent: Boolean) {
        ensureInitialized()
        if (adapter.count == 0) {
            return
        }
        swipeBottom(containers.first.getBottomSwipeTargetPoint()
                , containers.first.item, silent, artificial = true, expired = containers.first.isExpired())
    }

    private fun performSwipeTop(point: Point, artificial: Boolean, listener: Animator.AnimatorListener) {
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

    private fun performSwipeBottom(point: Point, artificial: Boolean, listener: Animator.AnimatorListener) {
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
        container.itemConfig = itemConfig
        if (adapter.count > 0) {
            container.setDraggable(true)
            container.setContainerEventListener(containerEventListener)
            container.firstTimeEventListener = firstTimeEventListener
            val child = adapter.getView(0,
                    container.frameContent.getChildAt(0), container.frameContent)
            if (container.frameContent.childCount == 0) {
                container.frameContent.addView(child)
            }
            val item = adapter.getItem(0)
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
        container.itemConfig = itemConfig
        if (adapter.count > 1) {
            container.setDraggable(false)
            container.setContainerEventListener(null)
            container.firstTimeEventListener = null
            val child = adapter.getView(1,
                    container.frameContent.getChildAt(0), container.frameContent)
            if (container.frameContent.childCount == 0) {
                container.frameContent.addView(child)
            }
            val item = adapter.getItem(1)
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
        if (adapter.count > 0) {
            containers.first.setDraggable(true)
            containers.first.setContainerEventListener(containerEventListener)
            containers.first.firstTimeEventListener = firstTimeEventListener
        }
    }

    private fun executePostSwipeTask(point: Point) {
        adapter.removeFirstItem()
        reorderForSwipe()
        clear()
        update(0f)
        loadNextView()
    }


    /***
     * Enable or Disable Bottom action feature
     */
    fun enableBottomAction(enable: Boolean): VerticalCardSwipe<T, VH> {
        itemConfig.actionBottom = enable
        return this
    }

    /***
     * Enable or Disable Top action feature
     */
    fun enableTopAction(enable: Boolean): VerticalCardSwipe<T, VH> {
        itemConfig.actionTop = enable
        return this
    }

    /***
     * Updater interface
     * use to handle updating an item
     */
    interface Updater<T> {
        /***
         * Update the old item
         */
        fun update(oldItem: T, newItem: T)
    }

    /***
     * A listener for getting state of the card
     */
    interface VerticalCardsStateListener<T> {
        /***
         * On dragging state
         */
        fun onDragging(percent: Float)

        /***
         * Calls when the card moved back to its origin position
         */
        fun onMovedToOrigin(fromDirection: SwipeDirection)
    }

    /***
     * A listener for swipe events
     */
    interface VerticalCardsSwipeListener<T> {
        /***
         * Calls when a card is swiped top
         */
        fun onSwipedTop(item: T?, expired: Boolean)

        /***
         * Calls when a card is swiped bottom
         */
        fun onSwipedBottom(item: T?, expired: Boolean)

    }

    /***
     * A listener for action events
     */
    interface VerticalCardsActionListener<T> {
        /***
         * Calls when a card is swiped to action bottom
         */
        fun onReleasedToActionBottom(item: T?, expired: Boolean)

        /***
         * Calls when a card is swiped to action top
         */
        fun onReleasedToActionTop(item: T?, expired: Boolean)
    }
}
