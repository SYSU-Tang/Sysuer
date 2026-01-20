package com.sysu.edu.academic;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
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
import com.google.android.material.snackbar.Snackbar;
import com.sysu.edu.R;
import com.sysu.edu.api.CourseSelectionViewModel;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.FragmentCourseSelectionBinding;
import com.sysu.edu.databinding.ItemCourseSelectionBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CourseSelectionFragment extends Fragment {

    FragmentCourseSelectionBinding binding;
    Handler handler;
    String cookie;
    int tmp;
    int page = 1;
    CourseAdapter adp;
    Integer total;
    String term;
    CourseSelectionViewModel vm;
    MutableLiveData<String> filter = new MutableLiveData<>();
    MutableLiveData<Integer> type = new MutableLiveData<>(1);
    MutableLiveData<Integer> category = new MutableLiveData<>(11);
    GridLayoutManager gm;
    Params params;
    HttpManager http;
    MediatorLiveData<List<Integer>> typeCate;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setSharedElementEnterTransition(TransitionInflater.from(requireContext())
                .inflateTransition(android.R.transition.move));
        setSharedElementReturnTransition(TransitionInflater.from(requireContext())
                .inflateTransition(android.R.transition.move));
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (binding == null) {
            binding = FragmentCourseSelectionBinding.inflate(inflater, container, false);
            vm = new ViewModelProvider(requireActivity()).get(CourseSelectionViewModel.class);
            params = new Params(requireActivity());
            params.setCallback(this, () -> {
                clear();
                getInfo();
            });
            vm.filterValue.observe(requireActivity(), f -> {
//                System.out.println(f);
                filter.setValue(vm.getReturnData());
                clear();
                getCourseList();
            });
            binding.head.type.setOnCheckedStateChangeListener((chipGroup, list) -> {
                int cid = chipGroup.getCheckedChipId();
                type.setValue((cid == R.id.my_major) ? 1 : (cid == R.id.college_public_selective) ? 4 : 2);
                if (cid != R.id.my_major && binding.head.category.getHeight() != 0) {
                    tmp = binding.head.category.getHeight();
                }
                ValueAnimator animator = ValueAnimator.ofInt(chipGroup.getCheckedChipId() == R.id.my_major ? new int[]{0, tmp} : new int[]{binding.head.category.getHeight() == 0 ? 0 : tmp, 0});
                animator.addUpdateListener(valueAnimator -> {
                    LinearLayout.LayoutParams lp = ((LinearLayout.LayoutParams) binding.head.category.getLayoutParams());
                    lp.height = (int) valueAnimator.getAnimatedValue();
                    binding.head.category.setLayoutParams(lp);
                });
                animator.start();
            });
            binding.zoom.setOnClickListener(v -> binding.head.getRoot().setVisibility(binding.head.getRoot().getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
            typeCate = new MediatorLiveData<>();
            typeCate.addSource(type, s -> typeCate.setValue(List.of(type.getValue() == null ? 1 : type.getValue(), s)));
            typeCate.addSource(category, s -> typeCate.setValue(List.of(type.getValue() == null ? 11 : type.getValue(), s)));
            typeCate.observe(requireActivity(), s -> {
                clear();
                getCourseList();
            });
            binding.head.category.setOnCheckedStateChangeListener((chipGroup, list) -> {
                int cid = chipGroup.getCheckedChipId();
                if (cid == R.id.major_compulsory) {
                    typeCate.setValue(List.of(1, 11));
                } else if (cid == R.id.major_selective) {
                    typeCate.setValue(List.of(1, 21));
                } else if (cid == R.id.school_public_selective) {
                    typeCate.setValue(List.of(1, 30));
                } else if (cid == R.id.pe) {
                    typeCate.setValue(List.of(3, 10));
                } else if (cid == R.id.en) {
                    typeCate.setValue(List.of(5, 1));
                } else if (cid == R.id.public_compulsory) {
                    typeCate.setValue(List.of(1, 10));
                } else if (cid == R.id.honor) {
                    typeCate.setValue(List.of(1, 31));
                }
            });
            cookie = params.getCookie();
            binding.course.setLayoutManager(gm = new GridLayoutManager(requireContext(), params.getColumn()));
            binding.course.addItemDecoration(new SpacesItemDecoration(params.dpToPx(8)));
            binding.course.setAdapter(adp = new CourseAdapter(this));
            binding.head.filter.setOnCheckedStateChangeListener((chipGroup, list) -> {
                clear();
                getCourseList();
            });
            handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    if (response != null && response.getInteger("code").equals(200)) {
                        switch (msg.what) {
                            case -1:
                                params.toast(R.string.no_wifi_warning);
                                break;
                            case 0:
                                term = response.getJSONObject("data").getString("semesterYear");
                                getCourseList();
                                break;
                            case 1:
                                if (response.getJSONObject("data") != null) {
                                    total = response.getJSONObject("data").getInteger("total");
                                    response.getJSONObject("data").getJSONArray("rows").forEach(e -> adp.add((JSONObject) e));
                                }
                                break;
                            case 3:
                                params.toast(response.getString("data"));
                                break;
                        }
                    } else if (response != null && response.getInteger("code").equals(50021000)) {
                        params.toast(response.getString("message"));
                    } else if (response != null && response.getInteger("code").equals(52021100)) {
                        params.toast(response.getString("message"));
                    } else {
                        params.toast(R.string.login_warning);
                        params.gotoLogin(getView(), TargetUrl.JWXT);
                    }
                    super.handleMessage(msg);
                }
            };

            binding.course.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView v, int dx, int dy) {
                    if (!v.canScrollVertically(1) && total / 10 + 1 > page && dy > 0) {
                        getCourseList();
                    }
                    binding.head.getRoot().setElevation(v.canScrollVertically(-1) ? params.dpToPx(2) : 0);
                    super.onScrolled(v, dx, dy);
                }
            });
            http = new HttpManager(handler);
            http.setReferrer("https://jwxt.sysu.edu.cn/jwxt/mk/courseSelection/?code=jwxsd_xk&resourceName=%E9%80%89%E8%AF%BE");
            http.setParams(params);
            getInfo();
        }
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            binding.head.addFilter.setOnClickListener(v ->
                    Navigation.findNavController(view).navigate(R.id.selection_to_filter1, null, new NavOptions.Builder()
                            .setEnterAnim(android.R.animator.fade_in)
                            .setExitAnim(android.R.animator.fade_out)
                            .build(), new FragmentNavigator.Extras(Map.of(v, "miniapp")))
            );
        }
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        gm.setSpanCount(params.getColumn());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
//        binding = null;
    }

    void clear() {
        if (adp != null) adp.clear();
        page = 0;
        total = -1;
    }

    int bool2int(boolean b) {
        return b ? 1 : 0;
    }

    void getCourseList() {
//        System.out.println("getCourseList");
        if (type.getValue() == null || category.getValue() == null || term == null)
            return;
        getCourseList(typeCate.getValue()==null || typeCate.getValue().get(0) == null ? 1 : typeCate.getValue().get(0), typeCate.getValue()==null || typeCate.getValue().get(1) == null ? 11 : typeCate.getValue().get(1), term, filter.getValue() == null ? "" : filter.getValue());
    }

    void getCourseList(int selectedType, int selectedCate, String term, String filterText) {
        http.postRequest("https://jwxt.sysu.edu.cn/jwxt/choose-course-front-server/classCourseInfo/course/list",
                String.format(Locale.getDefault(), "{\"pageNo\":%d,\"pageSize\":10,\"param\":{\"semesterYear\":\"%s\",\"selectedType\":\"%d\",\"selectedCate\":\"%d\",\"hiddenConflictStatus\":\"0\",\"hiddenSelectedStatus\":\"%d\",\"hiddenEmptyStatus\":\"%d\",\"vacancySortStatus\":\"%d\",\"collectionStatus\":\"%d\"%s}}", ++page, term, selectedType, selectedCate,
                        bool2int(binding.head.hideSelected.isChecked()), bool2int(binding.head.hideVacancy.isChecked()), bool2int(binding.head.vacancy.isChecked()), bool2int(binding.head.onlyCollection.isChecked()),
                        filterText), 1);
    }

    void like(String code) {
        http.postRequest("https://jwxt.sysu.edu.cn/jwxt/choose-course-front-server/stuCollectedCourse/create",
                String.format("{\"classesID\":\"%s\",\"selectedType\":\"1\"}", code),
                2);
    }

    void getInfo() {
        http.sendRequest("https://jwxt.sysu.edu.cn/jwxt/choose-course-front-server/classCourseInfo/selectCourseInfo", 0);
    }

    void select(String code) {
        http.postRequest("https://jwxt.sysu.edu.cn/jwxt/choose-course-front-server/classCourseInfo/course/choose",
                String.format(Locale.getDefault(), "{\"clazzId\":\"%s\",\"selectedType\":\"%d\",\"selectedCate\":\"%d\",\"check\":true}", code, type.getValue(), category.getValue()),
                3);

    }

    void unselect(String classId, String code) {
        http.postRequest("https://jwxt.sysu.edu.cn/jwxt/choose-course-front-server/classCourseInfo/course/back",
                String.format(Locale.getDefault(), "{\"courseId\":\"%s\",\"clazzId\":\"%s\",\"selectedType\":\"%d\"}", classId, code, type.getValue()),
                3);

    }

    static class SpacesItemDecoration extends RecyclerView.ItemDecoration {
        private final int space;

        public SpacesItemDecoration(int i) {
            this.space = i;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            //super.getItemOffsets(outRect, view, parent, state);
            outRect.top = space / 2;
            outRect.right = space;
            outRect.bottom = space / 2;
        }
    }
}

class CourseAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    final String[] info = new String[]{"courseUnitName", "credit", "examFormName", "courseNum", "clazzNum"};
    final CourseSelectionFragment c;
    final ArrayList<JSONObject> data = new ArrayList<>();

    public CourseAdapter(CourseSelectionFragment c) {
        super();
        this.c = c;
    }

    void add(JSONObject e) {
        data.add(e);
        notifyItemInserted(getItemCount() - 1);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        ItemCourseSelectionBinding binding = ItemCourseSelectionBinding.inflate(LayoutInflater.from(context), parent, false);
        for (int i = 0; i < info.length; i++) {
            Chip chip = (Chip) LayoutInflater.from(context).inflate(R.layout.item_action_chip, binding.courseInfo, false);
            chip.setOnLongClickListener(a -> {
                ((ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("", ((Chip) a).getText()));
                return false;
            });
            chip.setOnClickListener(a -> Snackbar.make(context, chip, ((Chip) a).getText(), Snackbar.LENGTH_LONG).show());
            binding.courseInfo.addView(chip);
        }
        return new RecyclerView.ViewHolder(binding.getRoot()) {
        };
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ItemCourseSelectionBinding binding = ItemCourseSelectionBinding.bind(holder.itemView);
        Context context = binding.getRoot().getContext();
        binding.courseName.setText(convert(position, "courseName"));
        binding.like.setSelected(data.get(position).getInteger("collectionStatus") == 1);
        binding.select.setSelected(data.get(position).getInteger("selectedStatus") == 3 || data.get(position).getInteger("selectedStatus") == 4);
        binding.select.setText(binding.select.isSelected() ? "退课" : "选课");
        binding.like.setText(binding.like.isSelected() ? "取消收藏" : "收藏");
        binding.select.setOnClickListener(v -> {
            if (v.isSelected()) {
                c.unselect(convert(position, "courseId"), convert(position, "teachingClassId"));
            } else {
                c.select(convert(position, "teachingClassId"));
            }
            v.setSelected(!v.isSelected());
            ((MaterialButton) v).setText(v.isSelected() ? "退课" : "选课");
        });
        binding.like.setOnClickListener(v -> {
            Snackbar.make(v, "已" + ((MaterialButton) v).getText(), Snackbar.LENGTH_LONG).show();
            c.like(convert(position, "teachingClassId"));
            v.setSelected(!v.isSelected());
            ((MaterialButton) v).setText(v.isSelected() ? "取消收藏" : "收藏");
        });
        binding.open.setOnClickListener(v -> context.startActivity(new Intent(context, CourseDetailActivity.class).putExtra("code", convert(position, "courseNum")).putExtra("id", convert(position, "courseId")).putExtra("class", convert(position, "clazzNum")), ActivityOptionsCompat.makeSceneTransitionAnimation((Activity) context, v, "miniapp").toBundle()));
        binding.courseName.setText(convert(position, "courseName"));
        binding.head.setText(convert(position, "teachingTimePlace").replace(";", " | ").replace(",", "\n"));
        for (int i = 0; i < info.length; i++) {
            String content = convert(position, info[i]);
            ((Chip) binding.courseInfo.getChildAt(i)).setText(String.format("%s：%s", (new String[]{"开设部门", "学分", "考查形式", "课程代码", "班级代码", "剩余空位", "待筛选人数", "选上人数"})[i], content));
        }
        String[] seats = new String[]{"baseReceiveNum", "filterSelectedNum", "courseSelectedNum"};
        for (int i = 0; i < seats.length; i++) {
            String content = convert(position, seats[i]);
            (new MaterialButton[]{binding.left, binding.filtering, binding.selected}[i]).setText(String.format("%s\n%s", (new String[]{"剩余", "待筛选", "选上"})[i], content));
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    String convert(int position, String key) {
        String a = data.get(position).getString(key);
        return (a == null ? "" : a).replace("\n\n", "\n");
    }

    public void clear() {
        int tmp = getItemCount();
        data.clear();
        notifyItemRangeRemoved(0, tmp);
    }
}