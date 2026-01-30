package com.sysu.edu.academic;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.ItemCardBinding;
import com.sysu.edu.databinding.RecyclerViewBinding;
import com.sysu.edu.databinding.RecyclerViewScrollBinding;
import com.sysu.edu.databinding.TwoColumnBinding;

import java.util.ArrayList;
import java.util.List;

public class StaggeredFragment extends Fragment {

    public static RecyclerViewScrollBinding binding;
    final MutableLiveData<Integer> orientation = new MutableLiveData<>(StaggeredGridLayoutManager.VERTICAL);
    final MutableLiveData<Runnable> scrollBottom = new MutableLiveData<>();
    final MutableLiveData<Boolean> nestedScrollingEnabled = new MutableLiveData<>(true);
    final MutableLiveData<Boolean> hideNull = new MutableLiveData<>(false);
    final MutableLiveData<StaggeredListener> staggeredListener = new MutableLiveData<>();
    public int position;
    protected Params params;
    protected StaggeredAdapter staggeredAdapter;
    StaggeredGridLayoutManager lm;

    public static StaggeredFragment newInstance(int position) {
        StaggeredFragment s = new StaggeredFragment();
        s.position = position;
        return s;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        params = new Params(requireActivity());
        binding = RecyclerViewScrollBinding.inflate(inflater);
        lm = new StaggeredGridLayoutManager(params.getColumn(), StaggeredGridLayoutManager.VERTICAL);
        binding.recyclerView.setLayoutManager(lm);
        if (staggeredAdapter == null) {
            staggeredAdapter = new StaggeredAdapter(requireContext());
        }
        orientation.observe(getViewLifecycleOwner(), o -> {
            if (o != null) {
                lm.setOrientation(o);
            }
        });
        scrollBottom.observe(getViewLifecycleOwner(), runnable -> {
            if (runnable != null) {
                binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(@NonNull RecyclerView v, int dx, int dy) {
                        if (!v.canScrollVertically(1) && dy > 0) {
                            runnable.run();
                        }
                    }
                });
            }
        });
        staggeredListener.observe(getViewLifecycleOwner(), v -> staggeredAdapter.setListener(v));
        hideNull.observe(getViewLifecycleOwner(), b -> {
            if (b != null) {
                staggeredAdapter.setHideNull(b);
            }
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

    public void setListener(StaggeredListener v) {
        staggeredListener.setValue(v);
    }

    public void add(String title, @Nullable Integer icon, List<String> keys, List<String> values) {
        staggeredAdapter.add(title, keys, values, icon);
    }

    public void add(String title, List<String> keys, List<String> values) {
        add(title, null, keys, values);
    }

    public void add(Context context, String title, Integer icon, List<String> keys, List<String> values) {
        if (staggeredAdapter == null) {
            staggeredAdapter = new StaggeredAdapter(context);
        }
        add(title, icon, keys, values);
    }

    public void add(Context context, String title, List<String> keys, List<String> values) {
        add(context, title, null, keys, values);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        lm.setSpanCount(params.getColumn());
    }

    public void clear() {
        staggeredAdapter.clear();
    }

    public static class TwoColumnsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        final boolean hideNull;
        public List<String> value;
        List<String> key;
        StaggeredListener rowListener;
        Integer itemCount;

        public TwoColumnsAdapter(List<String> data, List<String> value, boolean hideNull) {
            super();
            this.key = data;
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
            notifyItemInserted(getItemCount());
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TwoColumnBinding twoColumnBinding = TwoColumnBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            if (rowListener != null) {
                rowListener.onCreate(TwoColumnsAdapter.this, twoColumnBinding);
            }
            return new RecyclerView.ViewHolder(twoColumnBinding.getRoot()) {
            };
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            TwoColumnBinding b = TwoColumnBinding.bind(holder.itemView);
            b.key.setText(key.get(position));
            holder.itemView.setOnClickListener(v -> {
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
                rowListener.onBind(TwoColumnsAdapter.this, holder, position);
            }
        }

        public void setListener(StaggeredListener listener) {
            this.rowListener = listener;
        }

        @Override
        public int getItemCount() {
            return itemCount == null ? key.size() : itemCount;
        }

        public void setItemCount(Integer count) {
            itemCount = count;
        }
    }

    public static class StaggeredAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        public final ArrayList<String> titles = new ArrayList<>();
        final Context context;
        final ArrayList<List<String>> keys = new ArrayList<>();
        final ArrayList<Integer> icons = new ArrayList<>();
        final ArrayList<List<String>> values = new ArrayList<>();
        final ArrayList<TwoColumnsAdapter> twoColumnsAdapters = new ArrayList<>();

        StaggeredListener staggeredListener;
        boolean hideNull;

        public StaggeredAdapter(Context c) {
            super();
            this.context = c;
            this.hideNull = false;
        }

        public void setHideNull(boolean hideNull) {
            this.hideNull = hideNull;
        }

        public void setListener(StaggeredListener listener) {
            this.staggeredListener = listener;
        }

        public void add(String title, List<String> keys, List<String> values, Integer icon) {
            titles.add(title);
            this.icons.add(icon);
            this.keys.add(keys);
            this.values.add(values);
            notifyItemInserted(getItemCount());
        }

        /*public void add(String key,String value){
            twoColumnsAdapter.add(key,value);
        }*/

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
            ItemCardBinding item = ItemCardBinding.inflate(LayoutInflater.from(context), parent, false);
            RecyclerView list = RecyclerViewBinding.inflate(LayoutInflater.from(context), item.getRoot(), false).getRoot();
            list.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
            list.setNestedScrollingEnabled(false);
            item.card.addView(list);
            if (staggeredListener != null) {
                staggeredListener.onCreate(this, item);
            }
            return new RecyclerView.ViewHolder(item.getRoot()) {
            };
        }

        /* public List<String> getKeys(int pos) {
             return keys.get(pos);
         }
         public List<String> getValues(int pos) {
             return values.get(pos);
         }*/
        /*public void addRow(int pos, List<String> keys, List<String> values) {
            this.keys.get(pos).addAll(keys);
            this.values.get(pos).addAll(values);
        }*/

        public TwoColumnsAdapter getTwoColumnsAdapter(int pos) {
            return twoColumnsAdapters.get(pos);
        }

        public void addRow(int pos, String keys, String values) {
            if (pos < getItemCount()) {
                getTwoColumnsAdapter(pos).add(keys, values);
                notifyItemChanged(pos);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ItemCardBinding item = ItemCardBinding.bind(holder.itemView);
            if (icons.get(position) != null) {
                item.title.setCompoundDrawablePadding(new Params((FragmentActivity) context).dpToPx(8));
                Drawable icon = AppCompatResources.getDrawable(context, icons.get(position));
                if (icon != null) {
                    icon.setBounds(0, 0, 72, 72);
                    item.title.setCompoundDrawables(icon, null, null, null);
                }
            }// 设置图标
            item.title.setText(titles.get(position)); // 设置标题
            TwoColumnsAdapter twoColumnsAdapter;
            if (twoColumnsAdapters.size() < position + 1 || (twoColumnsAdapter = twoColumnsAdapters.get(position)) == null) {
                twoColumnsAdapter = new TwoColumnsAdapter(keys.get(position), values.get(position), hideNull);
                twoColumnsAdapters.add(twoColumnsAdapter);
            } else if (twoColumnsAdapters.size() >= position + 1 && (twoColumnsAdapter = twoColumnsAdapters.get(position)) != null) {
                twoColumnsAdapter.setKeyValue(keys.get(position), values.get(position));
            }
            ((RecyclerView) holder.itemView.findViewById(R.id.recycler_view)).setAdapter(twoColumnsAdapter);
            if (staggeredListener != null) {
                staggeredListener.onBind(this, holder, position);
            }
        }

        @Override
        public int getItemCount() {
            return titles.size();
        }
    }
}