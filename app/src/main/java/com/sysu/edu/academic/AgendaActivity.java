package com.sysu.edu.academic;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridLayout;
import android.widget.PopupMenu;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textview.MaterialTextView;
import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.ActivityAgendaBinding;
import com.sysu.edu.databinding.ItemAgendaBinding;
import com.sysu.edu.databinding.ItemDetailBinding;
import com.sysu.edu.databinding.ItemDurationBinding;
import com.sysu.edu.databinding.ItemWeekdayBinding;
import com.sysu.edu.extra.LoginActivity;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AgendaActivity extends AppCompatActivity {

    String cookie;
    Handler handler;
    ArrayList<String> terms = new ArrayList<>();
    ArrayList<Integer> weeks = new ArrayList<>();
    ActivityResultLauncher<Intent> launch;
    PopupMenu termPop;
    OkHttpClient http = new OkHttpClient.Builder().build();
    PopupMenu weekPop;
    String currentTerm = "";
    int currentWeekIndex = -1;
    ArrayList<View> views = new ArrayList<>();
    int currentWeek;
    BottomSheetDialog detailDialog;
    ActivityAgendaBinding binding;
    Params params;
    ItemDetailBinding detailBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAgendaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        params = new Params(this);
        cookie = params.getCookie();
        binding.toolbar.setNavigationOnClickListener(v -> supportFinishAfterTransition());
        String[] duration = getResources().getStringArray(R.array.duration);
        binding.toolbar.getMenu().add(R.string.today).setOnMenuItemClickListener(menuItem -> {
            getTable(currentTerm, currentWeek);
            getRange(currentTerm, currentWeek);
            return false;
        }).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        //binding.month.setText(new SimpleDateFormat("M月", Locale.CHINESE).format(new Date()));
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        binding.month.setText(getResources().getStringArray(R.array.months)[calendar.get(Calendar.MONTH)-1]);
        int weekday = calendar.get(Calendar.DAY_OF_WEEK);
        weekday = (weekday == 1) ? 8 : weekday;
        binding.last.setOnClickListener(v -> changeWeek(currentWeekIndex - 1));
        binding.next.setOnClickListener(v -> changeWeek(currentWeekIndex + 1));
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
        }
        for (int i = 0; i < 7; i++) {
            ItemWeekdayBinding itemBinding = ItemWeekdayBinding.inflate(getLayoutInflater(), binding.week, false);
            itemBinding.courseWeek.setText(getResources().getStringArray(R.array.weeks)[i]);
            itemBinding.courseDate.setText(getOldDate(i+2- weekday));
            View column = new View(this);
            if (i + 2 == weekday) {
                TypedValue typedValue = new TypedValue();
                getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurfaceDim, typedValue, true);
                itemBinding.courseDate.setTextColor(typedValue.data);
                itemBinding.courseWeek.setTextColor(typedValue.data);
                itemBinding.getRoot().setBackgroundResource(R.drawable.weekday);
                column.setBackground(new ColorDrawable(typedValue.data));
            }
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(GridLayout.spec(0, 11, 1.0f), GridLayout.spec(i + 1, 1.0f));
            lp.height = 0;
            lp.width = 0;
            lp.setGravity(Gravity.FILL);
            column.setLayoutParams(lp);
            binding.day.addView(column);
            binding.week.addView(itemBinding.getRoot());
        }
        launch = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
            if (o.getResultCode() == RESULT_OK) {
                cookie = params.getCookie();
                if (currentTerm != null) {
                    getTable(currentTerm, currentWeek);
                } else {
                    getTerm();
                    getAvailableTerms();
                }
            }
        });
        binding.term.setOnClickListener(v -> {
            if (termPop == null) {
                termPop = new PopupMenu(v.getContext(), v, 0, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow);
                terms.forEach(e -> termPop.getMenu().add(String.format(getString(R.string.term_x), e)).setOnMenuItemClickListener(item -> {
                    changeTerm(e);
                    return true;
                }));
            }
            termPop.show();
        });
        binding.weekTime.setOnClickListener(v -> {
            if (weekPop == null) {
                weekPop = new PopupMenu(v.getContext(), v, 0, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow);
                this.weeks.forEach(e -> weekPop.getMenu().add(String.format(getString(R.string.week_x), e)).setOnMenuItemClickListener(item -> {
                    changeWeek(this.weeks.indexOf(e));
                    return true;
                }));
            }
            weekPop.show();
        });
        detailDialog = new BottomSheetDialog(this);
        detailBinding = ItemDetailBinding.inflate(getLayoutInflater());
        detailDialog.setContentView(detailBinding.getRoot());
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                JSONObject response = JSONObject.parseObject((String) msg.obj);
                if (response.getInteger("code").equals(200)) {
                    switch (msg.what) {
                        case 1: {
                            views.forEach(e -> binding.day.removeView(e));
                            views.clear();
                            response.getJSONArray("data").forEach(e -> {
                                String week = ((JSONObject) e).getString("week");
                                String startClassTimes = ((JSONObject) e).getString("startClassTimes");
                                String endClassTimes = ((JSONObject) e).getString("endClassTimes");
                                JSONArray info = ((JSONObject) e).getJSONArray("teachingInfoList");
                                JSONObject detail = (JSONObject) info.get(0);
                                String course = detail.getString("courseName");
                                String teacher = detail.getString("teacherName");
                                String campus = detail.getString("teachingCampusName");
                                String isStop = detail.getString("whetherStopClass");
                                String teachingBuildingName = detail.getString("teachingBuildingName");
                                String classroomNum = detail.getString("classroomNum");
                                ItemAgendaBinding itemAgendaBinding = ItemAgendaBinding.inflate(getLayoutInflater(), binding.day, false);
                                View item = itemAgendaBinding.getRoot();
                                if (isStop != null && !isStop.equals("0")) {
                                    item.setEnabled(false);
                                    item.setBackgroundColor(getColor(R.color.teal_700));
                                }
                                views.add(item);
                                item.setOnClickListener(v -> {
                                    String location = (campus == null ? "" : campus) + "-" + (teachingBuildingName == null ? "" : teachingBuildingName) + "-" + (classroomNum == null ? "" : classroomNum);
                                    setDialogDetail(course, location, teacher, String.format(getString(R.string.from_to), startClassTimes, endClassTimes));
                                    detailDialog.show();
                                    //setDetail(course, location,teacher,String.format("第%s节到第%s节",startClassTimes,endClassTimes));
                                });
                                itemAgendaBinding.content.setText(String.format("%s/%s-%s", course, teachingBuildingName == null ? "" : teachingBuildingName, classroomNum == null ? "" : classroomNum));
                                GridLayout.LayoutParams gl = new GridLayout.LayoutParams();
                                gl.columnSpec = GridLayout.spec(Integer.parseInt(week), 1.0f);
                                gl.width = 0;
                                gl.height = 0;
                                gl.setGravity(Gravity.FILL);
                                gl.rowSpec = GridLayout.spec(Integer.parseInt(startClassTimes) - 1, Integer.parseInt(endClassTimes) - Integer.parseInt(startClassTimes) + 1, 1.0f);
                                item.setLayoutParams(gl);
                                binding.day.addView(item);
                            });
                            break;
                        }
                        case 2: {
                            currentTerm = response.getJSONObject("data").getString("acadYearSemester");
                            binding.term.setText(currentTerm);
                            getAvailableWeeks(currentTerm);
                            //getRange(currentTerm,currentWeek);
                            getTable(currentTerm, currentWeek);
                            break;
                        }
                        case 3: {
                            String from = response.getJSONObject("data").getString("startTime");
                            try {
                                Calendar c = Calendar.getInstance();
                                Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINESE).parse(from);
                                if (date != null) {
                                    c.setTime(date);
                                    //binding.month.setText(new SimpleDateFormat("M月", Locale.CHINESE).format(date.getTime()));
                                    calendar.setTime(date);
                                    binding.month.setText(getResources().getStringArray(R.array.months)[calendar.get(Calendar.MONTH)-1]);
                                }
                                for (int i = 0; i < 7; i++) {
                                    ((MaterialTextView) binding.week.getChildAt(i + 1).findViewById(R.id.course_date)).setText((new SimpleDateFormat("dd").format(c.getTime()))+getString(R.string.day));
                                    c.add(Calendar.DATE, 1);
                                }
                            } catch (ParseException e) {
                                throw new RuntimeException(e);
                            }
                            break;
                        }
                        case 4: {
                            terms.clear();
                            response.getJSONArray("data").forEach(e -> terms.add(((JSONObject) e).getString("acadYearSemester")));
                            break;
                        }
                        case 5: {
                            AgendaActivity.this.weeks.clear();
                            String nowWeekly = response.getJSONObject("data").getString("nowWeekly");
                            if (nowWeekly != null) {
                                currentWeek = Integer.parseInt(nowWeekly);
                            }
                            response.getJSONObject("data").getJSONArray("weeklyList").forEach(e -> AgendaActivity.this.weeks.add(((JSONObject) e).getInteger("weekly")));
                            currentWeekIndex = AgendaActivity.this.weeks.indexOf(currentWeek);
                            binding.weekTime.setText(String.format(getString(R.string.week_x), currentWeek));
                            getTable(currentTerm, currentWeek);
                            break;
                        }
                        case -1: {
                            params.toast(R.string.no_wifi_warning);
                        }
                    }
                } else {
                    params.toast(R.string.login_warning);
                    launch.launch(new Intent(AgendaActivity.this, LoginActivity.class).putExtra("url", TargetUrl.JWXT));
                }
            }
        };
        getTerm();
        getAvailableTerms();
    }

    public void getAvailableWeeks(String academicYear) {
        getResponse("https://jwxt.sysu.edu.cn/jwxt/base-info/school-calender/weekly?academicYear=" + academicYear,5);
    }

    void getAvailableTerms() {
        getResponse("https://jwxt.sysu.edu.cn/jwxt/base-info/acadyearterm/findAcadyeartermNamesBox",4);
    }

    String getOldDate(int distanceDay) {
        Calendar date = Calendar.getInstance();
        date.add(Calendar.DATE, distanceDay);
        return new SimpleDateFormat("dd", Locale.CHINESE).format(date.getTime())+getString(R.string.day);
    }

    void changeTerm(String newTerm) {
        if (!newTerm.equals(currentTerm)) {
            currentTerm = newTerm;
            binding.term.setText(currentTerm);
            getAvailableWeeks(currentTerm);
            getTable(currentTerm, currentWeek);
        }
    }

    void getRange(String academicYear, int week) {
        getResponse(String.format(Locale.getDefault(), "https://jwxt.sysu.edu.cn/jwxt/base-info/school-calender?academicYear=%s&weekly=%d", academicYear, week),3);
    }

    void setDialogDetail(String course, String location, String teacher, String classTime) {
        detailBinding.course.setText(course);
        detailBinding.location.setText(location);
        detailBinding.teacher.setText(teacher);
        detailBinding.classTime.setText(classTime);
    }
//    void setDetail(String course,String location,String teacher,String classTime){

    /// /        ((MaterialTextView) findViewById(R.id.course)).setText(course);
    /// /        ((MaterialTextView) findViewById(R.id.location)).setText(location);
    /// /        ((MaterialTextView) findViewById(R.id.teacher)).setText(teacher);
    /// /        ((MaterialTextView) findViewById(R.id.classTime)).setText(classTime);
//    }
    void changeWeek(int newWeek) {
        if (newWeek >= 0) {
            int currentWeek = weeks.get(newWeek);
            currentWeekIndex = newWeek;
            binding.weekTime.setText(String.format(getString(R.string.week_x), currentWeek));
            getTable(currentTerm, currentWeek);
            getRange(currentTerm, currentWeek);
        }
    }
    void getTable(String academicYear, int week) {
        if (academicYear.isEmpty() || week < 1) {
            return;
        }
        getResponse(String.format(Locale.getDefault(), "https://jwxt.sysu.edu.cn/jwxt/timetable-search/classTableInfo/queryStudentClassTable?academicYear=%s&weekly=%d", academicYear, week),1);
    }
    void getTerm() {
        getResponse("https://jwxt.sysu.edu.cn/jwxt/base-info/acadyearterm/showNewAcadlist",2);
    }
    void getResponse(String url,int what) {
        http.newCall(new Request.Builder().url(url)
                        .header("Referer","https://jwxt.sysu.edu.cn/jwxt//yd/classSchedule/")
                .header("Cookie", cookie).build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = new Message();
                msg.what = -1;
                handler.sendMessage(msg);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what = what;
                msg.obj = response.body().string();
                handler.sendMessage(msg);
            }
        });
    }
}