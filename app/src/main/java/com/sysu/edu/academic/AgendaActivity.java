package com.sysu.edu.academic;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textview.MaterialTextView;
import com.sysu.edu.R;
import com.sysu.edu.databinding.AgendaBinding;

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
    PopupMenu weekPop;
    String currentTerm="";
    int currentWeekIndex =-1;
    ArrayList<View> views= new ArrayList<>();
    int currentWeek;
    BottomSheetDialog detailDialog;
    private AgendaBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = AgendaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Toolbar toolbar = binding.toolbar;
        cookie=getSharedPreferences("privacy",MODE_PRIVATE).getString("Cookie","");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        String[] duration = getResources().getStringArray(R.array.duration);
        for (int i=0;i<11;i++)
        {
            LinearLayout row_item = (LinearLayout) getLayoutInflater().inflate(R.layout.duration, binding.day, false);
            ((MaterialTextView)row_item.findViewById(R.id.course_order)).setText(String.valueOf(i+1));
            ((MaterialTextView)row_item.findViewById(R.id.course_duration)).setText(duration[i].replace("~","\n"));
            if(i==10){
                row_item.measure(View.MEASURED_SIZE_MASK,View.MEASURED_SIZE_MASK);
                binding.month.getLayoutParams().width=row_item.getMeasuredWidth();}
            GridLayout.LayoutParams gl = (GridLayout.LayoutParams) row_item.getLayoutParams();
            gl.rowSpec=GridLayout.spec(i,1.0f);
            binding.day.addView(row_item);
        }
        binding.month.setText(new SimpleDateFormat("M月", Locale.CHINESE).format(new Date()));

        DisplayMetrics metrics = new DisplayMetrics();
        ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(metrics);
        int weekday = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        if(weekday==1){weekday=8;}
        binding.last.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeWeek(currentWeekIndex-1);
            }
        });
        binding.next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeWeek(currentWeekIndex+1);
            }
        });
//        int day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        String[] ws = getResources().getStringArray(R.array.weeks);
        for(int i=0;i<7;i++){
            LinearLayout column_item = (LinearLayout) getLayoutInflater().inflate(R.layout.weekday,binding.week, false);
            ((MaterialTextView)column_item.findViewById(R.id.course_week)).setText(ws[i]);
            ((MaterialTextView)column_item.findViewById(R.id.course_date)).setText(getOldDate(i-weekday+2));
            if(i+2==weekday){
                try {
                    //(getResources().getConfiguration().uiMode& Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES? :0
                    TypedArray cs = obtainStyledAttributes(new int[]{com.google.android.material.R.attr.colorSurfaceDim, com.google.android.material.R.attr.colorSurfaceContainerHighest});
                    ((MaterialTextView) column_item.findViewById(R.id.course_date)).setTextColor(cs.getColor(0, 0));
                    ((MaterialTextView) column_item.findViewById(R.id.course_week)).setTextColor(cs.getColor(1, 0));
                    cs.recycle();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                column_item.setBackgroundResource(R.drawable.weekday);
            }
            binding.week.addView(column_item);
        }
        launch = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult o) {
                if(o.getResultCode()==RESULT_OK){
                    cookie=getSharedPreferences("privacy",0).getString("Cookie","");
                    if(currentTerm!=null){getTable(currentTerm,currentWeek);}
                    else{getTerm();getAvailableTerms();}
                }
            }
        });
        binding.term.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(termPop==null){
                    termPop = new PopupMenu(v.getContext(), v,0, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow);
                    terms.forEach(e->termPop.getMenu().add("第"+e+"学期").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(@NonNull MenuItem item) {
                            changeTerm(e);
                            return true;
                        }
                    }));
                }
                termPop.show();
            }
        });
        binding.weekTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(weekPop==null){
                    weekPop = new PopupMenu(v.getContext(), v,0, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow);
                    weeks.forEach(e->weekPop.getMenu().add("第"+e+"周").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(@NonNull MenuItem item) {
                            changeWeek(weeks.indexOf(e));
                            return true;
                        }
                    }));
                }
                weekPop.show();
            }
        });
        detailDialog=new BottomSheetDialog(this);
        detailDialog.setContentView(R.layout.detail);
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
                                    View item = getLayoutInflater().inflate(R.layout.agenda_item, binding.day, false);
                                    if(!isStop.equalsIgnoreCase("0")){item.setEnabled(false);item.setBackgroundColor(getColor(R.color.teal_700));}
                                    views.add(item);
                                    item.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            setDetail(course,(campus == null ? "" : campus)+"-"+(teachingBuildingName == null ? "" : teachingBuildingName)+"-"+(classroomNum == null ? "" : classroomNum),teacher,String.format("第%s节到第%s节",startClassTimes,endClassTimes));
                                            detailDialog.show();
                                        }
                                    });
                                    ((MaterialTextView) item.findViewById(R.id.content)).setText(String.format("%s/%s-%s", course, teachingBuildingName == null ? "" : teachingBuildingName, classroomNum == null ? "" : classroomNum));
                                    GridLayout.LayoutParams gl = (GridLayout.LayoutParams) item.getLayoutParams();
                                    gl.columnSpec = GridLayout.spec(Integer.parseInt(week), 1.0f);
                                    gl.rowSpec = GridLayout.spec(Integer.parseInt(startClassTimes) - 1, Integer.parseInt(endClassTimes) - Integer.parseInt(startClassTimes) + 1, 1.0f);
                                    binding.day.addView(item);
                                });
                                break;
                        }
                        case 2: {
                            currentTerm = response.getJSONObject("data").getString("acadYearSemester");
                            binding.term.setText(currentTerm);
                            getAvailableWeeks(currentTerm);
                            getTable(currentTerm,currentWeek);
                            break;
                        }case 3: {
                            String from = response.getJSONObject("data").getString("startTime");
                            try {
                                Calendar c = Calendar.getInstance();
                                Date date = new SimpleDateFormat("yyyy-MM-dd").parse(from);
                                c.setTime(date);
                                binding.month.setText(new SimpleDateFormat("M 月").format(date.getTime()));
                                for(int i=0;i<7;i++){
                                    ((MaterialTextView)((LinearLayout)binding.week.getChildAt(i+1)).findViewById(R.id.course_date)).setText(new SimpleDateFormat("dd日").format(c.getTime()));
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
                            currentWeek = Integer.parseInt(response.getJSONObject("data").getString("nowWeekly"));
                            response.getJSONObject("data").getJSONArray("weeklyList").forEach(e->weeks.add(((JSONObject)e).getInteger("weekly")));
                            currentWeekIndex = weeks.indexOf(currentWeek);
                            binding.weekTime.setText(String.format("第%d周", currentWeek));
                            getTable(currentTerm, currentWeek);
                            System.out.println(response);
                            break;
                        }
                    }
                }else{
                launch.launch(new Intent(AgendaActivity.this, Login.class));
            }
            }
        };
        getTerm();
        getAvailableTerms();
    }
public void getAvailableWeeks(String academicYear){
    new OkHttpClient.Builder().build().newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/base-info/school-calender/weekly?academicYear="+academicYear)
            .header("Referer","https://jwxt.sysu.edu.cn/jwxt//yd/classSchedule/")
            .header("Cookie",cookie).build()).enqueue(new Callback() {
        @Override
        public void onFailure(@NonNull Call call, @NonNull IOException e) {
        }

        @Override
        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
            if (response.body() != null) {
                Message msg = new Message();
                msg.what=5;
                msg.obj=response.body().string();
                handler.sendMessage(msg);
            }
        }
    });
}
    public  void  getAvailableTerms(){
        new OkHttpClient.Builder().build().newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/base-info/acadyearterm/findAcadyeartermNamesBox")
                .header("Referer","https://jwxt.sysu.edu.cn/jwxt//yd/classSchedule/")
                .addHeader("Cookie",cookie).build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.body() != null) {
                    Message msg = new Message();
                    msg.what=4;
                    msg.obj=response.body().string();
                    handler.sendMessage(msg);
                }
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
        new OkHttpClient.Builder().build().newCall(new Request.Builder().url(String.format("https://jwxt.sysu.edu.cn/jwxt/base-info/school-calender?academicYear=%s&weekly=%d",academicYear,week))
                .header("Referer","https://jwxt.sysu.edu.cn/jwxt//yd/classSchedule/")
                .addHeader("Cookie",cookie).build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.body() != null) {
                    Message msg = new Message();
                    msg.what=3;
                    msg.obj=response.body().string();
                    handler.sendMessage(msg);
                }
            }
        });
    }
    void setDetail(String course,String location,String teacher,String classTime){
        ((MaterialTextView)detailDialog.findViewById(R.id.course)).setText(course);
        ((MaterialTextView)detailDialog.findViewById(R.id.location)).setText(location);
        ((MaterialTextView)detailDialog.findViewById(R.id.teacher)).setText(teacher);
        ((MaterialTextView)detailDialog.findViewById(R.id.classTime)).setText(classTime);
    }
    void changeWeek(int newWeek){
        if(newWeek>=0){
            int currentWeek = weeks.get(newWeek);
            currentWeekIndex =newWeek;
            binding.weekTime.setText(String.format("第%d周",currentWeek));
            getTable(currentTerm,currentWeek);
            getRange(currentTerm,currentWeek);
        }

    }
public void getTable(String academicYear,int week){
    if(academicYear.isEmpty()||week<0){return;}
    new OkHttpClient.Builder().build().newCall(new Request.Builder().url(String.format("https://jwxt.sysu.edu.cn/jwxt/timetable-search/classTableInfo/queryStudentClassTable?academicYear=%s&weekly=%d",academicYear,week)).addHeader("Cookie",cookie).build()).enqueue(new Callback() {
        @Override
        public void onFailure(@NonNull Call call, @NonNull IOException e) {

        }
        @Override
        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
            if (response.body() != null) {
                Message msg = new Message();
                msg.what=1;
                msg.obj=response.body().string();
                handler.sendMessage(msg);
            }
        }
    });
}
public void getTerm(){
    new OkHttpClient.Builder().build().newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/base-info/acadyearterm/showNewAcadlist")
                    .header("Referer","https://jwxt.sysu.edu.cn/jwxt//yd/classSchedule/")

            .addHeader("Cookie",cookie).build()).enqueue(new Callback() {
        @Override
        public void onFailure(@NonNull Call call, @NonNull IOException e) {
        }
        @Override
        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
            if (response.body() != null) {
                Message msg = new Message();
                msg.what=2;
                msg.obj=response.body().string();
                handler.sendMessage(msg);
            }
        }
    });
}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem today_menu = menu.add("今天");
        today_menu.setShowAsAction(1);
        today_menu.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(@NonNull MenuItem item) {
                getTerm();
                return true;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId())
        {
            case android.R.id.home:finishAfterTransition();
        }
        return super.onOptionsItemSelected(item);
    }
}