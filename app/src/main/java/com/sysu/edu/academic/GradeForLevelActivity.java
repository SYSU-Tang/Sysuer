package com.sysu.edu.academic;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityOptionsCompat;
import androidx.lifecycle.MutableLiveData;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.button.MaterialButton;
import com.sysu.edu.R;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.ActivityGradeForLevelBinding;
import com.sysu.edu.databinding.PreferenceEditBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GradeForLevelActivity extends AppCompatActivity {

    HttpManager http;
    int page = 1;
    int total = -1;
    StaggeredFragment fragment;
    PopupMenu yearPop;
    PopupMenu trainTypePop;
    PopupMenu courseTypePop;

    final MutableLiveData<String> trainType = new MutableLiveData<>();
    final MutableLiveData<String> year = new MutableLiveData<>();
    final MutableLiveData<String> courseType = new MutableLiveData<>();
    final MutableLiveData<String> courseName = new MutableLiveData<>();
    final MutableLiveData<String> courseNumber = new MutableLiveData<>();
    final MutableLiveData<String> minGrade = new MutableLiveData<>();
    MutableLiveData<String> input;
    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityGradeForLevelBinding binding = ActivityGradeForLevelBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Params params = new Params(this);
        params.setCallback(() -> {
            getData(0);
            regetGrade();
        });
        binding.toolbar.setNavigationOnClickListener(v -> supportFinishAfterTransition());
        fragment = binding.fragment.getFragment();
        fragment.setScrollBottom(() -> {
            if ((page - 1) * 10 < total)
                getGrade();
        });
        binding.toolbar.getMenu().add("导出").setIcon(R.drawable.export).setOnMenuItemClickListener(item -> {
            startActivity(new Intent(this, MarkdownViewActivity.class).putExtra("content", fragment.toTable()).putExtra("title", "成绩"),
                    ActivityOptionsCompat.makeSceneTransitionAnimation(this, binding.toolbar, "miniapp").toBundle());
            return false;
        }).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        yearPop = new PopupMenu(this, binding.year);
        trainTypePop = new PopupMenu(this, binding.trainType);
        courseTypePop = new PopupMenu(this, binding.courseType);
        PreferenceEditBinding courseNameEditText = PreferenceEditBinding.inflate(getLayoutInflater());
        PopupWindow courseNamePop = getPopupWindow(courseNameEditText);
        binding.year.setOnClickListener(v -> yearPop.show());
        binding.trainType.setOnClickListener(v -> trainTypePop.show());
        binding.courseType.setOnClickListener(v -> courseTypePop.show());
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
        year.observe(this, s -> regetGrade());
        trainType.observe(this, s -> regetGrade());
        courseType.observe(this, s -> regetGrade());
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
        handler = new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                int what = msg.what;
                if (what == -1) {
                    params.toast(R.string.no_wifi_warning);
                } else {
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    Integer code = response.getInteger("code");
                    if (code == 200) {
                        if (what == 3) {
                            if (total == -1)
                                total = response.getJSONObject("data").getInteger("total");
                            response.getJSONObject("data").getJSONArray("rows").forEach(item -> {
                                ArrayList<String> values = new ArrayList<>();
                                for (String i : new String[]{"achievementPoint", "classesNum", "courseCategoryName", "courseId", "courseName", "courseNum", "credit", "examNatureName", "finalAchievementStr", "grade", "openClassUnitName", "schoolSemester", "sumHours", "trainingCategoryName", "totalAchievement"}) {
                                    values.add(((JSONObject) item).getString(i));
                                }

                                fragment.add(values.get(4), List.of("绩点", "教学班编号", "课程类别", "课程ID", "课程名称", "课程编号", "学分", "考试性质", "等级", "年级", "开设单位", "学期", "总学时", "培养类别", "总成绩"), values);
                            });
                        } else {
                            Menu menu = List.of(trainTypePop, yearPop, courseTypePop).get(what).getMenu();
                            menu.clear();
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                menu.setGroupDividerEnabled(true);
                            }
                            MutableLiveData<String> realLiveDataValue = List.of(trainType, year, courseType).get(what);
                            MaterialButton button = List.of(binding.trainType, binding.year, binding.courseType).get(what);
                            menu.add(0, 0, 0, "重置").setOnMenuItemClickListener(item -> {
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
                            if (what < 2) getData(what + 1);
                        }
                    } else if (code == 53000007) {
                        params.toast(R.string.login_warning);
                        params.gotoLogin(binding.toolbar, TargetUrl.JWXT);
                    } else {
                        params.toast(response.getString("message"));
                    }
                }
            }
        };
        http = new HttpManager(handler);
        http.setParams(params);
        http.setReferrer("https://jwxt.sysu.edu.cn/jwxt/mk/");
        getGrade();
        getData(0);
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
            Editable text = courseNameEditText.textField.getText();
            if (!Objects.equals(input.getValue(), text == null ? "" : text.toString()))
                input.setValue(text == null ? null : text.toString());
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
        http.postRequest("https://jwxt.sysu.edu.cn/jwxt/achievement-manage/achievement/selfPageList", String.format("{\"pageNo\":%s,\"pageSize\":10,\"total\":true,\"param\":%s}", page++, getParam().toString()), 3);
    }

    void getYear() {
        http.getRequest("https://jwxt.sysu.edu.cn/jwxt/base-info/acadyearterm/findAcadyeartermNamesBox", 0);
    }

    void getTrainingType() {
        http.getRequest("https://jwxt.sysu.edu.cn/jwxt/base-info/codedata/findcodedataNames?datableNumber=97", 1);
    }

    void getCourseType() {
        http.getRequest("https://jwxt.sysu.edu.cn/jwxt/base-info/base-category/SfqyBox", 1);
    }

    void getData(int pos) {
        http.getRequest(List.of("https://jwxt.sysu.edu.cn/jwxt/base-info/codedata/findcodedataNames?datableNumber=97",
                "https://jwxt.sysu.edu.cn/jwxt/base-info/acadyearterm/findAcadyeartermNamesBox",
                "https://jwxt.sysu.edu.cn/jwxt/base-info/base-category/SfqyBox").get(pos), pos);
    }

    /*
     * {"categoryCode":"01","schoolSemester":"2025-1","courseTypeCode":"10","courseNum":"编码","courseName":"名称","finalAchievement":0,"achievementState":null}
     * */
    JSONObject getParam() {
        JSONObject params = new JSONObject();
        if (trainType.getValue() != null && !trainType.getValue().isEmpty())
            params.put("categoryCode", trainType.getValue());
        if (year.getValue() != null && !year.getValue().isEmpty())
            params.put("schoolSemester", year.getValue());
        if (courseType.getValue() != null && !courseType.getValue().isEmpty())
            params.put("courseTypeCode", courseType.getValue());
        if (courseName.getValue() != null && !courseName.getValue().isEmpty())
            params.put("courseName", courseName.getValue());
        if (courseNumber.getValue() != null && !courseNumber.getValue().isEmpty())
            params.put("courseNum", courseNumber.getValue());
        if (minGrade.getValue() != null && !minGrade.getValue().isEmpty())
            params.put("finalAchievement", Integer.parseInt(minGrade.getValue()));
        return params;
    }
}