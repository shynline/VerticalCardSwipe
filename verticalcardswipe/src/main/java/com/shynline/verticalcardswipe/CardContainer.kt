package com.shynline.verticalcardswipe

import android.animation.Animator
import android.content.Context
import android.graphics.Point
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import androidx.cardview.widget.CardView

internal class CardContainer<T> : CardView {

    private var containerEventListener: ContainerEventListener<T>? = null
    var firstTimeEventListener: FirstTimeEventListener<T>? = null
    private var viewConfiguration: ViewConfiguration? = null
    private var config: Config? = null
    var item: T? = null

    private var topSwipeTargetPoint: Point? = null
    private var viewOriginY = 0f
    private var motionOriginX: Float = 0.toFloat()
    private var motionOriginY: Float = 0.toFloat()
    private var intercepted = false
    private var isDragging = false
    private var isDraggable = false
    private var expired = false
    private var bottomLock: Boolean = false


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

    fun setConfig(config: Config) {
        this.config = config
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


    lateinit var frameContent: FrameLayout
    lateinit var frameOverlayTop: FrameLayout
    lateinit var frameOverlayBottom: FrameLayout
    lateinit var frameOverlayExpire: FrameLayout

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

    fun setAsAds() {
        frameOverlayBottom.alpha = 0f
        frameOverlayTop.alpha = 0f
        frameOverlayExpire.alpha = 0f
        bottomLock = true
    }

    fun setExpired() {
        frameOverlayBottom.alpha = 0f
        frameOverlayTop.alpha = 0f
        this.expired = true
        bottomLock = true
        moveToOrigin()
        frameOverlayExpire.animate().alpha(1f)
                .setDuration(300L)
                .setInterpolator(OvershootInterpolator(1.0f))
                .setUpdateListener(null)
                .setListener(null)
                .start()
    }

    fun reset() {
        bottomLock = false
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
            if (Math.abs(ev.rawX - motionOriginX) < viewConfiguration!!.scaledPagingTouchSlop) {
                if (Math.abs(ev.rawY - motionOriginY) > viewConfiguration!!.scaledTouchSlop) {
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

    fun getTopSwipeTargetPoint(): Point {
        if (topSwipeTargetPoint == null) {
            return Point(x.toInt(),
                    viewOriginY.toInt() - height)
            // I don't want to initiate here
        }
        return topSwipeTargetPoint!!
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_UP -> handleActionUp(event)
            MotionEvent.ACTION_MOVE -> handleActionMove(event)
        }
        return true
    }

    fun setDraggable(draggable: Boolean) {
        isDraggable = draggable
    }

    private fun handleActionUp(event: MotionEvent) {
        if (isDragging) {
            isDragging = false


            val motionCurrentY = event.rawY


            if (topSwipeTargetPoint == null) {
                topSwipeTargetPoint = Point(x.toInt(),
                        viewOriginY.toInt() - height)
            }

            val swipeDirection = if (motionCurrentY - motionOriginY > 0)
                SwipeDirection.Bottom
            else
                SwipeDirection.Top

            val percent = percentY
            if (swipeDirection == SwipeDirection.Top) {
                if (Math.abs(percent) > config!!.swipeThreshold) {
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
            } else {
                if (translationY > config!!.bottomDragCallThreshold * height) {
                    if (firstTimeEventListener?.isSwipingBottomForFirstTime(item) == true) {
                        firstTimeEventListener?.swipingBottomPaused(item, object : FirstTimeActions {
                            override fun proceed() {
                                moveToOriginOnAction()

                            }

                            override fun cancel() {
                                moveToOrigin()
                                containerEventListener?.onContainerMovedToOrigin(swipeDirection, expired)
                            }
                        })
                    } else {
                        moveToOriginOnAction()

                    }
                } else {
                    moveToOrigin()
                    containerEventListener?.onContainerMovedToOrigin(swipeDirection, expired)

                }

            }


        }
    }

    private fun moveToOriginOnAction() {
        animate().scaleX(1f).scaleY(1f)
                .translationY(viewOriginY)
                .setDuration(300L)
                .setInterpolator(OvershootInterpolator(1.0f))
                .setUpdateListener(null)
                .setListener(object : Animator.AnimatorListener {
                    override fun onAnimationRepeat(animation: Animator?) {
                    }

                    override fun onAnimationCancel(animation: Animator?) {
                    }

                    override fun onAnimationStart(animation: Animator?) {
                    }

                    override fun onAnimationEnd(animation: Animator?) {
                        if (containerEventListener != null) {
                            containerEventListener!!.onContainerReleasedFromBottom(item, expired)
                        }
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
        if (containerEventListener != null) {
            containerEventListener!!.onContainerDragging(py, expired)
        }
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
        var value = viewOriginY + diff
        if (value > height * config!!.bottomDragLimit) {
            value = height * config!!.bottomDragLimit
        }
        translationY = value
    }

    fun setContainerEventListener(containerEventListener: ContainerEventListener<T>?) {
        this.containerEventListener = containerEventListener
    }


    fun setViewOriginY() {
        viewOriginY = translationY
    }

    interface ContainerEventListener<T> {
        fun onContainerDragging(percentY: Float, expired: Boolean)

        fun onContainerSwipedTop(item: T?, point: Point, expired: Boolean)

        fun onContainerMovedToOrigin(fromDirection: SwipeDirection, expired: Boolean)

        fun onContainerReleasedFromBottom(item: T?, expired: Boolean)

    }

}

interface FirstTimeEventListener<T> {
    fun isSwipingTopForFirstTime(item: T?): Boolean

    fun isSwipingBottomForFirstTime(item: T?): Boolean

    fun swipingTopPaused(item: T?, callback: FirstTimeActions)

    fun swipingBottomPaused(item: T?, callback: FirstTimeActions)

}

interface FirstTimeActions {
    fun proceed()

    fun cancel()
}
