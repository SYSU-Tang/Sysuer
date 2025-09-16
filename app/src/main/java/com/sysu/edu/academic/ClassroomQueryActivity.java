package com.sysu.edu.academic;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textview.MaterialTextView;
import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.ChipBinding;
import com.sysu.edu.databinding.ClassroomQueryBinding;
import com.sysu.edu.databinding.ResultItemBinding;
import com.sysu.edu.extra.LoginActivity;

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
    MaterialDatePicker<Long> dateDialog=MaterialDatePicker.Builder.datePicker().build();
    HashMap<String,ArrayList<Chip>> classroom= new HashMap<>();
    String dateStr;
    String startClassTime="1";
    String endClassTime="11";
    List<String> classType= List.of("002","003");
    RoomAdp adp;
    int page=1;
    int total=0;
    HashMap<Integer, String> office= new HashMap<>();
    ActivityResultLauncher<Intent> launch;
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
        binding = ClassroomQueryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Params params = new Params(this);
        binding.campusSelectAll.setOnClickListener(v -> {
            for(int i=1;i<((ChipGroup)v.getParent()).getChildCount();i++){
                Chip chip=(Chip)((ChipGroup)v.getParent()).getChildAt(i);
                chip.setChecked(!chip.isChecked());
            }
        });
        binding.officeSelectAll.setOnClickListener(v -> {
            for(int i=1;i<((ChipGroup)v.getParent()).getChildCount();i++){
                Chip chip=(Chip)((ChipGroup)v.getParent()).getChildAt(i);
                chip.setChecked(!chip.isChecked());
            }
        });
        cookie=params.getCookie();
        http=getHttp();
        dateDialog.addOnPositiveButtonClickListener(selection -> {
            dateStr = new SimpleDateFormat("yyyy-MM-dd",Locale.CHINESE).format(new Date(selection));
            binding.dateText.setText(new SimpleDateFormat("yyyy年MM月dd日",Locale.CHINESE).format(new Date(selection)));
        });
        adp = new RoomAdp(this);
        binding.classroomQueryToolbar.setNavigationOnClickListener(view -> supportFinishAfterTransition());
        binding.result.setAdapter(adp);
        binding.result.setLayoutManager(new StaggeredGridLayoutManager(params.getColumn(), StaggeredGridLayoutManager.VERTICAL));
        BottomSheetBehavior.from(findViewById(R.id.result_sheet)).setState(BottomSheetBehavior.STATE_HIDDEN);
        binding.date.setOnClickListener(v -> dateDialog.show(getSupportFragmentManager(),null));
        binding.timeSlider.addOnChangeListener((slider, value, fromUser) -> {
            startClassTime=String.format(Locale.CHINA,"%.0f",slider.getValues().get(0));
            endClassTime=String.format(Locale.CHINA,"%.0f",slider.getValues().get(1));
            ((MaterialTextView)findViewById(R.id.time)).setText(String.format(getString(R.string.section_range_x), startClassTime,endClassTime));
        });
        binding.query.setOnClickListener(v -> {
            adp.clear();
            page=1;
            getRoom();
        });
        getCampus();
        launch = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
            if(o.getResultCode()==RESULT_OK){
                cookie=params.getCookie();
                http=getHttp();
                getCampus();
            }
        });
        binding.result.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if(recyclerView.canScrollVertically(1)&&total/20+1>=page){
                    getRoom();
                }
                super.onScrolled(recyclerView, dx, dy);
            }
        });
        binding.reset.setOnClickListener(view -> {
            binding.officeGroup.getCheckedChipIds().forEach(e-> ((Chip) findViewById(e)).setChecked(false));
            binding.campusGroup.getCheckedChipIds().forEach(e-> ((Chip) findViewById(e)).setChecked(false));
            binding.typeGroup.getCheckedChipIds().forEach(e-> ((Chip) findViewById(e)).setChecked(true));
            dateStr=new SimpleDateFormat("yyyy-MM-dd", Locale.CHINESE).format(new Date());
            binding.timeSlider.setValues(List.of(1.0f,11.0f));
            dateStr=new SimpleDateFormat("yyyy-MM-dd", Locale.CHINESE).format(new Date());
            binding.dateText.setText(new SimpleDateFormat("yyyy年MM月dd日", Locale.CHINESE).format(new Date()));
           });
        dateStr=new SimpleDateFormat("yyyy-MM-dd", Locale.CHINESE).format(new Date());
        binding.dateText.setText(new SimpleDateFormat("yyyy年MM月dd日", Locale.CHINESE).format(new Date()));
        handler = new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if(msg.what==0)
                {
                    Toast.makeText(ClassroomQueryActivity.this,(String)msg.obj,Toast.LENGTH_LONG).show();
                    return;
                }
                if (msg.what==-1){
                    Toast.makeText(ClassroomQueryActivity.this,getString(R.string.no_wifi_warning),Toast.LENGTH_LONG).show();
                    return;
                }
                JSONObject dataString = JSON.parseObject((String) msg.obj);
                if (dataString.getInteger("code") == 200) {
                    if (msg.what == 3) {
                        JSONObject data = dataString.getJSONObject("data");
                        total = data.getInteger("total");page++;data.getJSONArray("rows").forEach(a->adp.add((JSONObject) a));
                        BottomSheetBehavior.from(findViewById(R.id.result_sheet)).setState(BottomSheetBehavior.STATE_EXPANDED);
                    } else {binding.timeSlider.setValueFrom(1);
                        dataString.getJSONArray("data").forEach(campusInfo -> {
                            switch (msg.what) {
                                case 1: {
                                    String id = ((JSONObject) campusInfo).getString("id");
                                    Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip, binding.campusGroup, false);
                                    binding.campusGroup.addView(chip);
                                    chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                                        if (isChecked) {
                                            if (classroom.containsKey(id)) {
                                                Objects.requireNonNull(classroom.get(id)).forEach(e -> e.setVisibility(View.VISIBLE));
                                            } else {
                                                getOffice(id);
                                            }
                                        } else {
                                            Objects.requireNonNull(classroom.get(id)).forEach(e -> e.setVisibility(View.GONE));
                                        }
                                    });
                                    chip.setText(((JSONObject) campusInfo).getString("campusName"));
                                    break;
                                }
                                case 2: {
                                    classroom.computeIfAbsent(msg.getData().getString("campus"), k -> new ArrayList<>());
                                    Chip chip = ChipBinding.inflate(getLayoutInflater(), binding.officeGroup, false).getRoot();
                                    binding.officeGroup.addView(chip);
                                    office.put(chip.getId(), ((JSONObject) campusInfo).getString("id"));
                                    chip.setText(((JSONObject) campusInfo).getString("dataName"));
                                    Objects.requireNonNull(classroom.get(msg.getData().getString("campus"))).add(chip);
                                    break;
                                }
                            }
                        });
                    }
                }else {
                    Toast.makeText(ClassroomQueryActivity.this,getString(R.string.login_warning),Toast.LENGTH_LONG).show();
                    launch.launch(new Intent(ClassroomQueryActivity.this, LoginActivity.class));
                }
            }
        };
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
                new Callback(){
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
        binding.typeGroup.getCheckedChipIds().forEach(e->classType.add(((Chip) findViewById(e)).getText().toString().equals("自习室")?"003":"002"));
        binding.officeGroup.getCheckedChipIds().forEach(e->{if(findViewById(e).getVisibility()==View.VISIBLE){teachingBuildIDs.add(office.get(e));}});
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
    ArrayList<JSONObject> json = new ArrayList<>();
    public RoomAdp(Context context){
        super();
        this.context=context;
    }
    public void add(JSONObject jsonobject){
        json.add(jsonobject);
        notifyItemInserted(getItemCount());
    }
    public void clear(){
        int temp=getItemCount();
        json.clear();
        notifyItemRangeRemoved(0,temp);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecyclerView.ViewHolder(ResultItemBinding.inflate(LayoutInflater.from(context),parent, false).getRoot()){};
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((Chip)holder.itemView.findViewById(R.id.location)).setText(json.get(position).getString("teachingBuildingName"));
        ((Chip)holder.itemView.findViewById(R.id.time)).setText(json.get(position).getString("classTimes"));
        ((Chip)holder.itemView.findViewById(R.id.floor)).setText(json.get(position).getString("floor"));
        ((Chip)holder.itemView.findViewById(R.id.seat)).setText(json.get(position).getString("seats"));
        ((MaterialButton)holder.itemView.findViewById(R.id.type)).setText(json.get(position).getString("classRoomTag"));
        ((MaterialTextView)holder.itemView.findViewById(R.id.name)).setText(json.get(position).getString("classRoomNum"));
        Glide.with(context)
                .load(new GlideUrl("https://jwxt.sysu.edu.cn/jwxt/base-info/classroom/classRoomView?fileName=jspic.png&filePath="+json.get(position).get("photoPath"),new LazyHeaders.Builder()
                        .addHeader("Cookie", ((ClassroomQueryActivity)context).cookie)
                        .addHeader("Referer","https://jwxt.sysu.edu.cn/jwxt//yd/studyRoom/")
                        .build()))
                .placeholder(R.drawable.logo)
                .override(145*3, 132*3)
                .fitCenter()
                .into((ShapeableImageView)holder.itemView.findViewById(R.id.pic));
        holder.itemView.setOnClickListener(v -> {});
    }

    @Override
    public int getItemCount() {
        return json.size();
    }
}