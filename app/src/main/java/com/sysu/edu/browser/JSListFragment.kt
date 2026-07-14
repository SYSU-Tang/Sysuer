package com.sysu.edu.browser

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.FragmentNavigator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.transition.MaterialContainerTransform
import com.sysu.edu.R
import com.sysu.edu.browser.BrowserActivity.JSAdapter
import com.sysu.edu.browser.data.BrowserRepository
import com.sysu.edu.browser.data.JavaScriptEntity
import com.sysu.edu.browser.data.JsModel
import com.sysu.edu.browser.data.JsModelFactory
import com.sysu.edu.databinding.FragmentRecyclerFabBinding
import com.sysu.edu.view.AdapterListener

class JSListFragment : Fragment() {
	val model: JsModel by lazy {
		ViewModelProvider(this, JsModelFactory(BrowserRepository(requireContext(), lifecycleScope)))[JsModel::class.java]
	}
	lateinit var binding: FragmentRecyclerFabBinding
	private val jsAdapter: JSAdapter = JSAdapter()
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View {
		binding = FragmentRecyclerFabBinding.inflate(inflater)
		model.loadJs { jsAdapter.set(it.toMutableList()) }
		val toolbar = requireActivity().findViewById<MaterialToolbar>(R.id.toolbar)
		toolbar.menu.setGroupVisible(R.id.editor_group, false)
		jsAdapter.listener = object : AdapterListener {
			override fun onBind(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>,
			                    holder: RecyclerView.ViewHolder,
			                    position: Int) {
				val js = jsAdapter.get(position)
				holder.itemView.apply {
					setOnClickListener {
						transitionName = "script"
						val bundle = Bundle().apply {
							putLong("id", js.id)
						}
						findNavController(binding.root).navigate(R.id.list_to_info, bundle, null, FragmentNavigator.Extras.Builder()
							.addSharedElement(this, "script")
							.build())
					}
					setOnLongClickListener {
						val pop = PopupMenu(requireContext(), this)
						pop.menuInflater.inflate(R.menu.js_item_menu, pop.menu)
						pop.show()
						pop.menu.findItem(R.id.ban)
							.setTitle(if (js.state == 1) R.string.disable else R.string.enable)
						pop.setOnMenuItemClickListener { item: MenuItem? ->
							when (item?.itemId) {
								R.id.edit -> {
									performClick()
									true
								}
								R.id.delete -> {
									model.deleteJs(js) {
										jsAdapter.remove(position)
									}
									true
								}
								R.id.ban -> {
									js.state = 1 - js.state
									model.updateJs(js)
									jsAdapter.notifyItemChanged(position)
									true
								}
								else -> false
							}
						}
						false
					}
					alpha = if (js.state == 1) 1f else 0.5f
				}
			}
			
			override fun onCreate(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>,
			                      binding: ViewBinding?) {
			}
		}
		binding.recyclerViewScroll.root.layoutManager = LinearLayoutManager(requireContext())
		binding.recyclerViewScroll.root.adapter = jsAdapter
		binding.fab.setIconResource(R.drawable.add)
		binding.fab.setText(R.string.add)
		binding.fab.setContentDescription(getString(R.string.add))
		binding.fab.setOnClickListener { add() }
		return binding.root
	}
	
	fun add() {
		model.addJs(JavaScriptEntity(title = getString(R.string.new_script), script = "")) { id ->
			findNavController(binding.root).navigate(R.id.list_to_info, Bundle().apply { putLong("id", id) }, null, FragmentNavigator.Extras.Builder()
				.addSharedElement(binding.fab, "miniapp")
				.build())
		}
	}
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val transition = MaterialContainerTransform()
		transition.scrimColor = Color.TRANSPARENT
		transition.setAllContainerColors(requireContext().getColor(com.google.android.material.R.color.design_default_color_surface))
		sharedElementEnterTransition = transition
		sharedElementReturnTransition = transition
		if (requireActivity().intent.getStringExtra("operation") == "add") {
			add()
			requireActivity().intent.removeExtra("operation")
		}
	}
	
	override fun onResume() {
		super.onResume()
		jsAdapter.clear()
		model.loadJs { jsAdapter.set(it.toMutableList()) }
	}
}