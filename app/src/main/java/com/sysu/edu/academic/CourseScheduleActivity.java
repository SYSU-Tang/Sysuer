package com.sysu.edu.academic;

import static com.sysu.edu.api.CommonUtil.toStringOrDefault;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridLayout;
import android.widget.PopupMenu;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.lifecycle.MutableLiveData;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textview.MaterialTextView;
import com.sysu.edu.R;
import com.sysu.edu.api.CommonUtil;
import com.sysu.edu.databinding.ActivityCourseScheduleBinding;
import com.sysu.edu.databinding.ItemAgendaBinding;
import com.sysu.edu.databinding.ItemDetailBinding;
import com.sysu.edu.databinding.ItemDurationBinding;
import com.sysu.edu.databinding.ItemWeekdayBinding;
import com.sysu.edu.model.JwxtModel;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;


public class CourseScheduleActivity extends AppCompatActivity {
    
    final ArrayList<String> terms = new ArrayList<>();
    final ArrayList<Integer> weeks = new ArrayList<>();
    final ArrayList<View> views = new ArrayList<>();
    final MutableLiveData<String> id = new MutableLiveData<>();
    final CommonUtil.Tuple2<String, Integer> realTime = new CommonUtil.Tuple2<>();
    PopupMenu termPop;
    PopupMenu weekPop;
    String currentTerm = "";
    int currentWeekIndex = -1;
    int currentWeek;
    BottomSheetDialog detailDialog;
    ActivityCourseScheduleBinding binding;
    ItemDetailBinding detailBinding;
    JwxtModel model;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCourseScheduleBinding.inflate(getLayoutInflater());
        model = new JwxtModel(this);
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(_ -> supportFinishAfterTransition());
        String[] duration = getResources().getStringArray(R.array.duration);
        binding.toolbar.getMenu().add(R.string.today).setOnMenuItemClickListener(_ -> {
            changeTerm(realTime.first);
            changeWeek(realTime.second);
            return false;
        }).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        int weekday = LocalDate.now().getDayOfWeek().getValue() - 1;
        binding.month.setText(getResources().getStringArray(R.array.months)[LocalDate.now().getMonthValue() - 1]);
        binding.last.setOnClickListener(_ -> changeWeek(currentWeekIndex - 1));
        binding.next.setOnClickListener(_ -> changeWeek(currentWeekIndex + 1));
        for (int i = 0; i < duration.length; i++) {
            ItemDurationBinding durationBinding = ItemDurationBinding.inflate(getLayoutInflater(), binding.day, false);
            durationBinding.courseDuration.setText(duration[i].replace("~", "\n"));
            durationBinding.courseOrder.setText(String.valueOf(i + 1));
            if (i == 10) {
                durationBinding.getRoot().measure(View.MEASURED_SIZE_MASK, View.MEASURED_SIZE_MASK);
                binding.month.getLayoutParams().width = durationBinding.getRoot().getMeasuredWidth();
            }
            durationBinding.getRoot().setLayoutParams(new GridLayout.LayoutParams());
            binding.day.addView(durationBinding.getRoot());
        } // 初始化课程时间
        for (int i = 0; i < 7; i++) {
            ItemWeekdayBinding itemBinding = ItemWeekdayBinding.inflate(getLayoutInflater(), binding.week, false);
            itemBinding.courseWeek.setText(getResources().getStringArray(R.array.weeks_simple)[i]);
            itemBinding.courseDate.setText(getOldDate(i - weekday));
            View column = new View(this);
            if (i == weekday) {
                int color = model.getContextUtil().getColorFromAttr(com.google.android.material.R.attr.colorSurfaceDim);
                itemBinding.courseDate.setTextColor(color);
                itemBinding.courseWeek.setTextColor(color);
                itemBinding.getRoot().setBackgroundResource(R.drawable.weekday);
                column.setBackgroundColor(color);
            }
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(GridLayout.spec(0, 11, 1.0f), GridLayout.spec(i + 1, 1.0f));
            lp.height = 0;
            lp.width = 0;
            lp.setGravity(Gravity.FILL);
            column.setLayoutParams(lp);
            binding.day.addView(column);
            binding.week.addView(itemBinding.getRoot());
        } // 初始化周历
        binding.term.setOnClickListener(v -> {
            if (termPop == null) {
                termPop = new PopupMenu(v.getContext(), v, 0, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow);
                terms.forEach(e -> termPop.getMenu().add(String.format(getString(R.string.term_x), e)).setOnMenuItemClickListener(_ -> {
                    changeTerm(e);
                    return true;
                }));
            }
            termPop.show();
        }); // 初始化学期选择
        binding.weekTime.setOnClickListener(v -> {
            if (weekPop == null) {
                weekPop = new PopupMenu(v.getContext(), v, 0, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow);
                weeks.forEach(e -> weekPop.getMenu().add(String.format(getString(R.string.week_d), e)).setOnMenuItemClickListener(_ -> {
                    changeWeek(weeks.indexOf(e));
                    return true;
                }));
            }
            weekPop.show();
        }); // 初始化周次选择
        detailDialog = new BottomSheetDialog(this);
        detailBinding = ItemDetailBinding.inflate(getLayoutInflater());
        detailBinding.open.setOnClickListener(v -> startActivity(new Intent(this, CourseDetailActivity.class).putExtra("id", id.getValue()), ActivityOptionsCompat.makeSceneTransitionAnimation(this, v, "miniapp").toBundle())); // 初始化打开链接
        detailDialog.setContentView(detailBinding.getRoot());
        model.getMessage().observe(this, message -> {
            JSONObject response = (JSONObject) message.second;
            if (response.getInteger("code").equals(200)) {
                switch (message.getFirst()) {
                    case 1 -> {
                        views.forEach(e -> binding.day.removeView(e));
                        views.clear();
                        response.getJSONArray("data").forEach(e -> {
                            JSONObject data = (JSONObject) e;
                            String week = data.getString("week");
                            if (week != null) {
                                String startClassTimes = data.getString("startClassTimes");
                                String endClassTimes = data.getString("endClassTimes");
                                JSONArray info = data.getJSONArray("teachingInfoList");
                                info.forEach(o -> {
                                    JSONObject detail = (JSONObject) o;
                                    String course = detail.getString("courseName");
                                    String teacher = detail.getString("teacherName");
                                    String campus = detail.getString("teachingCampusName");
                                    String isStop = detail.getString("whetherStopClass");
                                    String teachingBuildingName = detail.getString("teachingBuildingName");
                                    String classroomNum = detail.getString("classroomNum");
                                    ItemAgendaBinding itemAgendaBinding = ItemAgendaBinding.inflate(getLayoutInflater(), binding.day, false);
                                    MaterialCardView item = itemAgendaBinding.getRoot();
                                    if (isStop != null && !"0".equals(isStop)) {
                                        item.setEnabled(false);
                                        item.setCardBackgroundColor(model.getContextUtil().getColorFromAttr(com.google.android.material.R.attr.colorErrorContainer));
                                    }
                                    views.add(item);
                                    item.setOnClickListener(_ -> {
                                        String location = toStringOrDefault(campus) + "-" + toStringOrDefault(teachingBuildingName) + "-" + toStringOrDefault(classroomNum);
                                        setDialogDetail(course, location, teacher, String.format(getString(R.string.from_to), startClassTimes, endClassTimes), detail.getString("assistantInfo"));
                                        id.setValue(detail.getString("classesId"));
                                        detailDialog.show();
                                    });
                                    itemAgendaBinding.content.setText(String.format("%s/%s-%s", course, toStringOrDefault(teachingBuildingName), toStringOrDefault(classroomNum)));
                                    GridLayout.LayoutParams gl = new GridLayout.LayoutParams();
                                    gl.columnSpec = GridLayout.spec(Integer.parseInt(week), 1.0f);
                                    gl.width = 0;
                                    gl.height = 0;
                                    gl.setGravity(Gravity.FILL);
                                    gl.rowSpec = GridLayout.spec(Integer.parseInt(startClassTimes) - 1, Integer.parseInt(endClassTimes) - Integer.parseInt(startClassTimes) + 1, 1.0f);
                                    item.setLayoutParams(gl);
                                    binding.day.addView(item);
                                });
                            }
                        });
                    }
                    case 2 -> {
                        currentTerm = response.getJSONObject("data").getString("acadYearSemester");
                        binding.term.setText(currentTerm);
                        getAvailableTerms();
                        getAvailableWeeks(currentTerm);
                        getTable(currentTerm, currentWeek);
                        realTime.setFirst(currentTerm);
                    }// 获取 Term
                    case 3 -> {
                        JSONObject data = response.getJSONObject("data");
                        if (data != null) {
                            LocalDate date = LocalDate.parse(data.getString("startTime"), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                            if (date != null) {
                                binding.month.setText(getResources().getStringArray(R.array.months)[date.getMonthValue() - 1]);
                                for (int i = 0; i < 7; i++)
                                    ((MaterialTextView) binding.week.getChildAt(i + 1).findViewById(R.id.course_date)).setText(String.format(Locale.getDefault(), "%2d%s", date.plusDays(i).getDayOfMonth(), getString(R.string.day)));
                            }
                        }
                    }
                    case 4 -> {
                        terms.clear();
                        response.getJSONArray("data").forEach(e -> terms.add(((JSONObject) e).getString("acadYearSemester")));
                    }// 获取 Term
                    case 5 -> {
                        weeks.clear();
                        String nowWeekly = response.getJSONObject("data").getString("nowWeekly");
                        if (nowWeekly != null) currentWeek = Integer.parseInt(nowWeekly);
                        response.getJSONObject("data").getJSONArray("weeklyList").forEach(e -> weeks.add(((JSONObject) e).getInteger("weekly")));
                        currentWeekIndex = weeks.indexOf(currentWeek);
                        binding.weekTime.setText(String.format(getString(R.string.week_d), currentWeek));
                        getTable(currentTerm, currentWeek);
                        realTime.setSecond(currentWeekIndex);
                    }
                }
                model.nextAll();
            } 
        });
        getTerm();
        model.next();
    }
    
    void getAvailableWeeks(String academicYear) {
        model.add("jwxt/base-info/school-calender/weekly?academicYear=" + academicYear, 5);
    }
    
    void getAvailableTerms() {
        model.add("jwxt/base-info/acadyearterm/findAcadyeartermNamesBox", 4);
    }
    
    String getOldDate(int distanceDay) {
        return LocalDate.now().plusDays(distanceDay).getDayOfMonth() + getString(R.string.day);
    }
    
    void changeTerm(String newTerm) {
        if (!newTerm.equals(currentTerm)) {
            currentTerm = newTerm;
            binding.term.setText(currentTerm);
            getAvailableWeeks(currentTerm);
            getTable(currentTerm, currentWeek);
            getRange(currentTerm, currentWeek);
            model.nextAll();
        }
    }
    
    void getRange(String academicYear, int week) {
        model.add(String.format(Locale.getDefault(), "jwxt/base-info/school-calender?academicYear=%s&weekly=%d", academicYear, week), 3);
    }
    
    void setDialogDetail(String course, String location, String teacher, String classTime, String assistant) {
        detailBinding.course.setText(course);
        detailBinding.location.setText(location);
        detailBinding.teacher.setText(teacher);
        detailBinding.classTime.setText(classTime);
        detailBinding.assistant.setText(assistant);
    }
    
    void changeWeek(int newWeek) {
        if (newWeek >= 0 && newWeek < weeks.size()) {
            currentWeek = weeks.get(newWeek);
            currentWeekIndex = newWeek;
            binding.weekTime.setText(String.format(getString(R.string.week_d), currentWeek));
            getTable(currentTerm, currentWeek);
            getRange(currentTerm, currentWeek);
            model.nextAll();
        } else if (newWeek == weeks.size())
            model.getContextUtil().toast(R.string.last_week_warning);
    }
    
    void getTable(String academicYear, int week) {
        if (!academicYear.isEmpty() && week > 0)
            model.add(String.format(Locale.getDefault(), "jwxt/timetable-search/classTableInfo/queryStudentClassTable?academicYear=%s&weekly=%d", academicYear, week), 1);
    }
    
    void getTerm() {
        model.add("jwxt/base-info/acadyearterm/showNewAcadlist", 2);
    }
}