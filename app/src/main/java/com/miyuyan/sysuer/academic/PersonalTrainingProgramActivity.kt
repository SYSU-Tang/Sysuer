package com.miyuyan.sysuer.academic

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.miyuyan.sysuer.BaseActivity
import com.miyuyan.sysuer.browser.RichTextRoute
import com.miyuyan.sysuer.nav.CourseDetail
import com.miyuyan.sysuer.nav.PersonalTrainingProgram as PersonalTrainingProgramKey
import com.miyuyan.sysuer.nav.RichText
import com.miyuyan.sysuer.nav.SysuerNavDisplay
import com.miyuyan.sysuer.theme.SysuerTheme

class PersonalTrainingProgramActivity : BaseActivity() {
    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val programId = intent.getStringExtra("programId")
        setContent {
            val backStack = rememberNavBackStack(PersonalTrainingProgramKey(programId))
            SysuerTheme(settingManager) {
                SharedTransitionLayout {
                    SysuerNavDisplay(backStack = backStack, entryProvider = entryProvider {
                        entry<PersonalTrainingProgramKey> { key ->
                            PersonalTrainingProgramRoute(
                                backStack,
                                key,
                                sharedTransitionScope = this@SharedTransitionLayout,
                                animatedVisibilityScope = LocalNavAnimatedContentScope.current
                            )
                        }
                        entry<CourseDetail> {
                            CourseDetailRoute(
                                backStack,
                                it,
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
