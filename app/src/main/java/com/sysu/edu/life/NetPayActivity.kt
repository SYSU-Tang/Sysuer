package com.sysu.edu.life

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.sysu.edu.BaseActivity
import com.sysu.edu.browser.RichTextRoute
import com.sysu.edu.nav.NetPay as NetPayKey
import com.sysu.edu.nav.RichText
import com.sysu.edu.nav.SysuerNavDisplay
import com.sysu.edu.theme.SysuerTheme

class NetPayActivity : BaseActivity() {
    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val backStack = rememberNavBackStack(NetPayKey)
            SysuerTheme(settingManager) {
                SharedTransitionLayout {
                    SysuerNavDisplay(backStack = backStack, entryProvider = entryProvider {
                        entry<NetPayKey> {
                            NetPayRoute(
                                backStack,
                                sharedTransitionScope = this@SharedTransitionLayout,
                                animatedVisibilityScope = LocalNavAnimatedContentScope.current
                            )
                        }
                        entry<RichText> {
                            RichTextRoute(
                                backStack,
                                sharedTransitionScope = this@SharedTransitionLayout,
                                animatedVisibilityScope = LocalNavAnimatedContentScope.current
                            )
                        }
                    })
                }
            }
        }
    }
}
