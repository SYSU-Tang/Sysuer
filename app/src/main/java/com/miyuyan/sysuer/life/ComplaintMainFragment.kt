package com.miyuyan.sysuer.life

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.imageview.ShapeableImageView
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.FileManager
import com.miyuyan.sysuer.databinding.FragmentComplaintMainBinding
import com.miyuyan.sysuer.databinding.ItemFileBinding
import com.miyuyan.sysuer.model.XinfangModel
import com.miyuyan.sysuer.view.AdapterListener
import com.miyuyan.sysuer.view.RecyclerAdapter
import okhttp3.MultipartBody

class ComplaintMainFragment : com.miyuyan.sysuer.BaseFragment() {
	val model: XinfangModel by lazy {
		XinfangModel(requireContext())
	}
	
	override fun onCreateView(
		inflater: LayoutInflater,
		container: android.view.ViewGroup?,
		savedInstanceState: android.os.Bundle?,
	                         ): android.view.View {
		super.onCreateView(inflater, container, savedInstanceState)
		val fileAdapter = FileAdapter().apply {
			listener = object : AdapterListener {
				override fun onBind(
					adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>,
					holder: RecyclerView.ViewHolder,
					position: Int,
				                   ) {
					ItemFileBinding.bind(holder.itemView).apply {
						root.setOnClickListener(config.browse("https://${model.host}${
							get(position).getString("path").toUri()
						}"))
						delete.setOnClickListener { remove(position) }
					}
				}
				
				override fun onCreate(
					adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>,
					binding: ViewBinding?,
				                     ) {
				}
			}
		}
		val fileLauncher = registerForActivityResult<Intent?, ActivityResult?>(
			ActivityResultContracts.StartActivityForResult()) { result: ActivityResult? ->
			if (result?.resultCode == Activity.RESULT_OK) {
				result.data?.data?.let {
					uploadAttachment(it)
				}
			}
		}
		val cm = ComplaintModel(model)
		val binding = FragmentComplaintMainBinding.inflate(inflater, container, false).apply {
			captchaImage.setOnClickListener { loadCaptcha(captchaImage) }
			attachments.layoutManager = LinearLayoutManager(requireContext())
			attachments.adapter = fileAdapter
			uploadAttachment.setOnClickListener { pickAttachment(fileLauncher) }
			submit.setOnClickListener {
				cm.submitForm(mutableMapOf("visitorName" to visitorName.editText?.text.toString(),
				                           "phone" to phone.editText?.text.toString(),
				                           "mobileCheckCode" to mobileCheckCode.editText?.text.toString(),
				                           "name" to name.editText?.text.toString(),
				                           "company" to company.editText?.text.toString(),
				                           "description" to description.editText?.text.toString(),
				                           "checkCode" to checkCode.editText?.text.toString()),
				              attachments = JSONArray(fileAdapter.data))
			}
		}
		loadCaptcha(binding.captchaImage)
		model.message.observe(viewLifecycleOwner) { (code, response) ->
			when (code) {
				0 -> {
					if (response.getBoolean("ok")) {
						response.getJSONArray("data").forEach { fileAdapter.add(it as JSONObject) }
					}
				}
				2 -> {
					if (response.getBoolean("ok")) model.contextUtil.toast(
						R.string.submit_successful)
					else model.contextUtil.toast(
						response.getString("msg") ?: getString(R.string.submit_fail))
				}            /*{"msg":"File upload processed successfully","data":[{"ext":"txt","path":"\/uploadfile\/api\/7cbc45dc8c6d42318e23ba4e6a466a39.txt","ownerName":"file","size":0,"mime":"text\/plain","name":"hook.txt"}],"ok":true,"params":{},"timestamp":"Thu Jul 16 21:43:17 CST 2026"}*/
			}
		}
		
		return binding.root
	}
	
	/*
     * 上传附件
     * */
	fun pickAttachment(fileLauncher: ActivityResultLauncher<in Intent?>) {
		fileLauncher.launch(
			Intent(Intent.ACTION_GET_CONTENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"))
	}
	
	fun uploadAttachment(uri: android.net.Uri) {
		val fileRequestBody = FileManager.getAttachmentRequestBody(requireContext(), uri)
		model.request(model.http.generateRequest("https://${model.host}/jsp_api/upload", null, null)
						  .post(MultipartBody.Builder()
									.setType(MultipartBody.FORM)
									.addFormDataPart("file", fileRequestBody.fileName, fileRequestBody.file)
									.build())
						  .build(), 0)
	}
	
	fun loadCaptcha(imageView: ShapeableImageView) {
		Glide.with(requireContext())
			.load("https://${model.host}/servlet/checkcode".toUri())
			.diskCacheStrategy(DiskCacheStrategy.NONE)
			.skipMemoryCache(true)
			.override(300, 120)
			.into(imageView)
	}
	
	internal class FileAdapter : RecyclerAdapter<JSONObject>() {
		override fun onCreateViewHolder(
			parent: android.view.ViewGroup,
			viewType: Int,
		                               ): RecyclerView.ViewHolder {
			return object : RecyclerView.ViewHolder(
				LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)) {}
		}
		
		override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
			val item = get(position)
			val binding = ItemFileBinding.bind(holder.itemView)
			val context = holder.itemView.context
			binding.title.text = item.getString("name", "")
			binding.type.text = item.getString("ext",
			                                   "")            //binding.description.text = item.getString("path", "")
			binding.detailContent.text = "${context.getString(R.string.size)}：${
				item.getString("size", "")
			}|${context.getString(R.string.type)}：${item.getString("mime", "")}"
			super.onBindViewHolder(holder, position)
		}
	}
}