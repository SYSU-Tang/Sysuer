package com.miyuyan.sysuer.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * 悬浮按钮拨号效果控制器
 */
class SpeedDialController(
    private val mainFab: FloatingActionButton,
    private val subFabContainer: ViewGroup,
    private val scrim: View
) {
    private var isExpanded = false

    init {
        // 初始化状态
        scrim.visibility = View.GONE
        subFabContainer.visibility = View.GONE
        
        mainFab.setOnClickListener {
            toggle()
        }

        scrim.setOnClickListener {
            if (isExpanded) collapse()
        }
    }

    fun toggle() {
        if (isExpanded) collapse() else expand()
    }

    fun expand() {
        if (isExpanded) return
        isExpanded = true

        // 1. 背景遮罩淡入
        scrim.visibility = View.VISIBLE
        scrim.animate()
            .alpha(1f)
            .setDuration(300)
            .setListener(null)
            .start()

        // 2. 主按钮旋转
        mainFab.animate()
            .rotation(45f)
            .setDuration(300)
            .setInterpolator(OvershootInterpolator())
            .start()

        // 3. 子按钮展开
        subFabContainer.visibility = View.VISIBLE
        for (i in 0 until subFabContainer.childCount) {
            val child = subFabContainer.getChildAt(i)
            child.alpha = 0f
            child.scaleX = 0f
            child.scaleY = 0f
            child.translationY = 50f // 从下方弹出

            child.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(200)
                .setStartDelay((subFabContainer.childCount - 1 - i) * 50L) // 从上往下依次出现
                .setInterpolator(OvershootInterpolator())
                .start()
        }
    }

    fun collapse() {
        if (!isExpanded) return
        isExpanded = false

        // 1. 背景遮罩淡出
        scrim.animate()
            .alpha(0f)
            .setDuration(300)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    scrim.visibility = View.GONE
                }
            })
            .start()

        // 2. 主按钮旋回
        mainFab.animate()
            .rotation(0f)
            .setDuration(300)
            .setInterpolator(OvershootInterpolator())
            .start()

        // 3. 子按钮收起
        for (i in 0 until subFabContainer.childCount) {
            val child = subFabContainer.getChildAt(i)
            child.animate()
                .alpha(0f)
                .scaleX(0f)
                .scaleY(0f)
                .translationY(50f)
                .setDuration(200)
                .setStartDelay(i * 50L) // 依次消失
                .setListener(if (i == subFabContainer.childCount - 1) {
                    object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            if (!isExpanded) subFabContainer.visibility = View.GONE
                        }
                    }
                } else null)
                .start()
        }
    }
}