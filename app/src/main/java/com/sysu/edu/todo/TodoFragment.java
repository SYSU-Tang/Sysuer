package com.sysu.edu.todo;

import static com.sysu.edu.api.CommonUtil.isEmpty;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.haibin.calendarview.Calendar;
import com.haibin.calendarview.CalendarView;
import com.sysu.edu.R;
import com.sysu.edu.databinding.FragmentTodoBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class TodoFragment extends Fragment {
    
    final androidx.recyclerview.widget.ConcatAdapter concatAdapter = new androidx.recyclerview.widget.ConcatAdapter(new androidx.recyclerview.widget.ConcatAdapter.Config.Builder().setIsolateViewTypes(true).build());
    final TodoInfo todoInfo = new TodoInfo();
    FragmentTodoBinding binding;
    String date;
    boolean due = true;
    boolean ddl = false;
    boolean todo = true;
    boolean done = true;
    private TodoManager todoManager;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTodoBinding.inflate(inflater, container, false);
        binding.recyclerView.setAdapter(concatAdapter);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        binding.calendarView.setOnCalendarSelectListener(new CalendarView.OnCalendarSelectListener() {
            @Override
            public void onCalendarOutOfRange(Calendar calendar) {
            }
            
            @Override
            public void onCalendarSelect(Calendar calendar, boolean isClick) {
                date = simpleDateFormat.format(calendar.getTimeInMillis());
                todoManager.performRefresh();
            }
        });
        Toolbar toolbar = requireActivity().findViewById(R.id.toolbar);
        binding.calendarView.setOnMonthChangeListener((year, month) -> toolbar.setSubtitle(String.format(Locale.getDefault(), "%d年%d月", year, month)));
        toolbar.setSubtitle(String.format(Locale.getDefault(), "%d年%d月", binding.calendarView.getCurYear(), binding.calendarView.getCurMonth()));
        binding.calendarView.setSelectSingleMode();
        todoManager = new TodoManager(requireActivity(), concatAdapter);
        todoManager.setOnRefreshListener(this::refresh);
        requireActivity().findViewById(R.id.add).setOnClickListener(_ -> {
            todoManager.showTodoAddDialog();
            todoManager.getTodoInfo().setDueDate(getDate(simpleDateFormat));
        });
        
        todoInfo.setStatus(null);
        date = getDate(simpleDateFormat);
        
        ((MaterialButtonToggleGroup) requireActivity().findViewById(R.id.todo_date)).addOnButtonCheckedListener((_, checkedId, isChecked) -> {
            if (checkedId == R.id.due_todo) due = isChecked;
            else if (checkedId == R.id.ddl_todo) ddl = isChecked;
            refresh();
        });
        MaterialButtonToggleGroup status = requireActivity().findViewById(R.id.todo_status);
        status.check(R.id.todo_todo);
        status.check(R.id.done_todo);
        status.addOnButtonCheckedListener((_, checkedId, isChecked) -> {
            if (checkedId == R.id.todo_todo) todo = isChecked;
            else if (checkedId == R.id.done_todo) done = isChecked;
            refresh();
        });
        refresh();
        return binding.getRoot();
    }
    
    private String getDate(SimpleDateFormat simpleDateFormat) {
        return simpleDateFormat.format(binding.calendarView.getSelectedCalendar().getTimeInMillis());
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    
    public void refresh() {
        ArrayList<String> a = new ArrayList<>();
        ArrayList<String> b = new ArrayList<>();
        HashMap<String, MutableLiveData<?>> map = new HashMap<>();
        System.out.println(todoInfo.getPriority().getValue());
//        map.put("due_date", todoInfo.getDueDate());
//        map.put("ddl", todoInfo.getDdlDate());
        map.put("status", todoInfo.getStatus());
        map.put("title", todoInfo.getTitle());
        map.put("description", todoInfo.getDescription());
        map.put("priority", todoInfo.getPriority());
        map.put("todo_type", todoInfo.getType());
        map.put("subtask", todoInfo.getSubtask());
        map.put("attachment", todoInfo.getAttachment());
        map.put("subject", todoInfo.getSubject());
        map.put("location", todoInfo.getLocation());
        map.put("color", todoInfo.getColor());
        map.put("label", todoInfo.getTag());
        map.put("due_time", todoInfo.getDueTime());
        map.put("remind_time", todoInfo.getRemindTime());
        map.put("done_datetime", todoInfo.getDoneDate());
        map.forEach((key, value) -> {
            if (!isEmpty(value.getValue())) {
                a.add(key + " = ?");
                b.add(String.valueOf(value.getValue()));
            }
        });
        if (due && ddl) {
            a.add("(due_date= ? OR ddl = ?)");
            b.add(date);
            b.add(date);
        } else if (due) {
            a.add("due_date= ?");
            b.add(date);
        } else if (ddl) {
            a.add("ddl= ?");
            b.add(date);
        }
        if (todo && done) {
            a.add("(status = ? OR status = ?)");
            b.add("0");
            b.add("1");
        } else if (todo) {
            a.add("status = ?");
            b.add("0");
        } else if (done) {
            a.add("status = ?");
            b.add("1");
        }
        todoManager.refresh(String.join(" AND ", a), b.toArray(new String[0]));
    }
    
    public TodoManager getTodoManager() {
        return todoManager;
    }
}