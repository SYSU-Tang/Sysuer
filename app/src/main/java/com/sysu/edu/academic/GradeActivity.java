package com.sysu.edu.academic;

import static android.text.TextUtils.isEmpty;
import static com.sysu.edu.api.CommonUtil.extractValue;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.ActivityGradeBinding;
import com.sysu.edu.databinding.ItemScoreBinding;
import com.sysu.edu.model.JwxtModel;
import com.sysu.edu.view.RecyclerAdapter;
import com.sysu.edu.view.StaggeredFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import io.noties.markwon.Markwon;

public class GradeActivity extends AppCompatActivity {
    
    final MutableLiveData<String> trainType = new MutableLiveData<>();
    final MutableLiveData<String> year = new MutableLiveData<>();
    final MutableLiveData<Integer> term = new MutableLiveData<>();
    final Map<String, Integer> gradeMap = Map.of("A", 100, "B", 90, "C", 80, "D", 70, "F", 60);
    PopupMenu termPop;
    PopupMenu yearPop;
    PopupMenu typePop;
    GridLayoutManager gridLayoutManager;
    ArrayList<String> years;
    JwxtModel model;
    Params params;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityGradeBinding binding = ActivityGradeBinding.inflate(getLayoutInflater());
        model = new JwxtModel(this);
        setContentView(binding.getRoot());
        termPop = new PopupMenu(this, binding.term, 0, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow);
        String[] terms = getResources().getStringArray(R.array.terms);
        for (int i = 0; i < terms.length; i++) {
            int finalI = i + 1;
            termPop.getMenu().add(terms[i]).setOnMenuItemClickListener(_ -> {
                term.setValue(finalI);
                return false;
            });
        }
        binding.tabs.setHorizontalScrollBarEnabled(false);
        yearPop = new PopupMenu(this, binding.year, 0, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow);
        typePop = new PopupMenu(this, binding.type, 0, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow);
        binding.toolbar.setNavigationOnClickListener(_ -> supportFinishAfterTransition());
        binding.term.setOnClickListener(_ -> termPop.show());
        binding.year.setOnClickListener(_ -> yearPop.show());
        binding.type.setOnClickListener(_ -> typePop.show());
        yearPop.getMenu().add(R.string.all).setOnMenuItemClickListener(_ -> {
            model.addAndNext(String.format("jwxt/achievement-manage/score-check/list?trainTypeCode=%s&addScoreFlag=true", trainType.getValue()), 1);
            model.addAndNext(String.format("jwxt/achievement-manage/score-check/getSortByYear?trainTypeCode=%s&addScoreFlag=true", trainType.getValue()), 4);
            binding.year.setText(R.string.all);
            return false;
        });
        params = new Params(this);
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
                if (this.maxGrade < 0) this.maxGrade = maxGrade;
                if (position < 0) position = pos;
                model.addAndNext("jwxt/gradua-degree/graduatemsg/studentsGraduationExamination/studentCourse", String.format("{\"pageNo\":1,\"pageSize\":10,\"total\":true,\"param\":{\"achievementCourseNumber\":\"%s\",\"beforeAchievementPoint\":\"%s\",\"afterAchievementPoint\":\"%s\",\"cultureTypeCode\":\"01\"}}", classNumber, maxGrade, maxGrade), 5);
            }
            
            void getGrade() {
                if (maxGrade - grade < 60) getGrade(classNumber, position, --grade);
                else isFetching = false;
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
            if (gradeManager.isFetching) model.getContextUtil().toast(R.string.grade_fetching);
            else {
                String level = adp.getLevel(position);
                if (!isEmpty(level)) {
                    int minGrade = Objects.requireNonNull(gradeMap.getOrDefault(level.substring(0, 1), 0)) - (level.length() == 2 ? 0 : 6);
                    gradeManager.getGrade(adp.getClassNumber(position), position, minGrade);
                }
            }
        });
        gridLayoutManager = new GridLayoutManager(this, params.getColumn());
        binding.scores.setLayoutManager(gridLayoutManager);
        StaggeredFragment header = binding.header.getFragment();
        header.setNested(false);
        trainType.observe(this, _ -> getScore());
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
        model.getMessage().observe(this, message -> {
            JSONObject response = message.getSecond();
            if (response.getInteger("code") == 200) {
                switch (message.getFirst()) {
                    case 1 -> {
                        adp.clear();
                        response.getJSONArray("data").forEach(a -> adp.add((JSONObject) a));
                    }
                    case 2 -> {
                        JSONObject pull = response.getJSONObject("data");
                        // 初始化培养类型选项
                        JSONArray type = pull.getJSONArray("selectTrainType");
                        type.forEach(a ->
                        {
                            JSONObject typeItem = (JSONObject) a;
                            typePop.getMenu().add(typeItem.getString("dataName")).setOnMenuItemClickListener(_ -> {
                                binding.type.setText(typeItem.getString("dataName"));
                                trainType.setValue(typeItem.getString("dataNumber"));
                                return false;
                            });
                        });
                        
                        // 选择培养类型的第一个选项
                        if (!type.isEmpty()) {
                            binding.type.setText(type.getJSONObject(0).getString("dataName"));
                            trainType.setValue(type.getJSONObject(0).getString("dataNumber"));
                        } else model.getContextUtil().toast(R.string.no_train_type);
                        
                        // 初始化学年选项
                        years = new ArrayList<>();
                        JSONArray selectYearPull = pull.getJSONArray("selectYearPull");
                        if (selectYearPull != null && !selectYearPull.isEmpty())
                            selectYearPull.forEach(a -> {
                                years.add(((JSONObject) a).getString("dataName"));
                                yearPop.getMenu().add(((JSONObject) a).getString("dataName")).setOnMenuItemClickListener(_ -> {
                                    year.postValue(((JSONObject) a).getString("dataName"));
                                    binding.year.setText(((JSONObject) a).getString("dataNumber"));
                                    return false;
                                });
                            });
                        
                        //获取这个学期的信息
                        getNow();
                    }
                    case 3 -> {
                        // 初始化学期选项
                        JSONObject pull = response.getJSONObject("data");
                        if (years != null && !years.contains(pull.getString("acadYear")))
                            yearPop.getMenu().add(pull.getString("acadYear")).setOnMenuItemClickListener(_ -> {
                                term.postValue(pull.getInteger("acadSemester"));
                                year.postValue(pull.getString("acadYear"));
                                return false;
                            });
                        term.postValue(pull.getInteger("acadSemester"));
                        year.postValue(pull.getString("acadYear"));
                    }
                    case 4 -> {
                        JSONObject pull = response.getJSONObject("data");
                        JSONObject compulsorySelectTotal = pull.getJSONArray("compulsorySelectTotal").getJSONObject(0);
                        String totalRank = compulsorySelectTotal.getString("rank");
                        String totalPoint = compulsorySelectTotal.getString("vegPoint");
                        String totalCredit = compulsorySelectTotal.getString("totalCredit");
                        String rank = "";
                        String point = "";
                        JSONArray compulsorySelectList = pull.getJSONArray("compulsorySelectList");
                        if (!compulsorySelectList.isEmpty()) {
                            rank = compulsorySelectList.getJSONObject(0).getString("rank");
                            point = compulsorySelectList.getJSONObject(0).getString("vegPoint");
                        }
                        String total = pull.getString("stuTotal");
                        header.clear();
                        header.add(getString(R.string.total_year), List.of(getString(R.string.total_rank), getString(R.string.total_credit), getString(R.string.total_point)), List.of(String.format("%s/%s", totalRank, total), totalCredit, totalPoint));
                        header.add(terms[term.getValue() == null ? 1 : term.getValue() - 1], List.of(getString(R.string.current_rank), getString(R.string.current_point)), List.of(String.format("%s/%s", rank, total), point));
                        header.add(getString(R.string.credit), List.of(getString(R.string.term_credit), getString(R.string.public_compulsory_credit), getString(R.string.public_select_credit), getString(R.string.major_compulsory_credit), getString(R.string.major_select_credit), getString(R.string.honor_credit)),
                                extractValue(pull.getJSONObject("stuCredit"),new String[]{"allGetCredit", "publicGetCredit", "publicSelectGetCredit", "majorGetCredit", "majorSelectGetCredit", "honorCourseGetCredit"}));
                    }
                    case 5 -> {
                        if (response.containsKey("data") && !response.getJSONObject("data").getInteger("total").equals(0))
                            gradeManager.setGrade();
                        else gradeManager.getGrade();
                    }
                }
            }
        });
        getPull();
    }
    
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        gridLayoutManager.setSpanCount(params.getColumn());
    }
    
    void getNow() {
        model.addAndNext("jwxt/base-info/acadyearterm/showNewAcadlist", 3);
    }
    
    void getScore() {
        if (year.getValue() != null && term.getValue() != null && trainType.getValue() != null) {
            getScore(year.getValue(), term.getValue(), trainType.getValue());
            getTotalScore(year.getValue(), term.getValue(), trainType.getValue());
        }
    }
    
    void getScore(String year, int term, String type) {
        model.addAndNext(String.format(Locale.getDefault(), "jwxt/achievement-manage/score-check/list?scoSchoolYear=%s&trainTypeCode=%s&addScoreFlag=true&scoSemester=%d", year, type, term), 1);
    }
    
    void getTotalScore(String year, int term, String type) {
        model.addAndNext(String.format(Locale.getDefault(), "jwxt/achievement-manage/score-check/getSortByYear?scoSchoolYear=%s&trainTypeCode=%s&addScoreFlag=true&scoSemester=%d", year, type, term), 4);
    }
    
    void getPull() {
        model.addAndNext("jwxt/achievement-manage/score-check/getPull", 2);
    }
    
    static class ScoreAdapter extends RecyclerAdapter<JSONObject> {
        
        Consumer<? super Integer> action;
        
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(ItemScoreBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false).getRoot()) {
            };
        }
        
        public void setAction(Consumer<? super Integer> action) {
            this.action = action;
        }
        
        public void setGrade(int position, String grade) {
            get(position).put("originalScore", grade);
            notifyItemChanged(position);
        }
        
        public String getLevel(int position) {
            return get(position).getString("scoFinalScore");
        }
        
        public String getClassNumber(int position) {
            return get(position).getString("scoCourseNumber");
        }
        
        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ItemScoreBinding binding = ItemScoreBinding.bind(holder.itemView);
            JSONObject info = data.get(position);
            binding.getRoot().setOnClickListener(_ -> {
                if (info.getString("originalScore") == null) action.accept(position);
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
                    (info.getString("originalScore") == null ? binding.getRoot().getContext().getString(R.string.click_for_grade) : Objects.requireNonNull(grade.getValue()) + "=" + info.getString("originalScore"))));
        }
    }
}
