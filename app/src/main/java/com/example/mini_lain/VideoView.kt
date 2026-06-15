package com.example.mini_lain

import android.content.Context
import android.util.AttributeSet
import android.widget.VideoView
import kotlin.math.roundToInt

class VideoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : VideoView(context, attrs) {

    private var anchoVideo: Int = 0
    private var altoVideo: Int = 0

    fun establecerTamanioVideo(
        ancho: Int,
        alto: Int
    ) {
        anchoVideo = ancho
        altoVideo = alto
        requestLayout()
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int
    ) {
        val altoPantalla = MeasureSpec.getSize(heightMeasureSpec)
        val anchoPantalla = MeasureSpec.getSize(widthMeasureSpec)

        if (anchoVideo <= 0 || altoVideo <= 0 || altoPantalla <= 0) {
            setMeasuredDimension(anchoPantalla, altoPantalla)
            return
        }

        val proporcionVideo = anchoVideo.toFloat() / altoVideo.toFloat()
        val anchoCalculado = (altoPantalla * proporcionVideo).roundToInt()

        setMeasuredDimension(anchoCalculado, altoPantalla)
    }
}