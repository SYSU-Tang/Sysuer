package com.sysu.edu.view

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation
import androidx.lifecycle.MutableLiveData
import com.google.android.material.appbar.MaterialToolbar
import com.sysu.edu.BaseFragment
import com.sysu.edu.R
import com.sysu.edu.academic.MarkdownViewActivity
import com.sysu.edu.theme.SysuerTheme

open class StaggerFragment : BaseFragment() {
	
	val sections: SnapshotStateList<SectionData> = mutableStateListOf()
	val hideNull: MutableLiveData<Boolean?> = MutableLiveData<Boolean?>(false)
	val staggeredListener: MutableLiveData<AdapterListener?> = MutableLiveData<AdapterListener?>()
	val scrollBottom: MutableLiveData<Runnable?> = MutableLiveData<Runnable?>()
	val isScrolledToTop: MutableLiveData<Boolean> = MutableLiveData<Boolean>(true)
	val nestedScrollingEnabled: MutableLiveData<Boolean?> = MutableLiveData<Boolean?>(true)
	var position: Int = 0
	val sectionAdapter: SectionAdapter = SectionAdapter()
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?,
	                         ): View? {
		super.onCreateView(inflater, container, savedInstanceState)
		return ComposeView(requireContext()).apply {
			setContent {
				SysuerTheme {
					StaggerScreen()
				}
			}
		}
	}
	
	@Composable
	fun StaggerScreen() {
		val isHideNull by hideNull.observeAsState(false)
		val isNestedEnabled by nestedScrollingEnabled.observeAsState(true)
		val scrollBottomTask by scrollBottom.observeAsState()
		
		StaggerScreen(
			sections = sections,
			isHideNull = isHideNull ?: false,
			isNestedEnabled = isNestedEnabled ?: true,
			onScrollBottom = { scrollBottomTask?.run() },
			onScrollTopChanged = { isScrolledToTop.value = it }
		             )
	}
	
	// Methods to maintain API compatibility
	fun setOrientation(o: Int) {
	// Managed by LazyVerticalStaggeredGrid configuration
	}
	
	fun setScrollBottom(runnable: Runnable?) {
		scrollBottom.value = runnable
	}
	
	fun setNested(nested: Boolean) {
		nestedScrollingEnabled.value = nested
	}
	
	fun setHideNull(hide: Boolean) {
		hideNull.value = hide
	}
	
	fun setListener(v: AdapterListener?) {
		staggeredListener.value = v
	}
	
	open fun addSection(
		title: String?,
		icon: Int?,
		keys: MutableList<String?>,
		values: MutableList<String?>,
		rowOrientation: RowOrientation = RowOrientation.Horizontal,
		footerMenus: SnapshotStateList<com.sysu.edu.view.MenuItem> = mutableStateListOf(),
		footer: (@Composable ColumnScope.() -> Unit)? = null,
		
	                   ) {
		val rows = mutableStateListOf<RowData>()
		keys.zip(values) { k, v -> rows.add(RowData(k, v)) }
		sections.add(SectionData(title, icon, rows, rowOrientation, footerMenus, footer))
	}
	
	fun addSection(title: String?, keys: MutableList<String?>, values: MutableList<String?>, rowOrientation: RowOrientation = RowOrientation.Horizontal) {
		addSection(title, null, keys, values, rowOrientation)
	}
	
	fun addRow(pos: Int = sections.size - 1, key: String?, value: String?) {
		if (pos in sections.indices) {
			sections[pos].rows.add(RowData(key, value))
		}
	}
	
	fun clear() {
		sections.clear()
	}
	
	fun addExportMenu(toolbar: MaterialToolbar) {
		toolbar.menu.add(R.string.export).setIcon(R.drawable.export).setOnMenuItemClickListener {
			export(toolbar, "${toolbar.title}")
			false
		}.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
	}
	
	fun export(toolbar: View, title: String) {
		startActivity(Intent(requireContext(), MarkdownViewActivity::class.java).putExtra("content", sections.toMarkdown()).putExtra("title", title), makeSceneTransitionAnimation(requireActivity(), toolbar, "miniapp").toBundle())
	}
	
	// Compatibility classes for old View-based logic
	inner class SectionAdapter {
		val titles: List<String?> get() = sections.map { it.title }
		val keys: List<MutableList<String?>>
			get() = sections.map { s ->
				s.rows.map { it.key }.toMutableList()
			}
		val values: List<MutableList<String?>>
			get() = sections.map { s ->
				s.rows.map { it.value }.toMutableList()
			}
		var hideNull: Boolean = false
			set(value) {
				field = value
				this@StaggerFragment.setHideNull(value)
			}
		
		fun add(
			title: String?,
			keys: MutableList<String?>?,
			values: MutableList<String?>?,
			icon: Int?,
		       ) {
			this@StaggerFragment.addSection(title,
			                                icon,
			                                keys ?: mutableListOf(),
			                                values ?: mutableListOf())
		}
		
		fun clear(): Unit = this@StaggerFragment.clear()
		fun getTwoColumnsAdapter(pos: Int): TwoColumnsAdapter = TwoColumnsAdapter(pos)
		/*fun addRow(pos: Int = itemCount - 1, key: String?, value: String?) {
			this@StaggerFragment.addRow(pos, key, value)
		}
		
		fun addFooter(pos: Int = itemCount - 1, content: com.sysu.edu.view.MenuItem) {
			if (pos in sections.indices) sections[pos].footerMenus.add(content)
		}*/
		
		fun setSectionFooter(pos: Int = itemCount - 1, content: (@Composable ColumnScope.() -> Unit)? = null) {
			if (pos in sections.indices) sections[pos].footer = content
		}
		/*fun setListener(listener: AdapterListener?) {
			staggeredListener.value = listener
		}*/
		
		val itemCount: Int get() = sections.size
	}
	
	inner class TwoColumnsAdapter(val sectionIndex: Int) {
		fun setKeyAndValue(keys: MutableList<String?>, values: MutableList<String?>) {
			if (sectionIndex in sections.indices) {
				val rows = sections[sectionIndex].rows
				rows.clear()
				keys.zip(values) { k, v -> rows.add(RowData(k, v)) }
			}
		}
		
		fun setValue(values: MutableList<String?>) {
			if (sectionIndex in sections.indices) {
				val rows = sections[sectionIndex].rows
				values.forEachIndexed { index, v ->
					if (index < rows.size) rows[index].value = v
				}
			}
		}
		
		fun setKey(keys: MutableList<String?>) {
			if (sectionIndex in sections.indices) {
				val rows = sections[sectionIndex].rows
				val currentValues = rows.map { it.value }
				rows.clear()
				keys.zip(currentValues) { k, v -> rows.add(RowData(k, v)) }
			}
		}
		
		fun setKeyAndValue(map: Map<String, Any?>) {
			val keys = map["keys"] as? MutableList<String?>
			val values = map["values"] as? MutableList<String?>
			if (keys != null && values != null) {
				setKeyAndValue(keys, values)
			}
		}
		
		fun add(row: Int = -1, key: String?, value: String?) {
			this@StaggerFragment.addRow(sectionIndex, key, value)
		}
		
		fun setRowClickListener(index: Int, onClick: () -> Unit) {
			if (sectionIndex in sections.indices && index in sections[sectionIndex].rows.indices) {
				sections[sectionIndex].rows[index].onClick = onClick
			}
		}
		
		fun setListener(listener: AdapterListener?) {
		}
		
		val itemCount: Int get() = sections.getOrNull(sectionIndex)?.rows?.size ?: 0
	}
	
	companion object {
		fun newInstance(position: Int): StaggerFragment {
			val s = StaggerFragment()
			s.position = position
			return s
		}
	}
}
