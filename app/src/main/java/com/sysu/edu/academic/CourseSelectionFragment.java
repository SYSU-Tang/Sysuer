package com.sysu.edu.academic;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.sysu.edu.R;
import com.sysu.edu.databinding.CourseSelectionBinding;
import com.sysu.edu.databinding.CourseSelectionItemBinding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CourseSelectionFragment extends Fragment{

    CourseSelectionBinding binding;
    OkHttpClient http = new OkHttpClient.Builder().build();
    Handler handler;
    String cookie;
    int tmp;
    int page = 1;
    HashMap<String,CourseAdapter> map = new HashMap<>();
    int selectedType;
    int selectedCate;
    CourseAdapter adp;
    int total;
    HashMap<String, Integer> totals= new HashMap<>();
    String key;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        binding = CourseSelectionBinding.inflate(inflater, container, false);
        binding.type.setOnCheckedStateChangeListener((chipGroup, list) -> {
            int cid = chipGroup.getCheckedChipId();
            selectedType = (cid==R.id.my_major)?1:(cid==R.id.public_selection)?4:2;
            selectedCate = 11;
            if(cid!=R.id.my_major&&binding.category.getHeight()!=0){
                tmp = binding.category.getHeight();
            }
            ValueAnimator a = ValueAnimator.ofInt(chipGroup.getCheckedChipId()==R.id.my_major?new int[]{0,tmp}:new int[]{binding.category.getHeight()==0?0:tmp, 0});
            a.addUpdateListener(valueAnimator -> {
                LinearLayout.LayoutParams lp = ((LinearLayout.LayoutParams) binding.category.getLayoutParams());
                lp.height = (int) valueAnimator.getAnimatedValue();
                binding.category.setLayoutParams(lp);
            });
            a.start();
            init();
        });
        binding.category.setOnCheckedStateChangeListener((chipGroup, list) -> {
            int cid = chipGroup.getCheckedChipId();
            selectedType = 1;
            if (cid == R.id.major_compulsory) {
                selectedCate = 11;
            } else if (cid == R.id.major_selective) {
                selectedCate = 21;
            } else if (cid == R.id.school_public_selective) {
                selectedCate = 30;
            } else if (cid == R.id.pe) {
                selectedType = 3;
                selectedCate = 10;
            } else if (cid == R.id.en) {
                selectedType = 5;
                selectedCate = 1;
            } else if (cid == R.id.public_compulsory) {
                selectedCate = 10;
            } else if (cid == R.id.honor) {
                selectedCate = 31;
            }
            init();
        });
        ActivityResultLauncher<Intent> launch = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
            if (o.getResultCode() == Activity.RESULT_OK) {
                cookie = requireContext().getSharedPreferences("privacy",Context.MODE_PRIVATE).getString("Cookie","");
                getCourseList();
            }
        });
        cookie = requireContext().getSharedPreferences("privacy",Context.MODE_PRIVATE).getString("Cookie","");
        selectedCate = 1;
        selectedType = 11;
        init();
        binding.course.setLayoutManager(new GridLayoutManager(requireContext(),1));
        binding.course.addItemDecoration(new SpacesItemDecoration(dpToPx(8)));
        handler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                System.out.println(msg.obj);
                JSONObject response = JSONObject.parseObject((String) msg.obj);
                if (response.getInteger("code").equals(200)) {
                    switch (msg.what) {
                        case -1:
                            Toast.makeText(requireContext(), getString(R.string.no_wifi_warning), Toast.LENGTH_LONG).show();
                            break;
                        case 1:
                            if(response.getJSONObject("data")!=null) {
                                total = response.getJSONObject("data").getInteger("total");
                                response.getJSONObject("data").getJSONArray("rows").forEach(e -> adp.add((JSONObject) e));
                                totals.put(key,total);
                            }
                            break;
                        case 2:
                            //Toast.makeText(requireContext(),response.getString("data"),Toast.LENGTH_LONG).show();

                            //Toast.makeText(requireContext(),"",Toast.LENGTH_LONG).show();
                            break;
                        case 3:
                            Toast.makeText(requireContext(),response.getString("data"),Toast.LENGTH_LONG).show();
                            break;
                    }
                }else if (response.getInteger("code").equals(50021000)){
                    Toast.makeText(requireContext(),response.getString("message"),Toast.LENGTH_LONG).show();
                }else if (response.getInteger("code").equals(52000000)){
                    Toast.makeText(requireContext(),response.getString("message"),Toast.LENGTH_LONG).show();
                }
                else{
                    Toast.makeText(requireContext(),getString(R.string.login_warning),Toast.LENGTH_LONG).show();
                    launch.launch(new Intent(requireContext(), LoginActivity.class));
                }
                super.handleMessage(msg);
            }
        };
        binding.course.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView v, int dx, int dy) {
                if(v.canScrollVertically(1)&&total/10+1>=page){
                    getCourseList();
                }
                binding.head.setElevation(v.canScrollVertically(-1)?12:0);
                super.onScrolled(v, dx, dy);
            }
        });
        return binding.getRoot();
    }
    int dpToPx(int dps) {
        return Math.round(getResources().getDisplayMetrics().density * dps);
    }
    void init() {
        key = String.format(Locale.CHINA,"%d%d", selectedType, selectedCate);
        if (map.containsKey(key)) {
            adp = map.get(key);
        } else {
            adp = new CourseAdapter(this);
            map.put(key,adp);
        }
        if (adp != null) {
            page = (int) Math.ceil((double) adp.getItemCount() /10);
        }
        if (totals.containsKey(key)){
            total = totals.get(key);
        }else {
            totals.put(key, -1);
            total = -1;
        }
        binding.course.setAdapter(adp);
        if(adp.getItemCount()!=total) {
            getCourseList();
        }
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

//        binding.buttonFirst.setOnClickListener(v ->
//                NavHostFragment.findNavController(FirstFragment.this)
//                        .navigate(R.id.action_FirstFragment_to_SecondFragment)
//        );

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    void getCourseList(){
        page++;
        http.newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/choose-course-front-server/classCourseInfo/course/list")
                .header("Cookie",cookie)
                .header("Referer","https://jwxt.sysu.edu.cn/jwxt/mk/courseSelection/?code=jwxsd_xk&resourceName=%E9%80%89%E8%AF%BE")
                .post(RequestBody.create(String.format("{\"pageNo\":%d,\"pageSize\":10,\"param\":{\"semesterYear\":\"2025-1\",\"selectedType\":\"%d\",\"selectedCate\":\"%d\",\"hiddenConflictStatus\":\"0\",\"hiddenSelectedStatus\":\"0\",\"hiddenEmptyStatus\":\"0\",\"vacancySortStatus\":\"0\",\"collectionStatus\":\"0\"}}",page,selectedType,selectedCate), MediaType.get("application/json"))).build()).enqueue(new Callback() {
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

    void like(String code){
        http.newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/choose-course-front-server/stuCollectedCourse/create")
                .header("Cookie",cookie)
                .header("Referer","https://jwxt.sysu.edu.cn/jwxt/mk/courseSelection/?code=jwxsd_xk&resourceName=%E9%80%89%E8%AF%BE")
                .post(RequestBody.create(String.format("{\"classesID\":\"%s\",\"selectedType\":\"1\"}",code), MediaType.get("application/json")))
                .build()).enqueue(new Callback() {
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

    void select(String code){
        http.newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/choose-course-front-server/classCourseInfo/course/choose")
                .header("Cookie",cookie)
                .header("Referer","https://jwxt.sysu.edu.cn/jwxt/mk/courseSelection/?code=jwxsd_xk&resourceName=%E9%80%89%E8%AF%BE")
                .post(RequestBody.create(String.format("{\"clazzId\":\"%s\",\"selectedType\":\"%d\",\"selectedCate\":\"%d\",\"check\":true}",code,selectedType,selectedCate), MediaType.get("application/json")))
                .build()).enqueue(new Callback() {
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

    void unselect(String classId,String code){
        http.newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/choose-course-front-server/classCourseInfo/course/back")
                .header("Cookie",cookie)
                .header("Referer","https://jwxt.sysu.edu.cn/jwxt/mk/courseSelection/?code=jwxsd_xk&resourceName=%E9%80%89%E8%AF%BE")
                .post(RequestBody.create(String.format("{\"courseId\":\"%s\",\"clazzId\":\"%s\",\"selectedType\":\"%d\"}",classId,code,selectedType), MediaType.get("application/json")))
                .build()).enqueue(new Callback() {
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

    private static class SpacesItemDecoration extends RecyclerView.ItemDecoration {
        private final int space;

        public SpacesItemDecoration(int i) {
            this.space = i;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            //super.getItemOffsets(outRect, view, parent, state);
            outRect.top=space/2;
            outRect.right=space;
            outRect.bottom=space/2;
        }
    }
}
class CourseAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
    String[] info = new String[]{"teachingTimePlace","courseUnitName", "credit",  "examFormName", "courseNum", "clazzNum","baseReceiveNum","filterSelectedNum","courseSelectedNum"
    };
    Context context;
    CourseSelectionFragment c;
    ArrayList<JSONObject> data = new ArrayList<>();
    public CourseAdapter(CourseSelectionFragment c){
        super();
        this.c=c;
        this.context=c.requireContext();
    }
    void add(JSONObject e){
        data.add(e);
        notifyItemInserted(getItemCount()-1);
    }
    //    void  clear(){
//        int tmp = getItemCount();
//        data.clear();
//        notifyItemRangeRemoved(0,tmp);
//    }
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        @NonNull CourseSelectionItemBinding binding = CourseSelectionItemBinding.inflate(LayoutInflater.from(context));
        for(int i = 0; i< info.length; i++) {
            Chip chip = (Chip) LayoutInflater.from(context).inflate(R.layout.service_item, binding.courseInfo, false);
            chip.setOnLongClickListener(a -> {((ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("",((Chip)a).getText()));return false;});
            chip.setOnClickListener(a-> Snackbar.make(context,chip,((Chip)a).getText(),Snackbar.LENGTH_LONG).show());
            binding.courseInfo.addView(chip);
        }
        return new RecyclerView.ViewHolder(binding.getRoot()) {
        };
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((TextView)holder.itemView.findViewById(R.id.course_name)).setText(convert(position,"courseName"));
        MaterialButton open = holder.itemView.findViewById(R.id.open);
        MaterialButton select = holder.itemView.findViewById(R.id.select);
        MaterialButton like = holder.itemView.findViewById(R.id.like);
        like.setSelected(data.get(position).getInteger("collectionStatus")==1);
        select.setSelected(data.get(position).getInteger("selectedStatus")==3);
        select.setText(select.isSelected()?"退课":"选课");
        like.setText(like.isSelected()?"取消收藏":"收藏");
        select.setOnClickListener(v->{
            //Snackbar.make(v,"已"+((MaterialButton)v).getText(),Snackbar.LENGTH_LONG).show();

            if(v.isSelected()){
                c.unselect(convert(position,"courseId"),convert(position,"teachingClassId"));
            }else{
                c.select(convert(position,"teachingClassId"));
            }
            v.setSelected(!v.isSelected());
            ((MaterialButton)v).setText(v.isSelected()?"退课":"选课");
        });
        like.setOnClickListener(v->{
            Snackbar.make(v,"已"+((MaterialButton)v).getText(),Snackbar.LENGTH_LONG).show();
            c.like(convert(position,"teachingClassId"));
            v.setSelected(!v.isSelected());
            ((MaterialButton)v).setText(v.isSelected()?"取消收藏":"收藏");
        });
        open.setOnClickListener(v->{
            context.startActivity(new Intent(context, CourseDetail.class).putExtra("code",convert(position,"courseNum")).putExtra("id",convert(position,"courseId")), ActivityOptionsCompat.makeSceneTransitionAnimation((Activity) context,v,"miniapp").toBundle());
        });
        ((TextView)holder.itemView.findViewById(R.id.course_name)).setText(convert(position,"courseName"));
        for(int i = 0; i< info.length; i++){
            String content = convert(position,info[i]);
            ((Chip)((ChipGroup)holder.itemView.findViewById(R.id.course_info)).getChildAt(i)).setText(String.format("%s：%s",(new String[]{"教学","开设部门","学分","考查形式","课程代码","班级代码","剩余空位","待筛选人数","选上人数"})[i],content));


        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
    String convert(int position,String key){
        String a = data.get(position).getString(key);
        return (a==null?"":a).replace("\n\n","\n");
    }

}