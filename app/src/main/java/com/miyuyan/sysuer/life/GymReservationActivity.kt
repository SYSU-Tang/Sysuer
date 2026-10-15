package com.miyuyan.sysuer.life

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.SharedTransitionLayout
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.miyuyan.sysuer.BaseActivity
import com.miyuyan.sysuer.nav.GymReservation as GymReservationKey
import com.miyuyan.sysuer.nav.SysuerNavDisplay
import com.miyuyan.sysuer.theme.SysuerTheme

/**
 * 体育场馆预订入口：仅负责承载 [GymRoute]，页面切换由 Navigation3 接管。
 */
class GymReservationActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val backStack = rememberNavBackStack(GymReservationKey)
            SysuerTheme(settingManager) {
                SharedTransitionLayout {
                    SysuerNavDisplay(backStack = backStack, entryProvider = entryProvider {
                        entry<GymReservationKey> {
                            GymRoute(
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
