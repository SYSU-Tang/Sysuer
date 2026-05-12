package com.sysu.edu.academic;

import static android.text.TextUtils.isEmpty;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.ActivityAgendaBinding;
import com.sysu.edu.databinding.ItemPreferenceBinding;
import com.sysu.edu.todo.TitleAdapter;
import com.sysu.edu.view.RecyclerAdapter;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class AgendaActivity extends AppCompatActivity {

    HttpManager http;
    ActivityAgendaBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAgendaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Params params = new Params(this);
        params.setCallback(this::getAgenda);
        binding.list.setLayoutManager(new LinearLayoutManager(this));
        ConcatAdapter concatAdapter = new ConcatAdapter();
        binding.list.setAdapter(concatAdapter);
        binding.toolbar.setNavigationOnClickListener(_ -> supportFinishAfterTransition());
        http = new HttpManager(new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (msg.what == -1) params.toast(R.string.no_net_connected);
                else if (!msg.getData().getBoolean("isJSON")) {
                    params.toast(R.string.login_warning);
                    params.gotoLogin(TargetUrl.PORTAL);
                } else {
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    if (response != null && response.getJSONObject("meta").getInteger("statusCode").equals(200) && response.get("data") != null) {
                        if (msg.what == 0) {
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
                    } else if (response != null && response.getJSONObject("meta").getInteger("statusCode").equals(401)) {
                        params.toast(response.getJSONObject("meta").getString("message"));
                    } else {
                        params.toast(getString(R.string.login_warning));
                        params.gotoLogin(TargetUrl.PORTAL);
                    }
                }
            }
        });
        http.setParams(params);
//        http.setTarget(TargetUrl.PORTAL);
//        System.out.println("loginForPortal:" + CookieManager.getInstance().getCookie(TargetUrl.PORTAL));
        getAgenda();
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
    }

    void getAgenda() {
        http.postRequest("https://portal.sysu.edu.cn/newClient/api/schedule/newSchedule/getScheduleByTimeZone", getParam().toString(), 0);
    }

    JSONObject getParam() {
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