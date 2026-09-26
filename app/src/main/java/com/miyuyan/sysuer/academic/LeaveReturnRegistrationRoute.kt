package com.miyuyan.sysuer.academic

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.CommonUtil.extractValue
import com.miyuyan.sysuer.nav.LeaveReturnRegistrationDetail
import com.miyuyan.sysuer.nav.navigateBack
import com.miyuyan.sysuer.view.ActivityPager
import com.miyuyan.sysuer.view.MenuItem
import com.miyuyan.sysuer.view.SectionData
import com.miyuyan.sysuer.view.StaggerScreen
import com.miyuyan.sysuer.view.StatePage

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LeaveReturnRegistrationRoute(
	backStack: MutableList<NavKey>,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
	val viewModel: LeaveReturnRegistrationViewModel = viewModel()
	val years by viewModel.years.collectAsStateWithLifecycle()
	val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
	val workList by viewModel.workList.collectAsStateWithLifecycle()
	val uiState by viewModel.uiState.collectAsStateWithLifecycle()

	val activity = LocalActivity.current

	val registrationKeys = stringArrayResource(R.array.registration_keys)
	val startRegistration = stringResource(R.string.start_registration)
	val modifyRegistration = stringResource(R.string.modify_registration)
	val viewDetail = stringResource(R.string.view_detail)

	val sections = remember(workList, startRegistration, modifyRegistration, viewDetail) {
		val list = mutableStateListOf<SectionData>()
		workList.forEach { item ->
			val rows = extractValue(
					item,
					registrationKeys,
					arrayOf("blxn", "lxdjsj", "gzsm", "jjrmc", "jjrrq", "gzzt", "zt")
			)
			val isRegistering = item.getInteger("gzztm") == 1
			val status = item.getString("zt")
			val buttonText = if (isRegistering) {
				if ("registering" == status) startRegistration else modifyRegistration
			} else {
				viewDetail
			}
			list.add(
					SectionData(
							title = item.getString("gzmc", ""),
							icon = if (isRegistering) R.drawable.uncheck else R.drawable.check,
							rows = rows,
							footerMenus = mutableStateListOf(
									MenuItem(
											title = buttonText,
											key = "LeaveReturnRegistrationDetail_${item.getString("cjlfxgzId")}",
									) {
										val id = item.getString("cjlfxgzId")
										if (!id.isNullOrEmpty()) {
											backStack.add(LeaveReturnRegistrationDetail(id))
										}
										true
									})
					)
			)
		}
		list
	}

	var expanded by remember { mutableStateOf(false) }

	ActivityPager(
			title = stringResource(R.string.leave_return_registration),
			onNavigationClick = { backStack.navigateBack(activity) },
			sharedTransitionScope = sharedTransitionScope,
			animatedVisibilityScope = animatedVisibilityScope,
			sharedKey = "LeaveReturnRegistration",
			isNestedScrollEnabled = false,
			topBarContent = {
				if (years.isNotEmpty()) {
					Box(
							Modifier
								.fillMaxWidth()
								.padding(
										dimensionResource(R.dimen.horizontal_margin),
										dimensionResource(R.dimen.vertical_margin),
								)
					) {
						ExposedDropdownMenuBox(
								expanded = expanded, onExpandedChange = { expanded = it }) {
							OutlinedTextField(
									value = years
								.find { it.getString("value") == selectedYear }?.getString("label")
								?: selectedYear ?: "",
									onValueChange = {},
									readOnly = true,
									trailingIcon = {
										ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
									},
									modifier = Modifier
										.fillMaxWidth()
										.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
							)
							ExposedDropdownMenu(
									expanded = expanded, onDismissRequest = { expanded = false }) {
								years.forEach { yearObj ->
									val label = yearObj.getString("label", "")
									val value = yearObj.getString("value", "")
									DropdownMenuItem(text = { Text(label) }, onClick = {
										viewModel.selectYear(value)
										expanded = false
									})
								}
							}
						}
					}
				}
			},
			pageContent = {
				StatePage(state = uiState) {
					StaggerScreen(
							sections = sections,
							animatedVisibilityScope = animatedVisibilityScope,
							sharedTransitionScope = sharedTransitionScope
					)
				}
			})
}