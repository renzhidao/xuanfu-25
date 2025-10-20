
package com.screenmask

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private val overlayViews = mutableListOf<View>()
    private val ruleManager by lazy { RuleManager(this) }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createOverlays()
    }

    private fun createOverlays() {
        overlayViews.forEach { runCatching { windowManager.removeView(it) } }
        overlayViews.clear()

        val enabledRules = ruleManager.getRules().filter { it.enabled }

        enabledRules.forEach { rule ->
            val view = View(this).apply {
                setBackgroundColor(rule.color)
                // 彻底断开一切输入可能性（有些 ROM 奇葩）
                isClickable = false
                isLongClickable = false
                isFocusable = false
                isFocusableInTouchMode = false
                setOnTouchListener(null)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }

            val params = WindowManager.LayoutParams(
                rule.right - rule.left,
                rule.bottom - rule.top,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                // 关键：不接触、不聚焦、布局在屏内；再加 NON_MODAL 保险
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = rule.left
                y = rule.top
                // 尽量不参与手势系统
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }

            windowManager.addView(view, params)
            overlayViews.add(view)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayViews.forEach { runCatching { windowManager.removeView(it) } }
        overlayViews.clear()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}