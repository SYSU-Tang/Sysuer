package com.sysu.edu.academic;

import static android.text.TextUtils.isEmpty;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
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

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;
import com.sysu.edu.R;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.DialogEditTextBinding;
import com.sysu.edu.databinding.FragmentQuestionnaireBinding;
import com.sysu.edu.databinding.ItemOptionBinding;
import com.sysu.edu.todo.TitleAdapter;
import com.sysu.edu.view.RecyclerViewHolder;

import java.util.ArrayList;
import java.util.Objects;


public class EvaluationQuestionnaireFragment extends Fragment {
    
    final JSONObject answers = JSONObject.parseObject("{\"pjidlist\":[],\"pjjglist\":[],\"pjzt\": \"2\"}");
    Params params;
    HttpManager http;
    EvaluationViewModel vm;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentQuestionnaireBinding binding = FragmentQuestionnaireBinding.inflate(inflater, container, false);
        params = new Params(this);
        vm = new ViewModelProvider(this).get(EvaluationViewModel.class);
        //StaggeredGridLayoutManager sgm = new StaggeredGridLayoutManager(params.getColumn(), 1);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        ConcatAdapter adp = new ConcatAdapter(new ConcatAdapter.Config.Builder().setIsolateViewTypes(true).build());
        binding.recyclerView.setAdapter(adp);
        params.setCallback(() -> getEvaluation(requireArguments().getString("rwid"),
                requireArguments().getString("wjid"),
                requireArguments().getString("sxz"),
                requireArguments().getString("pjrdm"),
                requireArguments().getString("bpdm"),
                requireArguments().getString("kcdm"),
                requireArguments().getString("rwh"),
                Objects.equals(requireArguments().getString("lsjgzt"), "2") ? "1" : "",
                requireArguments().getString("bpmc")));
        //{"rwid", "wjid","sxz","pjrdm","bpdm","kcdm","rwh"};
        http = new HttpManager(new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == -1) {
                    params.toast(R.string.no_net_connected);
                    params.gotoLogin(vm.authorizationManager.isAccessible() ? TargetUrl.PJXT : TargetUrl.PJXT_WEBVPN);
                } else if (msg.getData().getBoolean("isJSON")) {
                    JSONObject data = JSON.parseObject((String) msg.obj);
                    
                    switch (msg.what) {
                        case 1 -> {
                            if (data.get("code").equals("200")) {
                                data.getJSONObject("result").getJSONArray("assessedObjList").forEach(l ->
                                        ((JSONObject) l).getJSONArray("bpdxList").forEach(list ->
                                        {
                                            JSONObject pjjglist = ((JSONObject) list).clone();
                                            pjjglist.remove("dtjgList");
                                            pjjglist.put("pjxxlist", new JSONArray());
                                            answers.getJSONArray("pjjglist").add(pjjglist);
                                            String bprmc = ((JSONObject) list).getString("bprmc");
                                            // 被评名称
                                            if (!isEmpty(bprmc))
                                                adp.addAdapter(new TitleAdapter(bprmc, 1));
                                            ((JSONObject) list).getJSONArray("dtjgList").forEach(e ->
                                            {
                                                JSONObject pjxxlist = JSONObject.parse(String.format(
                                                        "{\"sjly\": \"1\",\"stlx\": \"1\",\"wjid\": \"%s\",\"wjssrwid\": \"%s\",\"wjstctid\": \"\",\"wjstid\": \"%s\",\"xxdalist\": []}",
                                                        ((JSONObject) list).getString("wjid"),
                                                        ((JSONObject) list).getString("wjssrwid"),
                                                        ((JSONObject) e).getString("tmid")
                                                ));
                                                JSONArray da = ((JSONObject) e).getJSONArray("tmxxda");
                                                pjxxlist.put("xxdalist", da);
                                                pjjglist.getJSONArray("pjxxlist").add(pjxxlist);
                                                
                                                adp.addAdapter(new TitleAdapter(((JSONObject) e).getString("tgmc"))); // 题目标题
                                                
                                                switch (((JSONObject) e).getString("tmlx")) {
                                                    case "1" -> {
                                                        OptionAdapter optionAdapter = new OptionAdapter(requireContext());
                                                        ((JSONObject) e).getJSONArray("tmxxlist").forEach(o -> optionAdapter.add((JSONObject) o));
                                                        optionAdapter.setAnswer(da);
                                                        adp.addAdapter(optionAdapter);
                                                    }
                                                    case "6" -> {
                                                        BlanketAdapter blanketAdapter = new BlanketAdapter(requireContext());
                                                        blanketAdapter.setAnswer(da);
                                                        adp.addAdapter(blanketAdapter);
                                                    }
                                                    case "5" -> {
                                                        RankAdapter rankAdapter = new RankAdapter(requireContext());
                                                        rankAdapter.setAnswer(da);
                                                        adp.addAdapter(rankAdapter);
                                                    }
                                                }
                                            });
                                        }));
                            } else
                                params.gotoLogin(vm.authorizationManager.isAccessible() ? TargetUrl.PJXT : TargetUrl.PJXT_WEBVPN);
                        }
                        case 2 -> {
                            if (data.get("code").equals("200"))
                                params.toast(R.string.save_successfully);
                            else
                                params.toast(String.format("%s：%s", getString(R.string.save_fail), data.getString("msg")));
                        }
                        case 3 -> {
                            if (data.get("code").equals("200"))
                                params.toast(R.string.submit_successfully);
                            else
                                params.toast(String.format("%s：%s", getString(R.string.submit_fail), data.getString("msg")));
                        }
                    }
                } else vm.authorizationManager.isAccessible((String) msg.obj);
            }
        });
        http.setParams(params);
        getEvaluation(requireArguments().getString("rwid"),
                requireArguments().getString("wjid"),
                requireArguments().getString("sxz"),
                requireArguments().getString("pjrdm"),
                requireArguments().getString("bpdm"),
                requireArguments().getString("kcdm"),
                requireArguments().getString("rwh"),
                Objects.equals(requireArguments().getString("lsjgzt"), "2") ? "1" : "",
                requireArguments().getString("bpmc"));
        binding.save.setOnClickListener(_ -> saveEvaluation());
        binding.submit.setOnClickListener(_ -> Snackbar.make(binding.getRoot(), "提交后不可更改", Snackbar.LENGTH_LONG).setAction(R.string.confirm, _ -> submitEvaluation()).show());
        binding.reset.setOnClickListener(_ -> adp.getAdapters().forEach(adapter -> {
            if (adapter instanceof OptionAdapter) ((OptionAdapter) adapter).clearAnswer();
            else if (adapter instanceof RankAdapter) ((RankAdapter) adapter).clearAnswer();
            else if (adapter instanceof BlanketAdapter) ((BlanketAdapter) adapter).clearAnswer();
        }));
        binding.auto.setOnClickListener(_ -> adp.getAdapters().forEach(adapter -> {
            if (adapter instanceof OptionAdapter) ((OptionAdapter) adapter).setLastOption();
            else if (adapter instanceof RankAdapter) ((RankAdapter) adapter).setLastRank();
            else if (adapter instanceof BlanketAdapter) ((BlanketAdapter) adapter).setLastContent();
        }));
        return binding.getRoot();
    }
    
    public void getEvaluation(String rwid, String wjid, String sxz, String pjrdm, String bpdm, String kcdm, String rwh, String pjzt, String bpmc) {
        http.getRequest(String.format(vm.authorizationManager.getBaseUrl() + "evaluationPattern/getQuestionnaireTopic?rwid=%s&wjid=%s&sxz=%s&pjrdm=%s&bpdm=%s&kcdm=%s&rwh=%s&pjzt=%s&bpmc=%s", rwid, wjid, sxz, pjrdm, bpdm, kcdm, rwh, pjzt, bpmc), 1);
    }
    
    public void saveEvaluation() {
        postEvaluation("2", 2);
    }
    
    public void submitEvaluation() {
        postEvaluation("1", 3);
    }
    
    public void postEvaluation(String mode, int what) {
        answers.put("pjzt", mode);
        http.postRequest(vm.authorizationManager.getBaseUrl() + "evaluationPattern/submitSaveEvaluation", answers.toString(), what);
    }
}

class OptionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    final Context context;
    final ArrayList<JSONObject> data = new ArrayList<>();
    int selected = -1;
    String option;
    JSONArray answer;
    
    public OptionAdapter(Context context) {
        this.context = context;
    }
    
    public void setOption(int pos) {
        if (selected != pos) {
            int old = selected;
            selected = pos;
            notifyItemChanged(old);
            notifyItemChanged(selected);
            answer.set(0, data.get(pos).getString("tmxxid"));
        }
    }
    
    public void setLastOption() {
        setOption(data.size() - 1);
    }
    
    public void clearAnswer() {
        answer.clear();
        int old = selected;
        selected = -1;
        option = null;
        notifyItemChanged(old);
    }
    
    public void setAnswer(JSONArray answers) {
        answer = answers;
        option = answers.isEmpty() ? null : answers.getString(0);
        notifyItemRangeChanged(0, getItemCount());
    }
    
    public void add(JSONObject item) {
        data.add(item);
        notifyItemInserted(data.size() - 1);
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecyclerView.ViewHolder(ItemOptionBinding.inflate(LayoutInflater.from(context), parent, false).getRoot()) {
        };
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int pos = holder.getBindingAdapterPosition();
        ItemOptionBinding binding = ItemOptionBinding.bind(holder.itemView);
        binding.getRoot().setOnClickListener(_ -> setOption(pos));
        JSONObject item = data.get(pos);
        if (selected == -1 && Objects.equals(item.getString("tmxxid"), option))
            selected = pos;
        binding.option.setChecked(selected == pos);
        binding.option.setText(item.getString("xxmc"));
        binding.getRoot().updateAppearance(pos, getItemCount());
    }
    
    @Override
    public int getItemCount() {
        return data.size();
    }
    
    @Override
    public int getItemViewType(int position) {
        return 6;
    }
}

class RankAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    final Context context;
    int rank;
    JSONArray answer;
    
    public RankAdapter(Context context) {
        this.context = context;
    }
    
    public void setAnswer(JSONArray answers) {
        answer = answers;
        rank = answers.isEmpty() ? 100 : Integer.parseInt(answers.getString(0));
        notifyItemChanged(0);
    }
    
    public void setLastRank() {
        rank = 100;
        answer.set(0, String.valueOf(rank));
        notifyItemChanged(0);
    }
    
    public void clearAnswer() {
        answer.clear();
        rank = 100;
        notifyItemChanged(0);
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Slider p = new Slider(context);
        p.setValue(rank == 0 ? 100 : rank);
        p.setStepSize(1);
        p.setValueFrom(0);
        p.setValueTo(100);
        p.setLabelBehavior(LabelFormatter.LABEL_FLOATING);
        p.addOnChangeListener((_, value, _) -> answer.set(0, String.valueOf((int) value)));
        return new RecyclerView.ViewHolder(p) {
        };
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    
    }
    
    @Override
    public int getItemCount() {
        return 1;
    }
}

class BlanketAdapter extends RecyclerView.Adapter<RecyclerViewHolder<DialogEditTextBinding>> {
    final Context context;
    String content;
    JSONArray answer;
    
    public BlanketAdapter(Context context) {
        this.context = context;
    }
    
    public void setAnswer(JSONArray answers) {
        this.answer = answers;
        this.content = answers.isEmpty() ? null : answers.getString(0);
        notifyItemChanged(0);
    }
    
    public void setLastContent() {
        /*content = null;
        if (!answer.isEmpty()) {
            answer.remove(0);
            notifyItemChanged(0);
        }*/
    }
    
    @NonNull
    @Override
    public RecyclerViewHolder<DialogEditTextBinding> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecyclerViewHolder<>(DialogEditTextBinding.inflate(LayoutInflater.from(context), parent, false)) {
        };
    }
    
    
    public void setText(String text) {
        this.content = text;
    }
    
    public void clearAnswer() {
        answer.clear();
        content = null;
        notifyItemChanged(0);
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerViewHolder<DialogEditTextBinding> holder, int position) {
        DialogEditTextBinding binding = holder.binding;
        binding.editLayout.setHint(R.string.please_enter_content);
        if (!isEmpty(content)) {
            answer.set(0, content);
            binding.edit.setText(content);
        }
        binding.edit.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
            }
            
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isEmpty(s)) {
                    if (!answer.isEmpty())
                        answer.remove(0);
                } else answer.set(0, s.toString());
            }
        });
        binding.executePendingBindings();
    }
    
    @Override
    public int getItemCount() {
        return 1;
    }
    
    @Override
    public int getItemViewType(int position) {
        return 5;
    }
}