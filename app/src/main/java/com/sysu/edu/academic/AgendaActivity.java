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
import com.sysu.edu.api.Http;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.AgendaBinding;
import com.sysu.edu.databinding.AgendaItemBinding;
import com.sysu.edu.databinding.DetailBinding;
import com.sysu.edu.databinding.DurationBinding;
import com.sysu.edu.databinding.WeekdayBinding;
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
    ArrayList<String> terms=new ArrayList<>();
    ArrayList<Integer> weeks=new ArrayList<>();
    ActivityResultLauncher<Intent> launch;
    PopupMenu termPop;
    OkHttpClient http;
    PopupMenu weekPop;
    String currentTerm="";
    int currentWeekIndex =-1;
    ArrayList<View> views= new ArrayList<>();
    int currentWeek;
    BottomSheetDialog detailDialog;
    AgendaBinding binding;
    Params params;
    DetailBinding detailBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        http = (new Http(this)).getJwxtHttpWithReferer("https://jwxt.sysu.edu.cn/jwxt//yd/classSchedule/");
        binding = AgendaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        params = new Params(this);
        cookie=params.getCookie();
        binding.toolbar.setNavigationOnClickListener(v->supportFinishAfterTransition());
        String[] duration = getResources().getStringArray(R.array.duration);
        binding.toolbar.getMenu().add(R.string.today).setOnMenuItemClickListener(menuItem -> {
            getTable(currentTerm, currentWeek);
            getRange(currentTerm,currentWeek);
            return false;
        }).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        binding.month.setText(new SimpleDateFormat("M月", Locale.CHINESE).format(new Date()));
        int weekday = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        //weekday=(weekday==1)?weekday:8;
        binding.last.setOnClickListener(v -> changeWeek(currentWeekIndex-1));
        binding.next.setOnClickListener(v -> changeWeek(currentWeekIndex+1));
        String[] ws = getResources().getStringArray(R.array.weeks);

        for (int i=0;i<duration.length;i++)
        {
            DurationBinding durationBinding = DurationBinding.inflate(getLayoutInflater(),binding.day,false);
            durationBinding.courseDuration.setText(duration[i].replace("~","\n"));
            durationBinding.courseOrder.setText(String.valueOf(i+1));
            if(i==10){
                durationBinding.getRoot().measure(View.MEASURED_SIZE_MASK,View.MEASURED_SIZE_MASK);
                binding.month.getLayoutParams().width=durationBinding.getRoot().getMeasuredWidth();
            }GridLayout.LayoutParams gl = new GridLayout.LayoutParams();
            //GridLayout.LayoutParams gl = new GridLayout.LayoutParams(GridLayout.spec(i,1.0f),GridLayout.spec(0,1.0f));
            //GridLayout.LayoutParams) durationBinding.getRoot().getLayoutParams();
//            gl.width=0;
//            gl.height=-2;
//            gl.setGravity(Gravity.FILL);
//            gl.columnSpec=GridLayout.spec(0);
            durationBinding.getRoot().setLayoutParams(gl);
            binding.day.addView(durationBinding.getRoot());
        }
        for(int i=0;i<7;i++){
            WeekdayBinding itemBinding = WeekdayBinding.inflate(getLayoutInflater(),binding.week,false);
            itemBinding.courseWeek.setText(ws[i]);
            itemBinding.courseDate.setText(getOldDate(i-weekday+2));
//            new View[]{
//                    ,binding.c2,binding.c3,binding.c4,binding.c5,binding.c6,binding.c7
//            }[i]

            View column = new View(this);
            if(i+2==weekday){
                TypedValue typedValue = new TypedValue();
                getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurfaceDim, typedValue, true);
                itemBinding.courseDate.setTextColor(typedValue.data);
               // ((GridLayout.LayoutParams)binding.c1.getLayoutParams()).columnSpec=GridLayout.spec(i+1,1.0f);
               // binding.c1.setBackground(new ColorDrawable(typedValue.data));
                itemBinding.courseWeek.setTextColor(typedValue.data);
                itemBinding.getRoot().setBackgroundResource(R.drawable.weekday);
                column.setBackground(new ColorDrawable(typedValue.data));
            }
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(GridLayout.spec(0,11,1.0f),GridLayout.spec(i+1,1.0f));
            lp.height=0;
            lp.width=0;
            lp.setGravity(Gravity.FILL);
            column.setLayoutParams(lp);
            binding.day.addView(column);
            binding.week.addView(itemBinding.getRoot());
        }

        launch = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
            if(o.getResultCode()==RESULT_OK){
                cookie=params.getCookie();
                if(currentTerm!=null){getTable(currentTerm,currentWeek);} else{getTerm();getAvailableTerms();}
            }
        });
        binding.term.setOnClickListener(v -> {
            if(termPop==null){
                termPop = new PopupMenu(v.getContext(), v,0, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow);
                terms.forEach(e->termPop.getMenu().add(String.format(a(R.string.term_x), e)).setOnMenuItemClickListener(item -> {
                    changeTerm(e);
                    return true;
                }));
            }
            termPop.show();
        });
        binding.weekTime.setOnClickListener(v -> {
            if(weekPop==null){
                weekPop = new PopupMenu(v.getContext(), v,0, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow);
                weeks.forEach(e->weekPop.getMenu().add(String.format(a(R.string.week_x), e)).setOnMenuItemClickListener(item -> {
                    changeWeek(weeks.indexOf(e));
                    return true;
                }));
            }
            weekPop.show();
        });
        detailDialog=new BottomSheetDialog(this);
        detailBinding = DetailBinding.inflate(getLayoutInflater());
        detailDialog.setContentView(detailBinding.getRoot());
        handler=new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                JSONObject response = JSONObject.parseObject((String) msg.obj);
                if (response.getInteger("code").equals(200)) {
                    switch(msg.what) {
                        case 1: {
                            views.forEach(e->binding.day.removeView(e));
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
                                AgendaItemBinding agendaItemBinding = AgendaItemBinding.inflate(getLayoutInflater(),binding.day,false);
                                View item = agendaItemBinding.getRoot();
                                if(isStop!=null&&!isStop.equals("0")){item.setEnabled(false);item.setBackgroundColor(getColor(R.color.teal_700));}
                                views.add(item);
                                item.setOnClickListener(v -> {
                                    String location = (campus == null ? "" : campus) + "-" + (teachingBuildingName == null ? "" : teachingBuildingName) + "-" + (classroomNum == null ? "" : classroomNum);
                                    setDialogDetail(course, location,teacher,String.format("第%s节到第%s节",startClassTimes,endClassTimes));
                                    detailDialog.show();
                                    //setDetail(course, location,teacher,String.format("第%s节到第%s节",startClassTimes,endClassTimes));
                                });
                                agendaItemBinding.content.setText(String.format("%s/%s-%s", course, teachingBuildingName == null ? "" : teachingBuildingName, classroomNum == null ? "" : classroomNum));
                                GridLayout.LayoutParams gl = new GridLayout.LayoutParams();
                                gl.columnSpec = GridLayout.spec(Integer.parseInt(week),1.0f);
                                gl.width=0;
                                gl.height=0;
                                gl.setGravity(Gravity.FILL);
                                gl.rowSpec = GridLayout.spec(Integer.parseInt(startClassTimes) - 1, Integer.parseInt(endClassTimes) - Integer.parseInt(startClassTimes) + 1,1.0f);
                                //gl.rowSpec = GridLayout.spec(0,4,1.0f);
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
                            getTable(currentTerm,currentWeek);
                            break;
                        }case 3: {
                            String from = response.getJSONObject("data").getString("startTime");
                            try {
                                Calendar c = Calendar.getInstance();
                                Date date = new SimpleDateFormat("yyyy-MM-dd",Locale.CHINESE).parse(from);
                                if(date!=null){c.setTime(date);
                                    binding.month.setText(new SimpleDateFormat("M月",Locale.CHINESE).format(date.getTime()));
                                }
                                for(int i=0;i<7;i++){
                                    ((MaterialTextView) binding.week.getChildAt(i+1).findViewById(R.id.course_date)).setText(new SimpleDateFormat("dd日",Locale.CHINESE).format(c.getTime()));
                                    c.add(Calendar.DATE,1);
                                }
                            } catch (ParseException e) {
                                throw new RuntimeException(e);
                            }
                            break;
                        } case 4:{
                            terms.clear();
                            response.getJSONArray("data").forEach(e->terms.add(((JSONObject)e).getString("acadYearSemester")));
                            break;
                        }case 5:{
                            weeks.clear();
                            String nowWeekly=response.getJSONObject("data").getString("nowWeekly");
                            if(nowWeekly!=null){currentWeek = Integer.parseInt(nowWeekly);}
                            response.getJSONObject("data").getJSONArray("weeklyList").forEach(e->weeks.add(((JSONObject)e).getInteger("weekly")));
                            currentWeekIndex = weeks.indexOf(currentWeek);
                            binding.weekTime.setText(String.format(a(R.string.week_x), currentWeek));
                            getTable(currentTerm, currentWeek);
                            break;
                        } case -1:{
                            params.toast(R.string.no_wifi_warning);
                        }
                    }
                }else{
                    params.toast(R.string.login_warning);
                    launch.launch(new Intent(AgendaActivity.this, LoginActivity.class));
                }
            }
        };
        getTerm();
        getAvailableTerms();
    }
    public void getAvailableWeeks(String academicYear){
        http.newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/base-info/school-calender/weekly?academicYear="+academicYear)
                .header("Cookie",cookie).build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = new Message();
                msg.what=-1;
                handler.sendMessage(msg);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what=5;
                msg.obj=response.body().string();
                handler.sendMessage(msg);
            }
        });
    }
    public void getAvailableTerms(){
        http.newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/base-info/acadyearterm/findAcadyeartermNamesBox")
                .header("Cookie",cookie).build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = new Message();
                msg.what=-1;
                handler.sendMessage(msg);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what=4;
                msg.obj=response.body().string();
                handler.sendMessage(msg);
            }
        });
    }
    String getOldDate(int distanceDay) {
        Calendar date = Calendar.getInstance();
        date.add(Calendar.DATE,distanceDay);
        return new SimpleDateFormat("dd日",Locale.CHINESE).format(date.getTime());
    }
    void changeTerm(String newTerm){
        if(!newTerm.equals(currentTerm)){
            currentTerm = newTerm;
            binding.term.setText(currentTerm);
            getAvailableWeeks(currentTerm);
            getTable(currentTerm,currentWeek);
        }
    }
    void getRange(String academicYear,int week)
    {
        http.newCall(new Request.Builder().url(String.format(Locale.CHINA,"https://jwxt.sysu.edu.cn/jwxt/base-info/school-calender?academicYear=%s&weekly=%d",academicYear,week))
                .header("Cookie",cookie).build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = new Message();
                msg.what=-1;
                handler.sendMessage(msg);
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what=3;
                msg.obj=response.body().string();
                handler.sendMessage(msg);
            }
        });
    }
    String a(int i){return getString(i);}
    void setDialogDetail(String course,String location,String teacher,String classTime){
        detailBinding.course.setText(course);
        detailBinding.location.setText(location);
        detailBinding.teacher.setText(teacher);
        detailBinding.classTime.setText(classTime);
    }
//    void setDetail(String course,String location,String teacher,String classTime){
    ////        ((MaterialTextView) findViewById(R.id.course)).setText(course);
    ////        ((MaterialTextView) findViewById(R.id.location)).setText(location);
    ////        ((MaterialTextView) findViewById(R.id.teacher)).setText(teacher);
    ////        ((MaterialTextView) findViewById(R.id.classTime)).setText(classTime);
//    }
    void changeWeek(int newWeek){
        if(newWeek>=0){
            int currentWeek = weeks.get(newWeek);
            currentWeekIndex =newWeek;
            binding.weekTime.setText(String.format(a(R.string.week_x),currentWeek));
            getTable(currentTerm,currentWeek);
            getRange(currentTerm,currentWeek);
        }
    }
    public void getTable(String academicYear,int week){
        if(academicYear.isEmpty()||week<1){return;}
        http.newCall(new Request.Builder().url(String.format(Locale.CHINA,"https://jwxt.sysu.edu.cn/jwxt/timetable-search/classTableInfo/queryStudentClassTable?academicYear=%s&weekly=%d",academicYear,week)).header("Cookie",cookie).build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = new Message();
                msg.what=-1;
                handler.sendMessage(msg);
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what=1;
                msg.obj=response.body().string();
                handler.sendMessage(msg);
            }
        });
    }
    public void getTerm(){
        http.newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/base-info/acadyearterm/showNewAcadlist")
                .header("Cookie",cookie).build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = new Message();
                msg.what=-1;
                handler.sendMessage(msg);
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what=2;
                msg.obj=response.body().string();
                handler.sendMessage(msg);
            }
        });
    }
}