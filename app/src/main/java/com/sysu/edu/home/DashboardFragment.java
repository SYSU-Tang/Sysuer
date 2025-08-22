package com.sysu.edu.home;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textview.MaterialTextView;
import com.sysu.edu.R;
import com.sysu.edu.databinding.FragmentDashboardBinding;
import com.sysu.edu.extra.LoginActivity;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;

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
    ArrayList<HashMap<String,String>> todayCourse=new ArrayList<>();
    ArrayList<HashMap<String,String>> tomorrowCourse=new ArrayList<>();
    LinkedList<JSONObject> thisWeekExams=new LinkedList<>();
    LinkedList<JSONObject> nextWeekExams=new LinkedList<>();
    private ActivityResultLauncher<Intent> launch;
    private Adp adp;
    private MaterialButtonToggleGroup toggle;
    RecyclerView examList;
    ExamAdp examAdp;
    FragmentDashboardBinding binding;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if(savedInstanceState==null){
            binding = FragmentDashboardBinding.inflate(inflater);
            RecyclerView list = binding.courseList;
            examList = binding.examList;
            launch = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
                if(o.getResultCode()== Activity.RESULT_OK){
                    cookie= requireActivity().getSharedPreferences("privacy",0).getString("Cookie","");
                    getTodayCourses();
                }
            });
            (binding.date).setText(String.format("%s 星期%s", new SimpleDateFormat("M月dd日", Locale.CHINESE).format(new Date()), (new String[]{"日","一","二","三","四","五","六"})[Calendar.getInstance().get(Calendar.DAY_OF_WEEK)-1]));
            toggle=binding.toggle;
            LinearLayoutManager lm2 = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
            LinearLayoutManager lm = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
            list.addItemDecoration(new DividerItemDecoration(requireContext(),0));
            list.setLayoutManager(lm);
            examList.addItemDecoration(new DividerItemDecoration(requireContext(),0));
            examList.setLayoutManager(lm2);
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    binding.time.setText(new SimpleDateFormat("hh:mm:ss",Locale.CHINESE).format(new Date()));
                    handler.postDelayed(this,1000);
                }
            });
            cookie=requireActivity().getSharedPreferences("privacy",0).getString("Cookie","");
            getTodayCourses();
            adp=new Adp(this.requireActivity());
            list.setAdapter(adp);
            examAdp=new ExamAdp(this.requireActivity());
            examList.setAdapter(examAdp);
            toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if(group.getCheckedButtonId()==checkedId){
                    adp.set(checkedId==R.id.today?todayCourse:tomorrowCourse);
                    binding.noClass.setVisibility(adp.getItemCount()==0?View.VISIBLE:View.GONE);
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
                    switch (msg.what) {
                        case 1: {
                            JSONObject data = JSON.parseObject((String) msg.obj);
                            if (data.get("code").equals(200)) {
                                data.getJSONArray("data").forEach(e -> {
                                    String flag = (String) ((JSONObject) e).get("useflag");
                                    addCourse(flag.equals("TD") ? todayCourse : tomorrowCourse, (String) ((JSONObject) e).get("courseName"), (String) ((JSONObject) e).get("teachingPlace"), ((JSONObject) e).get("startTime") + "~" + ((JSONObject) e).get("endTime")
                                            , "第" + ((JSONObject) e).get("startClassTimes") + "~" + ((JSONObject) e).get("endClassTimes") + "节课", (String) ((JSONObject) e).get("teacherName"), flag);
                                });
                                toggle.check(R.id.today);
                                getExams();
                            } else {
                                launch.launch(new Intent(getContext(), LoginActivity.class));
                            }
                            break;
                        }
                        case 2: {
                            JSONObject data = JSON.parseObject((String) msg.obj);
                            if (data.get("code").equals(200)) {
                                int k = 0;
                                for (Map.Entry<String, Object> entry : data.getJSONArray("data").getJSONObject(0).getJSONObject("timetable").entrySet()) {
                                    String key = entry.getKey();
                                    Object value = entry.getValue();
                                    if (k == 0) {
                                        k = Integer.parseInt(key);
                                    }
                                    if (Integer.parseInt(key) < k) {
                                        k = Integer.parseInt(key);
                                        if (value != null) {
                                            ((JSONArray) value).forEach(c -> thisWeekExams.addFirst((JSONObject) c));
                                        }
                                    } else {
                                        if (value != null) {
                                            ((JSONArray) value).forEach(c -> thisWeekExams.addLast((JSONObject) c));
                                        }
                                    }
                                }
                                for (Map.Entry<String, Object> entry : data.getJSONArray("data").getJSONObject(1).getJSONObject("timetable").entrySet()) {
                                    String a = entry.getKey();
                                    Object b = entry.getValue();
                                    if (Integer.parseInt(a) < k) {
                                        k = Integer.parseInt(a);
                                        if (b != null) {
                                            ((JSONArray) b).forEach(c -> nextWeekExams.addFirst((JSONObject) c));
                                        }
                                    } else {
                                        if (b != null) {
                                            ((JSONArray) b).forEach(c -> nextWeekExams.addLast((JSONObject) c));
                                        }
                                    }
                                }
                                binding.toggle2.check(R.id.this_week);
                            } else {
                                launch.launch(new Intent(getContext(), LoginActivity.class));
                            }
                            break;
                        }
                        case -1:{
                            Toast.makeText(requireActivity(),"网络状态不佳",Toast.LENGTH_LONG).show();
                            break;
                        }
                    }
                }
            };
        }
        return binding.getRoot();
    }
    public void getTodayCourses(){
        new OkHttpClient.Builder().build().newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/timetable-search/classTableInfo/queryTodayStudentClassTable?academicYear=2024-2")
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



    public void addCourse(ArrayList<HashMap<String,String>> data, String name, String location, String time, String course, String teacher, String flag){
        HashMap<String, String> map = new HashMap<>();
        map.put("courseName",name);
        map.put("location",location);
        map.put("time",time);
        map.put("course",course);
        map.put("teacher",teacher);
        map.put("flag",flag);
        data.add(map);
    }
    public void getExams(){
        new OkHttpClient.Builder().build().newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/examination-manage/classroomResource/queryStuEaxmInfo?code=jwxsd_ksxxck")
                .addHeader("Cookie",cookie)
                .addHeader("Referer","https://jwxt.sysu.edu.cn/jwxt/mk/")
                .post(RequestBody.create("{\"acadYear\":\"2024-2\",\"examWeekId\":\"1864116471884476417\",\"examWeekName\":\"18-19周期末考\",\"examDate\":\"\"}", MediaType.parse("application/json")))
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
class Adp extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    Context context;
    ArrayList<HashMap<String,String>> data=new ArrayList<>();
    public Adp(Context context){
        super();
        this.context=context;
    }
    public void set(ArrayList<HashMap<String,String>> mdata){
        clear();
        data.addAll(mdata);
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
        return new RecyclerView.ViewHolder(LayoutInflater.from(context).inflate(R.layout.course_item, parent, false)){};
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        holder.itemView.setOnClickListener(v -> {
        });
        ((MaterialTextView)holder.itemView.findViewById(R.id.course_title)).setText(data.get(position).get("courseName"));
        ((MaterialButton)holder.itemView.findViewById(R.id.location_container)).setText(data.get(position).get("location"));
        ((MaterialButton)holder.itemView.findViewById(R.id.time_container)).setText(data.get(position).get("time"));
        ((MaterialButton)holder.itemView.findViewById(R.id.teacher)).setText(data.get(position).get("teacher"));
        ((MaterialButton)holder.itemView.findViewById(R.id.course)).setText(data.get(position).get("course"));
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
        return new RecyclerView.ViewHolder(LayoutInflater.from(context).inflate(R.layout.exam_item, parent, false)){};
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        holder.itemView.setOnClickListener(v -> {
        });
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