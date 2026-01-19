package com.sysu.edu.academic;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.button.MaterialButton;
import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.ActivityCourseSelectedBinding;
import com.sysu.edu.databinding.ItemCourseSelectedBinding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CourseSelectedActivity extends AppCompatActivity {
    final MutableLiveData<String> response = new MutableLiveData<>();
    final OkHttpClient http = new OkHttpClient.Builder().build();
    Params params;
    ActivityResultLauncher<Intent> launcher;
    int page = 0;
    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCourseSelectedBinding binding = ActivityCourseSelectedBinding.inflate(getLayoutInflater());
        params = new Params(this);
        params.setCallback(this::getSelectedCourses);
        launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
        });
        CourseSelectedAdapter adp = new CourseSelectedAdapter();
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(v -> supportFinishAfterTransition());
        binding.search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                //adp.filter(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                page = 0;
                adp.clear();
                getSelectedCourses(newText);
                return true;
            }
        });

        binding.list.setLayoutManager(new StaggeredGridLayoutManager(params.getColumn(), StaggeredGridLayoutManager.VERTICAL));
        binding.list.setAdapter(adp);
        response.observe(this, d -> {
            JSONObject data = JSONObject.parse(d);
            if (data.getInteger("code") == 200) {
                data.getJSONObject("data").getJSONArray("rows").forEach(o -> adp.add((JSONObject) o));
                if (data.getJSONObject("data").getInteger("total") / 10.0 > page) {
                    handler.postDelayed(() -> getSelectedCourses(binding.search.getQuery().toString()), 500);
                }
            } else {
                params.toast(R.string.login_warning);
                params.gotoLogin(binding.toolbar, TargetUrl.JWXT);
            }
        });
        handler = new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case -1:
                        params.toast(R.string.no_wifi_warning);
                    case 1:
                        response.postValue((String) msg.obj);
                        break;
                }
            }
        };
        getSelectedCourses("");
    }

    public void getSelectedCourses() {
        getSelectedCourses("");
    }

    public void getSelectedCourses(String courseName) {
        http.newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/choose-course-front-server/selectedCourse/list")
                .header("Cookie", params.getCookie())
                .header("Referer", "https://jwxt.sysu.edu.cn/jwxt/mk/courseSelection/?code=jwxsd_xk&resourceName=%25E9%2580%2589%25E8%25AF%25BE")
                .post(RequestBody.create(String.format(Locale.getDefault(), "{\"pageNo\":%d,\"pageSize\":10,\"total\":true,\"param\":{\"courseName\":\"%s\",\"successStatus\":\"1\",\"failureStatus\":\"0\",\"retiredClass\":\"0\",\"waitingScreen\":\"0\"}}", ++page, courseName), MediaType.parse("application/json"))).build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> params.toast(R.string.no_wifi_warning));
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

    public static class CourseSelectedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        final ArrayList<JSONObject> data = new ArrayList<>();

        public CourseSelectedAdapter() {
            super();
        }

        public void add(JSONObject jsonObject) {
            data.add(jsonObject);
            notifyItemInserted(getItemCount() - 1);
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            ViewHolder vh = new ViewHolder(ItemCourseSelectedBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
            vh.setInfo(data.get(viewType));
            return vh;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ((ViewHolder) holder).setInfo(data.get(position));
            ((ViewHolder) holder).binding.getRoot().setOnClickListener(view -> view.getContext().startActivity(new Intent(view.getContext(), CourseDetail.class).putExtra("id", data.get(position).getString("teachingClassId")).putExtra("code", data.get(position).getString("courseNum")).putExtra("class", data.get(position).getString("teachingClassNum")),
                    ActivityOptionsCompat.makeSceneTransitionAnimation((Activity) view.getContext(), ((ViewHolder) holder).binding.title, "miniapp").toBundle()));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        public void clear() {
            int tmp = getItemCount();
            data.clear();
            notifyItemRangeRemoved(0, tmp);
        }

        static class ViewHolder extends RecyclerView.ViewHolder {

            final ItemCourseSelectedBinding binding;
            final ArrayList<Integer> ids = new ArrayList<>();
            final MutableLiveData<JSONObject> info = new MutableLiveData<>();

            public ViewHolder(ItemCourseSelectedBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
                info.observe((FragmentActivity) binding.getRoot().getContext(), this::loadInfo);
            }

            public void setInfo(JSONObject info) {
                this.info.postValue(info);
            }

            void loadInfo(JSONObject info) {
                String[] key = new String[]{"courseName", "courseCategoryName", "courseUnitName", "scheduleExamTime", "examFormName", "credit", "teachingClassId", "teachingClassNum", "teachingClassName", "courseNum"};
                String[] name = new String[]{"课程名称", "课程类别", "开设学院", "考试时间", "考核方式", "学分", "班级ID", "班级号", "班级名", "课程号"};
                ids.forEach(e -> binding.group.removeView(binding.group.findViewById(e)));
                ids.clear();
                binding.title.setText(info.getString("courseName"));

                String teachingTimePlace = info.getString("teachingTimePlace");
                if (teachingTimePlace == null || teachingTimePlace.isEmpty()) {
                    ids.add(addItem("无", "课程安排"));
                } else {
                    Pattern.compile(",").splitAsStream(teachingTimePlace).forEach(s -> ids.add(addItem(s.replace(";", "/"), "课程安排")));
                }
                for (int i = 0; i < key.length; i++) {
                    ids.add(addItem(info.getString(key[i]) == null ? "无" : info.getString(key[i]), name[i]));
                }
                binding.courseInfo.setReferencedIds(ids.stream().mapToInt(Integer::intValue).toArray());
            }

            int addItem(String value, String name) {
                MaterialButton item = new MaterialButton(binding.getRoot().getContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
                Params params = new Params((FragmentActivity) binding.getRoot().getContext());
                item.setTextAppearance(binding.getRoot().getContext(), com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
                item.setLayoutParams(new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT));
                item.setOnClickListener(v -> params.copy(name, value));
                item.setText(String.format(Locale.getDefault(), "%s: %s", name, value));
                item.setCornerRadius(params.dpToPx(8));
                item.setPadding(params.dpToPx(8), params.dpToPx(6), params.dpToPx(8), params.dpToPx(6));
                item.setGravity(Gravity.CENTER);
                int id = View.generateViewId();
                item.setId(id);
                binding.group.addView(item);
                return id;
            }
        }
    }
}