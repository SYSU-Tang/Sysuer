package com.sysu.edu.academic;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.transition.TransitionInflater;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.FragmentTrainingResultBinding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TrainingResultFragment extends Fragment {
    final OkHttpClient http = new OkHttpClient.Builder().build();
    final MutableLiveData<Boolean> isBottom = new MutableLiveData<>(true);
    Handler handler;
    Params params;
    int page = 0;
    Integer total;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSharedElementEnterTransition(TransitionInflater.from(requireContext()).inflateTransition(android.R.transition.move));
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FragmentTrainingResultBinding binding = FragmentTrainingResultBinding.inflate(inflater, container, false);
        params = new Params(requireActivity());
        StaggeredFragment staggeredFragment = new StaggeredFragment();
        // StaggeredFragment staggeredFragment = (StaggeredFragment) getParentFragmentManager().findFragmentById(R.id.result);
        getParentFragmentManager().beginTransaction().add(R.id.result, staggeredFragment).commit();
        isBottom.observe(getViewLifecycleOwner(), aBoolean -> {
            if (aBoolean && (total == null || total / 10.0 > page)) {
                getSelectedCourses(requireArguments().getString("unit"), requireArguments().getString("grade"), requireArguments().getString("profession"), requireArguments().getString("type"));
                isBottom.setValue(false);
            }
        });
        staggeredFragment.setScrollBottom(() -> isBottom.setValue(true));
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case -1:
                        params.toast(R.string.no_wifi_warning);
                    case 1:
                        JSONObject response = JSONObject.parse((String) msg.obj);
                        if (response.getIntValue("code") == 200) {
                            JSONObject data = response.getJSONObject("data");
                            data.getJSONArray("rows").forEach(profession -> {
                                JSONObject professionObj = (JSONObject) profession;
                                String[] names = new String[]{"专业", "年级", "学院", "培养类别", "修业年限", "学科门类", "学位", "专业代码", "专业ID"};
                                String[] keys = new String[]{"professionName", "grade", "manageUnitName", "trainTypeName", "educationalSystem", "disciplineCateName", "degreeGrantName", "professionCode", "professionId"};
                                ArrayList<String> values = new ArrayList<>();
                                for (int i = 0; i < names.length; i++) {
                                    values.add(professionObj.getString(keys[i]));
                                }
                                total = data.getInteger("total");
                                staggeredFragment.add(requireContext(), professionObj.getString("name"), List.of(names), values);
                            });
                            //isBottom.postValue(false);
                            /*{
                                "id": "1899295808275939419",
                                    "teachPlanNumber": "1939485079282544740",
                                    "code": "25n02010000",
                                    "name": "25级经济学类（岭南学院）",
                                    "professionId": "88274511",
                                    "professionCode": "n02010000",
                                    "professionName": "经济学类（岭南学院）",
                                    "grade": "2025",
                                    "disciplineCateCode": "02",
                                    "disciplineCateName": "经济学",
                                    "degreeGrantName": "经济学学士学位",
                                    "educationalSystem": 4.0,
                                    "manageUnitNum": "10000",
                                    "manageUnitName": "岭南学院",
                                    "trainTypeName": "主修"
                            },*/
                           /*
                            if (data.getInteger("total")/10 > page) {
                                getSelectedCourses(requireArguments().getString("unit"), requireArguments().getString("grade"), requireArguments().getString("profession"), requireArguments().getString("type"));
                            }*/
                        } else {
                            params.toast(response.getString("msg"));
                        }
                        break;
                }
            }
        };
        return binding.getRoot();
    }


    public void getSelectedCourses(String unit, String grade, String profession, String trainType) {
        http.newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/training-programe/training-programe/undergradute/profession-info")
                .header("Cookie", params.getCookie())
                .header("Referer", "https://jwxt.sysu.edu.cn/jwxt/mk")
                .post(RequestBody.create(String.format(Locale.getDefault(), "{\"pageNo\":%d,\"pageSize\":10,\"total\":true,\"param\":{\"manageUnitNum\":\"%s\",\"grade\":\"%s\",\"professionCode\":\"%s\",\"trainTypeCode\":\"%s\"}}", ++page, unit, grade, profession, trainType), MediaType.parse("application/json"))).build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                params.toast(R.string.no_wifi_warning);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response r) throws IOException {
                Message msg = Message.obtain();
                msg.what = 1;
                msg.obj = r.body().string();
                handler.sendMessage(msg);
            }
        });
    }

}