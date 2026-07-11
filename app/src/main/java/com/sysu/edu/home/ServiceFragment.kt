package com.sysu.edu.home

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.alibaba.fastjson2.JSONReader
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.sysu.edu.BaseFragment
import com.sysu.edu.MainActivity
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.api.ContextUtil
import com.sysu.edu.browser.BrowserActivity
import com.sysu.edu.databinding.DialogServiceActionBinding
import com.sysu.edu.databinding.DialogServiceOrderBinding
import com.sysu.edu.databinding.FragmentServiceBinding
import com.sysu.edu.databinding.ItemActionChipBinding
import com.sysu.edu.databinding.ItemServiceBoxBinding
import com.sysu.edu.view.AdapterListener
import com.sysu.edu.view.RecyclerAdapter
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.MarkwonSpansFactory
import io.noties.markwon.MarkwonVisitor
import io.noties.markwon.MarkwonVisitor.BlockHandler
import io.noties.markwon.RenderProps
import io.noties.markwon.SpanFactory
import io.noties.markwon.core.CoreProps
import io.noties.markwon.core.spans.LastLineSpacingSpan
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import org.commonmark.node.Heading
import org.commonmark.node.Node
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

class ServiceFragment : BaseFragment() {
	val list: MutableList<JSONObject> = mutableListOf()
	private val disposables = CompositeDisposable()
	lateinit var binding: FragmentServiceBinding
	var actionDialog: BottomSheetDialog? = null
	lateinit var db: HomeCollectionHelper
	var actionBinding: DialogServiceActionBinding? = null
	var orderDialog: BottomSheetDialog? = null
	var collectionAdapter: CollectionAdapter? = null
	var collectionBinding: ItemServiceBoxBinding? = null
	var viewModel: HomeViewModel? = null
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View {
		super.onCreateView(inflater, container, savedInstanceState)
		binding = FragmentServiceBinding.inflate(inflater)
		requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
		viewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]
		initAction(inflater)
		initOrder(inflater)
		initSearch()
		val reader = JSONReader.of(resources.openRawResource(R.raw.service), StandardCharsets.UTF_8)
		db = HomeCollectionHelper(requireContext())
		addCollection(inflater)
		reader.readJSONArray().forEach {
			binding.serviceContainer.addView(initBoxWithHashMap(inflater, (it as JSONObject).getString("name"), it.getJSONArray("items")).root)
		}
		reader.close()
		return binding.root
	}
	
	fun initAction(inflater: LayoutInflater) {
		actionDialog = BottomSheetDialog(requireContext())
		actionBinding = DialogServiceActionBinding.inflate(inflater).apply {
			order.setOnClickListener { orderDialog!!.show() }
			actionDialog?.setContentView(root)
		}
	}
	
	fun initOrder(inflater: LayoutInflater) {
		orderDialog = BottomSheetDialog(requireContext())
		collectionAdapter = CollectionAdapter()
		DialogServiceOrderBinding.inflate(inflater).apply {
			recyclerView.setLayoutManager(LinearLayoutManager(context))
			recyclerView.setAdapter(collectionAdapter)
			confirm.setOnClickListener {
				updateService()
				updateServiceCollection()
				orderDialog!!.dismiss()
			}
			orderDialog!!.setContentView(root)
			ItemTouchHelper(object : ItemTouchHelper.Callback() {
				override fun getMovementFlags(recyclerView: RecyclerView,
				                              viewHolder: RecyclerView.ViewHolder): Int {
					return makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)
				}
				
				override fun onMove(recyclerView: RecyclerView,
				                    source: RecyclerView.ViewHolder,
				                    target: RecyclerView.ViewHolder): Boolean {
					collectionAdapter!!.swap(source.getBindingAdapterPosition(), target.getBindingAdapterPosition())
					return true
				}
				
				override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
				}
			}).attachToRecyclerView(recyclerView)
		}
	}
	
	fun addCollection(inflater: LayoutInflater) {
		val collection = collection
		collectionBinding = initBoxWithHashMap(inflater, getString(R.string.collect), collection).apply {
			serviceBoxTitle.setOnClickListener { orderDialog!!.show() }
			binding.serviceContainer.addView(root, 0)
			if (collection.isEmpty()) root.visibility = View.GONE
		}
	}
	
	fun updateServiceCollection() {
		val collection = collection
		collectionBinding!!.apply {
			if (collection.isEmpty()) root.visibility = View.GONE
			else {
				root.visibility = View.VISIBLE
				serviceBoxItems.removeAllViews()
				addItems(getLayoutInflater(), collection, this)
			}
		}
	}
	
	private val collection: JSONArray
		get() {
			val cursor = db.writableDatabase.query("service_collection", null, null, null, null, null, "position ASC")
			val collection = JSONArray()
			collectionAdapter!!.clear()
			while (cursor.moveToNext()) JSONObject.parse(cursor.getString(cursor.getColumnIndexOrThrow("serviceJson")))
				.apply {
					collection.add(this)
					collectionAdapter!!.add(this)
				}
			cursor.close()
			return collection
		}
	
	fun initBoxWithHashMap(inflater: LayoutInflater,
	                       boxTitle: String?,
	                       items: JSONArray): ItemServiceBoxBinding {
		val binding = ItemServiceBoxBinding.inflate(inflater)
		binding.serviceBoxTitle.text = boxTitle
		addItems(inflater, items, binding)
		return binding
	}
	
	fun addItems(inflater: LayoutInflater, items: JSONArray, binding: ItemServiceBoxBinding) {
		items.indices.forEach { index: Int ->
			items.getJSONObject(index).let { item ->
				list.add(item)
				ItemActionChipBinding.inflate(inflater, binding.serviceBoxItems, false).root.apply {
					setOnClickListener(viewModel!!.actionMap[item.getIntValue("id")]
										   ?: View.OnClickListener {
											   getItemIntent(item, null)?.let { it1 ->
												   startActivity(it1, ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), it, "miniapp")
													   .toBundle())
											   } ?: config.toast(R.string.activity_not_found)
										   })
					setOnLongClickListener { showActionDialog(item) }
					text = item.getString("name")
					binding.serviceBoxItems.addView(this)
				}
			}
		}
	}
	
	fun showActionDialog(item: JSONObject): Boolean {
		val itemId = item.getIntValue("id")
		val isServiceCollected = MutableLiveData(db.isServiceCollected(itemId))
		val isShortcutCollected = MutableLiveData(db.isDashboardShortcutCollected(itemId))
		actionBinding?.run {
			collect.setText(if (true == isServiceCollected.value) R.string.cancel_collect else R.string.collect)
			addToDashboard.setText(if (true == isShortcutCollected.value) R.string.cancel_add_shortcut else R.string.add_to_dashboard)
			addToLauncher.setOnClickListener {
				if (ShortcutManagerCompat.isRequestPinShortcutSupported(requireContext())) {
					getItemIntent(item, Intent(requireContext(), MainActivity::class.java))?.let {
						ShortcutInfoCompat.Builder(requireContext(), "$itemId")
							.setShortLabel(item.getString("name"))
							.setLongLabel(item.getString("name"))
							.setIcon(IconCompat.createWithResource(requireContext(), R.mipmap.icon))
							.setIntent(it.setAction(Intent.ACTION_VIEW))
					}?.build()?.let {
						ShortcutManagerCompat.requestPinShortcut(requireContext(), it, PendingIntent.getBroadcast(requireContext(),  /* request code */0, ShortcutManagerCompat.createShortcutResultIntent(requireContext(), it),  /* flags */PendingIntent.FLAG_IMMUTABLE).intentSender)
					}
				}
				else config.toast(R.string.fail_to_add_shortcut)
			}
			collect.setOnClickListener {
				val isServiceCollect = true == isServiceCollected.value
				if (isServiceCollect) {
					db.deleteService(itemId)
					config.toast(R.string.cancel_collect_success)
				}
				else {
					db.addService(itemId, item.toJSONString(), collectionAdapter!!.itemCount)
					config.toast(R.string.collect_success)
				}
				updateServiceCollection()
				collect.setText(if (isServiceCollect) R.string.collect else R.string.cancel_collect)
				isServiceCollected.value = !isServiceCollect
			}
			addToDashboard.setOnClickListener {
				val isShortcutCollect = true == isShortcutCollected.value
				if (isShortcutCollect) {
					db.deleteDashboardShortcut(itemId)
					config.toast(R.string.cancel_add_shortcut_success)
				}
				else {
					db.addDashboardShortcut(itemId, item.toJSONString(), null)
					config.toast(R.string.add_shortcut_success)
				}
				viewModel!!.updateDashboardShortcut.value = true
				addToDashboard.setText(if (isShortcutCollect) R.string.add_to_dashboard else R.string.cancel_add_shortcut)
				isShortcutCollected.value = !isShortcutCollect
			}
			feedback.setOnClickListener {
				startActivity(Intent(Intent.ACTION_VIEW).setData("https://github.com/SYSU-Tang/Sysuer/issues/new?title=反馈：服务->${item.getString("name")}&labels=bug,crash-report".toUri())
								  .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
			}
			openAsUrl.setOnClickListener {
				val url = item.getString("url")
				if (!TextUtils.isEmpty(url)) startActivity(Intent(requireContext(), BrowserActivity::class.java).setData(Uri.parse(url)))
			}
			guide.setOnClickListener {
				if (item.containsKey("doc")) startActivity(Intent(requireContext(), BrowserActivity::class.java).setData(("https://sysu-tang.github.io/sysuer-website${CommonUtil.trim(item.getString("doc"))}").toUri()))
				else config.toast(R.string.undeveloped_warning)
			}
			val contextUtil = ContextUtil(requireContext())
			Markwon.builder(requireContext())
				.usePlugin(object : AbstractMarkwonPlugin() {
					override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
						super.configureSpansFactory(builder)
						builder.appendFactory(Heading::class.java, SpanFactory { _: MarkwonConfiguration?, configuration: RenderProps? ->
							if (CoreProps.HEADING_LEVEL.require(configuration!!) == 3) return@SpanFactory ForegroundColorSpan(contextUtil.getColorFromAttr(androidx.appcompat.R.attr.colorPrimary))
							null
						})
						builder.appendFactory(Heading::class.java) { _: MarkwonConfiguration?, _: RenderProps? -> LastLineSpacingSpan(24) }
					}
					
					override fun configureVisitor(builder: MarkwonVisitor.Builder) {
						super.configureVisitor(builder)
						builder.blockHandler(object : BlockHandler {
							override fun blockStart(visitor: MarkwonVisitor, node: Node) {
							}
							
							override fun blockEnd(visitor: MarkwonVisitor, node: Node) {
								if (visitor.hasNext(node)) visitor.ensureNewLine()
							}
						})
					}
				})
				.build()
				.setMarkdown(description, "### ${item.getString("name")}\n${item.getString("description")}\n\n`${CommonUtil.trim(item.getString("url"))}`")
		}
		actionDialog!!.show()
		return true
	}
	
	fun updateService() {
		(0 until collectionAdapter!!.itemCount).forEach {
			db.updateServicePosition(collectionAdapter!!.get(it).getInteger("id"), it)
		}
	}
	
	override fun onDestroy() {
		disposables.clear()
		super.onDestroy()
	}
	
	fun initSearch() {
		binding.run {
			ViewCompat.setOnApplyWindowInsetsListener(searchView) { v: View?, insets: WindowInsetsCompat? ->
				val left = insets!!.getInsets(WindowInsetsCompat.Type.systemBars()).left
				val right = insets.getInsets(WindowInsetsCompat.Type.systemBars()).right
				val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
				v!!.setPadding(left, 0, right, bottom)
				WindowInsetsCompat.CONSUMED
			}
			searchBar.getViewTreeObserver()
				.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
					override fun onGlobalLayout() {
						searchBar.getViewTreeObserver().removeOnGlobalLayoutListener(this)
						val layoutParams = searchBar.layoutParams as MarginLayoutParams
						serviceContainer.setPadding(0, searchBar.height + layoutParams.topMargin + layoutParams.bottomMargin, 0, 0)
					}
				})
			sugList.setLayoutManager(LinearLayoutManager(requireContext()))
			val serviceFragmentCollectionAdapter = CollectionAdapter()
			serviceFragmentCollectionAdapter.listener = object : AdapterListener {
				override fun onBind(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>,
				                    holder: RecyclerView.ViewHolder,
				                    position: Int) {
					val item = serviceFragmentCollectionAdapter.get(position)
					holder.itemView.setOnClickListener(viewModel!!.actionMap[item.getInteger("id")]
														   ?: View.OnClickListener { v: View? ->
															   getItemIntent(item, null)?.let {
																   startActivity(it, ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), v!!, "miniapp")
																	   .toBundle())
															   }
								                                   ?: config.toast(R.string.activity_not_found)
														   })
				}
				
				override fun onCreate(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>,
				                      binding: ViewBinding?) {
				}
			}
			sugList.setAdapter(serviceFragmentCollectionAdapter)
			val objectPublishSubject = PublishSubject.create<String>()
			disposables.add(objectPublishSubject.debounce(300, TimeUnit.MILLISECONDS)
								.distinctUntilChanged()
								.observeOn(Schedulers.computation())
								.map { query ->
									if (query.trim { it <= ' ' }.isEmpty()) return@map list
									val q = query.trim { it <= ' ' }
									list.filter { item ->
										item.getString("name")
											?.contains(q) == true || item.getString("description")
											?.contains(q) == true
									}.sortedWith { a: JSONObject?, b: JSONObject? ->
										val aNameMatch = a!!.getString("name").contains(q)
										val bNameMatch = b!!.getString("name").contains(q)
										if (aNameMatch && !bNameMatch) -1 else if (!aNameMatch && bNameMatch) 1 else 0
									}.toMutableList()
								}
								.observeOn(AndroidSchedulers.mainThread())
								.subscribe { d: MutableList<JSONObject> -> serviceFragmentCollectionAdapter.set(d) })
			searchView.editText.addTextChangedListener(object : TextWatcher {
				override fun beforeTextChanged(s: CharSequence?,
				                               start: Int,
				                               count: Int,
				                               after: Int) {
				}
				
				override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
					objectPublishSubject.onNext("$s")
				}
				
				override fun afterTextChanged(s: Editable?) {
				}
			})
			searchView.setupWithSearchBar(searchBar)
		}
	}
	
	fun getItemIntent(item: JSONObject, intent: Intent?): Intent? =
		if (item.containsKey("activity")) Intent(requireContext(), Class.forName(requireContext().packageName + item.getString("activity"))).takeIf { it.resolveActivity(requireContext().packageManager) != null }
			?: intent
		else if (item.containsKey("url")) Intent(requireContext(), BrowserActivity::class.java).setData(CommonUtil.trim(item.getString("url"))
																											.toUri())
		else intent
	
	class CollectionAdapter : RecyclerAdapter<JSONObject>() {
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
			return object : RecyclerView.ViewHolder(LayoutInflater.from(parent.context)
														.inflate(R.layout.item_sug, parent, false)) {}
		}
		
		override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
			(holder.itemView as TextView).text = get(position).getString("name")
			super.onBindViewHolder(holder, position)
		}
	}
}