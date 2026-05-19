package com.sysu.edu.academic;

import static com.sysu.edu.api.CommonUtil.bool2int;
import static com.sysu.edu.api.CommonUtil.toStringOrDefault;
import static com.sysu.edu.api.CommonUtil.trim;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
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

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.transition.MaterialContainerTransform;
import com.sysu.edu.R;
import com.sysu.edu.api.CommonUtil;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.FragmentCourseSelectionBinding;
import com.sysu.edu.databinding.ItemActionChipBinding;
import com.sysu.edu.databinding.ItemCourseSelectionBinding;
import com.sysu.edu.model.JwxtModel;
import com.sysu.edu.view.RecyclerAdapter;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

public class CourseSelectionFragment extends Fragment {
    
    final MutableLiveData<String> filter = new MutableLiveData<>();
    final MutableLiveData<Integer> type = new MutableLiveData<>(1);
    final MutableLiveData<Integer> category = new MutableLiveData<>(11);
    final MediatorLiveData<CommonUtil.Tuple2<Integer, Integer>> typeCate = new MediatorLiveData<>();
    FragmentCourseSelectionBinding binding;
    int tmp;
    int page = 1;
    CourseAdapter adp;
    Integer total;
    String term;
    CourseSelectionViewModel vm;
    GridLayoutManager gm;
    Params params;
    JwxtModel model;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (binding == null) {
            binding = FragmentCourseSelectionBinding.inflate(inflater, container, false);
            model = new JwxtModel(requireContext());
            vm = new ViewModelProvider(requireActivity()).get(CourseSelectionViewModel.class);
            params = new Params(this);
            vm.filterValue.observe(requireActivity(), _ -> {
                filter.setValue(vm.getReturnData());
                binding.head.seniorFilter.removeAllViews();
                vm.getFilterName().forEach((_, v) ->
                {
                    if (v != null && !v.isEmpty()) {
                        ItemActionChipBinding item = ItemActionChipBinding.inflate(inflater, binding.head.filter, false);
                        item.getRoot().setText(v);
                        binding.head.seniorFilter.addView(item.getRoot());
                    }
                });
                regetCourseList();
            });
            binding.head.type.setOnCheckedStateChangeListener((chipGroup, _) -> {
                int cid = chipGroup.getCheckedChipId();
                if (cid == R.id.my_major) selectCategory();
                else type.setValue((cid == R.id.college_public_selective) ? 4 : 2);
                if (cid != R.id.my_major && binding.head.category.getHeight() != 0)
                    tmp = binding.head.category.getHeight();
                ValueAnimator animator = ValueAnimator.ofInt(chipGroup.getCheckedChipId() == R.id.my_major ? new int[]{0, tmp} : new int[]{binding.head.category.getHeight() == 0 ? 0 : tmp, 0});
                animator.addUpdateListener(valueAnimator -> {
                    LinearLayout.LayoutParams lp = ((LinearLayout.LayoutParams) binding.head.category.getLayoutParams());
                    lp.height = (int) valueAnimator.getAnimatedValue();
                    binding.head.category.setLayoutParams(lp);
                });
                animator.start();
            });
            binding.zoom.setOnClickListener(_ -> binding.head.getRoot().setVisibility(binding.head.getRoot().getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
            typeCate.addSource(type, s -> typeCate.setValue(new CommonUtil.Tuple2<>(CommonUtil.toIntegerOrDefault(type.getValue(), 1), s)));
            typeCate.addSource(category, s -> typeCate.setValue(new CommonUtil.Tuple2<>(CommonUtil.toIntegerOrDefault(category.getValue(), 11), s)));
            typeCate.observe(requireActivity(), _ -> regetCourseList());
            binding.head.category.setOnCheckedStateChangeListener((_, _) -> selectCategory());
            binding.course.setLayoutManager(gm = new GridLayoutManager(requireContext(), params.getColumn()));
            binding.course.addItemDecoration(new SpacesItemDecoration(params.dpToPx(8)));
            binding.course.setAdapter(adp = new CourseAdapter());
            binding.head.filter.setOnCheckedStateChangeListener((_, _) -> regetCourseList());
            adp.setSelectAction(position -> {
                if (adp.get(position).getInteger("selectedStatus") == 3 || adp.get(position).getInteger("selectedStatus") == 4)
                    unselect(adp.convert(position, "courseId"), adp.convert(position, "teachingClassId"));
                else select(adp.convert(position, "teachingClassId"));
            });
            adp.setLikeAction(this::like);
            binding.course.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView v, int dx, int dy) {
                    if (!v.canScrollVertically(1) && total / 10 + 1 > page && dy > 0)
                        getCourseList();
                    binding.head.getRoot().setElevation(v.canScrollVertically(-1) ? params.dpToPx(2) : 0);
                }
            });
            model.getMessage().observe(requireActivity(), message -> {
                JSONObject response = message.getSecond();
                Integer code = response.getInteger("code");
                if (Objects.equals(code, 200)) {
                    switch (message.getFirst()) {
                        case 0 -> {
                            term = response.getJSONObject("data").getString("semesterYear");
                            getCourseList();
                        }
                        case 1 -> {
                            JSONObject data = response.getJSONObject("data");
                            if (data != null) {
                                total = data.getInteger("total");
                                data.getJSONArray("rows").forEach(e -> adp.add((JSONObject) e));
                            }
                        }
                        case 3 -> {
                            params.toast(response.getString("data"));
                            regetCourseList();
                        }
                    }
                    model.nextAll();
                }
            });
            getInfo();
        }
        return binding.getRoot();
    }
    
    private void regetCourseList() {
        clear();
        getCourseList();
    }
    
    private void selectCategory() {
        int cid = binding.head.category.getCheckedChipId();
        if (cid == R.id.major_compulsory) typeCate.setValue(new CommonUtil.Tuple2<>(1, 11));
        else if (cid == R.id.major_selective) typeCate.setValue(new CommonUtil.Tuple2<>(1, 21));
        else if (cid == R.id.school_public_selective)
            typeCate.setValue(new CommonUtil.Tuple2<>(1, 30));
        else if (cid == R.id.pe) typeCate.setValue(new CommonUtil.Tuple2<>(3, 10));
        else if (cid == R.id.en) typeCate.setValue(new CommonUtil.Tuple2<>(5, 1));
        else if (cid == R.id.public_compulsory) typeCate.setValue(new CommonUtil.Tuple2<>(1, 10));
        else if (cid == R.id.honor) typeCate.setValue(new CommonUtil.Tuple2<>(1, 31));
        
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            binding.head.addFilter.setOnClickListener(v ->
                            Navigation.findNavController(view).navigate(R.id.selection_to_filter1, null, new NavOptions.Builder()
//                                            .setExitAnim(androidx.navigation.ui.R.anim.nav_default_pop_enter_anim)
//                            .setEnterAnim()
//                            .setExitAnim(android.R.animator.fade_out)
                                    
                                    .build(), new FragmentNavigator.Extras.Builder().addSharedElement(v, "miniapp").build())
            );
        }
        MaterialContainerTransform transition = new MaterialContainerTransform();
        transition.setScrimColor(Color.TRANSPARENT);
        transition.setAllContainerColors(requireContext().getColor(com.google.android.material.R.color.design_default_color_surface));
        setSharedElementEnterTransition(transition);
        setSharedElementReturnTransition(transition);
        super.onViewCreated(view, savedInstanceState);
    }
    
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        gm.setSpanCount(params.getColumn());
    }
    
    void clear() {
        if (adp != null) adp.clear();
        page = 0;
        total = -1;
    }
    
    void getCourseList() {
        if (type.getValue() != null && category.getValue() != null && term != null)
            getCourseList(getType(), getCategory(), term, toStringOrDefault(filter.getValue()));
        
    }
    
    void getCourseList(int selectedType, int selectedCate, String term, String filterText) {
        model.addAndNext("jwxt/choose-course-front-server/classCourseInfo/course/list",
                String.format(Locale.getDefault(), "{\"pageNo\":%d,\"pageSize\":10,\"param\":{\"semesterYear\":\"%s\",\"selectedType\":\"%d\",\"selectedCate\":\"%d\",\"hiddenConflictStatus\":\"0\",\"hiddenSelectedStatus\":\"%d\",\"hiddenEmptyStatus\":\"%d\",\"vacancySortStatus\":\"%d\",\"collectionStatus\":\"%d\"%s}}", ++page, term, selectedType, selectedCate,
                        bool2int(binding.head.hideSelected.isChecked()), bool2int(binding.head.hideVacancy.isChecked()), bool2int(binding.head.vacancy.isChecked()), bool2int(binding.head.onlyCollection.isChecked()),
                        filterText), 1);
    }
    
    void like(String code) {
        model.addAndNext("jwxt/choose-course-front-server/stuCollectedCourse/create",
                String.format("{\"classesID\":\"%s\",\"selectedType\":\"1\"}", code),
                3);
    }
    
    void getInfo() {
        model.addAndNext("jwxt/choose-course-front-server/classCourseInfo/selectCourseInfo", 0);
    }
    
    void select(String code) {
        model.addAndNext("jwxt/choose-course-front-server/classCourseInfo/course/choose",
                String.format(Locale.getDefault(), "{\"clazzId\":\"%s\",\"selectedType\":\"%d\",\"selectedCate\":\"%d\",\"check\":true}", code, getType(), getCategory()),
                3);
        
    }
    
    int getType() {
        return CommonUtil.toIntegerOrDefault(typeCate.getValue().getFirst(), 1);
    }
    
    int getCategory() {
        return CommonUtil.toIntegerOrDefault(typeCate.getValue().getSecond(), 11);
    }
    
    void unselect(String classId, String code) {
        model.addAndNext("jwxt/choose-course-front-server/classCourseInfo/course/back",
                String.format(Locale.getDefault(), "{\"courseId\":\"%s\",\"clazzId\":\"%s\",\"selectedType\":\"%d\"}", classId, code, getType()),
                3);
        
    }
    
    static class SpacesItemDecoration extends RecyclerView.ItemDecoration {
        private final int space;
        
        public SpacesItemDecoration(int i) {
            space = i;
        }
        
        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            outRect.top = space / 2;
            outRect.right = space;
            outRect.left = space;
            outRect.bottom = space / 2;
        }
    }
    
    public static class CourseAdapter extends RecyclerAdapter<JSONObject> {
        final String[] info = new String[]{"credit", "clazzNum", "scheduleExamTime", "examFormName", "statusName"};
        Consumer<? super Integer> selectAction;
        Consumer<? super String> likeAction;
        
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
        
        public void setSelectAction(Consumer<? super Integer> action) {
            selectAction = action;
        }
        
        public void setLikeAction(Consumer<? super String> action) {
            likeAction = action;
        }
        
        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ItemCourseSelectionBinding binding = ItemCourseSelectionBinding.bind(holder.itemView);
            Context context = binding.getRoot().getContext();
            binding.courseName.setText(String.format("%s-%s", convert(position, "courseNum"), convert(position, "courseName")));
            JSONObject item = data.get(position);
            Integer selectedStatus = item.getInteger("selectedStatus");
            item.fluentPut("statusName", context.getString(selectedStatus == 4 ? R.string.status_selected : selectedStatus == 3 ? R.string.filtering : selectedStatus == 1 ? R.string.retired : R.string.unselected));
            binding.like.setSelected(item.containsKey("collectionStatus") && item.getInteger("collectionStatus") == 1);
            binding.select.setSelected(item.containsKey("selectedStatus") && (selectedStatus == 3 || selectedStatus == 4));
            binding.select.setText(binding.select.isSelected() ? context.getString(R.string.drop_course) : context.getString(R.string.select_course));
            binding.like.setText(binding.like.isSelected() ? context.getString(R.string.unlike) : context.getString(R.string.like));
            binding.select.setOnClickListener(_ -> {
                if (selectAction != null)
                    selectAction.accept(position);
            });
            binding.like.setOnClickListener(v -> {
                Snackbar.make(v, context.getString(R.string.already) + ((MaterialButton) v).getText(), Snackbar.LENGTH_LONG).show();
                ((MaterialButton) v).setText(v.isSelected() ? context.getString(R.string.unlike) : context.getString(R.string.like));
                if (likeAction != null)
                    likeAction.accept(convert(position, "teachingClassId"));
                v.setSelected(!v.isSelected());
            });
            binding.open.setOnClickListener(v -> context.startActivity(new Intent(context, CourseDetailActivity.class).putExtra("code", convert(position, "courseNum")).putExtra("id", convert(position, "courseId")).putExtra("class", convert(position, "clazzNum")), ActivityOptionsCompat.makeSceneTransitionAnimation((Activity) context, v, "miniapp").toBundle()));
            binding.head.setText(convert(position, "teachingTimePlace").replace(";", " | ").replace(",", "\n"));
            String[] courseInfoLabels = context.getResources().getStringArray(R.array.course_info_labels);
            String[] seatInfoLabels = context.getResources().getStringArray(R.array.seat_info_labels);
            for (int i = 0; i < info.length; i++)
                ((Chip) binding.courseInfo.getChildAt(i)).setText(String.format("%s：%s", courseInfoLabels[i], convert(position, info[i])));
            String[] seats = new String[]{"baseReceiveNum", "filterSelectedNum", "courseSelectedNum"};
            for (int i = 0; i < seats.length; i++) {
                String content = convert(position, seats[i]);
                (new MaterialButton[]{binding.left, binding.filtering, binding.selected}[i]).setText(String.format("%s\n%s", seatInfoLabels[i], content));
            }
        }
        
        public String convert(int position, String key) {
            return trim(get(position).getString(key)).replace("\n\n", "\n");
        }
    }
}

