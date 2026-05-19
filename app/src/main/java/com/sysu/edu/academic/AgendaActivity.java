package com.sysu.edu.academic;

import static android.text.TextUtils.isEmpty;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.haibin.calendarview.Calendar;
import com.haibin.calendarview.CalendarView;
import com.sysu.edu.R;
import com.sysu.edu.databinding.ActivityAgendaBinding;
import com.sysu.edu.databinding.ItemPreferenceBinding;
import com.sysu.edu.model.PortalModel;
import com.sysu.edu.todo.TitleAdapter;
import com.sysu.edu.view.RecyclerAdapter;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class AgendaActivity extends AppCompatActivity {
    
    ActivityAgendaBinding binding;
    PortalModel model;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAgendaBinding.inflate(getLayoutInflater());
        model = new PortalModel(this);
        setContentView(binding.getRoot());
        binding.list.setLayoutManager(new LinearLayoutManager(this));
        ConcatAdapter concatAdapter = new ConcatAdapter();
        binding.list.setAdapter(concatAdapter);
        binding.toolbar.setNavigationOnClickListener(_ -> supportFinishAfterTransition());
        binding.calendarView.setOnCalendarSelectListener(new CalendarView.OnCalendarSelectListener() {
            @Override
            public void onCalendarOutOfRange(Calendar calendar) {
            }
            
            @Override
            public void onCalendarSelect(Calendar calendar, boolean isClick) {
                getAgenda();
            }
        });
        binding.calendarView.setOnMonthChangeListener((year, month) -> binding.toolbar.setSubtitle(String.format(Locale.getDefault(), "%d年%d月", year, month)));
        binding.toolbar.setSubtitle(String.format(Locale.getDefault(), "%d年%d月", binding.calendarView.getCurYear(), binding.calendarView.getCurMonth()));
        binding.calendarView.setSelectSingleMode();
        model.getMessage().observe(this, message -> {
            JSONObject response = message.getSecond();
            if (response != null && response.getJSONObject("meta").getInteger("statusCode").equals(200) && response.get("data") != null) {
                if (message.getFirst() == 0) {
                    concatAdapter.getAdapters().forEach(concatAdapter::removeAdapter);
                    JSONArray data = response.getJSONArray("data");
                    if (!data.isEmpty())
                        data.getJSONObject(0).getJSONArray("newUserScheduleDetailList").forEach(i -> {
                            JSONObject item = (JSONObject) i;
                            concatAdapter.addAdapter(new TitleAdapter(item.getString("timeZone")));
                            AgendaAdapter agendaAdapter = new AgendaAdapter();
                            concatAdapter.addAdapter(agendaAdapter);
                            agendaAdapter.add(item);
                        });
                }
            }
        });
        getAgenda();
    }
    
    void getAgenda() {
        model.addAndNext("newClient/api/schedule/newSchedule/getScheduleByTimeZone", getArgs().toString(), 0);
    }
    
    JSONObject getArgs() {
        String day = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(binding.calendarView.getSelectedCalendar().getTimeInMillis());
        return JSONObject.of(
                "startTime", day,
                "endTime", day,
                "types", null,
                "isMine", "1",
                "teamWorkDeptId", null
        );
    }
    
    static class AgendaAdapter extends RecyclerAdapter<JSONObject> {
        
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(ItemPreferenceBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false).getRoot()) {
            };
        }
        
        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ItemPreferenceBinding binding = ItemPreferenceBinding.bind(holder.itemView);
            JSONObject item = get(position);
            binding.itemTitle.setText(item.getString("title"));
            String place = item.getString("place");
            if (!isEmpty(place))
                binding.itemContent.setText(place);
            else
                binding.itemContent.setVisibility(View.GONE);
            binding.itemIcon.setImageResource(R.drawable.text);
            binding.getRoot().updateAppearance(position, getItemCount());
            binding.getRoot().setOnClickListener(_ -> {
            });
            super.onBindViewHolder(holder, position);
        }
    }
}