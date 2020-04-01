package com.shynline.verticalcardswipe

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import androidx.cardview.widget.CardView
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

internal class CardContainer<T> : CardView {

    // Public fields set by parent
    // first time event listener
    var firstTimeEventListener: FirstTimeEventListener<T>? = null
    // item configuration
    var itemConfig = ItemConfig()
    // current item
    var item: T? = null
    // event listener
    var containerEventListener: ContainerEventListener<T>? = null
    // none null configuration object should be assign by parent
    var config = Config()
    // Determine if this card is draggable or not
    var isDraggable = false

    // Private fields
    // view configurations params
    private var viewConfiguration: ViewConfiguration? = null
    // swipe target points
    private var topSwipeTargetPoint: Point? = null
    private var bottomSwipeTargetPoint: Point? = null
    // original Y position for this card
    private var viewOriginY = 0f
    // Touch control variables
    private var motionOriginX: Float = 0.toFloat()
    private var motionOriginY: Float = 0.toFloat()
    private var intercepted = false
    private var isDragging = false

    // expired flag
    var expired = false
        set(value) {
            field = value
            if (value) {
                // Hide the top and bottom overlay
                // TODO: Animate hiding overlays
                frameOverlayBottom.alpha = 0f
                frameOverlayTop.alpha = 0f
                // Lock the card if it's necessary
                bottomLock = config.preventSwipeBottomIfExpired
                topLock = config.preventSwipeTopIfExpired
                // Move the Card to its original position
                moveToOrigin()
                // Animate the expire viewGroup to be visible
                frameOverlayExpire.animate().alpha(1f)
                        .setDuration(300L)
                        .setInterpolator(OvershootInterpolator(1.0f))
                        .setUpdateListener(null)
                        .setListener(null)
                        .start()
            }
        }

    // Swipe lock flags to constraint the card
    private var bottomLock: Boolean = false
    private var topLock: Boolean = false


    // ViewGroups
    lateinit var frameContent: FrameLayout
    lateinit var frameOverlayTop: FrameLayout
    lateinit var frameOverlayBottom: FrameLayout
    lateinit var frameOverlayExpire: FrameLayout


    private val percentY: Float
        get() {
            // Calculate the Y-Axis movement percentage of the card
            val py = max(-1f, min(1f, 2f * (translationY - viewOriginY) / height))
            return if (py.isNaN()) 0f else py
        }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }


    private fun init() {
        // Initializing the card
        viewConfiguration = ViewConfiguration.get(context)
        // Inflating base layout
        val v = LayoutInflater.from(context)
                .inflate(R.layout.base, this, false)
        // Add the inflated view to view hierarchy
        addView(v)
        // Assignment of content viewGroup and its overlays
        frameContent = v.findViewById(R.id.frame_content)
        frameOverlayTop = v.findViewById(R.id.frame_overlay_top)
        frameOverlayBottom = v.findViewById(R.id.frame_overlay_bottom)
        frameOverlayExpire = v.findViewById(R.id.frame_overlay_expire)
    }

    internal fun setAsAds() {
        // Hide the overlays
        frameOverlayBottom.alpha = 0f
        frameOverlayTop.alpha = 0f
        frameOverlayExpire.alpha = 0f
        // Lock the swiping functionality of the card
        // It can be locked both way
        bottomLock = config.preventSwipeBottomIfAd
        topLock = config.preventSwipeTopIfAd

    }

    internal fun reset() {
        // Reset all variable to its default state
        itemConfig = ItemConfig()
        bottomLock = false
        topLock = false
        expired = false
        frameOverlayBottom.alpha = 0f
        frameOverlayTop.alpha = 0f
        frameOverlayExpire.alpha = 0f
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            // reset everything
            motionOriginX = ev.rawX
            motionOriginY = ev.rawY
            intercepted = false
        } else if (ev.actionMasked == MotionEvent.ACTION_MOVE) {
            // If the touch event is Vertical, we'll intercept it
            if (abs(ev.rawX - motionOriginX) < viewConfiguration!!.scaledPagingTouchSlop) {
                if (abs(ev.rawY - motionOriginY) > viewConfiguration!!.scaledTouchSlop) {
                    intercepted = true
                }
            }
        }
        super.onInterceptTouchEvent(ev)
        return intercepted
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // Control dispatching the touch events
        // We don't let the card dispatch events if it's middle of parent animation
        if (!isDraggable) {
            return false
        }
        super.dispatchTouchEvent(ev)
        return true
    }

    internal fun getTopSwipeTargetPoint(): Point? {
        initializeTargetPoints()
        return topSwipeTargetPoint
    }

    internal fun getBottomSwipeTargetPoint(): Point? {
        initializeTargetPoints()
        return bottomSwipeTargetPoint
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_UP -> handleActionUp(event)
            MotionEvent.ACTION_MOVE -> handleActionMove(event)
        }
        return true
    }

    private fun handleActionMove(event: MotionEvent) {
        isDragging = true
        // update translationY of this card
        updateTranslation(event)

        val py = percentY
        // Notifying parent
        containerEventListener?.onContainerDragging(py, expired)

        // If the item is expired e.g: being removed
        if (expired)
            return
        if ((item as Ads).isCurrentItemAd()){
            return
        }
        // Handling Overlays
        if (py >= 0) {
            frameOverlayBottom.alpha = if (itemConfig.actionBottom) {
                py * (1 /config.bottomDragLimit * config.bottomOverlaySoftener)
            }else{
                py * (1 / config.bottomOverlaySoftener)
            }
            frameOverlayTop.alpha = 0f
        } else if (py < 0) {
            frameOverlayBottom.alpha = 0f
            frameOverlayTop.alpha = if (itemConfig.actionTop){
                -py * (1 / config.topDragLimit *  config.topOverlaySoftener)
            }else{
                -py * (1 / config.topOverlaySoftener)
            }
        }

    }


    private fun handleActionUp(event: MotionEvent) {
        // If is not dragging no need to do anything
        if (isDragging.not())
            return
        isDragging = false
        initializeTargetPoints()

        val motionCurrentY = event.rawY

        // Determine the direction
        val swipeDirection = if (motionCurrentY - motionOriginY > 0)
            SwipeDirection.Bottom
        else
            SwipeDirection.Top

        val py = percentY
        if (swipeDirection == SwipeDirection.Top) {
            if (itemConfig.actionTop) {
                // Top action case
                handleSwipeToActionUp(swipeDirection)
            } else {
                // Swipe top case
                handleSwipeUp(swipeDirection, py)
            }
        } else {  //swipeDirection != SwipeDirection.Top
            if (itemConfig.actionBottom) {
                // Bottom action case
                handleSwipeToActionBottom(swipeDirection)
            } else {
                // Swipe bottom case
                handleSwipeBottom(swipeDirection, py)
            }
        }
    }

    private fun handleSwipeToActionUp(swipeDirection: SwipeDirection) {

        if (translationY.absoluteValue > config.topDragCallThreshold * height) {
            // Swipe threshold has been satisfied
            if (firstTimeEventListener?.isSwipingTopForFirstTime(item) == true) {
                // It's first time we pause the view until user respond
                firstTimeEventListener?.swipingTopPaused(item, object : FirstTimeActions {
                    override fun proceed() {
                        // Proceed the action by moving to original position
                        // and notifying parent
                        moveToOriginOnAction {
                            containerEventListener?.onContainerReleasedFromTop(item, expired)
                        }
                    }

                    override fun cancel() {
                        // user requested revert the action
                        // return to original position and notify parent
                        moveToOrigin()
                        containerEventListener?.onContainerMovedToOrigin(swipeDirection, item, expired)
                    }
                })
            } else {
                // It's not first time
                // So move the card to original position and notify the parent
                moveToOriginOnAction {
                    containerEventListener?.onContainerReleasedFromTop(item, expired)
                }
            }
        } else {
            // If the swipe threshold is not satisfied
            // return to original position and notify parent
            moveToOrigin()
            containerEventListener?.onContainerMovedToOrigin(swipeDirection, item, expired)

        }
    }

    private fun handleSwipeToActionBottom(swipeDirection: SwipeDirection) {
        if (translationY.absoluteValue > config.bottomDragCallThreshold * height) {
            // Swipe threshold has been satisfied
            if (firstTimeEventListener?.isSwipingBottomForFirstTime(item) == true) {
                // It's first time we pause the view until user respond
                firstTimeEventListener?.swipingBottomPaused(item, object : FirstTimeActions {
                    // Proceed the action by moving to original position
                    // and notifying parent
                    override fun proceed() {
                        moveToOriginOnAction {
                            containerEventListener?.onContainerReleasedFromBottom(item, expired)
                        }
                    }

                    // user requested revert the action
                    // return to original position and notify parent
                    override fun cancel() {
                        moveToOrigin()
                        containerEventListener?.onContainerMovedToOrigin(swipeDirection, item, expired)
                    }
                })
            } else {
                // It's not first time
                // So move the card to original position and notify the parent
                moveToOriginOnAction {
                    containerEventListener?.onContainerReleasedFromBottom(item, expired)
                }

            }
        } else {
            // If the swipe threshold is not satisfied
            // return to original position and notify parent
            moveToOrigin()
            containerEventListener?.onContainerMovedToOrigin(swipeDirection, item, expired)
        }
    }

    private fun handleSwipeUp(swipeDirection: SwipeDirection, percent: Float) {
        if (abs(percent) > config.swipeThreshold) {
            // Swipe condition has been satisfied
            if (firstTimeEventListener?.isSwipingTopForFirstTime(item) == true) {
                // First time swipe
                firstTimeEventListener?.swipingTopPaused(item, object : FirstTimeActions {
                    // User want to proceed | make overlay visible and notify the parent
                    override fun proceed() {
                        frameOverlayTop.alpha = 1f
                        containerEventListener?.onContainerSwipedTop(item, topSwipeTargetPoint!!, expired)
                    }

                    // User want to undo the swipe
                    // return to origin and call the parent
                    override fun cancel() {
                        moveToOrigin()
                        containerEventListener?.onContainerMovedToOrigin(swipeDirection, item, expired)
                    }
                })
            } else {
                // not first time | make overlay visible and notify the parent
                frameOverlayTop.alpha = 1f
                containerEventListener?.onContainerSwipedTop(item, topSwipeTargetPoint!!, expired)
            }

        } else {
            // Swipe threshold has not been satisfied
            // return to origin and notify the parent
            moveToOrigin()
            containerEventListener?.onContainerMovedToOrigin(swipeDirection, item, expired)
        }
    }



    private fun handleSwipeBottom(swipeDirection: SwipeDirection, percent: Float) {
        if (abs(percent) > config.swipeThreshold) {
            // Swipe condition has been satisfied
            if (firstTimeEventListener?.isSwipingBottomForFirstTime(item) == true) {
                // First time swipe
                firstTimeEventListener?.swipingBottomPaused(item, object : FirstTimeActions {
                    // User want to proceed | make overlay visible and notify the parent
                    override fun proceed() {
                        frameOverlayTop.alpha = 1f
                        containerEventListener?.onContainerSwipedBottom(item, bottomSwipeTargetPoint!!, expired)
                    }

                    // User want to undo the swipe
                    // return to origin and call the parent
                    override fun cancel() {
                        moveToOrigin()
                        containerEventListener?.onContainerMovedToOrigin(swipeDirection, item, expired)
                    }
                })
            } else {
                // not first time | make overlay visible and notify the parent
                frameOverlayTop.alpha = 1f
                containerEventListener?.onContainerSwipedBottom(item, bottomSwipeTargetPoint!!, expired)
            }

        } else {
            // Swipe threshold has not been satisfied
            // return to origin and notify the parent
            moveToOrigin()
            containerEventListener?.onContainerMovedToOrigin(swipeDirection, item, expired)
        }
    }

    private fun initializeTargetPoints() {
        if (topSwipeTargetPoint == null) {
            topSwipeTargetPoint = Point(x.toInt(), viewOriginY.toInt() - height)
        }
        if (bottomSwipeTargetPoint == null) {
            bottomSwipeTargetPoint = Point(x.toInt(), viewOriginY.toInt() + height)
        }
    }


    /***
     * This method animates the card to its original position and calls the callback on finish
     */
    private fun moveToOriginOnAction(callback: () -> Unit) {
        // Animating the card
        animate().scaleX(1f).scaleY(1f)
                .translationY(viewOriginY)
                .setDuration(300L)
                .setInterpolator(OvershootInterpolator(1.0f))
                .setUpdateListener(null)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        callback()
                    }
                })
                .start()
        // Animating overlays
        frameOverlayTop.animate().alpha(0f)
                .setDuration(300L)
                .setInterpolator(OvershootInterpolator(1.0f))
                .setUpdateListener(null)
                .setListener(null)
                .start()
        frameOverlayBottom.animate().alpha(0f)
                .setInterpolator(OvershootInterpolator(1.0f))
                .setDuration(300L)
                .setUpdateListener(null)
                .setListener(null)
                .start()
    }

    private fun moveToOrigin() {
        // return to origin and call parent on update
        animate().scaleX(1f).scaleY(1f)
                .translationY(viewOriginY)
                .setDuration(300L)
                .setInterpolator(OvershootInterpolator(1.0f))
                .setUpdateListener {
                    containerEventListener?.onContainerDragging(percentY, expired)
                }
                .setListener(null)
                .start()
        // Animating Overlay
        frameOverlayTop.animate().alpha(0f)
                .setDuration(300L)
                .setInterpolator(OvershootInterpolator(1.0f))
                .setUpdateListener(null)
                .setListener(null)
                .start()
        frameOverlayBottom.animate().alpha(0f)
                .setDuration(300L)
                .setInterpolator(OvershootInterpolator(1.0f))
                .setUpdateListener(null)
                .setListener(null)
                .start()

    }


    private fun updateTranslation(event: MotionEvent) {
        // calculating the drift
        val diff = event.rawY - motionOriginY
        // cases where the card can not swipe because of lock
        if (diff > 0 && bottomLock)
            return
        if (diff < 0 && topLock)
            return
        // calculated translation which is sum of diff and
        var value = viewOriginY + diff
        // If the card suppose to be clamped
        if (itemConfig.actionBottom) {
            if (value > height * config.bottomDragLimit) {
                value = height * config.bottomDragLimit
            }
        }
        if (itemConfig.actionTop) {
            if (value < -height * config.topDragLimit) {
                value = -height * config.topDragLimit
            }
        }
        translationY = value
    }


    internal fun setViewOriginY() {
        viewOriginY = translationY
    }

    /***
     * Internal interface
     * ContainerEventListener
     */
    internal interface ContainerEventListener<T> {

        fun onContainerDragging(percentY: Float, expired: Boolean)

        fun onContainerSwipedTop(item: T?, point: Point, expired: Boolean)

        fun onContainerSwipedBottom(item: T?, point: Point, expired: Boolean)

        fun onContainerMovedToOrigin(fromDirection: SwipeDirection, item: T?, expired: Boolean)

        fun onContainerReleasedFromBottom(item: T?, expired: Boolean)

        fun onContainerReleasedFromTop(item: T?, expired: Boolean)
    }

}

