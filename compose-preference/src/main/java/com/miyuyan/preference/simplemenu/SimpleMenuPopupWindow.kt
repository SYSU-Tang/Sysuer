package com.miyuyan.preference.simplemenu

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.PopupWindow
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.miyuyan.preference.R
import com.miyuyan.preference.SimpleMenuPreference
import com.miyuyan.preference.databinding.SimpleMenuItemBinding
import com.miyuyan.preference.databinding.SimpleMenuListBinding
import com.miyuyan.preference.simplemenu.Light.resetLightCenterForPopupWindow
import com.miyuyan.preference.simplemenu.SimpleMenuAnimation.startEnterAnimation
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.properties.Delegates

/**
 * Extension of [PopupWindow] that implements Simple Menus in Material Design 1.
 */
open class SimpleMenuPopupWindow @SuppressLint("InflateParams") constructor(
	val context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int
) : PopupWindow(context, attrs, defStyleAttr, defStyleRes) {
	fun interface OnItemClickListener {
		fun onClick(i: Int)
	}

	protected val elevations: IntArray = IntArray(2)
	protected val margins = Array(2) { IntArray(2) }
	val listPaddings = Array(2) { IntArray(2) }
	protected val itemHeight: Int
	var dialogMaxWidth by Delegates.notNull<Int>()
	var unit by Delegates.notNull<Int>()
	var maxUnits by Delegates.notNull<Int>()
	var mode: Int = POPUP_MENU
		private set
	private var mRequestMeasure = true
	val mAdapter: SimpleMenuListAdapter = SimpleMenuListAdapter(this@SimpleMenuPopupWindow)
	lateinit var mListBinding: SimpleMenuListBinding
	lateinit var mList: RecyclerView
	var onItemClickListener: OnItemClickListener? = null
	var entries: Array<CharSequence> = emptyArray()

	/**
	 * 当前选中项索引；-1 表示无选中项，此时不勾选任何条目，
	 * 菜单显示在锚点正下方而不是把第 0 项当选中项对齐到锚点。
	 */
	var selectedItemIndex: Int = -1

	/**
	 * true(默认): 选中项与锚点中心对齐，Material Design 1 Simple Menu 规范;
	 * false: 作为标准下拉菜单显示在锚点正下方，菜单与锚点之间只保留列表自身的 8dp padding。
	 */
	var alignToSelectedItem: Boolean = true

	private var mMeasuredWidth = 0

	@JvmOverloads
	constructor(
		context: Context,
		attrs: AttributeSet? = null,
		defStyleAttr: Int = R.styleable.SimpleMenuPreference_android_popupMenuStyle
	) : this(context, attrs, defStyleAttr, R.style.Widget_Preference_SimpleMenuPreference_PopupMenu)

	init {
		isFocusable = true
		isOutsideTouchable = false
		context.withStyledAttributes(
				attrs, R.styleable.SimpleMenuPopup, defStyleAttr, defStyleRes
		) {
			elevations[POPUP_MENU] =
				getDimension(R.styleable.SimpleMenuPopup_listElevation, 4f).toInt()
			elevations[DIALOG] =
				getDimension(R.styleable.SimpleMenuPopup_dialogElevation, 48f).toInt()
			margins[POPUP_MENU][HORIZONTAL] =
				getDimension(R.styleable.SimpleMenuPopup_listMarginHorizontal, 0f).toInt()
			margins[POPUP_MENU][VERTICAL] =
				getDimension(R.styleable.SimpleMenuPopup_listMarginVertical, 0f).toInt()
			margins[DIALOG][HORIZONTAL] =
				getDimension(R.styleable.SimpleMenuPopup_dialogMarginHorizontal, 0f).toInt()
			margins[DIALOG][VERTICAL] =
				getDimension(R.styleable.SimpleMenuPopup_dialogMarginVertical, 0f).toInt()
			listPaddings[POPUP_MENU][HORIZONTAL] =
				getDimension(R.styleable.SimpleMenuPopup_listItemPadding, 0f).toInt()
			listPaddings[DIALOG][HORIZONTAL] =
				getDimension(R.styleable.SimpleMenuPopup_dialogItemPadding, 0f).toInt()
			dialogMaxWidth = getDimension(R.styleable.SimpleMenuPopup_dialogMaxWidth, 0f).toInt()
			unit = getDimension(R.styleable.SimpleMenuPopup_unit, 0f).toInt()
			maxUnits = getInteger(R.styleable.SimpleMenuPopup_maxUnits, 0)
			mListBinding = SimpleMenuListBinding.inflate(LayoutInflater.from(context))
			mList = mListBinding.root.apply {
				isFocusable = true
				layoutManager = LinearLayoutManager(context)
				itemAnimator = null
				outlineProvider = object : ViewOutlineProvider() {
					override fun getOutline(view: View?, outline: Outline) {
						this@SimpleMenuPopupWindow.background.getOutline(outline)
					}
				}
				clipToOutline = true
				adapter = mAdapter
			}

			setContentView(mList)

		}

		itemHeight = (context.resources.displayMetrics.density * 48).roundToInt()
		listPaddings[DIALOG][VERTICAL] = (context.resources.displayMetrics.density * 8).roundToInt()
		listPaddings[POPUP_MENU][VERTICAL] = listPaddings[DIALOG][VERTICAL]
	}

	val contentView: RecyclerView
		get() = super.contentView as RecyclerView

	override fun getBackground(): CustomBoundsDrawable {
		val background = super.getBackground()
		if (background != null && background !is CustomBoundsDrawable) {
			setBackgroundDrawable(background)
		}
		return super.getBackground() as CustomBoundsDrawable
	}

	override fun setBackgroundDrawable(background: Drawable) {
		var background = background

		if (background !is CustomBoundsDrawable) {
			background = CustomBoundsDrawable(background)
		}
		super.setBackgroundDrawable(background)
	}

	/**
	 * Show the PopupWindow
	 * 
	 * @param anchor      View that will be used to calc the position of windows
	 * @param container   View that will be used to calc the position of windows
	 * @param extraMargin extra margin start
	 */
	fun show(anchor: View, container: View, extraMargin: Int) {
		val maxMaxWidth = container.width - margins[POPUP_MENU][HORIZONTAL] * 2
		val measuredWidth = measureWidth(maxMaxWidth, entries)
		if (measuredWidth == -1) {
			mode = DIALOG
		} else if (measuredWidth != 0) {
			mode = POPUP_MENU

			mMeasuredWidth = measuredWidth
		}

		mAdapter.notifyItemRangeChanged(0, mAdapter.itemCount)

		// clear last bounds
		val zeroRect = Rect()
		background.setCustomBounds(zeroRect)
		contentView.invalidateOutline()

		if (mode == POPUP_MENU) {
			showPopupMenu(anchor, container, mMeasuredWidth, extraMargin)

			if (SimpleMenuPreference.isLightFixEnabled) {
				mList.post { resetLightCenterForPopupWindow(this@SimpleMenuPopupWindow) }
			}
		} else {
			showDialog(anchor, container)
		}
	}

	/**
	 * Show popup window in dialog mode
	 * 
	 * @param parent    a parent view to get the [View.getWindowToken] token from
	 * @param container Container view that holds preference list, also used to calc width
	 */
	private fun showDialog(parent: View?, container: View) {
		val index = max(0, this.selectedItemIndex)

		contentView.overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
		contentView.scrollToPosition(index)

		width = min(dialogMaxWidth, container.width - margins[DIALOG][HORIZONTAL] * 2)
		height = ViewGroup.LayoutParams.WRAP_CONTENT
		animationStyle = R.style.Animation_Preference_SimpleMenuCenter
		elevation = elevations[DIALOG].toFloat()

		super.showAtLocation(parent, Gravity.CENTER_VERTICAL, 0, 0)

		contentView.post {
			// disable over scroll when no scroll
			val lm = contentView.layoutManager as LinearLayoutManager
			if (lm.findFirstCompletelyVisibleItemPosition() == 0 && lm.findLastCompletelyVisibleItemPosition() == mAdapter.itemCount - 1) {
				contentView.overScrollMode = View.OVER_SCROLL_NEVER
			}

			val width = contentView.width
			val height = contentView.height
			val start = Rect(width / 2, height / 2, width / 2, height / 2)
			startEnterAnimation(
					getBackground(),
					contentView,
					width,
					height,
					width / 2,
					height / 2,
					start,
					itemHeight,
					elevations[DIALOG] / 4,
					index
			)
		}
	}

	/**
	 * Show popup window in popup mode
	 * 
	 * @param anchor    View that will be used to calc the position of the window
	 * @param container Container view that holds preference list, also used to calc width
	 * @param width     Measured width of this window
	 */
	private fun showPopupMenu(anchor: View, container: View, width: Int, extraMargin: Int) {
		val rtl = container.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL

		val hasSelection = selectedItemIndex in entries.indices
		val index = if (hasSelection) selectedItemIndex else 0
		val count = entries.size

		val location = IntArray(2)
		container.getLocationInWindow(location)

		// 用窗口坐标计算锚点相对 container 内容区的位置，锚点不必是 container 的直接子 View
		val anchorLocation = IntArray(2)
		anchor.getLocationInWindow(anchorLocation)
		val anchorTop = anchorLocation[1] - location[1] - container.paddingTop
		val anchorHeight = anchor.height
		val measuredHeight = itemHeight * count + listPaddings[POPUP_MENU][VERTICAL] * 2

		val containerTopInWindow = location[1] + container.paddingTop
		val containerHeight = container.height - container.paddingTop - container.paddingBottom

		// edge-to-edge 下窗口覆盖整块屏幕（含状态栏/导航栏区域），container 的边缘可能伸进系统栏
		// 之下，菜单会被钳到屏幕最底端、被导航栏遮住。rootWindowInsets 返回的本来就是窗口坐标系，
		// 直接用它把可显示范围收缩到系统栏内侧；container 未伸进系统栏时 max/min 不生效，
		// 可见范围仍是 container 自身，与原有行为一致。
		val insets = container.rootWindowInsets?.let {
				WindowInsetsCompat.toWindowInsetsCompat(it).getInsets(
						WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime()
				)
			} ?: Insets.NONE
		val visibleTop = max(containerTopInWindow, insets.top)
		val visibleBottom =
			min(containerTopInWindow + containerHeight, container.rootView.height - insets.bottom)
		val visibleHeight = (visibleBottom - visibleTop).coerceAtLeast(0)

		var y: Int

		val height: Int
		val elevation: Int = this.elevations[POPUP_MENU]
		// 菜单边缘对齐锚点。listItemPadding 只作用于条目内部文字留白，不参与窗口定位，
		// 否则菜单会整体向锚点一侧额外偏移一个 padding 的宽度。
		var centerX = if (rtl) location[0] + extraMargin - width
		else location[0] + extraMargin
		// 横向钳制在容器内，避免靠边的锚点把菜单推出屏幕
		centerX = centerX
			.coerceAtMost(location[0] + container.width - width - margins[POPUP_MENU][HORIZONTAL])
			.coerceAtLeast(location[0] + margins[POPUP_MENU][HORIZONTAL])
		val centerY: Int
		val animItemHeight = itemHeight + listPaddings[POPUP_MENU][VERTICAL] * 2
		val animStartRect: Rect?

		if (measuredHeight > visibleHeight) {
			// too high, use scroll
			y = visibleTop + margins[POPUP_MENU][VERTICAL]

			// scroll to select item; 无选中项时停在列表顶部
			val scroll =
				if (hasSelection) ((itemHeight * index - anchorTop) + listPaddings[POPUP_MENU][VERTICAL] + margins[POPUP_MENU][VERTICAL] - anchorHeight / 2 + itemHeight / 2)
				else 0

			contentView.post {
				contentView.scrollBy(0, -measuredHeight) // to top
				contentView.scrollBy(0, scroll)
			}
			contentView.overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS

			// 至少保留一个条目的高度，避免可见区域过小时出现非正高度
			height = (visibleHeight - margins[POPUP_MENU][VERTICAL] * 2).coerceAtLeast(itemHeight)

			centerY = itemHeight * index
		} else {
			y = if (alignToSelectedItem && hasSelection) {
				// calc align to selected
				(containerTopInWindow + anchorTop + anchorHeight / 2 - itemHeight / 2 - listPaddings[POPUP_MENU][VERTICAL] - index * itemHeight)
			} else {
				// align to anchor bottom, menu's own 8dp top padding keeps the gap
				containerTopInWindow + anchorTop + anchorHeight
			}

			// make sure window is in parent view
			val maxY = visibleBottom - measuredHeight - margins[POPUP_MENU][VERTICAL]
			y = min(y, maxY)

			val minY = visibleTop + margins[POPUP_MENU][VERTICAL]
			y = max(y, minY)

			contentView.setOverScrollMode(View.OVER_SCROLL_NEVER)

			height = measuredHeight

			// center of selected item; 无选中项时从菜单顶边（紧贴锚点下方）展开动画
			centerY =
				if (hasSelection) (listPaddings[POPUP_MENU][VERTICAL] + index * itemHeight + itemHeight * 0.5).toInt()
				else 0
		}

		setWidth(width)
		setHeight(height)
		setElevation(elevation.toFloat())
		animationStyle = R.style.Animation_Preference_SimpleMenuCenter

		enterTransition = null
		exitTransition = null

		super.showAtLocation(anchor, Gravity.NO_GRAVITY, centerX, y)

		val startTop = centerY - (itemHeight * 0.2).toInt()
		val startBottom = centerY + (itemHeight * 0.2).toInt()
		val startLeft: Int
		val startRight: Int

		// 背景Drawable坐标系是窗口内0..width，起点矩形必须用窗口内坐标，
		// 展开动画才能从锚点一侧的边缘生长（而不是从右边缘）
		if (!rtl) {
			startLeft = 0
			startRight = unit
		} else {
			startLeft = width - unit
			startRight = width
		}

		animStartRect = Rect(startLeft, startTop, startRight, startBottom)

		val animElevation = (elevation * 0.25).roundToInt()

		contentView.post {
			startEnterAnimation(
					getBackground(),
					contentView,
					width,
					height,
					0,
					centerY,
					animStartRect,
					animItemHeight,
					animElevation,
					index
			)
		}
	}

	/**
	 * Request a measurement before next show, call this when entries changed.
	 */
	fun requestMeasure() {
		mRequestMeasure = true
	}

	/**
	 * Measure window width
	 * 
	 * @param maxWidth max width for popup
	 * @param entries  Entries of preference hold this window
	 * @return 0: skip
	 * -1: use dialog
	 * other: measuredWidth
	 */
	private fun measureWidth(maxWidth: Int, entries: Array<CharSequence>): Int {
		// skip if should not measure
		var maxWidth = maxWidth
		val entries = entries.sortedBy { it.length }
		if (!mRequestMeasure) {
			return 0
		}
		mRequestMeasure = false


//		Arrays.sort(
//				entries,
//				Comparator { o1: CharSequence?, o2: CharSequence? -> o2!!.length - o1!!.length })

		var width = 0

		maxWidth = min(unit * maxUnits, maxWidth)

		val bounds = Rect()

		val textPaint: Paint =
			SimpleMenuItemBinding.inflate(LayoutInflater.from(context)).text1.paint

		entries.forEach { chs ->
			textPaint.getTextBounds(chs.toString(), 0, chs.toString().length, bounds)

			width = max(
					width,
					bounds.right + 1 + (listPaddings[POPUP_MENU][HORIZONTAL] * 2 + 1).toFloat()
						.roundToInt()
			)

			// more than one line should use dialog
			if (width > maxWidth || chs.toString().contains("\n")) {
				return -1
			}
		}

		// width is a multiple of a unit
		var w = 0
		while (width > w) {
			w += unit
		}

		return w
	}

	companion object {
		const val POPUP_MENU: Int = 0
		const val DIALOG: Int = 1

		const val HORIZONTAL: Int = 0
		const val VERTICAL: Int = 1
	}
}
