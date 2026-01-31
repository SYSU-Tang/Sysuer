package com.sysu.edu.academic;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.ActivityMarkdownViewBinding;

import org.commonmark.ext.gfm.tables.TableBlock;

import io.noties.markwon.Markwon;
import io.noties.markwon.recycler.MarkwonAdapter;
import io.noties.markwon.recycler.table.TableEntry;
import io.noties.markwon.recycler.table.TableEntryPlugin;

public class MarkdownViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMarkdownViewBinding binding = ActivityMarkdownViewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Params params = new Params(this);
        String content = getIntent().getStringExtra("content");
        String title = getIntent().getStringExtra("title");
        binding.toolbar.setTitle(title);
        binding.toolbar.setNavigationOnClickListener(v -> supportFinishAfterTransition());
        binding.copy.setOnClickListener(v -> {
            params.copy(title, content);
            params.toast(R.string.copy_successfully);
        });
        binding.recycler.setLayoutManager(new LinearLayoutManager(this));
        MarkwonAdapter adapter = MarkwonAdapter.builder(R.layout.item_textview, R.id.textView)
                .include(TableBlock.class, TableEntry.create(builder -> builder
                        .tableLayout(R.layout.item_table_layout, R.id.table_layout)
                        .textLayoutIsRoot(R.layout.item_textview)))
                .build();
        binding.recycler.setAdapter(adapter);
        adapter.setMarkdown(Markwon.builder(this)
                .usePlugin(TableEntryPlugin.create(this))
                .build(), content == null ? "" : content);
    }
}