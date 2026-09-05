package com.miyuyan.sysuer.academic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.setupWithNavController
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.databinding.FragmentContainerBinding

class CourseSelectionContainerFragment : Fragment() {
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View {
		return FragmentContainerBinding.inflate(inflater, container, false).getRoot()
	}
	
	private val navController: NavController by lazy {
		(childFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController
	}
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val position = requireArguments().getInt("position")
		navController.setGraph(listOf(R.navigation.course_selection_nav,
		                              R.navigation.course_preview_nav,
		                              R.navigation.course_selected_nav)[position])
		setupWithNavController(requireActivity().findViewById(R.id.toolbar),
		                       navController,
		                       AppBarConfiguration.Builder().setFallbackOnNavigateUpListener {
			                       requireActivity().supportFinishAfterTransition()
			                       true
		                       }.build())
	}
	
	override fun onResume() {
		setupWithNavController(requireActivity().findViewById(R.id.toolbar),
		                       navController,
		                       AppBarConfiguration.Builder().setFallbackOnNavigateUpListener {
			                       requireActivity().supportFinishAfterTransition()
			                       false
		                       }.build())
		super.onResume()
	}
	
	companion object {
		fun newInstance(position: Int): CourseSelectionContainerFragment {
			val fragment = CourseSelectionContainerFragment()
			fragment.setArguments(Bundle().apply {
				putInt("position", position)
			})
			return fragment
		}
	}
}
