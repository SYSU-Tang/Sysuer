package com.sysu.edu.browser;

import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.transition.MaterialContainerTransform;
import com.sysu.edu.R;
import com.sysu.edu.databinding.FragmentRecyclerFabBinding;
import com.sysu.edu.view.AdapterListener;

import java.util.Map;
import java.util.Objects;

public class JSListFragment extends Fragment {
    
    BrowserHelper db;
    FragmentRecyclerFabBinding binding;
    private BrowserActivity.JSAdapter jsAdapter;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentRecyclerFabBinding.inflate(getLayoutInflater());
        db = new BrowserHelper(requireContext());
        jsAdapter = new BrowserActivity.JSAdapter();
        MaterialToolbar toolbar = requireActivity().findViewById(R.id.toolbar);
        toolbar.getMenu().setGroupVisible(R.id.editor_group, false);
        jsAdapter.setListener(new AdapterListener() {
            @Override
            public void onBind(@NonNull RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, @NonNull RecyclerView.ViewHolder holder, int position) {
                holder.itemView.setOnClickListener(v -> {
                    v.setTransitionName("script");
                    Bundle bundle = new Bundle();
                    bundle.putString("item", jsAdapter.get(position).toString());
                    Navigation.findNavController(binding.getRoot()).navigate(R.id.list_to_info, bundle, null, new FragmentNavigator.Extras(Map.of(v, "script")));
                });
                holder.itemView.setOnLongClickListener(v -> {
                    PopupMenu pop = new PopupMenu(requireContext(), v);
                    pop.getMenuInflater().inflate(R.menu.js_item_menu, pop.getMenu());
                    pop.show();
                    pop.getMenu().findItem(R.id.ban).setTitle(jsAdapter.get(position).getInteger("state") == 1 ? R.string.disable : R.string.enable);
                    pop.setOnMenuItemClickListener(item -> {
                        if (item.getItemId() == R.id.edit) {
                            v.performClick();
                            return true;
                        } else if (item.getItemId() == R.id.delete) {
                            db.getWritableDatabase().delete("js", "id=?", new String[]{String.valueOf(jsAdapter.get(position).getLong("id"))});
                            jsAdapter.remove(position);
                            return true;
                        } else if (item.getItemId() == R.id.ban) {
                            ContentValues value = new ContentValues();
                            int state = 1 - jsAdapter.get(position).getInteger("state");
                            value.put("state", state);
                            db.getWritableDatabase().update("js", value, "id=?", new String[]{String.valueOf(jsAdapter.get(position).getLong("id"))});
                            jsAdapter.get(position).fluentPut("state", state);
                            jsAdapter.notifyItemChanged(position);
                            return true;
                        }
                        return false;
                    });
                    return false;
                });
                holder.itemView.setAlpha(jsAdapter.get(position).getInteger("state") == 1 ? 1f : 0.5f);
            }
            
            @Override
            public void onCreate(@NonNull RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, ViewBinding binding) {
            
            }
        });
        binding.recyclerViewScroll.getRoot().setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewScroll.getRoot().setAdapter(jsAdapter);
        
        getJSList();
        binding.fab.setIconResource(R.drawable.add);
        binding.fab.setText(R.string.add);
        binding.fab.setContentDescription(getString(R.string.add));
        binding.fab.setOnClickListener(_ -> add());
        binding.fab.setTransitionName("miniapp");
        return binding.getRoot();
    }
    
    void getJSList() {
        Cursor cursor = db.getReadableDatabase().query("js", null, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                jsAdapter.add(new JSONObject().fluentPut("title", cursor.getString(cursor.getColumnIndexOrThrow("title")))
                        .fluentPut("description", cursor.getString(cursor.getColumnIndexOrThrow("description")))
                        .fluentPut("matches", cursor.getString(cursor.getColumnIndexOrThrow("matches")))
                        .fluentPut("state", cursor.getInt(cursor.getColumnIndexOrThrow("state")))
                        .fluentPut("author", cursor.getString(cursor.getColumnIndexOrThrow("author")))
                        .fluentPut("id", cursor.getInt(cursor.getColumnIndexOrThrow("id")))
                        .fluentPut("run", cursor.getString(cursor.getColumnIndexOrThrow("run")))
                        .fluentPut("script", cursor.getString(cursor.getColumnIndexOrThrow("script"))));
            } while (cursor.moveToNext());
        }
        cursor.close();
    }
    
    
    void add() {
        ContentValues values = new ContentValues();
        values.put("title", "新脚本");
        values.put("description", "");
        values.put("matches", "[]");
        values.put("author", "");
        values.put("script", "");
        long id = db.getWritableDatabase().insert("js", null, values);
        Bundle bundle = new Bundle();
        bundle.putString("item", JSONObject.of("title", "新脚本", "description", "", "matches", "[]", "author", "", "run", 0, "script", "", "state", 1, "id", id).toString());
        Navigation.findNavController(binding.getRoot()).navigate(R.id.list_to_info, bundle, null, new FragmentNavigator.Extras(Map.of(binding.fab, "miniapp")));
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MaterialContainerTransform transition = new MaterialContainerTransform();
        transition.setScrimColor(Color.TRANSPARENT);
        transition.setAllContainerColors(requireContext().getColor(com.google.android.material.R.color.design_default_color_surface));
        setSharedElementEnterTransition(transition);
        setSharedElementReturnTransition(transition);
        if (Objects.equals(requireActivity().getIntent().getStringExtra("operation"), "add")) {
            add();
            requireActivity().getIntent().removeExtra("operation");
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        jsAdapter.clear();
        getJSList();
    }
}