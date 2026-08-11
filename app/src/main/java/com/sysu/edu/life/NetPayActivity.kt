package com.sysu.edu.life

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.Web
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.view.ActivityPager
import com.sysu.edu.view.MenuItem
import com.sysu.edu.view.StaggerScreen

class NetPayActivity : BaseActivity() {
	private val viewModel: NetPayViewModel by viewModels()
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		
		setContent {
			Box(modifier = Modifier.fillMaxSize()) {
				ActivityPager(
					onNavigationClick = { supportFinishAfterTransition() },
					title = stringResource(R.string.net_manager),
					navs = listOf(
						MenuItem(stringResource(R.string.order), Icons.Rounded.AttachMoney),
						MenuItem(stringResource(R.string.status), Icons.Rounded.Web),
					             ),
					isNestedScrollEnabled = false,
				             ) {
					StaggerScreen(when (it) {
						              0 -> viewModel.orderSections
						              1 -> viewModel.statusSections
						              else -> viewModel.orderSections
					              })
				}
				NetPayDialog(viewModel)
				NetPaySnackbar(viewModel)
			}
		}
	}
}