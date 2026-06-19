package com.sysu.edu.academic

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil.trim
import com.sysu.edu.databinding.ActivityMarkdownViewBinding
import io.noties.markwon.Markwon
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import io.noties.markwon.recycler.MarkwonAdapter
import io.noties.markwon.recycler.table.TableEntry
import io.noties.markwon.recycler.table.TableEntryPlugin
import org.commonmark.ext.gfm.tables.TableBlock

class MarkdownViewActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val content = intent.getStringExtra("content")
		val title = intent.getStringExtra("title")
		val adapter = MarkwonAdapter.builder(R.layout.item_textview, R.id.textView)
			.include(TableBlock::class.java, TableEntry.create { builder: TableEntry.Builder? ->
				builder!!.tableLayout(R.layout.item_table_layout, R.id.table_layout)
					.textLayoutIsRoot(R.layout.item_textview)
			})
			.build()
		val binding = ActivityMarkdownViewBinding.inflate(layoutInflater).apply {
			toolbar.title = title
			toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
			copy.setOnClickListener {
				config.copy(title, content)
				config.toast(R.string.copy_successfully)
			}
			recycler.layoutManager = LinearLayoutManager(this@MarkdownViewActivity)
			recycler.adapter = adapter
		}
		setContentView(binding.getRoot())
		adapter.setMarkdown(Markwon.builder(this)
								.usePlugin(TableEntryPlugin.create(this))
								.usePlugin(SoftBreakAddsNewLinePlugin.create())                                //                .usePlugin(new AbstractMarkwonPlugin() {
								//                    @Override
								//                    public void configure(@NonNull Registry registry) {
								//                        registry.require(MarkwonInlineParserPlugin.class, markwonInlineParserPlugin ->
								//                                markwonInlineParserPlugin.factoryBuilder().addInlineProcessor(new NewLineInlineProcessor()));
								//                        super.configure(registry);
								//                    }
								//                })
								//                .usePlugin(MarkwonInlineParserPlugin.create(factoryBuilder ->
								//                        factoryBuilder.addInlineProcessor(new NewLineInlineProcessor())))
								.build(), trim(content))
	}
}