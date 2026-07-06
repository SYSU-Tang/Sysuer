package com.sysu.edu.view

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.appbar.MaterialToolbar
import com.sysu.edu.BaseFragment
import com.sysu.edu.R
import com.sysu.edu.academic.MarkdownViewActivity
import com.sysu.edu.databinding.ItemCardBinding
import com.sysu.edu.databinding.ItemTwoColumnRowBinding
import com.sysu.edu.databinding.RecyclerViewBinding
import com.sysu.edu.databinding.RecyclerViewScrollBinding
import kotlin.math.min

open class StaggeredFragment : BaseFragment() {
	val staggeredAdapter: StaggeredAdapter = StaggeredAdapter()
	val orientation: MutableLiveData<Int?> = MutableLiveData<Int?>(StaggeredGridLayoutManager.VERTICAL)
	val scrollBottom: MutableLiveData<Runnable?> = MutableLiveData<Runnable?>()
	val nestedScrollingEnabled: MutableLiveData<Boolean?> = MutableLiveData<Boolean?>(true)
	val hideNull: MutableLiveData<Boolean?> = MutableLiveData<Boolean?>(false)
	val staggeredListener: MutableLiveData<AdapterListener?> = MutableLiveData<AdapterListener?>()
	var position: Int = 0
	protected var binding: RecyclerViewScrollBinding? = null
	var staggeredGridLayoutManager: StaggeredGridLayoutManager? = null
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View? {
		super.onCreateView(inflater, container, savedInstanceState)
		binding = RecyclerViewScrollBinding.inflate(inflater)
		staggeredGridLayoutManager = StaggeredGridLayoutManager(config.column, StaggeredGridLayoutManager.VERTICAL)
		binding!!.recyclerView.setLayoutManager(staggeredGridLayoutManager)
		orientation.observe(getViewLifecycleOwner(), Observer { o: Int? ->
			if (o != null) staggeredGridLayoutManager!!.setOrientation(o)
		})
		scrollBottom.observe(getViewLifecycleOwner(), Observer { runnable: Runnable? ->
			if (runnable != null) binding!!.recyclerView.addOnScrollListener(object :
																				 RecyclerView.OnScrollListener() {
				override fun onScrolled(v: RecyclerView, dx: Int, dy: Int) {
					if (!v.canScrollVertically(1) && dy > 0) runnable.run()
				}
			})
		})
		staggeredListener.observe(getViewLifecycleOwner(), Observer { listener: AdapterListener? -> staggeredAdapter.setListener(listener) })
		hideNull.observe(getViewLifecycleOwner(), Observer { b: Boolean? ->
			if (b != null) staggeredAdapter.hideNull = b
		})
		binding!!.recyclerView.setAdapter(staggeredAdapter)
		nestedScrollingEnabled.observe(getViewLifecycleOwner(), Observer { enabled: Boolean? -> binding!!.recyclerView.isNestedScrollingEnabled = enabled!! })
		return binding!!.root
	}
	
	fun setOrientation(o: Int) {
		orientation.value = o
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
	
	open fun add(title: String?,
	             icon: Int?,
	             keys: MutableList<String?>,
	             values: MutableList<String?>) {
		staggeredAdapter.add(title, keys, values, icon)
	}
	
	fun add(title: String?, keys: MutableList<String?>, values: MutableList<String?>) {
		add(title, null, keys, values)
	}
	
	override fun onConfigurationChanged(newConfig: Configuration) {
		super.onConfigurationChanged(newConfig)
		staggeredGridLayoutManager!!.setSpanCount(config.column)
	}
	
	fun clear() {
		staggeredAdapter.clear()
	}
	
	fun toTable(): String {
		val markdown = StringBuilder()
		var keys: MutableList<String?> = mutableListOf()
		(0..<staggeredAdapter.itemCount).forEach { i: Int ->
			staggeredAdapter.keys[i]?.let {
				if (keys.isEmpty() || !it.containsAll(keys)) keys = it.apply {
					markdown.append("\n")
						.append("序号|")
						.append(joinToString("|") { s -> s?.trim() ?: "" })
						.append("\n")
						.append(":---:|".repeat(size + 1))
						.append("\n")
				}
			}                // 表头
			markdown.append(i + 1)
				.append("|")
				.append(staggeredAdapter.values[i]?.joinToString("|") { it?.trim() ?: "" })
				.append("\n")
		}
		return "$markdown"
	}
	
	fun setViewTableMenu(toolbar: MaterialToolbar) {
		toolbar.menu.add(R.string.export).setIcon(R.drawable.export).setOnMenuItemClickListener {
			export(toolbar, toolbar.title.toString())
			false
		}.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
	}
	
	fun export(view: View, title: String?) {
		startActivity(Intent(requireContext(), MarkdownViewActivity::class.java).putExtra("content", toTable())
						  .putExtra("title", title), ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), view, "miniapp")
						  .toBundle())
	}
	
	class TwoColumnsAdapter(@JvmField var key: MutableList<String?>,
	                        @JvmField var value: MutableList<String?>,
	                        val hideNull: Boolean) :
		RecyclerView.Adapter<RecyclerView.ViewHolder?>() {
		var rowListener: AdapterListener? = null
		val map: LinkedHashMap<String, String?> = linkedMapOf()
		fun setValue(value: MutableList<String?>) {
			this.value = value
			notifyItemRangeChanged(0, itemCount)
		}
		
		fun setKey(key: MutableList<String?>) {
			this.key = key
			notifyItemRangeChanged(0, itemCount)
		}
		
		fun setKeyAndValue(key: MutableList<String?>, value: MutableList<String?>) {
			this.key = key
			this.value = value
			notifyItemRangeChanged(0, itemCount)
		}
		
		fun add(key: String?, value: String?) {
			add(key, value)
		}
		
		fun add(row: Int = itemCount - 1, key: String?, value: String?) {
			if (row in 1..<itemCount) {
				this.key.add(key)
				this.value.add(value)
				notifyItemInserted(row)
			}
		}
		
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
			val binding = ItemTwoColumnRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
			rowListener?.onCreate(this, binding)
			return object : RecyclerView.ViewHolder(binding.root) {}
		}
		
		override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
			val binding = ItemTwoColumnRowBinding.bind(holder.itemView)
			binding.key.text = key[position]
			position.takeIf { it < value.size }?.let { value[it] }?.let {
				binding.value.text = it
				holder.itemView.setOnClickListener { _ ->
					val clip = binding.root.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
					clip.setPrimaryClip(ClipData.newPlainText(key[position], it))
				}
			} ?: run {
				if (hideNull) {
					holder.itemView.visibility = View.GONE
					holder.itemView.layoutParams.height = 0
				}
			}
			rowListener?.onBind(this, holder, position)
		}
		
		fun setListener(listener: AdapterListener?) {
			rowListener = listener
		}
		
		override fun getItemCount(): Int = min(key.size, value.size)
	}
	
	class StaggeredAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder?>() {
		@JvmField val titles: MutableList<String?> = mutableListOf()
		@JvmField val keys: MutableList<MutableList<String?>?> = mutableListOf()
		@JvmField val icons: MutableList<Int?> = mutableListOf()
		@JvmField val values: MutableList<MutableList<String?>?> = mutableListOf()
		@JvmField val twoColumnsAdapters: MutableList<TwoColumnsAdapter?> = mutableListOf()
		@JvmField var adapterListener: AdapterListener? = null
		var hideNull: Boolean = false
		fun setListener(listener: AdapterListener?) {
			adapterListener = listener
		}
		
		fun add(title: String?,
		        keys: MutableList<String?>?,
		        values: MutableList<String?>?,
		        icon: Int?) {
			titles.add(title)
			icons.add(icon)
			this.keys.add(keys)
			this.values.add(values)
			notifyItemInserted(itemCount - 1)
		}
		
		fun clear() {
			val tmp = itemCount
			titles.clear()
			icons.clear()
			keys.clear()
			values.clear()
			notifyItemRangeRemoved(0, tmp)
		}
		
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
			val context = parent.context
			val item = ItemCardBinding.inflate(LayoutInflater.from(context), parent, false)
			item.card.addView(RecyclerViewBinding.inflate(LayoutInflater.from(context), item.root, false).root.apply {
				this.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
				this.isNestedScrollingEnabled = false
			})
			adapterListener?.onCreate(this, item)
			return object : RecyclerView.ViewHolder(item.root) {}
		}
		
		fun getTwoColumnsAdapter(pos: Int): TwoColumnsAdapter? {
			return twoColumnsAdapters[pos]
		}
		
		fun addRow(pos: Int = itemCount - 1, key: String?, value: String?) {
			if (pos < itemCount) {
				keys[pos]?.add(key)
				values[pos]?.add(value)
				notifyItemChanged(pos)
			}
		}
		
		override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
			val item = ItemCardBinding.bind(holder.itemView)
			val context = holder.itemView.context
			if (icons[position] != null) {
				item.title.setCompoundDrawablePadding(24)
				val icon = AppCompatResources.getDrawable(context, icons[position]!!)
				if (icon != null) {
					icon.setBounds(0, 0, 72, 72)
					item.title.setCompoundDrawables(icon, null, null, null)
				}
			} // 设置图标
			item.title.text = titles[position] // 设置标题
			val recyclerView = holder.itemView.findViewById<RecyclerView>(R.id.recycler_view)
			var twoColumnsAdapter = recyclerView.adapter as TwoColumnsAdapter?
			if (twoColumnsAdapter == null) {
				twoColumnsAdapter = TwoColumnsAdapter(keys[position]!!, values[position]!!, hideNull)
				recyclerView.setAdapter(twoColumnsAdapter)
			}
			else {
				twoColumnsAdapter.setKeyAndValue(keys[position]!!, values[position]!!)
			}
			if (twoColumnsAdapters.size <= position || twoColumnsAdapters[position] == null) twoColumnsAdapters.add(position, twoColumnsAdapter)
			
			adapterListener?.onBind(this, holder, position)
		}
		
		override fun getItemCount(): Int = titles.size
	}
	
	companion object {
		fun newInstance(position: Int): StaggeredFragment {
			val s = StaggeredFragment()
			s.position = position
			return s
		}
	}
}