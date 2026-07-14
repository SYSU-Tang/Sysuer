package com.sysu.edu.browser;

import android.content.ContentValues;
import android.os.Bundle;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.BaseActivity;
import com.sysu.edu.R;
import com.sysu.edu.databinding.ActivityJsEdiitorBinding;

import org.eclipse.tm4e.core.registry.IThemeSource;

import java.util.Objects;

import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme;
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage;
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel;
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver;

public class JSEditorActivity extends BaseActivity {
    
    BrowserHelper db;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityJsEdiitorBinding binding = ActivityJsEdiitorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        JSONObject data = JSONObject.parseObject(getIntent().getStringExtra("item"));
        if (data != null) {
            db = new BrowserHelper(this);
            FileProviderRegistry.getInstance().addFileProvider(new AssetsFileResolver(getAssets()));
            GrammarRegistry.getInstance().loadGrammars("textmate/languages.json");
            binding.editor.setEditorLanguage(TextMateLanguage.create("source.js", true));
            var themeRegistry = ThemeRegistry.getInstance();
            var name = "light"; // 主题名称
            var themeAssetsPath = "textmate/" + name + ".json";
            var model = new ThemeModel(IThemeSource.fromInputStream(Objects.requireNonNull(FileProviderRegistry.getInstance().tryGetInputStream(themeAssetsPath)), themeAssetsPath, null), name);
            try {
                themeRegistry.loadTheme(model);
                themeRegistry.setTheme(name);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            binding.editor.setColorScheme(TextMateColorScheme.create(themeRegistry));
            getMenuInflater().inflate(R.menu.editor, binding.toolbar.getMenu());
            binding.toolbar.setNavigationOnClickListener(_ -> supportFinishAfterTransition());
            var value = new ContentValues();
            binding.toolbar.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.save) {
                    value.clear();
                    value.put("script", binding.editor.getText().toString());
                    db.getWritableDatabase().update("js", value, "id = ?", new String[]{data.getString("id")});
                } else if (item.getItemId() == R.id.redo) binding.editor.redo();
                else if (item.getItemId() == R.id.undo) binding.editor.undo();
                return false;
            });
            binding.editor.setText(data.getString("script"));
        }
    }
}