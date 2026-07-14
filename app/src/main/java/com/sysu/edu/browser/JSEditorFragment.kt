package com.sysu.edu.browser

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.transition.MaterialContainerTransform
import com.sysu.edu.databinding.FragmentJsEditorBinding

class JSEditorFragment : Fragment() {
//	var db: BrowserHelper? = null
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View {
		val binding = FragmentJsEditorBinding.inflate(inflater, container, false)
//		db = BrowserHelper(requireContext())
//		val fragment = getChildFragmentManager().findFragmentById(R.id.js_info) as JSInfoFragment?
//		val data = JSONObject.parseObject(requireArguments().getString("item")) //        FileProviderRegistry.getInstance().addFileProvider(new AssetsFileResolver(requireContext().getAssets()));
		//        GrammarRegistry.getInstance().loadGrammars("textmate/languages.json");
		//        binding.editor.setEditorLanguage(TextMateLanguage.create("source.js", true));
		//        var themeRegistry = ThemeRegistry.getInstance();
		//        var name = "light"; // 主题名称
		//        var themeAssetsPath = "textmate/" + name + ".json";
		//        var model = new ThemeModel(IThemeSource.fromInputStream(Objects.requireNonNull(FileProviderRegistry.getInstance().tryGetInputStream(themeAssetsPath)), themeAssetsPath, null), name);
		//        try {
		//            themeRegistry.loadTheme(model);
		//            ThemeRegistry.getInstance().setTheme(name);
		//        } catch (Exception e) {
		//            throw new RuntimeException(e);
		//        }
		//        binding.editor.setColorScheme(TextMateColorScheme.create(themeRegistry));
		//        var value = new ContentValues();
		//        MaterialToolbar toolbar = requireActivity().findViewById(R.id.toolbar);
		//        toolbar.getMenu().setGroupVisible(R.id.editor_group, true);
		//        toolbar.setOnMenuItemClickListener(item -> {
		//            if (item.getItemId() == R.id.save) {
		//                value.dispose();
		//                value.put("script", binding.editor.getText().toString());
		//                if (fragment != null)
		//                    fragment.getData().forEach((k, v) -> {
		//                        if (!isEmpty(v))
		//                            value.put(k, v.toString());
		//                    });
		//                if (data != null)
		//                    db.getWritableDatabase().update("js", value, "id = ?", new String[]{data.getString("id")});
		//            } else if (item.getItemId() == R.id.redo) binding.editor.redo();
		//            else if (item.getItemId() == R.id.undo) binding.editor.undo();
		//            return false;
		//        });
//		if (data != null) {
//			binding.editor.setText(data.getString("script"))
//			fragment?.setData(data)
//		}
		
		return binding.root
	}
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		view.transitionName = "script"
		val transition = MaterialContainerTransform()
		transition.scrimColor = Color.TRANSPARENT
		transition.setAllContainerColors(requireContext().getColor(com.google.android.material.R.color.design_default_color_surface))
		sharedElementEnterTransition = transition
		sharedElementReturnTransition = transition
	}
}