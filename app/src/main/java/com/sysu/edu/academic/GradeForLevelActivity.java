package com.sysu.edu.academic;

import static com.sysu.edu.api.CommonUtil.extractValue;
import static com.sysu.edu.api.CommonUtil.isEmpty;
import static com.sysu.edu.api.CommonUtil.toStringOrDefault;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.PopupWindow;

import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityOptionsCompat;
import androidx.lifecycle.MutableLiveData;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.button.MaterialButton;
import com.sysu.edu.BaseActivity;
import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.ActivityGradeForLevelBinding;
import com.sysu.edu.databinding.PreferenceEditBinding;
import com.sysu.edu.model.JwxtModel;
import com.sysu.edu.view.StaggeredFragment;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

public class GradeForLevelActivity extends BaseActivity {
    
    final MutableLiveData<String> trainType = new MutableLiveData<>();
    final MutableLiveData<String> year = new MutableLiveData<>();
    final MutableLiveData<String> courseType = new MutableLiveData<>();
    final MutableLiveData<String> courseName = new MutableLiveData<>();
    final MutableLiveData<String> courseNumber = new MutableLiveData<>();
    final MutableLiveData<String> minGrade = new MutableLiveData<>();
    //    HttpManager http;
    int page = 1;
    int total = -1;
    StaggeredFragment fragment;
    PopupMenu yearPop;
    PopupMenu trainTypePop;
    PopupMenu courseTypePop;
    MutableLiveData<String> input;
    JwxtModel model;
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        model.dispose();
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityGradeForLevelBinding binding = ActivityGradeForLevelBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        model = new JwxtModel(this);
        Params params = new Params(this);
        params.setCallback(() -> {
            getData(0);
            regetGrade();
        });
        binding.toolbar.setNavigationOnClickListener(_ -> supportFinishAfterTransition());
        fragment = binding.fragment.getFragment();
        fragment.setScrollBottom(() -> {
            if ((page - 1) * 10 < total)
                getGrade();
        });
        binding.toolbar.getMenu().add("导出").setIcon(R.drawable.export).setOnMenuItemClickListener(_ -> {
            startActivity(new Intent(this, MarkdownViewActivity.class).putExtra("content", fragment.toTable()).putExtra("title", "成绩"),
                    ActivityOptionsCompat.makeSceneTransitionAnimation(this, binding.toolbar, "miniapp").toBundle());
            return false;
        }).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        yearPop = new PopupMenu(this, binding.year);
        trainTypePop = new PopupMenu(this, binding.trainType);
        courseTypePop = new PopupMenu(this, binding.courseType);
        PreferenceEditBinding courseNameEditText = PreferenceEditBinding.inflate(getLayoutInflater());
        PopupWindow courseNamePop = getPopupWindow(courseNameEditText);
        binding.year.setOnClickListener(_ -> yearPop.show());
        binding.trainType.setOnClickListener(_ -> trainTypePop.show());
        binding.courseType.setOnClickListener(_ -> courseTypePop.show());
        binding.courseName.setOnClickListener(v -> {
            input = courseName;
            courseNamePop.showAsDropDown(v);
            courseNameEditText.textInputLayout.setHint(R.string.course_name);
            courseNameEditText.textInputLayout.requestFocus();
            courseNameEditText.textField.setText(courseName.getValue());
        });
        binding.courseNumber.setOnClickListener(v -> {
            input = courseNumber;
            courseNamePop.showAsDropDown(v);
            courseNameEditText.textInputLayout.requestFocus();
            courseNameEditText.textInputLayout.setHint(R.string.course_number);
            courseNameEditText.textField.setText(courseNumber.getValue());
        });
        binding.minGrade.setOnClickListener(v -> {
            input = minGrade;
            courseNamePop.showAsDropDown(v);
            courseNameEditText.textInputLayout.requestFocus();
            courseNameEditText.textInputLayout.setHint(R.string.min_grade);
            courseNameEditText.textField.setText(minGrade.getValue());
        });
        year.observe(this, _ -> regetGrade());
        trainType.observe(this, _ -> regetGrade());
        courseType.observe(this, _ -> regetGrade());
        courseName.observe(this, s -> {
            binding.courseName.setText(s.isEmpty() ? getString(R.string.course_name) : s);
            regetGrade();
        });
        courseNumber.observe(this, s -> {
            binding.courseNumber.setText(s.isEmpty() ? getString(R.string.course_number) : s);
            regetGrade();
        });
        minGrade.observe(this, s -> {
            binding.minGrade.setText(s.isEmpty() ? getString(R.string.min_grade) : s);
            regetGrade();
        });
        getGrade();
        IntStream.range(0, 3).forEach(this::getData);
        model.getMessage().observe(this, message -> {
            JSONObject response = message.second;
            if (response.getInteger("code") == 200) {
                int what = message.first;
                switch (what) {
                    case 3 -> {
                        if (total == -1)
                            total = response.getJSONObject("data").getInteger("total");
                        response.getJSONObject("data").getJSONArray("rows").forEach(item -> fragment.add(((JSONObject) item).getString("courseName"), List.of("绩点", "教学班编号", "课程类别", "课程ID", "课程名称", "课程编号", "学分", "考试性质", "等级", "年级", "开设单位", "学期", "总学时", "培养类别", "总成绩"),
                                extractValue((JSONObject) item, new String[]{"achievementPoint", "classesNum", "courseCategoryName", "courseId", "courseName", "courseNum", "credit", "examNatureName", "finalAchievementStr", "grade", "openClassUnitName", "schoolSemester", "sumHours", "trainingCategoryName", "totalAchievement"})));
                    }
                    case 0, 1, 2 -> {
                        Menu menu = List.of(trainTypePop, yearPop, courseTypePop).get(what).getMenu();
//                        menu.dispose();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                            menu.setGroupDividerEnabled(true);
                        MutableLiveData<String> realLiveDataValue = List.of(trainType, year, courseType).get(what);
                        MaterialButton button = List.of(binding.trainType, binding.year, binding.courseType).get(what);
                        menu.add(0, 0, 0, R.string.reset).setOnMenuItemClickListener(_ -> {
                            button.setText(List.of(R.string.train_type, R.string.year, R.string.course_type).get(what));
                            realLiveDataValue.setValue("");
                            return true;
                        });
                        response.getJSONArray("data").forEach(e -> menu.add(1, 0, 0, ((JSONObject) e).getString(new String[]{"dataName", "acadYearSemester", "catName"}[what])).setOnMenuItemClickListener(item -> {
                            String menuValue = ((JSONObject) e).getString(new String[]{"dataNumber", "acadYearSemester", "catCode"}[what]);
                            if (!Objects.equals(menuValue, realLiveDataValue.getValue())) {
                                button.setText(item.getTitle());
                                realLiveDataValue.setValue(menuValue);
                            }
                            return true;
                        }));
                    }
                }
                model.nextAll();
            }
        });
    }
    
    private PopupWindow getPopupWindow(PreferenceEditBinding courseNameEditText) {
        PopupWindow courseNamePop = new PopupWindow(this, null, rikka.preference.simplemenu.R.attr.popupMenuStyle);
        courseNamePop.setContentView(courseNameEditText.getRoot());
        courseNamePop.setFocusable(true);
        courseNamePop.setOutsideTouchable(false);
        courseNamePop.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        courseNamePop.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
        courseNamePop.setWidth(-1);
        courseNamePop.setOnDismissListener(() -> {
            String text = toStringOrDefault(courseNameEditText.textField.getText());
            if (!Objects.equals(input.getValue(), text))
                input.setValue(text);
        });
        return courseNamePop;
    }
    
    private void regetGrade() {
        clear();
        getGrade();
    }
    
    private void clear() {
        fragment.clear();
        page = 1;
        total = -1;
    }
    
    void getGrade() {
        model.addAndNext("jwxt/achievement-manage/achievement/selfPageList", String.format("{\"pageNo\":%s,\"pageSize\":10,\"total\":true,\"param\":%s}", page++, getArgs().toString()), 3);
    }

//    void getYear() {
//        model.addAndNext("jwxt/base-info/acadyearterm/findAcadyeartermNamesBox", 0);
//    }
//
//    void getTrainingType() {
//        model.addAndNext("jwxt/base-info/codedata/findcodedataNames?datableNumber=97", 1);
//    }
//
//    void getCourseType() {
//        model.addAndNext("jwxt/base-info/base-category/SfqyBox", 2);
//    }
    
    void getData(int pos) {
        model.add(List.of("jwxt/base-info/codedata/findcodedataNames?datableNumber=97",
                "jwxt/base-info/acadyearterm/findAcadyeartermNamesBox",
                "jwxt/base-info/base-category/SfqyBox").get(pos), pos);
    }
    
    /*
     * {"categoryCode":"01","schoolSemester":"2025-1","courseTypeCode":"10","courseNum":"编码","courseName":"名称","finalAchievement":0,"achievementState":null}
     * */
    JSONObject getArgs() {
        JSONObject args = new JSONObject();
        if (!isEmpty(trainType.getValue()))
            args.put("categoryCode", trainType.getValue());
        if (!isEmpty(year.getValue()))
            args.put("schoolSemester", year.getValue());
        if (!isEmpty(courseType.getValue()))
            args.put("courseTypeCode", courseType.getValue());
        if (!isEmpty(courseName.getValue()))
            args.put("courseName", courseName.getValue());
        if (!isEmpty(courseNumber.getValue()))
            args.put("courseNum", courseNumber.getValue());
        if (!isEmpty(minGrade.getValue()))
            args.put("finalAchievement", Integer.parseInt(minGrade.getValue()));
        return args;
    }
}