package com.sysu.edu.academic;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.ActivityClassroomQueryBinding;
import com.sysu.edu.databinding.ItemClassroomResultBinding;
import com.sysu.edu.databinding.ItemFilterChipBinding;
import com.sysu.edu.model.JwxtModel;
import com.sysu.edu.view.RecyclerAdapter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ClassroomQueryActivity extends AppCompatActivity {
    
    final HashMap<Integer, String> office = new HashMap<>();
    final MutableLiveData<String> campusLiveData = new MutableLiveData<>();
    final ArrayList<String> classType = new ArrayList<>(List.of("002", "003"));
    JwxtModel model;
    String dateStr;
    String startClassTime = "1";
    String endClassTime = "11";
    RoomAdapter roomAdapter;
    int page = 1;
    int total = 0;
    ActivityClassroomQueryBinding binding;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final MaterialDatePicker<Long> dateDialog = MaterialDatePicker.Builder.datePicker().build();
        final HashMap<String, ArrayList<Chip>> classroom = new HashMap<>();
        binding = ActivityClassroomQueryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        model = new JwxtModel(this);
        Params params = new Params(this);
        binding.campusSelectAll.setOnClickListener(v -> {
            for (int i = 1; i < ((ChipGroup) v.getParent()).getChildCount(); i++)
                ((Chip) ((ChipGroup) v.getParent()).getChildAt(i)).toggle();
        });
        binding.officeSelectAll.setOnClickListener(v -> {
            for (int i = 1; i < ((ChipGroup) v.getParent()).getChildCount(); i++)
                ((Chip) ((ChipGroup) v.getParent()).getChildAt(i)).toggle();
        });
        dateDialog.addOnPositiveButtonClickListener(selection -> {
            LocalDate date = Instant.ofEpochMilli(selection)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            binding.dateText.setText(date.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")));
        });
        roomAdapter = new RoomAdapter();
        binding.toolbar.setNavigationOnClickListener(_ -> supportFinishAfterTransition());
        binding.result.setAdapter(roomAdapter);
        binding.result.setLayoutManager(new StaggeredGridLayoutManager(params.getColumn(), StaggeredGridLayoutManager.VERTICAL));
        BottomSheetBehavior.from(binding.resultSheet).setState(BottomSheetBehavior.STATE_HIDDEN);
        binding.date.setOnClickListener(_ -> dateDialog.show(getSupportFragmentManager(), null));
        binding.timeSlider.addOnChangeListener((slider, _, _) -> {
            startClassTime = String.format(Locale.getDefault(), "%.0f", slider.getValues().get(0));
            endClassTime = String.format(Locale.getDefault(), "%.0f", slider.getValues().get(1));
            binding.time.setText(String.format(getString(R.string.section_range_x), startClassTime, endClassTime));
        });
        binding.query.setOnClickListener(_ -> {
            roomAdapter.clear();
            page = 1;
            getRoom();
        });
        binding.result.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (!recyclerView.canScrollVertically(1) && total / 20 + 1 >= page) getRoom();
            }
        });
        binding.reset.setOnClickListener(_ -> {
            binding.officeGroup.getCheckedChipIds().forEach(e -> ((Chip) binding.officeGroup.findViewById(e)).setChecked(false));
            binding.campusGroup.getCheckedChipIds().forEach(e -> ((Chip) binding.campusGroup.findViewById(e)).setChecked(false));
            binding.typeGroup.getCheckedChipIds().forEach(e -> ((Chip) binding.typeGroup.findViewById(e)).setChecked(true));
            binding.timeSlider.setValues(List.of(1.0f, 11.0f));
            dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            binding.dateText.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")));
        });
        dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        binding.dateText.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")));
        getCampus();
        model.getMessage().observe(this, message -> {
            JSONObject response = message.getSecond();
            if (response.getInteger("code") == 200) {
                if (message.getFirst() == 3) {
                    JSONObject data = response.getJSONObject("data");
                    total = data.getInteger("total");
                    data.getJSONArray("rows").forEach(a -> roomAdapter.add((JSONObject) a));
                    BottomSheetBehavior.from(binding.resultSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
                    roomAdapter.setHost(model.getHost());
                    roomAdapter.setCookie(model.getCookieManager().toSimpleString(model.getHost()));
                } else {
                    binding.timeSlider.setValueFrom(1);
                    response.getJSONArray("data").forEach(campusInfo -> {
                        switch (message.getFirst()) {
                            case 1 -> {
                                String id = ((JSONObject) campusInfo).getString("id");
                                Chip chip = ItemFilterChipBinding.inflate(getLayoutInflater(), binding.campusGroup, false).getRoot();
                                binding.campusGroup.addView(chip);
                                chip.setOnCheckedChangeListener((_, isChecked) -> {
                                    if (isChecked) {
                                        if (classroom.containsKey(id))
                                            Objects.requireNonNull(classroom.get(id)).forEach(e -> e.setVisibility(View.VISIBLE));
                                        else getOffice(id);
                                    } else
                                        Objects.requireNonNull(classroom.get(id)).forEach(e -> e.setVisibility(View.GONE));
                                });
                                chip.setText(((JSONObject) campusInfo).getString("campusName"));
                            }
                            case 2 -> {
                                classroom.computeIfAbsent(campusLiveData.getValue(), _ -> new ArrayList<>());
                                Chip chip = ItemFilterChipBinding.inflate(getLayoutInflater(), binding.officeGroup, false).getRoot();
                                binding.officeGroup.addView(chip);
                                office.put(chip.getId(), ((JSONObject) campusInfo).getString("id"));
                                chip.setText(((JSONObject) campusInfo).getString("dataName"));
                                Objects.requireNonNull(classroom.get(campusLiveData.getValue())).add(chip);
                            }
                        }
                    });
                }
                model.nextAll();
            } 
        });
        model.next();
    }
    
    public void getCampus() {
        model.add("jwxt/base-info/campus/findCampusNamesBox", 1);
    }
    
    public void getOffice(String campus) {
        campusLiveData.setValue(campus);
        model.addAndNext("jwxt/schedule/agg/selfStudyClassRoom/buildingConditionPull", "{\"campusIdList\":[\"" + campus + "\"]}", 2);
    }
    
    public void getRoom() {
        ArrayList<String> teachingBuildIDs = new ArrayList<>();
        classType.clear();
        binding.typeGroup.getCheckedChipIds().forEach(e -> classType.add("自习室".equals(((Chip) findViewById(e)).getText().toString()) ? "003" : "002"));
        binding.officeGroup.getCheckedChipIds().forEach(e -> {
            if (findViewById(e).getVisibility() == View.VISIBLE)
                teachingBuildIDs.add(office.get(e));
        });
        if (teachingBuildIDs.isEmpty()) model.getContextUtil().toast("请先选择教学楼");
        else
            model.addAndNext("jwxt/schedule/agg/selfStudyClassRoom/pageListStudyClassroom", String.format(Locale.getDefault(), "{\"pageNo\":%d,\"pageSize\":20,\"param\":{\"dateStr\":\"%s\",\"teachingBuildIDs\":%s,\"startClassTimes\":%s,\"endClassTimes\":%s,\"classRoomTagList\":%s}}", page++, dateStr, JSON.toJSONString(teachingBuildIDs), startClassTime, endClassTime, JSON.toJSONString(classType)), 3);
    }
    
    static class RoomAdapter extends RecyclerAdapter<JSONObject> {
        
        private String host;
        private String cookie;
        
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(ItemClassroomResultBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false).getRoot()) {
            };
        }
        
        public void setHost(String host) {
            this.host = host;
        }
        
        public void setCookie(String cookie) {
            this.cookie = cookie;
        }
        
        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ItemClassroomResultBinding binding = ItemClassroomResultBinding.bind(holder.itemView);
            Context context = holder.itemView.getContext();
            JSONObject item = get(position);
            binding.location.setText(item.getString("teachingBuildingName"));
            binding.time.setText(item.getString("classTimes"));
            binding.floor.setText(item.getString("floor"));
            binding.seat.setText(item.getString("seats"));
            binding.type.setText(item.getString("classRoomTag"));
            binding.name.setText(item.getString("classRoomNum"));
            Glide.with(context)
                    .load(new GlideUrl("https://" + host + "/jwxt/base-info/classroom/classRoomView?fileName=jspic.png&filePath=" + item.get("photoPath"), new LazyHeaders.Builder()
                            .addHeader("Cookie", cookie)
                            .addHeader("Referer", "https://jwxt.sysu.edu.cn/")
                            .build()))
                    .placeholder(R.drawable.logo)
                    .override((int) (145 * 3.6), (int) (132 * 3.6))
                    .fitCenter()
                    .into(binding.image);
            binding.getRoot().setOnClickListener(_ -> {
            });
            super.onBindViewHolder(holder, position);
        }
    }
}
