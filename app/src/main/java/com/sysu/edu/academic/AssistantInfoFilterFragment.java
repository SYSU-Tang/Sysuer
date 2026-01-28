package com.sysu.edu.academic;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.navigation.Navigation;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.FragmentAssistantInfoFilterBinding;
import com.sysu.edu.databinding.ItemFilterChipBinding;

public class AssistantInfoFilterFragment extends Fragment {

    HttpManager http;
    MutableLiveData<String> term = new MutableLiveData<>();
    MutableLiveData<String> campus = new MutableLiveData<>();
    FragmentAssistantInfoFilterBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        if (binding == null) {
            binding = FragmentAssistantInfoFilterBinding.inflate(inflater, container, false);
            PopupMenu pop = new PopupMenu(requireContext(), binding.term.getRoot());
            Params params = new Params(requireActivity());
            params.setCallback(this, this::getTerms);
            binding.term.itemTitle.setText(R.string.term);
            binding.term.itemIcon.setImageResource(R.drawable.calendar);
            binding.term.getRoot().setOnClickListener(view -> pop.show());
            binding.filter.setOnClickListener(view -> {
                Bundle data = new Bundle();
                data.putString("term", term.getValue());
                data.putString("campus", campus.getValue());
                data.putString("courseNumber", String.valueOf(binding.courseNumber.getText()));
                data.putString("courseName", String.valueOf(binding.courseName.getText()));
                data.putString("teacherName", String.valueOf(binding.teacher.getText()));
                Navigation.findNavController(binding.getRoot()).navigate(R.id.filter_to_result, data);
            });
            term.observe(requireActivity(), acadYearSemester -> {
                if (acadYearSemester != null) {
                    binding.term.itemContent.setText(acadYearSemester);
                }
            });
            Handler handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    super.handleMessage(msg);
                    if (msg.what == -1) {
                        params.toast(R.string.login_warning);
                    } else {
                        JSONObject response = JSONObject.parseObject((String) msg.obj);
//                    System.out.println(response);
                        if (response.getInteger("code") == 200) {
                            switch (msg.what) {
                                case 0:
                                    response.getJSONArray("data").forEach(t -> pop.getMenu().add(((JSONObject) t).getString("acadYearSemester")).setOnMenuItemClickListener(menuItem -> {
                                        term.setValue(((JSONObject) t).getString("acadYearSemester"));
                                        return false;
                                    }));
                                    getCampuses();
                                    break;
                                case 1:
                                    response.getJSONArray("data").forEach(c -> {
                                        ItemFilterChipBinding item = ItemFilterChipBinding.inflate(inflater, binding.campus, false);
                                        item.getRoot().setText(((JSONObject) c).getString("campusName"));
                                        item.getRoot().setOnCheckedChangeListener((buttonView, isChecked) -> {
                                            if (isChecked) {
                                                campus.setValue(((JSONObject) c).getString("id"));
                                            }
                                        });
                                        binding.campus.addView(item.getRoot());
                                    });
                                    break;
                            }
                        } else {
                            params.toast(R.string.login_warning);
                            params.gotoLogin(binding.filter, TargetUrl.JWXT);
                        }
                    }
                }
            };
            http = new HttpManager(handler);
            http.setParams(params);
            http.setReferrer("https://jwxt.sysu.edu.cn/jwxt/mk/zjgl/");
            getTerms();
        }
        return binding.getRoot();
    }


    void getTerms() {
        http.getRequest("https://jwxt.sysu.edu.cn/jwxt/base-info/acadyearterm/findAcadyeartermNamesBox", 0);
    }


    void getCampuses() {
        http.getRequest("https://jwxt.sysu.edu.cn/jwxt/base-info/campus/findCampusNamesBox", 1);
    }
}