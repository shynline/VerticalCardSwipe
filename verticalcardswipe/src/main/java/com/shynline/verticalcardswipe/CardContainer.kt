package com.shynline.verticalcardswipe

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
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

internal class CardContainer<T> : CardView {

    private var containerEventListener: ContainerEventListener<T>? = null
    var firstTimeEventListener: FirstTimeEventListener<T>? = null
    private var viewConfiguration: ViewConfiguration? = null
    private lateinit var config: Config
    var itemConfig = ItemConfig()
    var item: T? = null

    private var topSwipeTargetPoint: Point? = null
    private var bottomSwipeTargetPoint: Point? = null
    private var viewOriginY = 0f
    private var motionOriginX: Float = 0.toFloat()
    private var motionOriginY: Float = 0.toFloat()
    private var intercepted = false
    private var isDragging = false
    private var isDraggable = false
    private var expired = false
    private var bottomLock: Boolean = false
    private var topLock: Boolean = false

    lateinit var frameContent: FrameLayout
    lateinit var frameOverlayTop: FrameLayout
    lateinit var frameOverlayBottom: FrameLayout
    lateinit var frameOverlayExpire: FrameLayout


    private val percentY: Float
        get() {
            var percent = 2f * (translationY - viewOriginY) / height
            if (percent > 1) {
                percent = 1f
            }
            if (percent < -1) {
                percent = -1f
            }
            return percent
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

    internal fun setConfig(config: Config) {
        this.config = config
    }


    private fun init() {
        viewConfiguration = ViewConfiguration.get(context)
        val v = LayoutInflater.from(context)
                .inflate(R.layout.base, this, false)
        addView(v)
        frameContent = v.findViewById(R.id.frame_content)
        frameOverlayTop = v.findViewById(R.id.frame_overlay_top)
        frameOverlayBottom = v.findViewById(R.id.frame_overlay_bottom)
        frameOverlayExpire = v.findViewById(R.id.frame_overlay_expire)
    }

    internal fun setAsAds() {
        frameOverlayBottom.alpha = 0f
        frameOverlayTop.alpha = 0f
        frameOverlayExpire.alpha = 0f
        bottomLock = config.preventSwipeBottomIfAd
        topLock = config.preventSwipeTopIfAd

    }

    internal fun isExpired(): Boolean {
        return expired
    }

    internal fun setExpired() {
        frameOverlayBottom.alpha = 0f
        frameOverlayTop.alpha = 0f
        this.expired = true
        bottomLock = config.preventSwipeBottomIfExpired
        topLock = config.preventSwipeTopIfExpired
        moveToOrigin()
        frameOverlayExpire.animate().alpha(1f)
                .setDuration(300L)
                .setInterpolator(OvershootInterpolator(1.0f))
                .setUpdateListener(null)
                .setListener(null)
                .start()
    }

    internal fun reset() {
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
        if (!isDraggable) {
            return false
        }
        super.dispatchTouchEvent(ev)
        return true
    }

    internal fun getTopSwipeTargetPoint(): Point {
        if (topSwipeTargetPoint == null) {
            return Point(x.toInt(),
                    viewOriginY.toInt() - height)
            // I don't want to initiate here
        }
        return topSwipeTargetPoint!!
    }

    internal fun getBottomSwipeTargetPoint(): Point {
        if (bottomSwipeTargetPoint == null) {
            return Point(x.toInt(),
                    viewOriginY.toInt() + height)
            // I don't want to initiate here
        }
        return bottomSwipeTargetPoint!!
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_UP -> handleActionUp(event)
            MotionEvent.ACTION_MOVE -> handleActionMove(event)
        }
        return true
    }

    internal fun setDraggable(draggable: Boolean) {
        isDraggable = draggable
    }


    private fun handleSwipeToActionUp(swipeDirection: SwipeDirection) {
        if (translationY < config.topDragCallThreshold * height) {
            if (firstTimeEventListener?.isSwipingTopForFirstTime(item) == true) {
                firstTimeEventListener?.swipingTopPaused(item, object : FirstTimeActions {
                    override fun proceed() {
                        moveToOriginOnAction {
                            containerEventListener?.onContainerReleasedFromTop(item, expired)
                        }
                    }

                    override fun cancel() {
                        moveToOrigin()
                        containerEventListener?.onContainerMovedToOrigin(swipeDirection, expired)
                    }
                })
            } else {
                moveToOriginOnAction {
                    containerEventListener?.onContainerReleasedFromTop(item, expired)
                }
            }
        } else {
            moveToOrigin()
            containerEventListener?.onContainerMovedToOrigin(swipeDirection, expired)

        }
    }

    private fun handleSwipeUp(swipeDirection: SwipeDirection, percent: Float) {
        if (abs(percent) > config.swipeThreshold) {
            if (firstTimeEventListener?.isSwipingTopForFirstTime(item) == true) {
                firstTimeEventListener?.swipingTopPaused(item, object : FirstTimeActions {
                    override fun proceed() {
                        frameOverlayTop.alpha = 1f
                        containerEventListener?.onContainerSwipedTop(item, topSwipeTargetPoint!!, expired)
                    }

                    override fun cancel() {
                        moveToOrigin()
                        containerEventListener?.onContainerMovedToOrigin(swipeDirection, expired)
                    }
                })
            } else {
                frameOverlayTop.alpha = 1f
                containerEventListener?.onContainerSwipedTop(item, topSwipeTargetPoint!!, expired)
            }

        } else {
            moveToOrigin()
            containerEventListener?.onContainerMovedToOrigin(swipeDirection, expired)
        }
    }

    private fun handleSwipeToActionBottom(swipeDirection: SwipeDirection) {
        if (translationY > config.bottomDragCallThreshold * height) {
            if (firstTimeEventListener?.isSwipingBottomForFirstTime(item) == true) {
                firstTimeEventListener?.swipingBottomPaused(item, object : FirstTimeActions {
                    override fun proceed() {
                        moveToOriginOnAction {
                            containerEventListener?.onContainerReleasedFromBottom(item, expired)
                        }
                    }

                    override fun cancel() {
                        moveToOrigin()
                        containerEventListener?.onContainerMovedToOrigin(swipeDirection, expired)
                    }
                })
            } else {
                moveToOriginOnAction {
                    containerEventListener?.onContainerReleasedFromBottom(item, expired)
                }

            }
        } else {
            moveToOrigin()
            containerEventListener?.onContainerMovedToOrigin(swipeDirection, expired)
        }
    }

    private fun handleSwipeBottom(swipeDirection: SwipeDirection, percent: Float) {
        if (abs(percent) > config.swipeThreshold) {
            if (firstTimeEventListener?.isSwipingBottomForFirstTime(item) == true) {
                firstTimeEventListener?.swipingBottomPaused(item, object : FirstTimeActions {
                    override fun proceed() {
                        frameOverlayTop.alpha = 1f
                        containerEventListener?.onContainerSwipedBottom(item, bottomSwipeTargetPoint!!, expired)
                    }

                    override fun cancel() {
                        moveToOrigin()
                        containerEventListener?.onContainerMovedToOrigin(swipeDirection, expired)
                    }
                })
            } else {
                frameOverlayTop.alpha = 1f
                containerEventListener?.onContainerSwipedBottom(item, bottomSwipeTargetPoint!!, expired)
            }

        } else {
            moveToOrigin()
            containerEventListener?.onContainerMovedToOrigin(swipeDirection, expired)
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

    private fun handleActionUp(event: MotionEvent) {
        if (isDragging.not())
            return
        isDragging = false
        initializeTargetPoints()

        val motionCurrentY = event.rawY

        val swipeDirection = if (motionCurrentY - motionOriginY > 0)
            SwipeDirection.Bottom
        else
            SwipeDirection.Top

        val percent = percentY
        if (swipeDirection == SwipeDirection.Top) {
            if (itemConfig.actionTop) {
                handleSwipeToActionUp(swipeDirection)
            } else {
                handleSwipeUp(swipeDirection, percent)
            }
        } else {  //swipeDirection != SwipeDirection.Top
            if (itemConfig.actionBottom) {
                handleSwipeToActionBottom(swipeDirection)
            } else {
                handleSwipeBottom(swipeDirection, percent)
            }
        }
    }


    private fun moveToOriginOnAction(callback: () -> Unit) {
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
        animate().scaleX(1f).scaleY(1f)
                .translationY(viewOriginY)
                .setDuration(300L)
                .setInterpolator(OvershootInterpolator(1.0f))
                .setUpdateListener {
                    if (containerEventListener != null) {
                        containerEventListener!!.onContainerDragging(percentY, expired)
                    }
                }
                .setListener(null)
                .start()
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

    private fun handleActionMove(event: MotionEvent) {
        isDragging = true
        updateTranslation(event)
        val py = percentY
        containerEventListener?.onContainerDragging(py, expired)

        if (expired)
            return
        if (py >= 0) {
            frameOverlayBottom.alpha = py * (1 / config!!.bottomDragLimit * config!!.bottomOverlaySoftener)
            frameOverlayTop.alpha = 0f
        } else if (py < 0) {
            frameOverlayBottom.alpha = 0f
            frameOverlayTop.alpha = -py * config!!.topOverLayHardener
        }

    }

    private fun updateTranslation(event: MotionEvent) {
        val diff = event.rawY - motionOriginY
        if (diff > 0 && bottomLock)
            return
        if (diff < 0 && topLock)
            return
        var value = viewOriginY + diff
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

    /**
     * ContainerEventListener
     */
    fun setContainerEventListener(containerEventListener: ContainerEventListener<T>?) {
        this.containerEventListener = containerEventListener
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

        fun onContainerMovedToOrigin(fromDirection: SwipeDirection, expired: Boolean)

        fun onContainerReleasedFromBottom(item: T?, expired: Boolean)
        fun onContainerReleasedFromTop(item: T?, expired: Boolean)

    }

}

