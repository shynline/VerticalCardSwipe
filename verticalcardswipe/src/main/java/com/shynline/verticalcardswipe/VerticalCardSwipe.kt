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
import kotlin.math.abs

/***
 * VerticalCardSwipe
 * a container for having 2 cards with ability of swiping vertically
 */
class VerticalCardSwipe<T, VH : BaseViewHolder> : FrameLayout {

    // Public variables
    // Default configuration instantiation
    val config = Config()

    // State listener: notify changes in state of top card
    var verticalCardsStateListener: VerticalCardsStateListener<T>? = null
    // Action listener: notify when user interact with cards
    var verticalCardsActionListener: VerticalCardsActionListener<T>? = null
    // Swipe listener: notify when card is swiping
    var verticalCardsSwipeListener: VerticalCardsSwipeListener<T>? = null

    // Private variables
    // A linkedList container for storing CardContainer views
    private val containers = LinkedList<CardContainer<T>>()

    // VerticalCardSwipe Adapter needs to be initialized by user | null is not acceptable
    private lateinit var adapter: VerticalCardAdapter<T, VH>

    // Internal animation flag
    private var performSwipeAnimationCallbackUnLock = false

    // Overlay layouts ID | null means there's no overlay
    // Top overlay will be shown when card is swiping top
    private var topLayout: Int? = null
    // Top overlay will be shown when card is swiping bottom
    private var bottomLayout: Int? = null
    // Top overlay will be shown when card is marked as expired ( by user )
    private var expireLayout: Int? = null

    // Lambda functions which run after each overlay layout inflation
    private var topLayoutInitializer: ((View) -> Unit)? = null
    private var bottomLayoutInitializer: ((View) -> Unit)? = null
    private var expireLayoutInitializer: ((View) -> Unit)? = null

    // ItemConfig default Initiation | ItemConfig indicates that the top card
    // is going to swipe or act as an action and in which direction
    private val itemConfig = ItemConfig()

    // Internal flag to track initialization
    private var initialized = false

    private fun ensureInitialized() {
        if (!initialized) {
            throw java.lang.RuntimeException("The method initializeTheOrb should be called first")
        }
    }

    // A card listener which binds to the top card and react on drags, moves and actions
    private val containerEventListener = object : CardContainer.ContainerEventListener<T> {
        // While dragging the percentage of dragging relative to the height of the card will pass
        override fun onContainerDragging(percentY: Float, expired: Boolean) {
            // Position, Animation and, ... will be modified here
            update(percentY)
            // Notify user state listener if exists
            verticalCardsStateListener?.onDragging(percentY)
        }

        // When the card didn't meet dragging threshold and returns to its original position
        override fun onContainerMovedToOrigin(fromDirection: SwipeDirection, item: T?, expired: Boolean) {
            // Notify user state listener if exists
            verticalCardsStateListener?.onMovedToOrigin(fromDirection, item, expired)
        }

        // When the card perform an action because it passed its threshold
        // from bottom and returns to its original position
        override fun onContainerReleasedFromBottom(item: T?, expired: Boolean) {
            // Notify user action listener if exists
            verticalCardsActionListener?.onReleasedToActionBottom(item, expired)
        }

        // When the card perform an action because it passed its threshold
        // from top and returns to its original position
        override fun onContainerReleasedFromTop(item: T?, expired: Boolean) {
            // Notify user action listener if exists
            verticalCardsActionListener?.onReleasedToActionTop(item, expired)
        }

        // When the card passed the designated threshold for swiping from top
        // It calls the parent complete animations and possible notifications
        override fun onContainerSwipedTop(item: T?, point: Point, expired: Boolean) {
            // Calling parent to finalize the card animation
            swipe(point, item, silent = false, artificial = false, expired = expired, direction = SwipeDirection.Top)
        }

        // When the card passed the designated threshold for swiping from bottom
        // It calls the parent complete animations and possible notifications
        override fun onContainerSwipedBottom(item: T?, point: Point, expired: Boolean) {
            // Calling parent to finalize the card animation
            swipe(point, item, silent = false, artificial = false, expired = expired, direction = SwipeDirection.Bottom)
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
        // Simply add the item to adapter
        adapter.getItems().add(item)
        // If we have one item we need to load the top card
        // and if we have 2 item we have to load the bottom card
        // otherwise both cards have been already shown and the new one went to back stack
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
        //  Make sure index is in the bound | return false if not
        if (index >= adapter.count) {
            return false
        }
        when (index) {
            // If the item which is going to be removed is visible we mark it as expired and
            // call the onViewExpired on the adapter for that specific view
            0 -> {
                adapter.onViewExpired(adapter.getHolder(containers.first.frameContent.getChildAt(0)), 0)
                containers.first.expired = true
            }
            1 -> {
                adapter.onViewExpired(adapter.getHolder(containers.last.frameContent.getChildAt(0)), 1)
                containers.last.expired = true
            }
            // If the item is not visible we just remove it, like it didn't exist at all
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
        // Finding the item to be removed ( equals should be implemented for this to work correctly)
        adapter.getItems().find {
            it == item
        }?.let {
            // Remove it by index if such card exists
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

        adapter.getItems().find {
            it == newItem
        }?.let {
            // Getting index of the item
            val index = adapter.getItems().indexOf(it)
            // Notifying user to update
            updater.update(adapter.getItem(index), newItem)

            // In case the item is visible we have to update the object in card container
            // and call the onUpdateViewHolder method on adapter for the specific view
            if (index == 0) { // update top card
                adapter.onUpdateViewHolder(adapter.getHolder(containers.first.frameContent.getChildAt(0)), 0)
                containers.first.item = it
            } else if (index == 1) { // update bottom card
                adapter.onUpdateViewHolder(adapter.getHolder(containers.last.frameContent.getChildAt(0)), 1)
                containers.last.item = it
            }
            return true
        }

        // If the card doesn't exist | return false
        // We add it if user passed true for addIfNotExist flag
        if (addIfNotExist) {
            addCard(newItem)
        }
        return false
    }


    /***
     * This method needs to be called one time
     * it requires an adapter and 3 additional layouts and callbacks
     * top layout is the card at top and bottom layout is the one at bottom
     * expire layout is shown when a card mark as EXPIRED
     * the corresponding callback will be called after inflating the View
     */
    fun initialize(adapter: VerticalCardAdapter<T, VH>
                   , topLayout: Int? = null
                   , topLayoutInitializer: ((View) -> Unit)? = null
                   , bottomLayout: Int? = null
                   , bottomLayoutInitializer: ((View) -> Unit)? = null
                   , expireLayout: Int? = null
                   , expireLayoutInitializer: ((View) -> Unit)? = null): VerticalCardSwipe<T, VH> {
        if (initialized) {
            throw IllegalStateException("You can't call initializeTheOrb more than once")
        }
        // Setting the view as initiated
        initialized = true
        // Assigning adapters, layout resource ids, and callbacks
        this.adapter = adapter
        this.topLayout = topLayout
        this.bottomLayout = bottomLayout
        this.expireLayout = expireLayout
        this.topLayoutInitializer = topLayoutInitializer
        this.bottomLayoutInitializer = bottomLayoutInitializer
        this.expireLayoutInitializer = expireLayoutInitializer

        // Initializing the view
        init()
        return this
    }

    private fun createContainer(): CardContainer<T> {
        // Instantiating a container
        val vcsContainer = CardContainer<T>(context)
        val layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        // Applying configuration
        // Assigning layout params
        layoutParams.setMargins(Utils.toPx(context, config.cardLeftMarginDP).toInt(), Utils.toPx(context, config.cardTopMarginDP).toInt(), Utils.toPx(context, config.cardRightMarginDP).toInt(), Utils.toPx(context, config.cardBottomMarginDP).toInt())
        vcsContainer.layoutParams = layoutParams
        // Assigning radius
        vcsContainer.radius = Utils.toPx(context, config.cardRadiusDP)

        // Inflating and setting overlay if exists | calling corresponding callback for the view
        // if it exists
        topLayout?.let {
            val tv = LayoutInflater.from(context)
                    .inflate(it, vcsContainer.frameOverlayTop, false)
            topLayoutInitializer?.invoke(tv)
            vcsContainer.frameOverlayTop.addView(tv)
        }
        bottomLayout?.let {
            val bv = LayoutInflater.from(context)
                    .inflate(it, vcsContainer.frameOverlayBottom, false)
            bottomLayoutInitializer?.invoke(bv)
            vcsContainer.frameOverlayBottom.addView(bv)
        }
        expireLayout?.let {
            val ev = LayoutInflater.from(context)
                    .inflate(it, vcsContainer.frameOverlayExpire, false)
            expireLayoutInitializer?.invoke(ev)
            vcsContainer.frameOverlayExpire.addView(ev)
        }
        vcsContainer.reset()
        return vcsContainer
    }

    private fun init() {
        // Clear everything
        removeAllViews()
        containers.clear()
        // create top view and add it to containers
        containers.add(createContainer())
        // create bottom view and add it to containers
        containers.add(createContainer())

        // Add cards to layout
        addView(containers.last)
        addView(containers.first)

        // Force Cards to setup their origin position
        containers.first.setViewOriginY()
        containers.last.setViewOriginY()

        // Pass on configuration to cards
        containers.first.config = config
        containers.last.config = config

        // Removing all listeners ( not necessary! )
        containers.last.containerEventListener = null
        containers.first.containerEventListener = null
        containers.last.firstTimeEventListener = null
        containers.first.firstTimeEventListener = null

        // Set their visibility to Gone (There is no items in the adapter)
        containers.first.visibility = View.GONE
        containers.last.visibility = View.GONE
        // make sure position is set zero state
        update(0f)
    }


    private fun update(percentY: Float) {
        // Getting the bottom cardContainer
        var view: CardContainer<*> = containers.last

        // Calculating bottom card container scale value base on percentY
        var firstScale = 1f - config.lowerCardScaleDiff
        var secondScale = 1f
        var scale = firstScale + (secondScale - firstScale) * abs(percentY)
        view.scaleX = scale
        view.scaleY = scale

        // If card is scrolling to top and actionTop is activated no need to resize
        if (percentY <= 0 && containers.first.itemConfig.actionTop) {
            return
        }
        // If card is scrolling to bottom and actionBottom is activated no need to resize
        if (percentY >= 0 && containers.first.itemConfig.actionBottom) {
            return
        }

        // Getting the top cardContainer
        view = containers.first
        // Calculating top card container scale value base on percentY
        firstScale = 1f
        secondScale = 1f - config.upperCardScaleDiff
        scale = firstScale + (secondScale - firstScale) * abs(percentY)
        view.scaleX = scale
        view.scaleY = scale
    }


    private fun swipe(point: Point, item: T?, silent: Boolean = false, artificial: Boolean = false, expired: Boolean, direction: SwipeDirection) {
        // Pre swipe tasks
        executePreSwipeTask()
        // Swiping
        performSwipe(point, artificial, object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animator: Animator) {
                // Post swipe tasks
                executePostSwipeTask()
                // Notify Swipe listener if the call is not silent and the listener is not null
                if (!silent) {
                    if (direction == SwipeDirection.Bottom) {
                        verticalCardsSwipeListener?.onSwipedBottom(item, expired)
                    } else {
                        verticalCardsSwipeListener?.onSwipedTop(item, expired)
                    }
                }

            }
        })
    }

    private fun executePreSwipeTask() {
        // Remove all listeners and make Cards not draggable
        containers.first.containerEventListener = null
        containers.first.firstTimeEventListener = null
        containers.first.isDraggable = false
        containers.last.containerEventListener = null
        containers.last.firstTimeEventListener = null
        containers.last.isDraggable = false
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
        containers.first.getTopSwipeTargetPoint()?.let {
            swipe(
                    point = it,
                    item = containers.first.item,
                    silent = silent,
                    artificial = true,
                    expired = containers.first.expired,
                    direction = SwipeDirection.Top
            )
        }
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
        containers.first.getBottomSwipeTargetPoint()?.let {
            swipe(
                    point = it,
                    item = containers.first.item,
                    silent = silent,
                    artificial = true,
                    expired = containers.first.expired,
                    direction = SwipeDirection.Bottom
            )
        }
    }

    private fun performSwipe(point: Point, artificial: Boolean, listener: Animator.AnimatorListener) {
        // Animate both cards to complete the swipe
        // TODO: needs a better approach
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

        val animator = containers.first.animate()
        if (artificial) {
            animator.scaleX(0.5f).scaleY(0.5f)
        }
        animator.translationY(point.y.toFloat())
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
        // Bring the card to behind
        val parent = container.parent as VerticalCardSwipe<*, *>
        parent.removeView(container)
        parent.addView(container, 0)
    }

    private fun reorderForSwipe() {
        // Swap CardContainer views
        // Change in parent
        moveToBottom(containers.first)
        // Change in back stack
        containers.addLast(containers.removeFirst())
    }

    private fun resetScaleAndTranslationOfBottomContainer() {
        val view = containers.last
        view.translationX = 0f
        view.translationY = 0f
        view.scaleX = 1f
        view.scaleY = 1f
    }

    private fun loadTopView() {
        // Get the top container
        val container = containers.first
        // Reset the container's state
        container.reset()
        // Assigning item configuration
        container.itemConfig = itemConfig
        if (adapter.count > 0) {
            // There is at least one item available
            // make it draggable and setup the listeners
            container.isDraggable = true
            container.containerEventListener = containerEventListener
            container.firstTimeEventListener = firstTimeEventListener

            // Getting view from adapter if it doesn't exist the adapter will create one
            val child = adapter.getView(0,
                    container.frameContent.getChildAt(0),
                    container.frameContent)
            // Adding the view to container hierarchy if it doesn't exist
            if (container.frameContent.childCount == 0) {
                container.frameContent.addView(child)
            }
            // Get the first item in adapter
            val item = adapter.getItem(0)
            // If the item implements Ads interface
            // Set the container as ad if the item is marked as ad
            if (item is Ads) {
                if (item.isCurrentItemAd()) {
                    container.setAsAds()
                }
            }
            // Set the item
            container.item = item
            // Make container visible
            container.setVisibility(View.VISIBLE)
        } else {
            // If adapter is empty make the container not draggable, remove the listeners and make the visibility Gone
            container.isDraggable = false
            container.containerEventListener = null
            container.firstTimeEventListener = null
            container.setVisibility(View.GONE)
        }
    }

    private fun loadBottomView() {
        // Get the bottom container
        val container = containers.last
        // Reset the container's state
        container.reset()
        // Assigning item configuration
        container.itemConfig = itemConfig
        if (adapter.count > 1) {
            // There is at least two item available
            // make it draggable and setup the listeners
            container.isDraggable = false
            container.containerEventListener = null
            container.firstTimeEventListener = null
            // Getting view from adapter if it doesn't exist the adapter will create one
            val child = adapter.getView(1,
                    container.frameContent.getChildAt(0), container.frameContent)
            // Adding the view to container hierarchy if it doesn't exist
            if (container.frameContent.childCount == 0) {
                container.frameContent.addView(child)
            }
            // Get the second item in adapter
            val item = adapter.getItem(1)
            // If the item implements Ads interface
            // Set the container as ad if the item is marked as ad
            if (item is Ads) {
                if (item.isCurrentItemAd()) {
                    container.setAsAds()
                }
            }
            // Set the item
            container.item = item
            // Make container visible
            container.setVisibility(View.VISIBLE)
        } else {
            // If adapter is empty make the container not draggable, remove the listeners and make the visibility Gone
            container.isDraggable = false
            container.containerEventListener = null
            container.firstTimeEventListener = null
            container.setVisibility(View.GONE)
        }
    }

    private fun loadNextView() {
        // Load the next view which is bottom view
        loadBottomView()
        // If the adapter is not empty set the top view draggable and setup the internal listeners
        if (adapter.count > 0) {
            containers.first.isDraggable = true
            containers.first.containerEventListener = containerEventListener
            containers.first.firstTimeEventListener = firstTimeEventListener
        }
    }

    private fun executePostSwipeTask() {
        // Removing the swiped item
        adapter.removeFirstItem()
        // Swap Cards
        reorderForSwipe()
        // Reset the swiped card to its original position
        resetScaleAndTranslationOfBottomContainer()
        // Reset physical state of cards
        update(0f)
        // Load the next view to bottom card
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
        fun onMovedToOrigin(fromDirection: SwipeDirection, item: T?, expired: Boolean)
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
