package com.example.android.camera2basic

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.Log
import android.util.Size
import android.view.SurfaceView
import android.widget.FrameLayout

class AspectFrameLayout constructor(context: Context,
                                    attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    private var mRatioWidth:Int = 0
    private var mRatioHeight:Int = 0

    public fun setAspectRatio(size: Size) {
        if (size.width < 0 || size.height < 0) {
            throw IllegalArgumentException("Size cannot be negative.");
        }
        mRatioWidth = size.width.coerceAtMost(size.height)
        mRatioHeight = size.width.coerceAtLeast(size.height)
        post { requestLayout() }

    }

    protected override fun onMeasure(widthMeasureSpec:Int, heightMeasureSpec:Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        var width = MeasureSpec.getSize(widthMeasureSpec);
        var height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height)
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth)
            } else {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height)
            }
        }
    }
}