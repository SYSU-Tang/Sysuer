package com.sysu.edu.academic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.setupWithNavController
import com.sysu.edu.R
import com.sysu.edu.databinding.FragmentContainerBinding

class CourseSelectionContainerFragment : Fragment() {
	var nav: NavController? = null
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View {
		return FragmentContainerBinding.inflate(inflater, container, false).getRoot()
	}
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		(getChildFragmentManager().findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController.let {
			it.setGraph(listOf(R.navigation.course_selection_nav, R.navigation.course_preview_nav, R.navigation.course_selected_nav)[requireArguments().getInt("position")])
			setupWithNavController(requireActivity().findViewById(R.id.toolbar), it, AppBarConfiguration.Builder()
				.setFallbackOnNavigateUpListener {
					requireActivity().supportFinishAfterTransition()
					true
				}.build())
		}
	}
	
	override fun onResume() {
		nav?.let {
			setupWithNavController(requireActivity().findViewById(R.id.toolbar), it, AppBarConfiguration.Builder()
				.setFallbackOnNavigateUpListener {
					requireActivity().supportFinishAfterTransition()
					false
				}.build())
		}
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
