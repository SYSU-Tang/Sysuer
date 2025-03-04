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
import android.widget.Button;
import android.widget.PopupMenu;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textview.MaterialTextView;
import com.sysu.edu.R;
import com.sysu.edu.activity.Login;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class ActivityFragment extends Fragment {
    Handler handler;
    String cookie;
    ArrayList<HashMap<String,String>> todayCourse=new ArrayList<>();
    ArrayList<HashMap<String,String>> tomorrowCourse=new ArrayList<>();
    View fragment;
    private ActivityResultLauncher<Intent> launch;
    private Adp adp;
    private MaterialButtonToggleGroup toggle;
    private RecyclerView list;
    private MaterialTextView time;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
         if(fragment==null){
        fragment= inflater.inflate(R.layout.fragment_activity,container,false);
        list = fragment.findViewById(R.id.course_list);
        time=fragment.findViewById(R.id.time);
        launch = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<>() {
            @Override
            public void onActivityResult(ActivityResult o) {
                if(o.getResultCode()== Activity.RESULT_OK){
                    cookie=getActivity().getSharedPreferences("privacy",0).getString("Cookie","");
                    getTodayCourses();
                }
            }
        });


        ((MaterialTextView)fragment.findViewById(R.id.date)).setText(String.format("%s 星期%s", new SimpleDateFormat("M月dd日", Locale.CHINESE).format(new Date()), (new String[]{"日","一","二","三","四","五","六"})[Calendar.getInstance().get(Calendar.DAY_OF_WEEK)-1]));
        toggle=fragment.findViewById(R.id.toggle);
             LinearLayoutManager lm = new LinearLayoutManager(this.getContext(), LinearLayoutManager.HORIZONTAL, false);
             list.addItemDecoration(new DividerItemDecoration(this.requireContext(),0));
             list.setLayoutManager(lm);
             new Handler().post(new Runnable() {
                 @Override
                 public void run() {
                     time.setText(new SimpleDateFormat("hh:mm:ss").format(new Date()));
                     handler.postDelayed(this,1000);
                 }
             });
             cookie=requireActivity().getSharedPreferences("privacy",0).getString("Cookie","");
             getTodayCourses();
             adp=new Adp(this.requireActivity());
             list.setAdapter(adp);
             toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                 if(group.getCheckedButtonId()==checkedId){
                 adp.set(checkedId==R.id.today?todayCourse:tomorrowCourse);
                 fragment.findViewById(R.id.noClass).setVisibility(adp.getItemCount()==0?View.VISIBLE:View.GONE);
                     }
             });
             handler=new Handler(Looper.getMainLooper()){
                 @Override
                 public void handleMessage(@NonNull Message msg) {
                     if (msg.what == 1) {
                         JSONObject data = JSON.parseObject((String) msg.obj);
                         if (data.get("code").equals(200)) {
                             data.getJSONArray("data").forEach(e -> {
                                 String flag = (String) ((JSONObject) e).get("useflag");
                                 addCourse(flag.equals("TD") ? todayCourse : tomorrowCourse, (String) ((JSONObject) e).get("courseName"), (String) ((JSONObject) e).get("teachingPlace"), ((JSONObject) e).get("startTime") + "~" + ((JSONObject) e).get("endTime")
                                         , "第" + ((JSONObject) e).get("startClassTimes") + "~" + ((JSONObject) e).get("endClassTimes") + "节课", (String) ((JSONObject) e).get("teacherName"), flag);
                             });
                             toggle.check(R.id.today);
                         } else {
                             launch.launch(new Intent(getContext(), Login.class));
                         }
                     }
                 }
             };
        }
        return fragment;
    }
    public void getTodayCourses(){
        new OkHttpClient.Builder().build().newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/timetable-search/classTableInfo/queryTodayStudentClassTable?academicYear=2024-2")
                .addHeader("Cookie",cookie)

                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what=1;
                if (response.body() != null) {
                    msg.obj=response.body().string();
                }
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
}
class Adp extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    Context context;
    ArrayList<HashMap<String,String>> data=new ArrayList<>();
    public Adp(Context context){
        super();
        this.context=context;
    }
//    public void addCourse(String name,String location,String time,String course,String teacher,String flag){
//        HashMap<String, String> map = new HashMap<>();
//        map.put("courseName",name);
//        map.put("location",location);
//        map.put("time",time);
//        map.put("course",course);
//        map.put("teacher",teacher);
//        map.put("flag",flag);
//        data.add(map);
//        notifyItemInserted(getItemCount()-1);
//    }
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
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
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
