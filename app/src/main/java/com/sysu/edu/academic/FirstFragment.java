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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson2.JSONObject;
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

public class FirstFragment extends Fragment{

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
    private String key;

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
        page=1;
        init();
        binding.course.setLayoutManager(new GridLayoutManager(requireContext(),1));
        binding.course.addItemDecoration(new SpacesItemDecoration(dpToPx(8)));
        handler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                //System.out.println(msg.obj);
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
                    }
                }else if (response.getInteger("code").equals(50021000)){
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
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if(recyclerView.canScrollVertically(1)&&total/10+1>=page){
                   getCourseList();
                }
                super.onScrolled(recyclerView, dx, dy);
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
            adp = new CourseAdapter(requireContext());
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

    private static class SpacesItemDecoration extends RecyclerView.ItemDecoration {
        private final int space;

        public SpacesItemDecoration(int i) {
            this.space = i;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            //super.getItemOffsets(outRect, view, parent, state);
            outRect.top=space;
            outRect.right=space;
        }
    }
}
class CourseAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
    Context context;
    ArrayList<JSONObject> data = new ArrayList<>();
    public CourseAdapter(Context context){
        super();
        this.context=context;
    }
    void add(JSONObject e){
        data.add(e);
        notifyItemInserted(getItemCount()-1);
    }
    void  clear(){
        int tmp = getItemCount();
        data.clear();
        notifyItemRangeRemoved(0,tmp);
    }
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        @NonNull CourseSelectionItemBinding binding = CourseSelectionItemBinding.inflate(LayoutInflater.from(context));
        return new RecyclerView.ViewHolder(binding.getRoot()) {
        };
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((TextView)holder.itemView.findViewById(R.id.course_name)).setText(data.get(position).getString("courseName"));
        //((TextView)holder.itemView.findViewById(R.id.course_name)).setText(data.get(position).getString("courseName"));
        String[] info = new String[]{"courseUnitName","credit","teachingTimePlace","examFormName","courseNum","clazzNum"};
        ChipGroup infos = holder.itemView.findViewById(R.id.course_info);
        for(int i=0;i<info.length;i++){
            Chip chip = (Chip) LayoutInflater.from(context).inflate(R.layout.service_item, infos, false);
            String content = data.get(position).getString(info[i]);
            chip.setText(String.format("%s：%s",
                    (new String[]{
                            "开设部门","学分","教学","考查形式","课程代码","班级代码"
                    })[i],content));
            chip.setOnLongClickListener(view -> {((ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("",content));return false;});
            chip.setOnClickListener(a-> Snackbar.make(context,chip,content,Snackbar.LENGTH_LONG).show());
            infos.addView(chip);
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}