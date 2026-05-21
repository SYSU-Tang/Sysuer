package com.sysu.edu.home;

import static android.text.TextUtils.isEmpty;
import static com.sysu.edu.api.CommonUtil.trim;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONReader;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.sysu.edu.MainActivity;
import com.sysu.edu.R;
import com.sysu.edu.api.ContextUtil;
import com.sysu.edu.api.Params;
import com.sysu.edu.browser.BrowserActivity;
import com.sysu.edu.databinding.DialogServiceActionBinding;
import com.sysu.edu.databinding.DialogServiceOrderBinding;
import com.sysu.edu.databinding.FragmentServiceBinding;
import com.sysu.edu.databinding.ItemActionChipBinding;
import com.sysu.edu.databinding.ItemServiceBoxBinding;
import com.sysu.edu.view.AdapterListener;
import com.sysu.edu.view.RecyclerAdapter;

import org.commonmark.node.Heading;
import org.commonmark.node.Node;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.Markwon;
import io.noties.markwon.MarkwonSpansFactory;
import io.noties.markwon.MarkwonVisitor;
import io.noties.markwon.core.CoreProps;
import io.noties.markwon.core.spans.LastLineSpacingSpan;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class ServiceFragment extends Fragment {
    
    final ArrayList<JSONObject> list = new ArrayList<>();
    FragmentServiceBinding binding;
    Params params;
    BottomSheetDialog actionDialog;
    HomeCollectionHelper db;
    DialogServiceActionBinding actionBinding;
    BottomSheetDialog orderDialog;
    CollectionAdapter collectionAdapter;
    ItemServiceBoxBinding collectionBinding;
    HomeViewModel viewModel;
    private final CompositeDisposable disposables = new CompositeDisposable();
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (binding == null) {
            binding = FragmentServiceBinding.inflate(inflater);
            params = new Params(this);
            viewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
            initAction(inflater);
            initOrder(inflater);
            initSearch();
            JSONReader reader = JSONReader.of(getResources().openRawResource(R.raw.service), StandardCharsets.UTF_8);
            JSONArray array = reader.readJSONArray();
            reader.close();
            db = new HomeCollectionHelper(requireContext());
            addCollection(inflater);
            IntStream.range(0, array.size()).forEach(i -> {
                JSONObject serviceGroup = array.getJSONObject(i);
                binding.serviceContainer.addView(initBoxWithHashMap(inflater, serviceGroup.getString("name"), serviceGroup.getJSONArray("items")));
            });
        }
        return binding.getRoot();
    }
    
    void initAction(@NonNull LayoutInflater inflater) {
        actionDialog = new BottomSheetDialog(requireContext());
        actionBinding = DialogServiceActionBinding.inflate(inflater);
        actionBinding.order.setOnClickListener(_ -> orderDialog.show());
        actionDialog.setContentView(actionBinding.getRoot());
    }
    
    void initOrder(@NonNull LayoutInflater inflater) {
        Context context = requireContext();
        orderDialog = new BottomSheetDialog(context);
        DialogServiceOrderBinding orderBinding = DialogServiceOrderBinding.inflate(inflater);
        orderBinding.recyclerView.setLayoutManager(new LinearLayoutManager(context));
        collectionAdapter = new CollectionAdapter();
        orderBinding.recyclerView.setAdapter(collectionAdapter);
        orderBinding.confirm.setOnClickListener(_ -> {
            updateService();
            updateServiceCollection();
            orderDialog.dismiss();
        });
        orderDialog.setContentView(orderBinding.getRoot());
        new ItemTouchHelper(new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
            }
            
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder source, @NonNull RecyclerView.ViewHolder target) {
                collectionAdapter.swap(source.getBindingAdapterPosition(), target.getBindingAdapterPosition());
                return true;
            }
            
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            
            }
        }).attachToRecyclerView(orderBinding.recyclerView);
        binding.searchView.setupWithSearchBar(binding.searchBar);
    }
    
    void addCollection(@NonNull LayoutInflater inflater) {
        JSONArray collection = getCollection();
        ViewGroup container = initBoxWithHashMap(inflater, getString(R.string.collect), collection);
        collectionBinding = ItemServiceBoxBinding.bind(container);
        if (collection.isEmpty()) container.setVisibility(View.GONE);
        container.getChildAt(0).setOnClickListener(_ -> orderDialog.show());
        binding.serviceContainer.addView(container, 0);
    }
    
    void updateServiceCollection() {
        JSONArray collection = getCollection();
        if (collection.isEmpty()) collectionBinding.getRoot().setVisibility(View.GONE);
        else {
            collectionBinding.getRoot().setVisibility(View.VISIBLE);
            collectionBinding.serviceBoxItems.removeAllViews();
            addItems(getLayoutInflater(), collection, collectionBinding);
        }
    }
    
    @NonNull
    private JSONArray getCollection() {
        Cursor cursor = db.getWritableDatabase().query("service_collection", null, null, null, null, null, "position ASC");
        JSONArray collection = new JSONArray();
        collectionAdapter.clear();
        while (cursor.moveToNext()) {
            JSONObject serviceJson = JSONObject.parse(cursor.getString(cursor.getColumnIndexOrThrow("serviceJson")));
            collection.add(serviceJson);
            collectionAdapter.add(serviceJson);
        }
        cursor.close();
        return collection;
    }
    
    ViewGroup initBoxWithHashMap(LayoutInflater inflater, String box_title, JSONArray items) {
        ItemServiceBoxBinding binding = ItemServiceBoxBinding.inflate(inflater);
        binding.serviceBoxTitle.setText(box_title);
        addItems(inflater, items, binding);
        return binding.getRoot();
    }
    
    void addItems(LayoutInflater inflater, JSONArray items, ItemServiceBoxBinding binding) {
        IntStream.range(0, items.size()).forEach(index -> {
            JSONObject item = items.getJSONObject(index);
            list.add(item);
            int itemId = item.getIntValue("id");
            ItemActionChipBinding chip = ItemActionChipBinding.inflate(inflater, binding.serviceBoxItems, false);
            View.OnClickListener action = viewModel.actionMap.get(itemId);
            String url = item.getString("url");
            String activity = item.getString("activity");
            chip.getRoot().setOnClickListener(action != null ? action : isEmpty(activity) ? isEmpty(url) ? _ -> params.toast(R.string.undeveloped) : v -> startActivity(new Intent(requireContext(), BrowserActivity.class).setData(Uri.parse(url)), ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), v, "miniapp").toBundle()) : v -> {
                try {
                    Intent intent = new Intent(requireContext(), Class.forName(requireContext().getPackageName() + activity));
                    if (intent.resolveActivity(requireContext().getPackageManager()) != null)
                        startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), v, "miniapp").toBundle());
                } catch (ClassNotFoundException _) {
                    params.toast("未找到对应活动");
                }
            });
            chip.getRoot().setOnLongClickListener(_ -> showActionDialog(item));
            chip.getRoot().setText(item.getString("name"));
            binding.serviceBoxItems.addView(chip.getRoot());
        });
    }
    
    boolean showActionDialog(JSONObject item) {
        int itemId = item.getIntValue("id");
        MutableLiveData<Boolean> isServiceCollected = new MutableLiveData<>(db.isServiceCollected(itemId));
        MutableLiveData<Boolean> isShortcutCollected = new MutableLiveData<>(db.isDashboardShortcutCollected(itemId));
        actionBinding.collect.setText(Boolean.TRUE.equals(isServiceCollected.getValue()) ? R.string.cancel_collect : R.string.collect);
        actionBinding.addToDashboard.setText(Boolean.TRUE.equals(isShortcutCollected.getValue()) ? R.string.cancel_add_shortcut : R.string.add_to_dashboard);
        actionBinding.addToLauncher.setOnClickListener(_ -> {
            if (ShortcutManagerCompat.isRequestPinShortcutSupported(requireContext())) {
                Intent intent = new Intent(requireContext(), MainActivity.class);
                if (item.containsKey("activity")) try {
                    intent = new Intent(requireContext(), Class.forName(requireContext().getPackageName() + item.getString("activity")));
                } catch (ClassNotFoundException _) {
                }
                else if (item.containsKey("url"))
                    intent = new Intent(requireContext(), BrowserActivity.class).setData(Uri.parse(trim(item.getString("url"))));
                ShortcutInfoCompat pinShortcutInfo = new ShortcutInfoCompat.Builder(requireContext(), String.valueOf(itemId))
                        .setShortLabel(item.getString("name"))
                        .setLongLabel(item.getString("name"))
                        .setIcon(IconCompat.createWithResource(requireContext(), R.mipmap.icon))
                        .setIntent(intent.setAction(Intent.ACTION_VIEW))
                        .build();
                ShortcutManagerCompat.requestPinShortcut(requireContext(), pinShortcutInfo, PendingIntent.getBroadcast(requireContext(), /* request code */ 0, ShortcutManagerCompat.createShortcutResultIntent(requireContext(), pinShortcutInfo), /* flags */ PendingIntent.FLAG_IMMUTABLE).getIntentSender());
            } else params.toast(R.string.fail_to_add_shortcut);
        });
        actionBinding.collect.setOnClickListener(_ -> {
            boolean isServiceCollect = Boolean.TRUE.equals(isServiceCollected.getValue());
            if (isServiceCollect) {
                db.deleteService(itemId);
                params.toast(R.string.cancel_collect_success);
            } else {
                db.addService(itemId, item.toJSONString(), collectionAdapter.getItemCount());
                params.toast(R.string.collect_success);
            }
            updateServiceCollection();
            actionBinding.collect.setText(isServiceCollect ? R.string.collect : R.string.cancel_collect);
            isServiceCollected.setValue(!isServiceCollect);
        });
        actionBinding.addToDashboard.setOnClickListener(_ -> {
            boolean isShortcutCollect = Boolean.TRUE.equals(isShortcutCollected.getValue());
            if (isShortcutCollect) {
                db.deleteDashboardShortcut(itemId);
                params.toast(R.string.cancel_add_shortcut_success);
            } else {
                db.addDashboardShortcut(itemId, item.toJSONString(), null);
                params.toast(R.string.add_shortcut_success);
            }
            viewModel.updateDashboardShortcut.setValue(true);
            actionBinding.addToDashboard.setText(isShortcutCollect ? R.string.add_to_dashboard : R.string.cancel_add_shortcut);
            isShortcutCollected.setValue(!isShortcutCollect);
        });
        actionBinding.feedback.setOnClickListener(_ -> startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(String.format("https://github.com/%s/%s/issues/new?title=反馈：服务->%s&labels=bug,crash-report", "SYSU-Tang", "Sysuer", item.getString("name")))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
        actionBinding.openAsUrl.setOnClickListener(_ -> {
            String url = item.getString("url");
            if (!isEmpty(url))
                startActivity(new Intent(requireContext(), BrowserActivity.class).setData(Uri.parse(url)));
        });
        ContextUtil contextUtil = new ContextUtil(requireContext());
        Markwon.builder(requireContext()).usePlugin(new AbstractMarkwonPlugin() {
            @Override
            public void configureSpansFactory(@NonNull MarkwonSpansFactory.Builder builder) {
                super.configureSpansFactory(builder);
                builder.appendFactory(Heading.class, (_, configuration) -> {
                    if (CoreProps.HEADING_LEVEL.require(configuration) == 3)
                        return new ForegroundColorSpan(contextUtil.getColorFromAttr(androidx.appcompat.R.attr.colorPrimary));
                    return null;
                });
                builder.appendFactory(Heading.class, (_, _) -> new LastLineSpacingSpan(24));
            }
            
            @Override
            public void configureVisitor(@NonNull MarkwonVisitor.Builder builder) {
                super.configureVisitor(builder);
                builder.blockHandler(new MarkwonVisitor.BlockHandler() {
                    @Override
                    public void blockStart(@NonNull MarkwonVisitor visitor, @NonNull Node node) {
                    }
                    
                    @Override
                    public void blockEnd(@NonNull MarkwonVisitor visitor, @NonNull Node node) {
                        if (visitor.hasNext(node))
                            visitor.ensureNewLine();
                    }
                });
            }
        }).build().setMarkdown(actionBinding.description, String.format("### %s\n%s\n\n%s", item.getString("name"), item.getString("description"), trim(item.getString("url"))));
        actionDialog.show();
        return true;
    }
    
    void updateService() {
        IntStream.range(0, collectionAdapter.getItemCount()).forEach(i -> {
            collectionAdapter.get(i);
            db.updateServicePosition(collectionAdapter.get(i).getInteger("id"), i);
        });
    }
    
    @Override
    public void onDestroy() {
        disposables.clear();
        super.onDestroy();
    }
    
    void initSearch() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.searchView, (v, insets) -> {
            int left = insets.getInsets(WindowInsetsCompat.Type.systemBars()).left;
            int right = insets.getInsets(WindowInsetsCompat.Type.systemBars()).right;
            int bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            v.setPadding(left, 0, right, bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        binding.searchBar.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                binding.searchBar.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) binding.searchBar.getLayoutParams();
                binding.serviceContainer.setPadding(0, binding.searchBar.getHeight() + params.topMargin + params.bottomMargin, 0, 0);
            }
        });
        binding.sugList.setLayoutManager(new LinearLayoutManager(requireContext()));
        CollectionAdapter adapter = new CollectionAdapter();
        adapter.setListener(new AdapterListener() {
            @Override
            public void onBind(RecyclerView.Adapter<RecyclerView.ViewHolder> adp, RecyclerView.ViewHolder holder, int position) {
                JSONObject item = adapter.get(position);
                View.OnClickListener action = viewModel.actionMap.get(item.getInteger("id"));
                String url = item.getString("url");
                String activity = item.getString("activity");
                holder.itemView.setOnClickListener(action!=null? action : v -> {
                    if (!isEmpty(activity)) try {
                        Intent intent = new Intent(requireContext(), Class.forName(requireContext().getPackageName() + activity));
                        if (intent.resolveActivity(requireContext().getPackageManager()) != null)
                            startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), v, "miniapp").toBundle());
                    } catch (ClassNotFoundException _) {
                        params.toast(R.string.activity_not_found);
                    }
                    else if (!isEmpty(url))
                        startActivity(new Intent(requireContext(), BrowserActivity.class).setData(Uri.parse(url)), ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), v, "miniapp").toBundle());
                    else params.toast(R.string.undeveloped);
                });
            }
            
            @Override
            public void onCreate(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, ViewBinding binding) {
            
            }
        });
        binding.sugList.setAdapter(adapter);
        PublishSubject<Object> objectPublishSubject = PublishSubject.create();
        disposables.add(objectPublishSubject
                .debounce(300, TimeUnit.MILLISECONDS)
                .distinctUntilChanged()
                .observeOn(Schedulers.computation())
                .map(query -> {
                    if (query == null || ((String) query).trim().isEmpty()) return list;
                    String q = ((String) query).trim();
                    return list.stream().filter(item -> (item.getString("name").contains(q) || item.getString("description").contains(q))).sorted(
                            (a, b) -> {
                                boolean aNameMatch = a.getString("name").contains(q);
                                boolean bNameMatch = b.getString("name").contains(q);
                                return (aNameMatch && !bNameMatch) ? -1 : (!aNameMatch && bNameMatch) ? 1 : 0;
                            }
                    ).collect(Collectors.toList());
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(adapter::set, _ -> {
                }));
        binding.searchView.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                objectPublishSubject.onNext(s.toString());
            }
            
            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }
    
    static class CollectionAdapter extends RecyclerAdapter<JSONObject> {
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sug, parent, false)) {
            };
        }
        
        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ((TextView) holder.itemView).setText(get(position).getString("name"));
            super.onBindViewHolder(holder, position);
        }
    }
    
}