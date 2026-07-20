package com.sysu.edu.browser

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.children
import com.alibaba.fastjson2.JSONArray
import com.sysu.edu.R
import com.sysu.edu.browser.data.BrowserRepository
import com.sysu.edu.browser.data.JavaScriptEntity
import com.sysu.edu.browser.data.JsModel
import com.sysu.edu.browser.data.JsModelFactory
import com.sysu.edu.browser.data.ScriptManager.checkForUpdate
import com.sysu.edu.browser.data.ScriptParser.updateScriptByEntity
import com.sysu.edu.preference.EditPreference
import com.sysu.edu.view.EditTextDialog
import kotlinx.coroutines.launch
import rikka.material.preference.MaterialSwitchPreference
import rikka.preference.SimpleMenuPreference

class JSInfoFragment : PreferenceFragmentCompat() {
	var data: JavaScriptEntity? = null
	val model: JsModel by lazy {
		ViewModelProvider(this,
		                  JsModelFactory(BrowserRepository(requireContext(),
		                                                   lifecycleScope)))[JsModel::class.java]
	}
	val dialog: EditTextDialog by lazy { EditTextDialog(requireContext()) }
	override fun onCreatePreferences(savedInstanceState: Bundle?, p1: String?) {
		setPreferencesFromResource(R.xml.js_info, p1)
	}
	
	fun save(onResult: () -> Unit = {}) {
		val entity = data ?: run {
			init()
			return
		}
		with(entity) {
			title = findPreference<EditPreference>("title")?.value
			description = findPreference<EditPreference>("description")?.value
			author = findPreference<EditPreference>("author")?.value
			val excludeLink = JSONArray()
			val matchLink = JSONArray()
			findPreference<PreferenceCategory>("exclude")?.children?.forEach {
				if (it is EditPreference) it.value?.takeIf { it1 -> it1.isNotEmpty() }
					?.let { pattern ->
						excludeLink.add(pattern)
					}
			}
			excludes = excludeLink
			findPreference<PreferenceCategory>("match")?.children?.forEach {
				if (it is EditPreference) it.value?.takeIf { it1 -> it1.isNotEmpty() }
					?.let { pattern ->
						matchLink.add(pattern)
					}
			}
			namespace = findPreference<EditPreference>("namespace")?.value
			matches = matchLink
			state = if (findPreference<MaterialSwitchPreference>("state")?.isChecked == true) 1 else 0
			runAt = findPreference<SimpleMenuPreference>("run")?.value
			updateScriptByEntity(this)
		}
		model.updateJs(entity, onResult)
	}
	
	val jsId: Long by lazy { requireArguments().getLong("id") }
	private fun init() {
		model.getJs(jsId) {
			if (it != null) {
				data = it
				println(it)
				load()
			}
		}
	}
	
	override fun onResume() {
		super.onResume()
		init()
	}
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val navController = findNavController(view)
		requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
		                                                      object : OnBackPressedCallback(true) {
			                                                      override fun handleOnBackPressed() {
				                                                      save {
					                                                      navController.navigateUp()
				                                                      }
			                                                      }
		                                                      })
		findPreference<Preference?>("edit")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
			data?.let { entity ->
				save {
					navController.navigate(R.id.info_to_editor, Bundle().apply {
						putLong("id", entity.id)
					})
				}
			}
			false
		}
		findPreference<Preference?>("save")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
			save()
			false
		}
		findPreference<Preference?>("delete")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
			data?.let { entity ->
				model.deleteJs(entity) {
					navController.navigateUp()
				}
			}
			false
		}
		val dialog = EditTextDialog(requireContext()).apply {
			setTitle(R.string.add)
		}
		findPreference<Preference?>("match_add")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
			dialog.setTitle(R.string.matches)
			dialog.value = ""
			dialog.getDialog().setButton(Dialog.BUTTON_POSITIVE, getString(R.string.add)) { _, _ ->
				dialog.getText().takeIf { it.isNotEmpty() }?.let {
					addMatch(it)
					save()
				}
			}
			dialog.show()
			false
		}
		findPreference<Preference?>("exclude_add")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
			dialog.setTitle(R.string.exclude)
			dialog.value = ""
			dialog.getDialog().setButton(Dialog.BUTTON_POSITIVE, getString(R.string.add)) { _, _ ->
				dialog.getText().takeIf { it.isNotEmpty() }?.let {
					addExclude(it)
					save()
				}
			}
			dialog.show()
			false
		}
		preferenceScreen.children.forEach {
			it.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
				save()
				true
			}
		}
	}
	
	private fun load() {
		val entity = data ?: return
		view?.post {
			if (!isAdded) return@post
			with(entity) {
				findPreference<EditPreference>("title")?.value = title
				findPreference<EditPreference>("description")?.value = description
				findPreference<EditPreference>("author")?.value = author
				findPreference<EditPreference>("namespace")?.value = namespace
				removeAll("match")
				matches.forEach { match ->
					if (match is String) addMatch(match)
				}
				removeAll("exclude")
				excludes.forEach { exclude ->
					if (exclude is String) addExclude(exclude)
				}
				
				findPreference<MaterialSwitchPreference>("state")?.isChecked = (state == 1)
				findPreference<SimpleMenuPreference>("run")?.value = runAt ?: "document-idle"
				findPreference<EditPreference>("version")?.value = version
				findPreference<EditPreference>("namespace")?.value = namespace
				findPreference<EditPreference>("link")?.value = downloadURL
				findPreference<EditPreference>("update")?.value = updateURL
				findPreference<EditPreference>("homepage")?.value = homepage //				findPreference<EditPreference>("icon")?.value = icon
				findPreference<EditPreference>("support")?.value = supportURL
				
				lifecycleScope.launch {
					if (checkForUpdate(entity) != null) {
						findPreference<Preference>("update_check")?.isVisible = true
						findPreference<Preference>("update_check")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
							false
						}
					}
				}
			}
		}
	}
	
	private fun addExclude(exclude: String) {
		if (exclude.isNotEmpty()) {
			val preference = findPreference<PreferenceCategory>("exclude")
			val editPreference = EditPreference(requireContext()).apply {
				this@apply.value = exclude
				this@apply.title = getString(R.string.exclude)
				onPreferenceClickListener = {
					dialog.setTitle(R.string.matches)
					dialog.value = value
					dialog.getDialog()
						.setButton(Dialog.BUTTON_POSITIVE, getString(R.string.confirm)) { _, _ ->
							dialog.getText().takeIf { it.isNotEmpty() && it != value }?.let {
								value = it
							}
						}
					dialog.getDialog()
						.setButton(Dialog.BUTTON_NEUTRAL, getString(R.string.delete)) { _, _ ->
							dialog.getText().takeIf { it.isNotEmpty() }?.let {
								preference?.removePreference(this)
								save()
							}
						}
					dialog.show()
					true
				}
			}
			preference?.addPreference(editPreference)
		}
	}
	
	private fun addMatch(match: String) {
		if (match.isNotEmpty()) {
			val preference = findPreference<PreferenceCategory>("match")
			val editPreference = EditPreference(requireContext()).apply {
				this@apply.value = match
				this@apply.title = getString(R.string.matches)
				onPreferenceClickListener = {
					dialog.setTitle(R.string.matches)
					dialog.value = value
					dialog.getDialog()
						.setButton(Dialog.BUTTON_POSITIVE, getString(R.string.confirm)) { _, _ ->
							dialog.getText().takeIf { it.isNotEmpty() && it != value }?.let {
								value = it
							}
						}
					dialog.getDialog()
						.setButton(Dialog.BUTTON_NEUTRAL, getString(R.string.delete)) { _, _ ->
							preference?.removePreference(this)
							save()
						}
					dialog.show()
					true
				}
			}
			preference?.addPreference(editPreference)
		}
	}
	
	private fun removeAll(key: String) {
		val preference = findPreference<PreferenceCategory>(key)
		val count = preference?.preferenceCount?.takeIf { it > 1 } ?: return
		((count - 1)downTo 1).forEach {
			println(preference.getPreference(it))
			preference.removePreference(preference.getPreference(it))
		}
	}
}
