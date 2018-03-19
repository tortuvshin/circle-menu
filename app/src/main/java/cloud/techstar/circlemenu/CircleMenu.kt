package cloud.techstar.circlemenu

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.support.v4.view.GestureDetectorCompat
import android.support.v4.view.MotionEventCompat
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.Scroller

class CircleMenu : ViewGroup {
    private var childViewsList: List<View>? = null
    private var downX: Float = 0.toFloat()
    private var downY: Float = 0.toFloat()
    private var offsetAngle: Float = 0.toFloat()
    private var preOffsetAngle: Float = 0.toFloat()
    private var lastAngle: Float = 0.toFloat()
    private var firstView: View? = null
    private var detectorCompat: GestureDetectorCompat? = null
    private var mScroller: Scroller? = null
    private var flingAngle: Float = 0.toFloat()
    private var flingClockWise: Boolean = false

    constructor(context: Context) : super(context) {
        initData()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initData()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initData()
    }

    fun setChildViewsList(childViewsList: List<View>) {
        this.childViewsList = childViewsList
        for (view in this.childViewsList!!) {
            addView(view)
        }

        firstView = getChildAt(0)
        requestLayout()
    }

    private fun initData() {
        val gestureListener = FlingListener()
        detectorCompat = GestureDetectorCompat(context, gestureListener)
        mScroller = Scroller(context)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        measureChildren(widthMeasureSpec, heightMeasureSpec)

        val minHeight = 100
        val minWidth = 100

        setMeasuredDimension(View.resolveSize(minWidth, widthMeasureSpec), View.resolveSize(minHeight,
                heightMeasureSpec))
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val count = childCount
        val eachAngle = (2 * Math.PI / count).toFloat()
        for (i in 0 until count) {
            val thisView = getChildAt(i)
            val thisAngle = i * eachAngle + offsetAngle + lastAngle + flingAngle

            val anchor = if (thisView.measuredWidth > thisView.measuredHeight)
                thisView.measuredWidth
            else
                thisView.measuredHeight

            val x = (Math.cos(thisAngle.toDouble()) * ((measuredWidth - anchor) / 2.0f)).toFloat() + measuredWidth / 2.0f - thisView.measuredWidth / 2
            val y = (measuredHeight / 2.0f
                    - (Math.sin(thisAngle.toDouble()) * ((measuredHeight - anchor) / 2.0f)).toFloat()
                    - (thisView.measuredHeight / 2).toFloat())

            thisView.layout(x.toInt(), y.toInt(), x.toInt() + thisView.measuredWidth, y.toInt() + measuredHeight)
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        mScroller!!.forceFinished(true)
        val type = MotionEventCompat.getActionMasked(ev)
        val currentX = MotionEventCompat.getX(ev, 0)
        val currentY = MotionEventCompat.getY(ev, 0)

        when (type) {
            MotionEvent.ACTION_DOWN -> {
                downX = currentX
                downY = currentY

                val x = firstView!!.x + firstView!!.measuredWidth / 2.0f
                val y = firstView!!.y + firstView!!.measuredHeight / 2.0f

                lastAngle = getRelativeAngle(x, y).toFloat()
            }
            MotionEvent.ACTION_MOVE -> {
                val distance = getDistance(downX, downY, currentX, currentY)
                if (distance > ViewConfiguration.getTouchSlop()) {
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
            }
        }

        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        flingAngle = 0f

        val revl = detectorCompat!!.onTouchEvent(event)
        if (revl) {
            return revl
        }

        val type = MotionEventCompat.getActionMasked(event)
        val currentX = MotionEventCompat.getX(event, 0)
        val currentY = MotionEventCompat.getY(event, 0)

        val deltaAngle = getRelativeAngle(currentX, currentY) - getRelativeAngle(downX, downY)
        preOffsetAngle = offsetAngle
        offsetAngle = deltaAngle.toFloat()

        when (type) {
            MotionEvent.ACTION_MOVE -> requestLayout()
        }

        return true
    }

    private fun getRelativeAngle(x: Float, y: Float): Double {
        val mCos = (x - measuredWidth / 2.0f) / getDistance(x, y, measuredWidth / 2.0f,
                measuredHeight / 2.0f)
        var angleR = Math.acos(mCos.toDouble())
        //        double angleD = Math.toDegrees(angleR);
        if (y > measuredHeight / 2.0f) {
            angleR = 2 * Math.PI - angleR
        }
        return angleR
    }

    private fun getDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val distance = Math.hypot((x1 - x2).toDouble(), (y1 - y2).toDouble())
        return distance.toFloat()
    }

    internal inner class FlingListener : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            flingClockWise = offsetAngle <= preOffsetAngle
            mScroller!!.fling(0, 0, velocityX.toInt(), velocityY.toInt(), -10000, 10000, -10000, 10000)
            requestLayout()
            return true
        }
    }

    override fun computeScroll() {
        if (mScroller!!.computeScrollOffset()) {
            val x = mScroller!!.currX
            val y = mScroller!!.currY
            val distance = getDistance(x.toFloat(), y.toFloat(), 0f, 0f)

            val radius = measuredWidth / 2.0f
            var radian = distance / radius
            if (flingClockWise) {
                radian = -radian
            }
            flingAngle = radian
            requestLayout()
        }
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        val ss = SavedState(superState)
        ss.stateToSave = lastAngle + offsetAngle
        return ss
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        super.onRestoreInstanceState(state.superState)

        lastAngle = state.stateToSave
    }

    internal class SavedState : View.BaseSavedState {
        var stateToSave: Float = 0.toFloat()

        constructor(superState: Parcelable) : super(superState) {}

        private constructor(`in`: Parcel) : super(`in`) {
            this.stateToSave = `in`.readFloat()
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeFloat(this.stateToSave)
        }

        companion object {

            //required field that makes Parcelables from a Parcel
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(`in`: Parcel): SavedState {
                    return SavedState(`in`)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }
}