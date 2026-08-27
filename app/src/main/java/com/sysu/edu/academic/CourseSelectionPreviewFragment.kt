package com.sysu.edu.academic

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.viewModels
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.FragmentNavigator
import com.sysu.edu.BaseFragment
import com.sysu.edu.api.SettingManager
import com.sysu.edu.theme.SysuerTheme

class CourseSelectionPreviewFragment : BaseFragment() {
	private val viewModel: CourseSelectionPreviewViewModel by viewModels()
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?,
	                         ): View {
		super.onCreateView(inflater, container, savedInstanceState)
		return ComposeView(requireContext()).apply {
			setContent {
				SysuerTheme(SettingManager(requireContext())) {
					CourseSelectionPreviewScreen(
						viewModel = viewModel,
						onNavigateToFilter = { filterName, filterValue ->
							val action = CourseSelectionPreviewFragmentDirections.previewToFilter().apply {
								setCourseSelectionNameFilter(filterName)
								setCourseSelectionValueFilter(filterValue)
							}
							findNavController(this@apply).navigate(
								action.actionId,
								action.arguments,
								null,
								FragmentNavigator.Extras.Builder().addSharedElement(this@apply, "miniapp").build(),
							                                      )
						},
						onNavigateToDetail = { id, code, className ->
							startActivity(
								Intent(requireContext(), CourseDetailActivity::class.java).putExtra("id", id).putExtra("code", code).putExtra("class", className),
								ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity()).toBundle(),
							             )
						},
					                            )
				}
				transitionName = "miniapp"
			}
		}
	}
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val navController = findNavController(view)
		navController.currentBackStackEntry?.savedStateHandle?.getLiveData<CourseFilterNameData>("filter_name")?.observe(viewLifecycleOwner) { result ->
			viewModel.setFilterName(result)
			navController.currentBackStackEntry?.savedStateHandle?.remove<CourseFilterNameData>("filter_name")
		}
		navController.currentBackStackEntry?.savedStateHandle?.getLiveData<CourseFilterValueData>("filter_value")?.observe(viewLifecycleOwner) { result ->
			viewModel.setFilterValue(result)
			navController.currentBackStackEntry?.savedStateHandle?.remove<CourseFilterValueData>("filter_value")
		}
	}
}