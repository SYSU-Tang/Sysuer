package com.sysu.edu.academic

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil.trim
import com.sysu.edu.databinding.FragmentCourseOutlineBinding
import com.sysu.edu.databinding.ItemCourseOutlineBinding
import com.sysu.edu.view.RecyclerAdapter

class CourseOutlineFragment : Fragment() {
	var data: JSONArray? = null
	var root: View? = null
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View? {
		if (root == null) {
			val binding = FragmentCourseOutlineBinding.inflate(inflater)
			val adp = CourseOutlineAdapter()
			binding.recyclerViewScroll.recyclerView.setLayoutManager(GridLayoutManager(requireContext(), 1))
			binding.recyclerViewScroll.recyclerView.setAdapter(adp)
			binding.fab.setOnClickListener {
				startActivity(Intent(requireContext(), MarkdownViewActivity::class.java).putExtra("content", adp.toMarkdown())
								  .putExtra("title", getString(R.string.course_outline)))
			}
			data?.forEach { e: Any? ->
				if (e != null) adp.add(e as JSONObject)
			}
			root = binding.getRoot()
		}
		return root
	}
	
	override fun setArguments(args: Bundle?) {
		if (args != null) data = JSONArray.parse(args.getString("data"))
		super.setArguments(args)
	}
	
	internal class CourseOutlineAdapter : RecyclerAdapter<JSONObject?>() {
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
			return object :
				RecyclerView.ViewHolder(ItemCourseOutlineBinding.inflate(LayoutInflater.from(parent.context))
											.getRoot()) {}
		}
		
		override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
			ItemCourseOutlineBinding.bind(holder.itemView).apply {
				title.text = "${convert(position, "sectionDesignation")}（${convert(position, "teachingHours")}${
					root.context.getString(R.string.study_hour)
				}）"
				intro.text = root.context.getString(R.string.course_outline_content, convert(position, "teachingMainContent"), convert(position, "courseElements"), convert(position, "keyPoints"))
			}
			super.onBindViewHolder(holder, position)
		}
		
		fun convert(position: Int, key: String?): String {
			return trim(data[position]!!.getString(key)).replace("\n\n", "\n")
		}
		
		fun toMarkdown(): String {
			val md = StringBuilder()
			md.append("|章节|学时|教学内容|育人元素|重点、难点|\n|---|---|---|---|---|\n")
			data.forEach { e: JSONObject? ->
				if (e != null) {
					md.append(trim(e.getString("sectionDesignation")).replace("\n", ">"))
						.append("|")
					md.append(trim(e.getString("teachingHours"))).append("|")
					md.append(trim(e.getString("teachingMainContent")).replace("\n", ""))
						.append("|")
					md.append(trim(e.getString("courseElements")).replace("\n", "")).append("|")
					md.append(trim(e.getString("keyPoints")).replace("\n", "")).append("|\n")
				}
			}
			return "$md"
		}
	}
}
