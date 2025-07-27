package com.sysu.edu.academic;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.slider.RangeSlider;
import com.google.android.material.textview.MaterialTextView;
import com.sysu.edu.R;
import com.sysu.edu.databinding.ClassroomQueryBinding;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ClassroomQueryActivity extends AppCompatActivity {
    Handler handler;
    String cookie="";
    OkHttpClient http;
    ChipGroup campusGroup;
    ChipGroup officeGroup;
    ChipGroup typeGroup;
    MaterialDatePicker<Long> dateDialog=MaterialDatePicker.Builder.datePicker().build();
    HashMap<String,ArrayList<Chip>> classroom= new HashMap<>();
    MaterialTextView dateText;
    String dateStr;
    String startClassTime="1";
    String endClassTime="11";
    List<String> classType= List.of("002","003");
    RoomAdp adp;
    int page=1;
    int total=0;
    HashMap<Integer, String> office= new HashMap<>();
    RecyclerView result;
    private ActivityResultLauncher<Intent> launch;
    ClassroomQueryBinding binding;

    public OkHttpClient getHttp(){
        return new OkHttpClient.Builder().addInterceptor(new Interceptor() {
            @NonNull
            @Override
            public Response intercept(@NonNull Chain chain) throws IOException {
                Request origin = chain.request();
                return chain.proceed(origin.newBuilder()
                        .header("Accept-Language","zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6")
                        .header("Cookie", cookie)
                        .header("Referer","https://jwxt.sysu.edu.cn/jwxt//yd/studyRoom/")
                        .method(origin.method(),origin.body())
                        .build());
            }
        }).build();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ClassroomQueryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            binding.tool.setPadding(0,systemBars.top,0,0);
            return insets;
        });
        findViewById(R.id.campus_select_all).setOnClickListener(v -> {
            for(int i=1;i<((ChipGroup)v.getParent()).getChildCount();i++){
                Chip chip=(Chip)((ChipGroup)v.getParent()).getChildAt(i);
                chip.setChecked(!chip.isChecked());
            }
        });
        findViewById(R.id.office_select_all).setOnClickListener(v -> {
            for(int i=1;i<((ChipGroup)v.getParent()).getChildCount();i++){
                Chip chip=(Chip)((ChipGroup)v.getParent()).getChildAt(i);
                chip.setChecked(!chip.isChecked());
            }
        });
        cookie=getSharedPreferences("privacy",0).getString("Cookie","");
        http=getHttp();
        MaterialToolbar tool= findViewById(R.id.classroom_query_toolbar);
        setSupportActionBar(tool);
        dateDialog.addOnPositiveButtonClickListener(selection -> {
            dateStr = new SimpleDateFormat("yyyy-MM-dd",Locale.CHINESE).format(new Date(selection));
            dateText.setText(new SimpleDateFormat("yyyy年MM月dd日",Locale.CHINESE).format(new Date(selection)));
        });
        campusGroup=findViewById(R.id.campusGroup);
        officeGroup=findViewById(R.id.officeGroup);
        typeGroup=findViewById(R.id.typeGroup);
        adp = new RoomAdp(this);
        result=findViewById(R.id.result);
        result.setAdapter(adp);
        //BottomSheetBehavior.from(findViewById(R.id.result_sheet)).setState(BottomSheetBehavior.STATE_COLLAPSED);
        findViewById(R.id.date).setOnClickListener(v -> dateDialog.show(getSupportFragmentManager(),null));
        ((RangeSlider)findViewById(R.id.timeSlider)).addOnChangeListener((slider, value, fromUser) -> {
            startClassTime=String.format(Locale.CHINA,"%.0f",slider.getValues().get(0));
            endClassTime=String.format(Locale.CHINA,"%.0f",slider.getValues().get(1));
            ((MaterialTextView)findViewById(R.id.time)).setText(String.format("第%s节到第%s节", startClassTime,endClassTime));
        });
        findViewById(R.id.query).setOnClickListener(v -> {
            adp.clear();
            page=1;
            getRoom();
        });
        getCampus();
        launch = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
            if(o.getResultCode()==RESULT_OK){
                cookie=getSharedPreferences("privacy",0).getString("Cookie","");
                http=getHttp();
                getCampus();
            }
        });
        ((RecyclerView)findViewById(R.id.result)).addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if(recyclerView.canScrollVertically(1)&&total/20+1>=page){
                    getRoom();
                }
                super.onScrolled(recyclerView, dx, dy);
            }
        });
        dateText= findViewById(R.id.dateText);
        dateStr=new SimpleDateFormat("yyyy-MM-dd", Locale.CHINESE).format(new Date(System.currentTimeMillis()));
        dateText.setText(new SimpleDateFormat("yyyy年MM月dd日", Locale.CHINESE).format(new Date(System.currentTimeMillis())));
        handler = new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if(msg.what==0)
                {
                    Toast.makeText(ClassroomQueryActivity.this,(String)msg.obj,Toast.LENGTH_LONG).show();
                    return;
                }
                if (msg.what==-1){
                    Toast.makeText(ClassroomQueryActivity.this,"网络状态不佳",Toast.LENGTH_LONG).show();
                    return;
                }
                JSONObject dataString = JSON.parseObject((String) msg.obj);
                if (dataString.getInteger("code") == 200) {
                    if (msg.what == 3) {
                        BottomSheetBehavior.from(findViewById(R.id.result_sheet)).setState(BottomSheetBehavior.STATE_EXPANDED);
                        JSONObject data = dataString.getJSONObject("data");
                        total = data.getInteger("total");page++;
                        for (Object classroom : data.getJSONArray("rows").toArray()) {
                            String name = (String) ((JSONObject) classroom).get("classRoomNum");
                            String pic = (String) ((JSONObject) classroom).get("photoPath");
                            String floor = (String) ((JSONObject) classroom).get("floor");
                            String seat = (String) ((JSONObject) classroom).get("seats");
                            String office = (String) ((JSONObject) classroom).get("teachingBuildingName");
                            String time = (String) ((JSONObject) classroom).get("classTimes");
                            String type = (String) ((JSONObject) classroom).get("classRoomTag");
                            adp.add(name, pic, office, type, time, floor, seat);
                        }
                    } else {
                        for (Object campusInfo : dataString.getJSONArray("data").toArray()) {
                            if (msg.what == 1) {
                                String cid = ((JSONObject) campusInfo).getString("id");
                                Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip, campusGroup, false);
                                campusGroup.addView(chip);
                                chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                                    if (isChecked) {
                                        if (classroom.containsKey(cid)) {
                                            Objects.requireNonNull(classroom.get(cid)).forEach(e -> e.setVisibility(View.VISIBLE));
                                        } else {
                                            getOffice(cid);
                                        }
                                    } else {
                                        Objects.requireNonNull(classroom.get(cid)).forEach(e -> e.setVisibility(View.GONE));
                                    }
                                });
                                chip.setText(((JSONObject) campusInfo).getString("campusName"));
                            } else if (msg.what == 2) {
                                classroom.computeIfAbsent(msg.getData().getString("campus"), k -> new ArrayList<>());
                                Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip, officeGroup, false);
                                officeGroup.addView(chip);
                                office.put(chip.getId(), ((JSONObject) campusInfo).getString("id"));
                                chip.setText(((JSONObject) campusInfo).getString("dataName"));
                                Objects.requireNonNull(classroom.get(msg.getData().getString("campus"))).add(chip);
                            }
                        }
                    }
                }else {
                    Toast.makeText(ClassroomQueryActivity.this,"请先登录",Toast.LENGTH_LONG).show();
                    launch.launch(new Intent(ClassroomQueryActivity.this, LoginActivity.class));
                }
            }
        };
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId()==android.R.id.home)
        {
            supportFinishAfterTransition();
        }
        return true;
    }
    public void getCampus(){
        http.newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/base-info/campus/findCampusNamesBox").build()).enqueue(
                new  Callback(){
                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        Message message = new Message();
                        message.what=1;
                        message.obj=response.body().string();
                        handler.sendMessage(message);
                    }
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Message message = new Message();
                        message.what=-1;
                        handler.sendMessage(message);
                    }
                }
        );
    }
    public void getOffice(String c){
        http.newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/schedule/agg/selfStudyClassRoom/buildingConditionPull").post(RequestBody.create("{\"campusIdList\":[\""+c+"\"]}",MediaType.parse("application/json"))).build()).enqueue(
                new  Callback(){
                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        Message message = new Message();
                        message.what=2;
                        Bundle data=new Bundle();
                        data.putString("campus",c);
                        message.setData(data);
                        message.obj=response.body().string();
                        handler.sendMessage(message);
                    }
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Message message = new Message();
                        message.what=-1;
                        handler.sendMessage(message);
                    }
                }
        );
    }
    public void getRoom(){
        ArrayList<String> teachingBuildIDs= new ArrayList<>();
        classType= new ArrayList<>();
        HashMap<String, String> map = new HashMap<>();
        map.put("自习室","003");
        map.put("有声研讨室","002");
        typeGroup.getCheckedChipIds().forEach(e->classType.add(map.get((String)((Chip)findViewById(e)).getText())));
        officeGroup.getCheckedChipIds().forEach(e->{if(findViewById(e).getVisibility()==View.VISIBLE){teachingBuildIDs.add(office.get(e));}});
        if(teachingBuildIDs.isEmpty()) {
            Message message = new Message();
            message.what = 0;
            message.obj = "请先选择教学楼";
            handler.sendMessage(message);
            return;
        }
        http.newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/schedule/agg/selfStudyClassRoom/pageListStudyClassroom").post(RequestBody.create("{\"pageNo\":"+page+",\"pageSize\":20,\"param\":{\"dateStr\":\""+dateStr+"\",\"teachingBuildIDs\":"+JSON.toJSONString(teachingBuildIDs)+",\"startClassTimes\":"+startClassTime+",\"endClassTimes\":"+endClassTime+",\"classRoomTagList\":"+JSON.toJSONString(classType)+"}}",MediaType.parse("application/json"))).build()).enqueue(
                new  Callback(){
                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        Message message = new Message();
                        message.what=3;
                        message.obj=response.body().string();
                        handler.sendMessage(message);
                    }
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Message message = new Message();
                        message.what=-1;
                        handler.sendMessage(message);
                    }
                }
        );
    }
}
class RoomAdp extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
    Context context;
    ArrayList<HashMap<String, String>> data = new ArrayList<>();
    public RoomAdp(Context context){
        super();
        this.context=context;
    }

    public void add(HashMap<String, String> map) {
        data.add(map);
        notifyItemRangeInserted(getItemCount()-2,1);
    }
    public void clear(){
        int temp=getItemCount();
        data.clear();
        notifyItemRangeRemoved(0,temp);
    }
    public void add(String roomName, String pic, String office, String tag, String time, String floor, String seat) {
        HashMap<String, String> map = new HashMap<>();
        map.put("name", roomName);
        map.put("pic", pic);
        map.put("office", office);
        map.put("type", tag);
        map.put("time", time);
        map.put("floor", floor);
        map.put("seat", seat);
        add(map);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecyclerView.ViewHolder(LayoutInflater.from(context).inflate(R.layout.result_item, parent, false)){};
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((Chip)holder.itemView.findViewById(R.id.location)).setText(data.get(position).get("office"));
        ((Chip)holder.itemView.findViewById(R.id.time)).setText(data.get(position).get("time"));
        ((Chip)holder.itemView.findViewById(R.id.floor)).setText(data.get(position).get("floor"));
        ((Chip)holder.itemView.findViewById(R.id.seat)).setText(data.get(position).get("seat"));
        ((MaterialButton)holder.itemView.findViewById(R.id.type)).setText(data.get(position).get("type"));

        ((MaterialTextView)holder.itemView.findViewById(R.id.name)).setText(data.get(position).get("name"));

        Glide.with(context)
                .load(new GlideUrl("https://jwxt.sysu.edu.cn/jwxt/base-info/classroom/classRoomView?fileName=jspic.png&filePath="+data.get(position).get("pic"),new LazyHeaders.Builder()
                        .addHeader("Accept-Language","zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6")
                        .addHeader("Cookie", ((ClassroomQueryActivity)context).cookie)
                        .addHeader("Referer","https://jwxt.sysu.edu.cn/jwxt//yd/studyRoom/")
                        .build()))
                .placeholder(R.drawable.logo)
                .override(127*3, 116*3)
                .fitCenter()
                .into((ShapeableImageView)holder.itemView.findViewById(R.id.pic));
        holder.itemView.setOnClickListener(v -> {

        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}