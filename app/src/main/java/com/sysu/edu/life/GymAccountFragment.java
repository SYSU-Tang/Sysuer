package com.sysu.edu.life;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.ItemPreferenceBinding;
import com.sysu.edu.databinding.RecyclerViewScrollBinding;
import com.sysu.edu.todo.info.TitleAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GymAccountFragment extends Fragment {

    final OkHttpClient http = new OkHttpClient.Builder().build();
    GymReservationViewModel viewModel;
    Handler handler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        RecyclerViewScrollBinding binding = RecyclerViewScrollBinding.inflate(inflater, container, false);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        viewModel = new ViewModelProvider(requireActivity()).get(GymReservationViewModel.class);
        ConcatAdapter concatAdapter = new ConcatAdapter(new ConcatAdapter.Config.Builder().setIsolateViewTypes(true).build());
        binding.recyclerView.setAdapter(concatAdapter);
        Params params = new Params(requireActivity());
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (msg.what == -1) {
                    params.toast(R.string.no_wifi_warning);
                    // 处理错误
                } else {
                    if (msg.getData().getBoolean("isJson")) {
                        if (msg.what == 0) {
                            JSONObject json = JSONObject.parseObject(Objects.requireNonNull(msg.getData().getString("data")));

                            String[] keys = {"Type", "Name", "HostKey", "UserId"};
                            PreferenceAdapter preferenceAdapter = new PreferenceAdapter(requireContext());
                            for (int i = 0; i < keys.length; i++) {
                                preferenceAdapter.addItem(getString(List.of(R.string.type, R.string.name, R.string.school_id, R.string.net_id, R.string.sport_credit, R.string.wallet).get(i)), json.getString(keys[i]));
                            }
                            concatAdapter.addAdapter( new TitleAdapter(getContext(), getString(R.string.account)));
                            concatAdapter.addAdapter(preferenceAdapter);

                            String[] cash_keys = {"Credits", "CashWallet"};
                            PreferenceAdapter cashAdapter = new PreferenceAdapter(requireContext());
                            for (int i = 0; i < cash_keys.length; i++) {
                                cashAdapter.addItem(getString(List.of(R.string.sport_credit, R.string.wallet).get(i)), json.getString(cash_keys[i]), R.drawable.money);
                            }
                            concatAdapter.addAdapter( new TitleAdapter(getContext(), getString(R.string.wallet)));
                            concatAdapter.addAdapter(cashAdapter);

                            String[] id_keys = {"validSwimmer", "IsAdmin"};
                            PreferenceAdapter idAdapter = new PreferenceAdapter(requireContext());
                            for (int i = 0; i < id_keys.length; i++) {
                                idAdapter.addItem(getString(List.of(R.string.is_swimmer_valid, R.string.admin).get(i)), json.getBoolean(id_keys[i]) ? getString(R.string.yes) : getString(R.string.no), R.drawable.help);
                            }
                            concatAdapter.addAdapter( new TitleAdapter(getContext(), getString(R.string.other)));
                            concatAdapter.addAdapter(idAdapter);
                        }
                    } else {
                        params.toast(R.string.educational_wifi_warning);
                    }
                }
            }
        };
        getAccount();
        return binding.getRoot();
    }

    void sendRequest(String url, int what) {
        http.newCall(new Request.Builder()
                .url(url)
                .header("Accept", "application/json, text/plain, */*")
                .header("Cookie", viewModel.token)
                .header("Authorization", viewModel.authorization.getValue())
                .header("User-Agent", viewModel.ua)
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message message = new Message();
                message.what = -1;
                handler.sendMessage(message);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message message = new Message();
                message.what = what;
                Bundle data = new Bundle();
                String dataString = response.body().string();
                data.putBoolean("isJson", Objects.requireNonNull(response.header("Content-Type", "")).startsWith("application/json"));
                data.putString("data", dataString);
                message.setData(data);
                handler.sendMessage(message);
            }
        });
    }

    void getAccount() {
        sendRequest("https://gym.sysu.edu.cn/api/Credit/Me", 0);
    }
}

class PreferenceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    final ArrayList<String> titles = new ArrayList<>();
    final ArrayList<String> contents = new ArrayList<>();
    final ArrayList<Integer> icons = new ArrayList<>();

        final Context context;

    public PreferenceAdapter(Context context) {
        super();
        this.context = context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecyclerView.ViewHolder(ItemPreferenceBinding.inflate(LayoutInflater.from(context), parent, false).getRoot()) {
        };
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int pos = holder.getBindingAdapterPosition();
        ItemPreferenceBinding binding = ItemPreferenceBinding.bind(holder.itemView);
        binding.itemTitle.setText(titles.get(pos));
        binding.itemContent.setText(contents.get(pos));
        binding.getRoot().setOnClickListener(v -> {
           // params.toast(titles.get(pos) + ": " + contents.get(pos));
        });
        if (icons.size() > pos && icons.get(pos) != null) {
            binding.itemIcon.setImageResource(icons.get(pos));
        } else {
            binding.itemIcon.setImageResource(R.drawable.account);
        }
        binding.getRoot().updateAppearance(pos, getItemCount());
    }

    void addItem(String title, String content, Integer icon) {
        titles.add(title);
        contents.add(content);
        icons.add(icon);
        notifyItemInserted(titles.size() - 1);
    }

    void addItem(String title, String content) {
        addItem(title, content, null);
    }

    @Override
    public int getItemCount() {
        return titles.size();
    }
}