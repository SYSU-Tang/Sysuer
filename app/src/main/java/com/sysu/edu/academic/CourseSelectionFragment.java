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
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionInflater;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.sysu.edu.R;
import com.sysu.edu.api.CourseSelectionViewModel;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.FragmentCourseSelectionBinding;
import com.sysu.edu.databinding.ItemCourseSelectionBinding;
import com.sysu.edu.extra.LoginActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CourseSelectionFragment extends Fragment{

    FragmentCourseSelectionBinding binding;
    OkHttpClient http = new OkHttpClient.Builder().build();
    Handler handler;
    String cookie;
    int tmp;
    int page = 1;
    HashMap<String,CourseAdapter> map = new HashMap<>();
    int selectedType;
    int selectedCate;
    CourseAdapter adp;
    Integer total;
    HashMap<String, Integer> totals= new HashMap<>();
    String key;
    String term;
    CourseSelectionViewModel vm;
    String filterText="";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setSharedElementEnterTransition(TransitionInflater.from(requireContext())
                .inflateTransition(android.R.transition.move));
        setSharedElementReturnTransition(TransitionInflater.from(requireContext())
                .inflateTransition(android.R.transition.move));
        vm = new ViewModelProvider(requireActivity()).get(CourseSelectionViewModel.class);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        if(savedInstanceState==null) {
            binding = FragmentCourseSelectionBinding.inflate(inflater, container, false);
            Params params = new Params(requireActivity());
            binding.type.setOnCheckedStateChangeListener((chipGroup, list) -> {
                int cid = chipGroup.getCheckedChipId();
                selectedType = (cid == R.id.my_major) ? 1 : (cid == R.id.public_selection) ? 4 : 2;
                selectedCate = 11;
                if (cid != R.id.my_major && binding.category.getHeight() != 0) {
                    tmp = binding.category.getHeight();
                }
                ValueAnimator a = ValueAnimator.ofInt(chipGroup.getCheckedChipId() == R.id.my_major ? new int[]{0, tmp} : new int[]{binding.category.getHeight() == 0 ? 0 : tmp, 0});
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
                    cookie = requireContext().getSharedPreferences("privacy", Context.MODE_PRIVATE).getString("Cookie", "");
                    getCourseList();
                }
            });
            cookie = requireContext().getSharedPreferences("privacy", Context.MODE_PRIVATE).getString("Cookie", "");
            selectedCate = 1;
            selectedType = 11;
            getInfo();
            binding.course.setLayoutManager(new GridLayoutManager(requireContext(), params.getColumn()));
            binding.course.addItemDecoration(new SpacesItemDecoration(params.dpToPx(8)));
            binding.filter.setOnCheckedStateChangeListener((chipGroup, list) -> {adp.clear();totals.put(key, -1);getInfo();});
            handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    System.out.println(response);
                    if (response != null && response.getInteger("code").equals(200)) {
                        switch (msg.what) {
                            case -1:
                                Toast.makeText(requireContext(), getString(R.string.no_wifi_warning), Toast.LENGTH_LONG).show();
                                break;
                            case 0:
                                term = response.getJSONObject("data").getString("semesterYear");
                                init();
                                break;
                            case 1:
                                if (response.getJSONObject("data") != null) {
                                    total = response.getJSONObject("data").getInteger("total");
                                    response.getJSONObject("data").getJSONArray("rows").forEach(e -> adp.add((JSONObject) e));
                                    totals.put(key, total);
                                }
                                break;
                            case 2:
                                //Toast.makeText(requireContext(),response.getString("data"),Toast.LENGTH_LONG).show();
                                //Toast.makeText(requireContext(),"",Toast.LENGTH_LONG).show();
                                break;
                            case 3:
                                Toast.makeText(requireContext(), response.getString("data"), Toast.LENGTH_LONG).show();
                                break;
                        }
                    } else if (response != null && response.getInteger("code").equals(50021000)) {
                        Toast.makeText(requireContext(), response.getString("message"), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.login_warning), Toast.LENGTH_LONG).show();
                        launch.launch(new Intent(requireContext(), LoginActivity.class).putExtra("url", TargetUrl.JWXT));
                    }
                    super.handleMessage(msg);
                }
            };
            binding.course.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView v, int dx, int dy) {
                    if (v.canScrollVertically(1) && total / 10 + 1 >= page) {
                        getCourseList();
                    }
                    binding.head.setElevation(v.canScrollVertically(-1) ? params.dpToPx(2) : 0);
                    super.onScrolled(v, dx, dy);
                }
            });
           // getInfo();
        }
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (!Objects.equals(vm.getReturnData(), filterText)&&vm.getReturnData()!=null) {
            filterText = vm.getReturnData();
            adp.clear();
            totals.put(key, -1);
            //map.put(key, null);
            getInfo();
            vm.clearReturnData();
        }
        if (savedInstanceState==null) {
            binding.addFilter.setOnClickListener(v ->
                            Navigation.findNavController(view).navigate(R.id.filter_fragment, null, new NavOptions.Builder()
                                    .setEnterAnim(android.R.animator.fade_in)
                                    .setExitAnim(android.R.animator.fade_out)
                                    .build(), new FragmentNavigator.Extras(Map.of(v, "miniapp")))
                    //launchFilter.launch(new Intent(requireContext(), CourseQuery.class),ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(),v,"miniapp"))
            );

        }
        super.onViewCreated(view, savedInstanceState);
    }

    void init() {
        key = String.format(Locale.CHINA,"%d%d", selectedType, selectedCate);
        if (map.getOrDefault(key,null)!=null) {
            adp = map.get(key);
        } else {
            adp = new CourseAdapter(this);
            map.put(key,adp);
        }
        if (adp != null) {
            page = (int) Math.ceil((double) adp.getItemCount() /10);
        }
        total = totals.getOrDefault(key,null);
        if (total!=null){
            totals.put(key, -1);
        }
        binding.course.setAdapter(adp);
        if(!Objects.equals(adp.getItemCount(), total)) {
            getCourseList();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //binding = null;
    }
    int boolean2int(boolean b){
        return b?1:0;
    }
    void getCourseList(){
        page++;
        String body = String.format(Locale.CHINA, "{\"pageNo\":%d,\"pageSize\":10,\"param\":{\"semesterYear\":\"%s\",\"selectedType\":\"%d\",\"selectedCate\":\"%d\",\"hiddenConflictStatus\":\"0\",\"hiddenSelectedStatus\":\"%d\",\"hiddenEmptyStatus\":\"%d\",\"vacancySortStatus\":\"%d\",\"collectionStatus\":\"%d\"%s}}", page, term, selectedType, selectedCate,
                boolean2int(binding.hideSelected.isChecked()), boolean2int(binding.hideVacancy.isChecked()), boolean2int(binding.vacancy.isChecked()), boolean2int(binding.onlyCollection.isChecked()),
                filterText);
        http.newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/choose-course-front-server/classCourseInfo/course/list")
                .header("Cookie",cookie)
                .header("Referer","https://jwxt.sysu.edu.cn/jwxt/mk/courseSelection/?code=jwxsd_xk&resourceName=%E9%80%89%E8%AF%BE")
                .post(RequestBody.create(body, MediaType.get("application/json"))).build()).enqueue(new Callback() {
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
    void getInfo(){
        http.newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/choose-course-front-server/classCourseInfo/selectCourseInfo")
                .header("Cookie",cookie)
                .header("Referer","https://jwxt.sysu.edu.cn/jwxt/mk/courseSelection/?code=jwxsd_xk&resourceName=%E9%80%89%E8%AF%BE")
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
                msg.what=0;
                msg.obj=response.body().string();
                handler.sendMessage(msg);
            }
        });
    }
    void select(String code){
        http.newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/choose-course-front-server/classCourseInfo/course/choose")
                .header("Cookie",cookie)
                .header("Referer","https://jwxt.sysu.edu.cn/jwxt/mk/courseSelection/?code=jwxsd_xk&resourceName=%E9%80%89%E8%AF%BE")
                .post(RequestBody.create(String.format(Locale.CHINA,"{\"clazzId\":\"%s\",\"selectedType\":\"%d\",\"selectedCate\":\"%d\",\"check\":true}",code,selectedType,selectedCate), MediaType.get("application/json")))
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
                .post(RequestBody.create(String.format(Locale.CHINA,"{\"courseId\":\"%s\",\"clazzId\":\"%s\",\"selectedType\":\"%d\"}",classId,code,selectedType), MediaType.get("application/json")))
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
//    String parseFilter(){
//        StringBuilder str = new StringBuilder();
//        String[] keys = new String[]{"course","campus", "day", "section", "school","teacher", "language", "special"};
//        String[] key = new String[]{"courseName","studyCampusId","week","classTimes","courseUnitNum","teachingTeacherNum","teachingLanguageCode","specialClassCode"};
//        for(int i=0;i<keys.length;i++){
//            String v = filter.getOrDefault(keys[i], "");
//            if (v != null && !v.isEmpty()) {
//                str.append(String.format(",\"%s\":\"%s\"", key[i], v));
//            }
//        }
//        return str.toString();
//    }
}
class CourseAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
    String[] info = new String[]{"courseUnitName", "credit",  "examFormName", "courseNum", "clazzNum"};
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
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        @NonNull ItemCourseSelectionBinding binding = ItemCourseSelectionBinding.inflate(LayoutInflater.from(context));
        for(int i = 0; i< info.length; i++) {
            Chip chip = (Chip) LayoutInflater.from(context).inflate(R.layout.item_action_chip, binding.courseInfo, false);
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
        open.setOnClickListener(v-> context.startActivity(new Intent(context, CourseDetail.class).putExtra("code",convert(position,"courseNum")).putExtra("id",convert(position,"courseId")), ActivityOptionsCompat.makeSceneTransitionAnimation((Activity) context,v,"miniapp").toBundle()));
        ((TextView)holder.itemView.findViewById(R.id.course_name)).setText(convert(position,"courseName"));
        ((TextView)holder.itemView.findViewById(R.id.head)).setText(convert(position,"teachingTimePlace").replace(";"," | ").replace(",","\n"));
        for(int i = 0; i< info.length; i++){
            String content = convert(position,info[i]);
            ((Chip)((ChipGroup)holder.itemView.findViewById(R.id.course_info)).getChildAt(i)).setText(String.format("%s：%s",(new String[]{"开设部门","学分","考查形式","课程代码","班级代码","剩余空位","待筛选人数","选上人数"})[i],content));
        }
        String[] seats = new String[]{"baseReceiveNum", "filterSelectedNum", "courseSelectedNum"};
        for(int i = 0; i< seats.length; i++){
            String content = convert(position,seats[i]);
            ((MaterialButton)holder.itemView.findViewById(new int[]{R.id.left,R.id.filtering,R.id.selected}[i])).setText(String.format("%s\n%s",(new String[]{"剩余","待筛选","选上"})[i],content));
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
    public void clear(){
        int tmp = getItemCount();
        data.clear();
        notifyItemRangeRemoved(0,tmp);
    }
}