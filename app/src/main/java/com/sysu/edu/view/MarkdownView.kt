package com.sysu.edu.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.MutableLiveData
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.rememberMarkdownState

class MarkdownView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val composeView = ComposeView(context)

    private val content = MutableLiveData("")

    init {
        addView(composeView)
        composeView.setContent({
            MaterialTheme {
                CompositionLocalProvider(LocalInspectionMode provides true) {
                    val markdownText by content.observeAsState("")
                    markdownText?.let {
                        Markdown(
                            rememberMarkdownState(it),
                            colors = markdownColor(

                            ),
                            typography = markdownTypography(
                                h1 = MaterialTheme.typography.headlineMedium,
                                h2 = MaterialTheme.typography.titleLargeEmphasized,
                                h3 = MaterialTheme.typography.titleMediumEmphasized
                            ),
                        )
                    }
                }
            }
        })
    }

    /**
     * 供 Java 代码调用的方法：设置并渲染 Markdown 文本
     */
    fun setMarkdown(text: String) {
        content.value = text
    }
}
