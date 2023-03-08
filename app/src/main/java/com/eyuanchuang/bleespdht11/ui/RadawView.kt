package com.eyuanchuang.bleespdht11.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.eyuanchuang.bleespdht11.R

//import com.fht.kotlin.R

/**
 * @author fenghaitao
 * @time 2021/11/2 09:47
 * 雷达扫描控件
 */
class RadarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyelAttr: Int = 0) :
    View(context, attrs, defStyelAttr) {
    private val scanPaint = Paint() //雷达画笔
    private val circlePaint = Paint() //中心圆圈画笔

    private var scanCircleRadius = 0f  //雷达扫描的半径
    private var circleRadius = 0f //中心圆圈半径
    private var mRoration = 360f //旋转角度

    private var scanShader = Shader() //阴影
    private var scanMatrix = Matrix() //阴影矩阵

    private var stopScan = true//默认不扫描

    init {
        scanPaint.color = resources.getColor(R.color.radar, null)
        scanPaint.isAntiAlias = true
        scanPaint.style = Paint.Style.FILL
        circlePaint.color = Color.GREEN
        circlePaint.isAntiAlias = true
        circlePaint.style = Paint.Style.FILL
    }

    /**
     * 开始扫描
     */
    fun startScan() {
        stopScan = false
        invalidate()
    }

    /**
     * 停止扫描
     */
    fun stopScan() {
        stopScan = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        scanCircleRadius = w / 2f
        circleRadius = scanCircleRadius * 0.035f  //计算半径
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startScan()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopScan()
    }

    @SuppressLint("DrawAllocation")
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        scanShader = SweepGradient(width / 2f, height / 2f,
            intArrayOf(resources.getColor(R.color.radar, null),
                Color.WHITE), floatArrayOf(0f, 1f))
        scanPaint.shader = scanShader
        scanShader.setLocalMatrix(scanMatrix)
    }


    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas != null) {
            canvas.drawCircle(width / 2f, height / 2f, scanCircleRadius, scanPaint)
            canvas.drawCircle(width / 2f, height / 2f, circleRadius, circlePaint)
            setRotation()
        }
    }

    /***
     * 改变旋转角度
     * ***/
    private fun setRotation() {
        if (mRoration <= 0) {
            mRoration = 360f
        }
        mRoration -= 2
        scanMatrix.setRotate(mRoration, width / 2f, height / 2f)
        scanShader.setLocalMatrix(scanMatrix)
        if (!stopScan) {
            invalidate()
        }
    }
}
