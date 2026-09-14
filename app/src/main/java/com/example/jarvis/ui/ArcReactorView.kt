package com.example.jarvis.ui

import android.content.Context
import android.util.AttributeSet
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import androidx.appcompat.widget.AppCompatImageView
import com.example.jarvis.R

class ArcReactorView(context: Context, attrs: AttributeSet) : AppCompatImageView(context, attrs) {

    init {
        // सिर्फ आर्क रिएक्टर वाली इमेज यहाँ सेट होगी और मूव करेगी
        setImageResource(R.drawable.jarvis_bg) 
        startRotation()
    }

    private fun startRotation() {
        val rotate = RotateAnimation(
            0f, 360f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 10000 // 10 सेकंड में एक पूरा चक्कर (स्पीड कम या ज्यादा करने के लिए इसे बदल सकते हैं)
            repeatCount = Animation.INFINITE
            interpolator = LinearInterpolator()
        }
        startAnimation(rotate)
    }
}
