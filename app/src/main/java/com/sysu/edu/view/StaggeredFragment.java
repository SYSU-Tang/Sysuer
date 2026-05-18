package com.sysu.edu.view;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.sysu.edu.R;
import com.sysu.edu.academic.MarkdownViewActivity;
import com.sysu.edu.api.CommonUtil;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.ItemCardBinding;
import com.sysu.edu.databinding.RecyclerViewBinding;
import com.sysu.edu.databinding.RecyclerViewScrollBinding;
import com.sysu.edu.databinding.TwoColumnBinding;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

public class StaggeredFragment extends Fragment {

    protected final StaggeredAdapter staggeredAdapter = new StaggeredAdapter();
    final MutableLiveData<Integer> orientation = new MutableLiveData<>(StaggeredGridLayoutManager.VERTICAL);
    final MutableLiveData<Runnable> scrollBottom = new MutableLiveData<>();
    final MutableLiveData<Boolean> nestedScrollingEnabled = new MutableLiveData<>(true);
    final MutableLiveData<Boolean> hideNull = new MutableLiveData<>(false);
    final MutableLiveData<AdapterListener> staggeredListener = new MutableLiveData<>();
    public int position;
    protected RecyclerViewScrollBinding binding;
    protected Params params;
    StaggeredGridLayoutManager staggeredGridLayoutManager;

    public static StaggeredFragment newInstance(int position) {
        StaggeredFragment s = new StaggeredFragment();
        s.position = position;
        return s;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        params = new Params(this);
        binding = RecyclerViewScrollBinding.inflate(inflater);
        staggeredGridLayoutManager = new StaggeredGridLayoutManager(params.getColumn(), StaggeredGridLayoutManager.VERTICAL);
        binding.recyclerView.setLayoutManager(staggeredGridLayoutManager);
        orientation.observe(getViewLifecycleOwner(), o -> {
            if (o != null) staggeredGridLayoutManager.setOrientation(o);
        });
        scrollBottom.observe(getViewLifecycleOwner(), runnable -> {
            if (runnable != null)
                binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(@NonNull RecyclerView v, int dx, int dy) {
                        if (!v.canScrollVertically(1) && dy > 0) runnable.run();
                    }
                });
        });
        staggeredListener.observe(getViewLifecycleOwner(), staggeredAdapter::setListener);
        hideNull.observe(getViewLifecycleOwner(), b -> {
            if (b != null) staggeredAdapter.setHideNull(b);
        });
        binding.recyclerView.setAdapter(staggeredAdapter);
        nestedScrollingEnabled.observe(getViewLifecycleOwner(), binding.recyclerView::setNestedScrollingEnabled);

        return binding.getRoot();
    }

    public void setOrientation(int o) {
        orientation.setValue(o);
    }

    public void setScrollBottom(Runnable runnable) {
        scrollBottom.setValue(runnable);
    }

    public void setNested(boolean nested) {
        nestedScrollingEnabled.setValue(nested);
    }

    public void setHideNull(boolean hide) {
        hideNull.setValue(hide);
    }

    public void setListener(AdapterListener v) {
        staggeredListener.setValue(v);
    }

    public StaggeredAdapter getStaggeredAdapter() {
        return staggeredAdapter;
    }

    public void add(String title, Integer icon, List<String> keys, List<String> values) {
        staggeredAdapter.add(title, keys, values, icon);
    }

    public void add(String title, List<String> keys, List<String> values) {
        add(title, null, keys, values);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        staggeredGridLayoutManager.setSpanCount(params.getColumn());
    }

    public void clear() {
        staggeredAdapter.clear();
    }

    public String toTable() {
        StringBuilder markdown = new StringBuilder();
        AtomicReference<List<String>> keys = new AtomicReference<>();
        IntStream.range(0, staggeredAdapter.getItemCount()).forEach(i -> {
            if (keys.get() == null || !new HashSet<>(staggeredAdapter.getKeys(i)).containsAll(keys.get())) {
                keys.set(staggeredAdapter.getKeys(i));
                markdown.append("\n").append("序号|").append(String.join("|", keys.get().stream().map(CommonUtil::trim).toArray(String[]::new))).append("\n")
                        .append(":---:|".repeat(keys.get().size() + 1)).append("\n"); // 表头
            }
            markdown.append(i + 1).append("|").append(String.join("|", staggeredAdapter.getValues(i).stream().map(CommonUtil::trim).toArray(String[]::new))).append("\n");
        });
        return markdown.toString();
    }

    public void setViewTableMenu(MaterialToolbar toolbar) {
        toolbar.getMenu().add(R.string.export).setIcon(R.drawable.export).setOnMenuItemClickListener(_ -> {
            export(toolbar, toolbar.getTitle().toString());
            return false;
        }).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    public void export(View view, String title) {
        startActivity(new Intent(requireContext(), MarkdownViewActivity.class).putExtra("content", toTable()).putExtra("title", title),
                ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), view, "miniapp").toBundle());
    }

    public static class TwoColumnsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        final boolean hideNull;
        public List<String> value;
        List<String> key;
        AdapterListener rowListener;
        Integer itemCount;

        public TwoColumnsAdapter(List<String> data, List<String> value, boolean hideNull) {
            key = data;
            this.hideNull = hideNull;
            this.value = value;
        }

        public void setValue(List<String> value) {
            this.value = value;
            notifyItemRangeChanged(0, getItemCount());
        }

        public void setKey(List<String> key) {
            this.key = key;
            notifyItemRangeChanged(0, getItemCount());
        }

        public void setKeyValue(List<String> key, List<String> value) {
            this.key = key;
            this.value = value;
            notifyItemRangeChanged(0, getItemCount());
        }

        public void add(String key, String value) {
            ArrayList<String> newKey = new ArrayList<>(this.key);
            ArrayList<String> newValue = new ArrayList<>(this.value);
            newKey.add(key);
            newValue.add(value);
            setKeyValue(newKey, newValue);
            notifyItemInserted(getItemCount() - 1);
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TwoColumnBinding twoColumnBinding = TwoColumnBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            if (rowListener != null) {
                rowListener.onCreate(this, twoColumnBinding);
            }
            return new RecyclerView.ViewHolder(twoColumnBinding.getRoot()) {
            };
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            TwoColumnBinding b = TwoColumnBinding.bind(holder.itemView);
            b.key.setText(key.get(position));
            holder.itemView.setOnClickListener(_ -> {
                ClipboardManager clip = (ClipboardManager) b.getRoot().getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                clip.setPrimaryClip(ClipData.newPlainText(key.get(position), value.get(position)));
            });
            if (position < value.size() && value.get(position) != null) {
                b.value.setText(value.get(position));
            } else if (hideNull) {
                holder.itemView.setVisibility(View.GONE);
                holder.itemView.getLayoutParams().height = 0;
            }
            if (rowListener != null) {
                rowListener.onBind(this, holder, position);
            }
        }

        public void setListener(AdapterListener listener) {
            rowListener = listener;
        }

        @Override
        public int getItemCount() {
            return itemCount == null ? key.size() : itemCount;
        }
    }

    public static class StaggeredAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        final ArrayList<String> titles = new ArrayList<>();
        final ArrayList<List<String>> keys = new ArrayList<>();
        final ArrayList<Integer> icons = new ArrayList<>();
        final ArrayList<List<String>> values = new ArrayList<>();
        final ArrayList<TwoColumnsAdapter> twoColumnsAdapters = new ArrayList<>();

        AdapterListener adapterListener;
        boolean hideNull = false;

        public void setHideNull(boolean hideNull) {
            this.hideNull = hideNull;
        }

        public void setListener(AdapterListener listener) {
            adapterListener = listener;
        }

        public void add(String title, List<String> keys, List<String> values, Integer icon) {
            titles.add(title);
            icons.add(icon);
            this.keys.add(keys);
            this.values.add(values);
            notifyItemInserted(getItemCount() - 1);
        }


        public void clear() {
            int tmp = getItemCount();
            titles.clear();
            icons.clear();
            keys.clear();
            values.clear();
            notifyItemRangeRemoved(0, tmp);
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            ItemCardBinding item = ItemCardBinding.inflate(LayoutInflater.from(context), parent, false);
            RecyclerView list = RecyclerViewBinding.inflate(LayoutInflater.from(context), item.getRoot(), false).getRoot();
            list.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
            list.setNestedScrollingEnabled(false);
            item.card.addView(list);
            if (adapterListener != null) adapterListener.onCreate(this, item);
            return new RecyclerView.ViewHolder(item.getRoot()) {
            };
        }

        public List<String> getKeys(int pos) {
            return keys.get(pos);
        }

        public List<String> getValues(int pos) {
            return values.get(pos);
        }

        public TwoColumnsAdapter getTwoColumnsAdapter(int pos) {
            return twoColumnsAdapters.get(pos);
        }

        public void addRow(int pos, String key, String value) {
            if (pos < getItemCount()) {
//                getTwoColumnsAdapter(pos).add(key, value);
                ArrayList<String> newKeys = new ArrayList<>(getKeys(pos));
                newKeys.add(key);
                keys.set(pos, newKeys);
                ArrayList<String> newValues = new ArrayList<>(getValues(pos));
                newValues.add(value);
                values.set(pos, newValues);
                notifyItemChanged(pos);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ItemCardBinding item = ItemCardBinding.bind(holder.itemView);
            Context context = holder.itemView.getContext();
            if (icons.get(position) != null) {
                item.title.setCompoundDrawablePadding(24);
                Drawable icon = AppCompatResources.getDrawable(context, icons.get(position));
                if (icon != null) {
                    icon.setBounds(0, 0, 72, 72);
                    item.title.setCompoundDrawables(icon, null, null, null);
                }
            }// 设置图标
            item.title.setText(titles.get(position)); // 设置标题

            RecyclerView recyclerView = holder.itemView.findViewById(R.id.recycler_view);
            TwoColumnsAdapter twoColumnsAdapter = (TwoColumnsAdapter) recyclerView.getAdapter();
            if (twoColumnsAdapter == null) {
                twoColumnsAdapter = new TwoColumnsAdapter(keys.get(position), values.get(position), hideNull);
                recyclerView.setAdapter(twoColumnsAdapter);
            } else {
                twoColumnsAdapter.setKeyValue(keys.get(position), values.get(position));
            }
            if (twoColumnsAdapters.size() <= position || twoColumnsAdapters.get(position) == null)
                twoColumnsAdapters.add(position, twoColumnsAdapter);

            if (adapterListener != null)
                adapterListener.onBind(this, holder, position);
        }

        @Override
        public int getItemCount() {
            return titles.size();
        }
    }
}