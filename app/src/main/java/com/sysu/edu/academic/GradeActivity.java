package com.sysu.edu.academic;

import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.ActivityGradeBinding;
import com.sysu.edu.databinding.ItemScoreBinding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import io.noties.markwon.Markwon;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GradeActivity extends AppCompatActivity {

    final MutableLiveData<String> trainType = new MutableLiveData<>();
    final MutableLiveData<String> year = new MutableLiveData<>();
    final MutableLiveData<Integer> term = new MutableLiveData<>();
    final Map<String, Integer> gradeMap = Map.of("A", 100, "B", 90, "C", 80, "D", 70, "F", 60);
    ActivityGradeBinding binding;
    Handler handler;
    PopupMenu termPop;
    PopupMenu yearPop;
    PopupMenu typePop;
    GridLayoutManager gridLayoutManager;
    Params params;
    final OkHttpClient http = new OkHttpClient.Builder().addInterceptor(new Interceptor() {
        @NonNull
        @Override
        public Response intercept(@NonNull Chain chain) throws IOException {
            Request origin = chain.request();
            return chain.proceed(origin.newBuilder()
                    .header("Cookie", params.getCookie())
                    .header("Referer", "https://jwxt.sysu.edu.cn/jwxt/mk/studentWeb/")
                    .method(origin.method(), origin.body())
                    .build());
        }
    }).build();

    ArrayList<String> years;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGradeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        termPop = new PopupMenu(GradeActivity.this, binding.term, 0, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow);
        String[] terms = getResources().getStringArray(R.array.terms);
        for (int i = 0; i < terms.length; i++) {
            int finalI = i + 1;
            termPop.getMenu().add(terms[i]).setOnMenuItemClickListener(menuItem -> {
                term.postValue(finalI);
                return false;
            });
        }
        binding.tabs.setHorizontalScrollBarEnabled(false);
        yearPop = new PopupMenu(this, binding.year, 0, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow);
        typePop = new PopupMenu(this, binding.type, 0, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow);
        binding.toolbar.setNavigationOnClickListener(v -> supportFinishAfterTransition());
        binding.term.setOnClickListener(v -> termPop.show());
        binding.year.setOnClickListener(v -> yearPop.show());
        binding.type.setOnClickListener(v -> typePop.show());
        yearPop.getMenu().add(R.string.all).setOnMenuItemClickListener(menuItem -> {
            sendRequest(String.format("https://jwxt.sysu.edu.cn/jwxt/achievement-manage/score-check/list?trainTypeCode=%s", trainType.getValue()), 1);
            binding.year.setText(R.string.all);
            return false;
        });

        ScoreAdapter adp = new ScoreAdapter();
        binding.scores.setAdapter(adp);

        class GradeManager {
            String classNumber;
            int grade = -1;
            int position = -1;
            int maxGrade = -1;
            boolean isFetching = false;

            void getGrade(String classNumber, int pos, int maxGrade) {
                this.classNumber = classNumber;
                grade = maxGrade;
                isFetching = true;
                if (this.maxGrade < 0) {
                    this.maxGrade = maxGrade;
                }
                if (position < 0) {
                    position = pos;
                }
                postRequest("https://jwxt.sysu.edu.cn/jwxt/gradua-degree/graduatemsg/studentsGraduationExamination/studentCourse", String.format("{\"pageNo\":1,\"pageSize\":10,\"total\":true,\"param\":{\"achievementCourseNumber\":\"%s\",\"beforeAchievementPoint\":\"%s\",\"afterAchievementPoint\":\"%s\",\"cultureTypeCode\":\"01\"}}", classNumber, maxGrade, maxGrade), 5);
            }

            void getGrade() {
                if (maxGrade - grade < 60) {
                    getGrade(classNumber, position, --grade);
                } else {
                    isFetching = false;
                }
            }

            void setGrade() {
                adp.setGrade(position, String.valueOf(grade));
                params.toast(String.valueOf(grade));
                grade = -1;
                position = -1;
                maxGrade = -1;
                classNumber = "";
                isFetching = false;
            }
        }
        GradeManager gradeManager = new GradeManager();
        adp.setAction(position -> {
            if (gradeManager.isFetching) {
                params.toast(R.string.grade_fetching);
            } else {
                String level = adp.getLevel(position);
                int minGrade = gradeMap.getOrDefault(level.substring(0, 1), 0) - (level.length() == 2 ? 0 : 6);
                gradeManager.getGrade(adp.getClassNumber(position), position, minGrade);
            }
        });

        params = new Params(this);
        params.setCallback(this::getPull);
        gridLayoutManager = new GridLayoutManager(this, params.getColumn());
        binding.scores.setLayoutManager(gridLayoutManager);
        StaggeredFragment header = binding.header.getFragment();
        header.setNested(false);
        trainType.observe(this, s -> getScore());
        year.observe(this, s -> {
            if (year.getValue() != null && term.getValue() != null) {
                binding.year.setText(s);
                getScore();
            }
        });

        term.observe(this, s -> {
            binding.term.setText(terms[s - 1]);
            getScore();
        });
        handler = new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == -1) {
                    params.toast(R.string.no_wifi_warning);
                    return;
                }
                JSONObject dataString = JSON.parseObject((String) msg.obj);
                if (dataString.getInteger("code") == 200) {
                    switch (msg.what) {
                        case 1:
                            adp.clear();
                            dataString.getJSONArray("data").forEach(a -> adp.add((JSONObject) a));
                            break;
                        case 2: {
                            JSONObject pull = dataString.getJSONObject("data");

                            // 初始化培养类型选项
                            JSONArray type = pull.getJSONArray("selectTrainType");
                            type.forEach(a ->
                            {
                                JSONObject typeItem = (JSONObject) a;
                                typePop.getMenu().add(typeItem.getString("dataName")).setOnMenuItemClickListener(menuItem -> {
                                    binding.type.setText(typeItem.getString("dataName"));
                                    trainType.setValue(typeItem.getString("dataNumber"));
                                    return false;
                                });
                            });

                            // 选择培养类型的第一个选项
                            if (!type.isEmpty()) {
                                binding.type.setText(type.getJSONObject(0).getString("dataName"));
                                trainType.setValue(type.getJSONObject(0).getString("dataNumber"));
                            } else {
                                params.toast(R.string.no_train_type);
                            }

                            // 初始化学年选项
                            years = new ArrayList<>();
                            JSONArray selectYearPull = pull.getJSONArray("selectYearPull");
                            if (selectYearPull != null && !selectYearPull.isEmpty()) {
                                selectYearPull.forEach(a -> {
                                    years.add(((JSONObject) a).getString("dataName"));
                                    yearPop.getMenu().add(((JSONObject) a).getString("dataName")).setOnMenuItemClickListener(menuItem -> {
                                        year.postValue(((JSONObject) a).getString("dataName"));
                                        binding.year.setText(((JSONObject) a).getString("dataNumber"));
                                        return false;
                                    });
                                });
                            }

                            //获取这个学期的信息
                            getNow();
                            break;
                        }
                        case 3: {
                            // 初始化学期选项
                            JSONObject pull = dataString.getJSONObject("data");
                            if (years != null && !years.contains(pull.getString("acadYear"))) {
                                yearPop.getMenu().add(pull.getString("acadYear")).setOnMenuItemClickListener(menuItem -> {
                                    term.postValue(pull.getInteger("acadSemester"));
                                    year.postValue(pull.getString("acadYear"));
                                    return false;
                                });
                            }
                            term.postValue(pull.getInteger("acadSemester"));
                            year.postValue(pull.getString("acadYear"));
                            break;
                        }
                        case 4: {
                            JSONObject pull = dataString.getJSONObject("data");
                            JSONObject compulsorySelectTotal = pull.getJSONArray("compulsorySelectTotal").getJSONObject(0);
                            String totalRank = compulsorySelectTotal.getString("rank");
                            String totalPoint = compulsorySelectTotal.getString("vegPoint");
                            String totalCredit = compulsorySelectTotal.getString("totalCredit");
                            String rank = "";
                            String point = "";
                            if (!pull.getJSONArray("compulsorySelectList").isEmpty()) {
                                rank = pull.getJSONArray("compulsorySelectList").getJSONObject(0).getString("rank");
                                point = pull.getJSONArray("compulsorySelectList").getJSONObject(0).getString("vegPoint");
                            }
                            String total = pull.getString("stuTotal");
                            JSONObject stuCredit = pull.getJSONObject("stuCredit");
                            ArrayList<String> values = new ArrayList<>();
                            for (String key : new String[]{"allGetCredit", "publicGetCredit", "publicSelectGetCredit", "majorGetCredit", "majorSelectGetCredit", "honorCourseGetCredit"}) {
                                values.add(stuCredit.getString(key));
                            }
                            header.clear();
                            header.add(getString(R.string.total_year), List.of(getString(R.string.total_rank), getString(R.string.total_credit), getString(R.string.total_point)), List.of(String.format("%s/%s", totalRank, total), totalCredit, totalPoint));
                            header.add(terms[term.getValue() == null ? 1 : term.getValue() - 1], List.of(getString(R.string.current_rank), getString(R.string.current_point)), List.of(String.format("%s/%s", rank, total), point));
                            header.add(getString(R.string.credit), List.of(getString(R.string.term_credit), getString(R.string.public_compulsory_credit), getString(R.string.public_select_credit), getString(R.string.major_compulsory_credit), getString(R.string.major_select_credit), getString(R.string.honor_credit)), values);
                            break;
                        }
                        case 5:
//                            System.out.println(dataString);
                            if (dataString.containsKey("data") && !dataString.getJSONObject("data").getInteger("total").equals(0)) {
                                gradeManager.setGrade();
                            } else {
                                gradeManager.getGrade();
                            }
                    }
                } else {
                    params.toast(R.string.login_warning);
                    params.gotoLogin(binding.toolbar, TargetUrl.JWXT);
                }
            }
        };
        getPull();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        gridLayoutManager.setSpanCount(new Params(this).getColumn());
    }

    void sendRequest(String url, int what) {
        http.newCall(new Request.Builder().url(url).build()).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message message = new Message();
                message.what = what;
                message.obj = response.body().string();
                handler.sendMessage(message);
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message message = new Message();
                message.what = -1;
                handler.sendMessage(message);
            }
        });
    }

    void postRequest(String url, String data, int what) {
        http.newCall(new Request.Builder().url(url)
                .post(RequestBody.create(data, MediaType.parse("application/json"))).build()).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message message = new Message();
                message.what = what;
                message.obj = response.body().string();
                handler.sendMessage(message);
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message message = new Message();
                message.what = -1;
                handler.sendMessage(message);
            }
        });
    }

    void getNow() {
        sendRequest("https://jwxt.sysu.edu.cn/jwxt/base-info/acadyearterm/showNewAcadlist", 3);
    }

    void getScore() {
        if (year.getValue() == null || term.getValue() == null || trainType.getValue() == null) {
            return;
        }
        getScore(year.getValue(), term.getValue(), trainType.getValue());
        getTotalScore(year.getValue(), term.getValue(), trainType.getValue());
    }

    void getScore(String year, int term, String type) {
        sendRequest(String.format(Locale.getDefault(), "https://jwxt.sysu.edu.cn/jwxt/achievement-manage/score-check/list?scoSchoolYear=%s&trainTypeCode=%s&addScoreFlag=true&scoSemester=%d", year, type, term), 1);
    }

    void getTotalScore(String year, int term, String type) {
        sendRequest(String.format(Locale.getDefault(), "https://jwxt.sysu.edu.cn/jwxt/achievement-manage/score-check/getSortByYear?scoSchoolYear=%s&trainTypeCode=%s&addScoreFlag=true&scoSemester=%d", year, type, term), 4);
    }

    void getPull() {
        sendRequest("https://jwxt.sysu.edu.cn/jwxt/achievement-manage/score-check/getPull", 2);
    }

    void getGrade(String classNumber, int minGrade, int maxGrade) {
        postRequest("https://jwxt.sysu.edu.cn/jwxt/gradua-degree/graduatemsg/studentsGraduationExamination/studentCourse", String.format("{\"pageNo\":1,\"pageSize\":10,\"total\":true,\"param\":{\"achievementCourseNumber\":\"%s\",\"beforeAchievementPoint\":\"%s\",\"afterAchievementPoint\":\"%s\",\"cultureTypeCode\":\"01\"}}", classNumber, minGrade, maxGrade), 5);
    }

    static class ScoreAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        final ArrayList<JSONObject> data = new ArrayList<>();
        Consumer<Integer> action;

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(ItemScoreBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false).getRoot()) {
            };
        }

        public void setAction(Consumer<Integer> action) {
            this.action = action;
        }

        public void setGrade(int position, String grade) {
            data.get(position).put("originalScore", grade);
            notifyItemChanged(position);
        }

        public String getLevel(int position) {
            return data.get(position).getString("scoFinalScore");
        }

        public String getClassNumber(int position) {
            return data.get(position).getString("scoCourseNumber");
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ItemScoreBinding binding = ItemScoreBinding.bind(holder.itemView);
            JSONObject info = data.get(position);
            binding.getRoot().setOnClickListener(view -> {
                if (info.getString("originalScore") == null) {
                    action.accept(position);
                }
            });
            MutableLiveData<String> grade = new MutableLiveData<>("");
            if (info.containsKey("scoreList"))
                info.getJSONArray("scoreList").forEach(a -> grade.setValue(String.format("%s（%s）%s×%s%%+", grade, ((JSONObject) a).getString("FXMC"), ((JSONObject) a).getString("FXCJ"), ((JSONObject) a).getString("MRQZ"))));
            binding.subject.setText(info.getString("scoCourseName"));
            binding.score.setText(String.format("%s%s", info.getString("scoFinalScore"), info.getString("scoPoint") == null ? "" : "/" + info.getString("scoPoint")));
            Markwon.builder(binding.getRoot().getContext()).build().setMarkdown(binding.info, String.format("- 学期：**%s**\n- 学分：**%s**\n- 班级排名：**%s**\n- 年级排名：**%s**\n- 课程类别：**%s**\n- 老师：**%s**\n- 是否通过：**%s**\n- 考试性质：**%s**\n- 班级号：**%s**\n- 教学班号：**%s**\n- 成绩：**%s**",
                    String.format("%s第%s学期", info.getString("scoSchoolYear"), info.getString("scoSemester")),
                    info.getString("scoCredit"),
                    info.getString("teachClassRank"),
                    info.getString("gradeMajorRank"),
                    info.getString("scoCourseCategoryName"),
                    info.getString("scoTeacherName"),
                    info.getString("accessFlag"),
                    info.getString("examCharacter"),
                    info.getString("scoCourseNumber"),
                    info.getString("teachClassNumber"),
                    (info.getString("originalScore") == null ? "点击查看总成绩" : Objects.requireNonNull(grade.getValue()) + "=" + info.getString("originalScore"))));
        }

        public void add(JSONObject a) {
            int tmp = getItemCount();
            data.add(a);
            notifyItemInserted(tmp);
        }

        public void clear() {
            int tmp = getItemCount();
            data.clear();
            notifyItemRangeRemoved(0, tmp);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }
}
