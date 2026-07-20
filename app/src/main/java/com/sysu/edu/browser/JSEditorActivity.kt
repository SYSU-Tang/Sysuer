package com.sysu.edu.browser

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.browser.data.BrowserRepository
import com.sysu.edu.browser.data.JavaScriptEntity
import com.sysu.edu.browser.data.JsModel
import com.sysu.edu.browser.data.JsModelFactory
import com.sysu.edu.browser.data.ScriptParser.updateEntityByScript
import com.sysu.edu.databinding.ActivityJsEdiitorBinding
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import org.eclipse.tm4e.core.registry.IThemeSource

class JSEditorActivity : BaseActivity() {
	val model: JsModel by lazy {
		ViewModelProvider(this,
		                  JsModelFactory(BrowserRepository(this,
		                                                   lifecycleScope)))[JsModel::class.java]
	}
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val binding = ActivityJsEdiitorBinding.inflate(layoutInflater)
		setContentView(binding.root)
		model.getJs(intent.getLongExtra("id", 0)) { data ->
			if (data != null) {
				var data: JavaScriptEntity = data
				FileProviderRegistry.getInstance().addFileProvider(AssetsFileResolver(assets))
				GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
				binding.editor.setEditorLanguage(TextMateLanguage.create("source.js", true))
				val themeRegistry = ThemeRegistry.getInstance()
				val name = "light" // 主题名称
				val themeAssetsPath = "textmate/$name.json"
				val themeModel = ThemeModel(IThemeSource.fromInputStream(FileProviderRegistry.getInstance()
					                                                         .tryGetInputStream(
						                                                         themeAssetsPath),
				                                                         themeAssetsPath,
				                                                         null), name)
				try {
					themeRegistry.loadTheme(themeModel)
					themeRegistry.setTheme(name)
				} catch (e: Exception) {
					throw RuntimeException(e)
				}
				binding.editor.setColorScheme(TextMateColorScheme.create(themeRegistry))
				menuInflater.inflate(R.menu.editor, binding.toolbar.menu)
				binding.toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
				binding.toolbar.setOnMenuItemClickListener {
					when (it.itemId) {
						R.id.save -> {
							data = updateEntityByScript(data, binding.editor.text.toString())
							model.updateJs(data)
						}
						R.id.redo -> binding.editor.redo()
						R.id.undo -> binding.editor.undo()
					}
					false
				}
				binding.editor.setText(data.script)
			}
		}
	}
}