package com.sysu.edu.home;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;
import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.CourseItemBinding;
import com.sysu.edu.databinding.ExamItemBinding;
import com.sysu.edu.databinding.FragmentDashboardBinding;
import com.sysu.edu.extra.LoginActivity;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiConsumer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class DashboardFragment extends Fragment {
    Handler handler;
    String cookie;
    ArrayList<JSONObject> todayCourse=new ArrayList<>();
    ArrayList<JSONObject> tomorrowCourse=new ArrayList<>();
    LinkedList<JSONObject> thisWeekExams=new LinkedList<>();
    LinkedList<JSONObject> nextWeekExams=new LinkedList<>();
    private ActivityResultLauncher<Intent> launch;
    Params params;
    RecyclerView examList;
    ExamAdp examAdp;
    FragmentDashboardBinding binding;
    CourseAdp courseAdp;
    OkHttpClient http=new OkHttpClient.Builder().build();
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if(binding==null){
            binding = FragmentDashboardBinding.inflate(inflater);
            RecyclerView courseList = binding.courseList;
            examList = binding.examList;
            launch = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
                if(o.getResultCode()== Activity.RESULT_OK){
                    cookie = params.getCookie();
                    getTerm();
                }
            });
            courseList.addItemDecoration(new DividerItemDecoration(requireContext(),0));
            courseList.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
            examList.addItemDecoration(new DividerItemDecoration(requireContext(),0));
            examList.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    binding.time.setText(new SimpleDateFormat("hh:mm:ss",Locale.CHINESE).format(new Date()));
                    handler.postDelayed(this,1000);
                }
            });
            params = new Params(requireActivity());
            cookie= params.getCookie();
            courseAdp =new CourseAdp(requireActivity());
            courseList.setAdapter(courseAdp);
            examAdp=new ExamAdp(requireActivity());
            examList.setAdapter(examAdp);
            binding.toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if(group.getCheckedButtonId()==checkedId){
                    courseAdp.set(checkedId==R.id.today?todayCourse:tomorrowCourse);
                    binding.noClass.setVisibility(courseAdp.getItemCount()==0?View.VISIBLE:View.GONE);
                }
            });
            binding.toggle2.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if(group.getCheckedButtonId()==checkedId){
                    examAdp.set(checkedId==R.id.this_week?thisWeekExams:nextWeekExams);
                    binding.noExam.setVisibility(examAdp.getItemCount()==0?View.VISIBLE:View.GONE);
                }
            });
            handler=new Handler(Looper.getMainLooper()){
                @Override
                public void handleMessage(@NonNull Message msg) {
                    JSONObject response = JSON.parseObject((String) msg.obj);
                    if (response.get("code").equals(200)) {
                        switch (msg.what) {
                            case 1:
                                response.getJSONArray("data").forEach(e -> {
                                    ((JSONObject) e).put("status",getTimePosition(((JSONObject) e).getString("teachingDate") + " " +((JSONObject) e).getString("startTime"),((JSONObject) e).getString("teachingDate")  + " " + ((JSONObject) e).getString("endTime")));
                                    ((JSONObject) e).put("time",((JSONObject) e).get("startTime") + "~" + ((JSONObject) e).get("endTime"));
                                    ((JSONObject) e).put("course","第" + ((JSONObject) e).get("startClassTimes") + "~" + ((JSONObject) e).get("endClassTimes") + "节课");
                                    String flag = (String) ((JSONObject) e).get("useflag");
                                    (flag.equals("TD") ? todayCourse : tomorrowCourse).add((JSONObject) e);
//                                    addCourse(flag.equals("TD") ? todayCourse : tomorrowCourse, (String) ((JSONObject) e).get("courseName"), (String) ((JSONObject) e).get("teachingPlace"), ((JSONObject) e).get("startTime") + "~" + ((JSONObject) e).get("endTime")
//                                            , "第" + ((JSONObject) e).get("startClassTimes") + "~" + ((JSONObject) e).get("endClassTimes") + "节课", (String) ((JSONObject) e).get("teacherName"), flag);
                                });
                                binding.toggle.check(R.id.today);
                                break;
                            case 2:
                                class k{
                                    public int k;
                                    public k(int k){
                                        this.k = k;
                                    }
                                }
                                final k k = new k(0);
                                if(response.getJSONArray("data").isEmpty()){break;}
                                response.getJSONArray("data").getJSONObject(0).getJSONObject("timetable").forEach((key,value)->{
                                    if (k.k == 0) {
                                        k.k = Integer.parseInt(key);
                                    }
                                    if (Integer.parseInt(key) < k.k) {
                                        k.k = Integer.parseInt(key);
                                        if (value != null) {
                                            ((JSONArray) value).forEach(c -> thisWeekExams.addFirst((JSONObject) c));
                                        }
                                    } else {
                                        if (value != null) {
                                            ((JSONArray) value).forEach(c -> thisWeekExams.addLast((JSONObject) c));
                                        }
                                    }
                                });
                                response.getJSONArray("data").getJSONObject(1).getJSONObject("timetable").forEach((a,b)->{
                                      if (Integer.parseInt(a) < k.k) {
                                          k.k = Integer.parseInt(a);
                                          if (b != null) {
                                              ((JSONArray) b).forEach(c -> nextWeekExams.addFirst((JSONObject) c));
                                          }
                                      } else {
                                          if (b != null) {
                                              ((JSONArray) b).forEach(c -> nextWeekExams.addLast((JSONObject) c));
                                          }
                                      }
                                  });
                                binding.toggle2.check(R.id.this_week);
                                break;
                            case 3:
                                String term = response.getJSONObject("data").getString("acadYearSemester");
                                binding.date.setText(String.format("%s/星期%s/第%s学期", new SimpleDateFormat("M月dd日", Locale.CHINESE).format(new Date()), new String[]{"日","一","二","三","四","五","六"}[Calendar.getInstance().get(Calendar.DAY_OF_WEEK)-1],term));
                                getTodayCourses(term);
                                getExams(term);
                                break;
                            case -1:{
                                Toast.makeText(requireActivity(),getString(R.string.no_wifi_warning),Toast.LENGTH_LONG).show();
                                break;
                            }
                        }
                    } else {
                        launch.launch(new Intent(getContext(), LoginActivity.class));
                    }
                }
            };
            getTerm();
        }
        return binding.getRoot();
    }
    void getTerm(){
        http.newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/base-info/acadyearterm/showNewAcadlist")
                .header("Cookie",cookie)
                .header("Referer","https://jwxt.sysu.edu.cn/jwxt//yd/classSchedule/").build()
        ).enqueue(new Callback() {
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
    public void getTodayCourses(String term){
        new OkHttpClient.Builder().build().newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/timetable-search/classTableInfo/queryTodayStudentClassTable?academicYear="+term)
                .header("Cookie",cookie)
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message message = new Message();
                message.what=-1;
                handler.sendMessage(message);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what=1;
                msg.obj = response.body().string();
                handler.sendMessage(msg);
            }
        });
    }

    public String getTimePosition(String from,String to){
        Date now = new Date();
        try {
            Date fromDate = new SimpleDateFormat("yy-MM-dd hh:mm", Locale.CHINA).parse(from);
            Date toDate = new SimpleDateFormat("yy-MM-dd hh:mm", Locale.CHINA).parse(to);
            return now.before(fromDate)?"after":now.after(toDate)?"before":"in";
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
    public void getExams(String term){
        new OkHttpClient.Builder().build().newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/examination-manage/classroomResource/queryStuEaxmInfo?code=jwxsd_ksxxck")
                .addHeader("Cookie",cookie)
                .addHeader("Referer","https://jwxt.sysu.edu.cn/jwxt/mk/")
                .post(RequestBody.create(String.format("{\"acadYear\":\"%s\",\"examWeekId\":\"1928284621349085186\",\"examWeekName\":\"18-19周期末考\",\"examDate\":\"\"}",term), MediaType.parse("application/json")))
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message message = new Message();
                message.what=-1;
                handler.sendMessage(message);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what=2;
                msg.obj = response.body().string();
                handler.sendMessage(msg);
            }
        });
    }
}
class CourseAdp extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    Context context;
    ArrayList<JSONObject> data=new ArrayList<>();
    public CourseAdp(Context context){
        super();
        this.context=context;
    }
    public void set(ArrayList<JSONObject> d){
        clear();
        data.addAll(d);
        notifyItemRangeInserted(0,getItemCount());
    }
    public void clear(){
        int temp = getItemCount();
        data.clear();
        notifyItemRangeRemoved(0,temp);
    }
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecyclerView.ViewHolder(CourseItemBinding.inflate(LayoutInflater.from(context)).getRoot()){};
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        BiConsumer<Integer,String> a = (id, s)-> ((TextView)holder.itemView.findViewById(id)).setText(data.get(position).getString(s));
        holder.itemView.setOnClickListener(v -> {});
        a.accept(R.id.course_title,"courseName");
        a.accept(R.id.location_container,"teachingPlace");
        a.accept(R.id.time_container,"time");
        a.accept(R.id.teacher,"teacherName");
        a.accept(R.id.course,"course");
        //TypedArray arrt = new TypedArray();
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurfaceDim, typedValue, true);
        ((TextView)holder.itemView.findViewById(R.id.course_title)).setTextAppearance(!Objects.equals(data.get(position).getString("status"), "before") ?com.google.android.material.R.style.TextAppearance_Material3_TitleMedium_Emphasized:com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        ((GradientDrawable)((RippleDrawable)holder.itemView.getBackground()).getDrawable(1)).setColor(Objects.equals(data.get(position).getString("status"), "in") ?typedValue.data:Objects.equals(data.get(position).getString("status"), "before") ?0x00000000:0xffffffff);
//        addCourse(flag.equals("TD") ? todayCourse : tomorrowCourse, (String) ((JSONObject) e).get("courseName"), (String) ((JSONObject) e).get("teachingPlace"), ((JSONObject) e).get("startTime") + "~" + ((JSONObject) e).get("endTime")
//                , "第" + ((JSONObject) e).get("startClassTimes") + "~" + ((JSONObject) e).get("endClassTimes") + "节课", (String) ((JSONObject) e).get("teacherName"), flag);
//    });
    }
    @Override
    public int getItemCount() {
        return data.size();
    }
}
class ExamAdp extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    Context context;
    LinkedList<JSONObject> data=new LinkedList<>();
    public ExamAdp(Context context){
        super();
        this.context=context;
    }
    public void set(LinkedList<JSONObject> examData){
        clear();
        data.addAll(examData);
        notifyItemRangeInserted(0,getItemCount());
    }
    public void clear(){
        int temp = getItemCount();
        data.clear();
        notifyItemRangeRemoved(0,temp);
    }
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecyclerView.ViewHolder(ExamItemBinding.inflate(LayoutInflater.from(context)).getRoot()){};
    }
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        holder.itemView.setOnClickListener(v -> {});
        int startClassTimes = data.get(position).getIntValue("startClassTimes");
        int endClassTimes = data.get(position).getIntValue("endClassTimes");
        ((MaterialTextView)holder.itemView.findViewById(R.id.exam_name)).setText(data.get(position).getString("examSubjectName"));
        ((MaterialButton)holder.itemView.findViewById(R.id.exam_location)).setText(data.get(position).getString("classroomNumber"));
        ((MaterialButton)holder.itemView.findViewById(R.id.exam_date)).setText(data.get(position).getString("examDate"));
        ((MaterialButton)holder.itemView.findViewById(R.id.exam_duration)).setText(String.format("%s%s", data.get(position).getString("duration"),context.getString(R.string.minute)));
        ((MaterialButton)holder.itemView.findViewById(R.id.exam_time)).setText(data.get(position).getString("durationTime"));
        ((MaterialButton)holder.itemView.findViewById(R.id.exam_class_time)).setText(String.format(context.getString(R.string.section_range), startClassTimes, endClassTimes));
        ((MaterialButton)holder.itemView.findViewById(R.id.exam_mode)).setText(String.format("%s：%s",context.getString(R.string.exam_mode), data.get(position).getString("examMode")));
        ((MaterialButton)holder.itemView.findViewById(R.id.exam_stage)).setText(String.format("%s：%s",context.getString(R.string.exam_stage), data.get(position).getString("examStage")));
    }
    @Override
    public int getItemCount() {
        return data.size();
    }
}