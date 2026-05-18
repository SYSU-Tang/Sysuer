package com.sysu.edu.home;

import static com.sysu.edu.api.CommonUtil.isEmpty;
import static com.sysu.edu.api.CommonUtil.trim;

import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.sysu.edu.ClassNotificationWorker;
import com.sysu.edu.MainActivity;
import com.sysu.edu.R;
import com.sysu.edu.academic.AgendaActivity;
import com.sysu.edu.academic.CourseDetailActivity;
import com.sysu.edu.academic.CourseScheduleActivity;
import com.sysu.edu.academic.ExamActivity;
import com.sysu.edu.api.CalendarManager;
import com.sysu.edu.api.ContextUtil;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.PreferenceViewModel;
import com.sysu.edu.browser.BrowserActivity;
import com.sysu.edu.databinding.DialogServiceActionBinding;
import com.sysu.edu.databinding.DialogServiceOrderBinding;
import com.sysu.edu.databinding.FragmentDashboardBinding;
import com.sysu.edu.databinding.ItemCourseBinding;
import com.sysu.edu.databinding.ItemExamBinding;
import com.sysu.edu.model.JwxtModel;
import com.sysu.edu.todo.TodoActivity;
import com.sysu.edu.todo.TodoInfo;
import com.sysu.edu.todo.TodoManager;
import com.sysu.edu.view.RecyclerAdapter;

import org.commonmark.node.Heading;
import org.commonmark.node.Node;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.Markwon;
import io.noties.markwon.MarkwonSpansFactory;
import io.noties.markwon.MarkwonVisitor;
import io.noties.markwon.core.CoreProps;

public class DashboardFragment extends Fragment {
    
    final ArrayList<JSONObject> todayCourse = new ArrayList<>();
    final ArrayList<JSONObject> tomorrowCourse = new ArrayList<>();
    final LinkedList<JSONObject> thisWeekExams = new LinkedList<>();
    final LinkedList<JSONObject> nextWeekExams = new LinkedList<>();
    final MutableLiveData<String> todoDate = new MutableLiveData<>("");
    Params params;
    JwxtModel model;
    HomeCollectionHelper db;
    FragmentDashboardBinding binding;
    boolean isRefreshRequired = true;
    HomeViewModel viewModel;
    BottomSheetDialog orderDialog;
    ServiceFragment.CollectionAdapter collectionAdapter;
    BottomSheetDialog actionDialog;
    DialogServiceActionBinding actionBinding;
    private TodoManager todoManager;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (isRefreshRequired) {
            binding = FragmentDashboardBinding.inflate(inflater, container, false);
            CalendarManager calendar = new CalendarManager();
            model = new JwxtModel(requireContext());
            db = new HomeCollectionHelper(requireContext());
            viewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
            viewModel.updateDashboardShortcut.observe(requireActivity(), _ -> getShortcutCollection());
            binding.scan.setOnClickListener(_ -> {
                try {
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName("com.tencent.mm", "com.tencent.mm.ui.LauncherUI"));
                    intent.putExtra("LauncherUI.From.Scaner.Shortcut", true);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setAction("android.intent.initActionDialog.VIEW");
                    if (intent.resolveActivity(requireContext().getPackageManager()) != null)
                        startActivity(intent);
                } catch (ActivityNotFoundException _) {
                }
            });
            binding.qrcode.setOnClickListener(_ -> {
                String linking = androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext()).getString("qrcode", "");
                if (!linking.isEmpty()) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(linking));
                        if (intent.resolveActivity(requireContext().getPackageManager()) != null)
                            startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        params.toast(R.string.fix_sysu_code_warning);
                    }
                } else {
                    params.toast(R.string.set_sysu_code_warning);
                    //new LaunchMiniProgram(requireActivity()).launchMiniProgram("gh_85575b9f544e");
                }
            });
            binding.agenda.setOnClickListener(gotoActivity(CourseScheduleActivity.class));
            binding.courseList.addItemDecoration(new DividerItemDecoration(requireContext(), 0));
            binding.courseList.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
            binding.examList.addItemDecoration(new DividerItemDecoration(requireContext(), 0));
            binding.examList.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
            binding.courseTitle.setOnClickListener(gotoActivity(CourseScheduleActivity.class));
            binding.examTitle.setOnClickListener(gotoActivity(ExamActivity.class));
            binding.todoTitle.setOnClickListener(gotoActivity(TodoActivity.class));
            binding.nextClass.setOnClickListener(gotoActivity(CourseScheduleActivity.class));
            binding.nextClassCard.setOnClickListener(gotoActivity(CourseScheduleActivity.class));
            binding.timeCard.setOnClickListener(gotoActivity(AgendaActivity.class));
            params = new Params(this);
            CourseAdapter courseAdapter = new CourseAdapter();
            courseAdapter.setParams(params);
            courseAdapter.setOnClick((jsonObject, view) -> startActivity(new Intent(getContext(), CourseDetailActivity.class).putExtra("code", jsonObject.getString("courseNum")).putExtra("class", jsonObject.getString("classesNum")), ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), view, "miniapp").toBundle()));
            binding.courseList.setAdapter(courseAdapter);
            ExamAdapter examAdapter = new ExamAdapter();
            examAdapter.setParams(params);
            binding.examList.setAdapter(examAdapter);
            binding.toggle.addOnButtonCheckedListener((_, checkedId, isChecked) -> {
                if (R.id.today == checkedId) {
                    courseAdapter.set(isChecked ? todayCourse : tomorrowCourse);
                    binding.noClass.setVisibility(courseAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                }
            });
            binding.toggle2.addOnButtonCheckedListener((_, checkedId, isChecked) -> {
                if (R.id.week_18 == checkedId) {
                    examAdapter.set(new ArrayList<>(isChecked ? thisWeekExams : nextWeekExams));
                    binding.noExam.setVisibility(examAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                }
            });
            String date = LocalDate.now().format(DateTimeFormatter.ofPattern("M月dd日", Locale.getDefault()));
            binding.date.setText(String.format("%s/%s", date, getResources().getStringArray(R.array.weeks)[LocalDate.now().getDayOfWeek().getValue() - 1]));
            model.getMessage().observe(requireActivity(), message -> {
                JSONObject response = (JSONObject) message.getSecond();
                if (response.get("code").equals(200)) {
                    switch (message.getFirst()) {
                        case 1 -> {
                            ArrayList<JSONObject> beforeArray = new ArrayList<>();
                            ArrayList<JSONObject> afterArray = new ArrayList<>();
                            response.getJSONArray("data").forEach(e -> {
                                JSONObject jsonObject = (JSONObject) e;
                                String status = getTimePosition(jsonObject.getString("teachingDate") + " " + jsonObject.getString("startTime"), jsonObject.getString("teachingDate") + " " + jsonObject.getString("endTime"));
                                jsonObject.put("status", status);
                                jsonObject.put("time", jsonObject.get("startTime") + "~" + jsonObject.get("endTime"));
                                jsonObject.put("course", "第" + jsonObject.get("startClassTimes") + "~" + jsonObject.get("endClassTimes") + "节课");
                                boolean isToday = "TD".equals(jsonObject.getString("useflag"));
                                if (isToday)
                                    (Objects.equals(status, "before") ? beforeArray : afterArray).add(jsonObject);
                                (isToday ? todayCourse : tomorrowCourse).add(jsonObject);
                            });
                            ContextUtil contextUtil = new ContextUtil(requireContext());
                            binding.progress.setMax(todayCourse.size());
                            binding.progress.setProgress(beforeArray.size());
                            binding.courseList.scrollToPosition(beforeArray.size());
                            Markwon.builder(requireContext()).usePlugin(new AbstractMarkwonPlugin() {
                                @Override
                                public void configureSpansFactory(@NonNull MarkwonSpansFactory.Builder builder1) {
                                    super.configureSpansFactory(builder1);
                                    builder1.appendFactory(Heading.class, (_, configuration) -> {
                                        if (CoreProps.HEADING_LEVEL.require(configuration) == 3)
                                            return new ForegroundColorSpan(contextUtil.getColorFromAttr(androidx.appcompat.R.attr.colorPrimary));
                                        return null;
                                    });
                                }
                                
                                @Override
                                public void configureVisitor(@NonNull MarkwonVisitor.Builder builder1) {
                                    super.configureVisitor(builder1);
                                    builder1.blockHandler(new MarkwonVisitor.BlockHandler() {
                                        @Override
                                        public void blockStart(@NonNull MarkwonVisitor visitor, @NonNull Node node) {
                                        }
                                        
                                        @Override
                                        public void blockEnd(@NonNull MarkwonVisitor visitor, @NonNull Node node) {
                                            if (visitor.hasNext(node))
                                                visitor.ensureNewLine();
                                        }
                                    });
                                }
                            }).build().setMarkdown(binding.nextClass, afterArray.isEmpty() ? String.format("### %s\n\n%s：**%s**\n\n%s：**%s**\n\n%s：**%s**",
                                    getString(R.string.noClass),
                                    getString(R.string.next_class), tomorrowCourse.isEmpty() ? getString(R.string.none) : tomorrowCourse.get(0).getString("courseName"),
                                    getString(R.string.location), tomorrowCourse.isEmpty() ? getString(R.string.none) : tomorrowCourse.get(0).getString("teachingPlace"),
                                    getString(R.string.time), tomorrowCourse.isEmpty() ? getString(R.string.none) : tomorrowCourse.get(0).getString("time")) :
                                    String.format("### %s\n\n%s：**%s**\n\n%s：**%s**\n\n%s：**%s**",
                                            todayCourse.get(beforeArray.size()).getString("courseName"),
                                            getString(R.string.location), todayCourse.get(beforeArray.size()).getString("teachingPlace"),
                                            getString(R.string.time), todayCourse.get(beforeArray.size()).getString("time"),
                                            getString(R.string.date), todayCourse.get(beforeArray.size()).getString("teachingDate")));
                            binding.toggle.clearChecked();
                            binding.toggle.check(R.id.today);
                            
                            JSONObject array = afterArray.isEmpty() ? tomorrowCourse.get(0) : todayCourse.get(beforeArray.size());
                            long delta = LocalDateTime.parse(String.format("%s %s", array.getString("teachingDate"), array.getString("startTime")), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - System.currentTimeMillis();
                            if (delta > 0)
                                WorkManager.getInstance(requireContext().getApplicationContext())
                                        .enqueueUniqueWork("next_class_notification_update",
                                                ExistingWorkPolicy.KEEP, new OneTimeWorkRequest.Builder(ClassNotificationWorker.class).setInputData(new Data.Builder().putString("courseName", array.getString("courseName")).putString("teachingPlace", array.getString("teachingPlace")).putString("time", array.getString("time")).build())
                                                        .setInitialDelay(delta < 1000 * 60 * 15 ? 0 : delta - 1000 * 60 * 15, TimeUnit.MILLISECONDS).build());
                        }
                        case 2 -> {
                            JSONArray dataArray = response.getJSONArray("data");
                            if (!dataArray.isEmpty()) {
                                for (int i = 0; i < dataArray.size(); i++) {
                                    LinkedList<JSONObject> exams = List.of(thisWeekExams, nextWeekExams).get(i);
                                    TreeMap<Integer, JSONArray> sortedTimetable = new TreeMap<>();
                                    dataArray.getJSONObject(i).getJSONObject("timetable").forEach((s1, t) -> {
                                        if (t != null)
                                            sortedTimetable.put(Integer.parseInt(s1), (JSONArray) t);
                                    });
                                    sortedTimetable.forEach((key, value) -> {
                                        if (key.equals(sortedTimetable.firstKey()))
                                            value.forEach(c -> exams.addFirst((JSONObject) c));
                                        else
                                            value.forEach(c -> exams.addLast((JSONObject) c));
                                    });
                                }
                                binding.toggle2.clearChecked();
                                binding.toggle2.check(R.id.week_18);
                            }
                        }
                        case 3 -> {
                            String term = response.getJSONObject("data").getString("acadYearSemester");
                            binding.date.setText(String.format("第%s学期\n%s\n%s", term, date, getResources().getStringArray(R.array.weeks)[LocalDate.now().getDayOfWeek().getValue() - 1]));
                            getTodayCourses(term);
                            getExams(term);
                            getWeek(term);
                            isRefreshRequired = false;
                        }
                        case 4 -> {
                            String week = response.getJSONArray("data").getJSONObject(0).getString("weekTimes");
                            binding.date.setText(String.format("第%s周\n%s", week, binding.date.getText().toString()));
                            binding.toggle2.check("19".equals(week) ? R.id.week_19 : R.id.week_18);
                        }
                    }
                }
            });
            PreferenceViewModel preferenceViewModel = new ViewModelProvider(requireActivity()).get(PreferenceViewModel.class);
            preferenceViewModel.setPM(androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireActivity()));
            preferenceViewModel.getIsAgreeLiveData().observe(getViewLifecycleOwner(), a -> {
                if (!a) getTerm();
            });
            preferenceViewModel.initLiveData();
            ConcatAdapter todoAdapter = new ConcatAdapter();
            binding.todoList.setLayoutManager(new LinearLayoutManager(requireActivity()));
            binding.todoList.setAdapter(todoAdapter);
            todoManager = new TodoManager(requireActivity(), todoAdapter);
            todoManager.setOnRefreshListener(this::refresh);
            binding.add.setOnClickListener(_ -> todoManager.showTodoAddDialog());
            binding.todoView.setOnClickListener(gotoActivity(TodoActivity.class));
            PopupMenu pop = new PopupMenu(requireActivity(), binding.todoDate, 0, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow);
            Menu menu = pop.getMenu();
            menu.add(0, Menu.NONE, 0, R.string.all).setChecked(true).setOnMenuItemClickListener(_ -> {
                todoDate.setValue("");
                return false;
            });
            menu.add(0, Menu.NONE, 0, R.string.today).setOnMenuItemClickListener(_ -> {
                todoDate.setValue(calendar.toDateStringAdd(0));
                return false;
            });
            menu.add(0, Menu.NONE, 0, R.string.tomorrow).setOnMenuItemClickListener(_ -> {
                todoDate.setValue(calendar.toDateStringAdd(1));
                return false;
            });
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            menu.add(1, Menu.NONE, 0, R.string.select).setOnMenuItemClickListener(_ -> {
                MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker();
                if (isEmpty(todoDate.getValue()))
                    builder.setSelection(System.currentTimeMillis());
                else {
                    try {
                        builder.setSelection(Objects.requireNonNull(dateFormat.parse(todoDate.getValue())).getTime() + 86400000);
                    } catch (ParseException _) {
                    }
                }
                MaterialDatePicker<Long> datePicker = builder.build();
                datePicker.show(requireActivity().getSupportFragmentManager(), "date_picker");
                datePicker.addOnPositiveButtonClickListener(aLong -> todoDate.setValue(dateFormat.format(aLong)));
                return false;
            });
            todoDate.observe(getViewLifecycleOwner(), _ -> refresh());
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) menu.setGroupDividerEnabled(true);
            binding.todoDate.setOnClickListener(_ -> pop.show());
            binding.toggle3.addOnButtonCheckedListener((_, checkedId, _) -> {
                if (checkedId == R.id.filter_todo) todoManager.performRefresh();
            });
            binding.toggle3.check(R.id.filter_todo);
            preferenceViewModel.getDashboardLiveData().observe(getViewLifecycleOwner(), s -> {
                HashSet<String> visible = new HashSet<>(List.of("0", "1", "2", "3", "4", "5"));
                if (!s.isEmpty()) visible.removeAll(s);
                visible.forEach(i -> List.of(binding.shortcutGroup, binding.nextClassCard, binding.timeCard, binding.courseGroup, binding.examGroup, binding.todoGroup).get(Integer.parseInt(i)).setVisibility(View.GONE));
            });
            initOrder(inflater);
            initAction(inflater);
            getShortcutCollection();
        }
        return binding.getRoot();
    }
    
    private View.OnClickListener gotoActivity(Class<?> cls) {
        return v -> startActivity(new Intent(getContext(), cls), ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), v, "miniapp").toBundle());
    }
    
    void refresh() {
        String value = todoDate.getValue();
        boolean isAll = isEmpty(value);
        if (isAll)
            todoManager.refresh("status = ?", new String[]{String.valueOf(binding.filterTodo.isChecked() ? TodoInfo.TODO : TodoInfo.DONE)});
        else todoManager.refresh("due_date = ? AND status = ?", new String[]{
                value, String.valueOf(binding.filterTodo.isChecked() ? TodoInfo.TODO : TodoInfo.DONE)
        });
        binding.todoDate.setText(isAll ? getString(R.string.all) : value);
    }
    
    void getTerm() {
        model.addAndNext("jwxt/base-info/acadyearterm/showNewAcadlist", 3);
    }
    
    void getWeek(String term) {
        model.addAndNext("jwxt/timetable-search/classTableInfo/getDateWeekly?academicYear=" + term, 4);
    }
    
    void getTodayCourses(String term) {
        model.addAndNext("jwxt/timetable-search/classTableInfo/queryTodayStudentClassTable?academicYear=" + term, 1);
    }
    
    void getExams(String term) {
        model.addAndNext("jwxt/examination-manage/classroomResource/queryStuEaxmInfo?code=jwxsd_ksxxck" + term, String.format("{\"acadYear\":\"%s\",\"examWeekId\":\"1928284621349085186\",\"examWeekName\":\"18-19周期末考\",\"examDate\":\"\"}", term), 2);
    }
    
    String getTimePosition(String from, String to) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return now.isBefore(LocalDateTime.parse(from, formatter)) ? "after" : now.isAfter(LocalDateTime.parse(to, formatter)) ? "before" : "in";
    }
    
    void getShortcutCollection() {
        if (binding.shortcutGroup.getChildCount() > 4)
            IntStream.range(3, binding.shortcutGroup.getChildCount() - 1).forEach(_ -> binding.shortcutGroup.removeViewAt(3));
        try (Cursor cursor = db.getWritableDatabase().query("dashboard_shortcut_collection", null, null, null, null, null, "position")) {
            if (cursor.moveToFirst()) {
                collectionAdapter.clear();
                do {
                    Integer id = cursor.getInt(cursor.getColumnIndexOrThrow("shortcutId"));
                    JSONObject shortcut = JSON.parseObject(cursor.getString(cursor.getColumnIndexOrThrow("shortcutJson")));
                    MaterialButton button = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonTonalStyle);
                    button.setText(shortcut.getString("name"));
                    binding.shortcutGroup.addView(button);
                    String url = shortcut.getString("url");
                    String activity = shortcut.getString("activity");
                    if (viewModel.actionMap.containsKey(id))
                        button.setOnClickListener(viewModel.actionMap.get(id));
                    button.setOnClickListener(viewModel.actionMap.containsKey(id) ? viewModel.actionMap.get(id) : TextUtils.isEmpty(activity) ? TextUtils.isEmpty(url) ? _ -> params.toast(R.string.undeveloped) : v -> startActivity(new Intent(requireContext(), BrowserActivity.class).setData(Uri.parse(url)), ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), v, "miniapp").toBundle()) : v -> {
                        try {
                            Intent intent = new Intent(requireContext(), Class.forName(requireContext().getPackageName() + activity));
                            if (intent.resolveActivity(requireContext().getPackageManager()) != null)
                                startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), v, "miniapp").toBundle());
                        } catch (ClassNotFoundException _) {
                            params.toast("未找到对应活动");
                        }
                    });
                    button.setOnLongClickListener(_ -> action(shortcut));
                    collectionAdapter.add(shortcut);
                } while (cursor.moveToNext());
            }
        }
    }
    
    void initOrder(@NonNull LayoutInflater inflater) {
        Context context = requireContext();
        orderDialog = new BottomSheetDialog(context);
        DialogServiceOrderBinding orderBinding = DialogServiceOrderBinding.inflate(inflater);
        orderBinding.recyclerView.setLayoutManager(new LinearLayoutManager(context));
        collectionAdapter = new ServiceFragment.CollectionAdapter();
        orderBinding.recyclerView.setAdapter(collectionAdapter);
        orderBinding.confirm.setOnClickListener(_ -> {
            updateShortcut();
            getShortcutCollection();
            orderDialog.dismiss();
        });
        orderDialog.setContentView(orderBinding.getRoot());
        new ItemTouchHelper(new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
            }
            
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder source, @NonNull RecyclerView.ViewHolder target) {
                collectionAdapter.swap(source.getBindingAdapterPosition(), target.getBindingAdapterPosition());
                return true;
            }
            
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }
        }).attachToRecyclerView(orderBinding.recyclerView);
    }
    
    void updateShortcut() {
        IntStream.range(0, collectionAdapter.getItemCount()).forEach(i -> {
            collectionAdapter.get(i);
            db.updateDashboardShortcutPosition(collectionAdapter.get(i).getInteger("id"), i);
        });
    }
    
    void initAction(@NonNull LayoutInflater inflater) {
        actionDialog = new BottomSheetDialog(requireContext());
        actionBinding = DialogServiceActionBinding.inflate(inflater);
        actionBinding.order.setOnClickListener(_ -> orderDialog.show());
        actionDialog.setContentView(actionBinding.getRoot());
    }
    
    boolean action(JSONObject item) {
        int itemId = item.getIntValue("id");
        MutableLiveData<Boolean> isServiceCollected = new MutableLiveData<>(db.isServiceCollected(itemId));
        MutableLiveData<Boolean> isShortcutCollected = new MutableLiveData<>(db.isDashboardShortcutCollected(itemId));
        actionBinding.collect.setText(Boolean.TRUE.equals(isServiceCollected.getValue()) ? R.string.cancel_collect : R.string.collect);
        actionBinding.addToDashboard.setText(Boolean.TRUE.equals(isShortcutCollected.getValue()) ? R.string.cancel_add_shortcut : R.string.add_to_dashboard);
        actionBinding.addToLauncher.setOnClickListener(_ -> {
            if (ShortcutManagerCompat.isRequestPinShortcutSupported(requireContext())) {
                Intent intent = new Intent(requireContext(), MainActivity.class);
                if (item.containsKey("activity")) {
                    try {
                        intent = new Intent(requireContext(), Class.forName(requireContext().getPackageName() + item.getString("activity")));
                    } catch (ClassNotFoundException _) {
                    }
                } else if (item.containsKey("url"))
                    intent = new Intent(requireContext(), BrowserActivity.class).setData(Uri.parse(trim(item.getString("url"))));
                ShortcutInfoCompat pinShortcutInfo = new ShortcutInfoCompat.Builder(requireContext(), String.valueOf(itemId))
                        .setShortLabel(item.getString("name"))
                        .setLongLabel(item.getString("name"))
                        .setIcon(IconCompat.createWithResource(requireContext(), R.mipmap.icon))
                        .setIntent(intent.setAction(Intent.ACTION_VIEW))
                        .build();
                ShortcutManagerCompat.requestPinShortcut(requireContext(), pinShortcutInfo, PendingIntent.getBroadcast(requireContext(), /* request code */ 0, ShortcutManagerCompat.createShortcutResultIntent(requireContext(), pinShortcutInfo), /* flags */ PendingIntent.FLAG_IMMUTABLE).getIntentSender());
            } else {
                params.toast(R.string.fail_to_add_shortcut);
            }
        });
        actionBinding.collect.setOnClickListener(_ -> {
            boolean isServiceCollect = Boolean.TRUE.equals(isServiceCollected.getValue());
            if (isServiceCollect) {
                db.deleteService(itemId);
                params.toast(R.string.cancel_collect_success);
            } else {
                db.addService(itemId, item.toJSONString(), collectionAdapter.getItemCount());
                params.toast(R.string.collect_success);
            }
            getShortcutCollection();
            actionBinding.collect.setText(isServiceCollect ? R.string.collect : R.string.cancel_collect);
            isServiceCollected.setValue(!isServiceCollect);
        });
        actionBinding.addToDashboard.setOnClickListener(_ -> {
            boolean isShortcutCollect = Boolean.TRUE.equals(isShortcutCollected.getValue());
            if (isShortcutCollect) {
                db.deleteDashboardShortcut(itemId);
                params.toast(R.string.cancel_add_shortcut_success);
            } else {
                db.addDashboardShortcut(itemId, item.toJSONString(), collectionAdapter.getItemCount());
                params.toast(R.string.add_shortcut_success);
            }
            viewModel.updateDashboardShortcut.setValue(true);
            actionBinding.addToDashboard.setText(isShortcutCollect ? R.string.add_to_dashboard : R.string.cancel_add_shortcut);
            isShortcutCollected.setValue(!isShortcutCollect);
        });
        actionBinding.feedback.setOnClickListener(_ -> startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(String.format("https://github.com/%s/%s/issues/new?title=反馈：服务->%s&labels=bug,crash-report", "SYSU-Tang", "Sysuer", item.getString("name")))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
        Markwon.create(requireContext()).setMarkdown(actionBinding.description, String.format("### %s\n%s", item.getString("name"), item.getString("description")));
        actionDialog.show();
        return true;
    }
}

class CourseAdapter extends RecyclerAdapter<JSONObject> {
    BiConsumer<? super JSONObject, ? super View> onClick;
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecyclerView.ViewHolder(ItemCourseBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false).getRoot()) {
        };
    }
    
    public void setOnClick(BiConsumer<? super JSONObject, ? super View> onClick) {
        this.onClick = onClick;
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ItemCourseBinding binding = ItemCourseBinding.bind(holder.itemView);
        JSONObject item = get(position);
        holder.itemView.setOnClickListener(v -> onClick.accept(item, v));
        Map.of(binding.courseTitle, "courseName", binding.location, "teachingPlace", binding.time, "time", binding.teacher, "teacherName", binding.course, "course").forEach((v, s) -> {
            v.setText(item.getString(s));
            v.setOnLongClickListener(_ -> {
                params.copy(s, item.getString(s));
                params.toast(R.string.copy_successfully);
                return true;
            });
        });
        TypedValue colorSurfaceDim = new TypedValue();
        TypedValue colorSurface = new TypedValue();
        Resources.Theme theme = holder.itemView.getContext().getTheme();
        theme.resolveAttribute(com.google.android.material.R.attr.colorSurfaceDim, colorSurfaceDim, true);
        theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, colorSurface, true);
        boolean isBefore = Objects.equals(item.getString("status"), "before");
        binding.courseTitle.setTextAppearance(isBefore ? com.google.android.material.R.style.TextAppearance_Material3_TitleMedium : com.google.android.material.R.style.TextAppearance_Material3_TitleMedium_Emphasized);
        holder.itemView.getBackground().setTint(Objects.equals(item.getString("status"), "in") ? colorSurfaceDim.data : isBefore ? 0x0 : colorSurface.data);
        binding.item.setAlpha(isBefore ? 0.64f : 1.0f);
        super.onBindViewHolder(holder, position);
    }
}

class ExamAdapter extends RecyclerAdapter<JSONObject> {
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecyclerView.ViewHolder(ItemExamBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false).getRoot()) {
        };
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ItemExamBinding binding = ItemExamBinding.bind(holder.itemView);
        Context context = holder.itemView.getContext();
        holder.itemView.setOnClickListener(_ -> {
        });
        JSONObject examData = get(position);
        int startClassTimes = examData.getIntValue("startClassTimes");
        int endClassTimes = examData.getIntValue("endClassTimes");
        String[] text = new String[]{examData.getString("examSubjectName"),
                examData.getString("classroomNumber"),
                examData.getString("examDate"),
                String.format("%s%s", examData.getString("duration"), context.getString(R.string.minute)),
                examData.getString("durationTime"),
                String.format(context.getString(R.string.section_range), startClassTimes, endClassTimes),
                String.format("%s：%s", context.getString(R.string.exam_mode), examData.getString("examMode")),
                String.format("%s：%s", context.getString(R.string.exam_stage), examData.getString("examStage"))};
        TextView[] materialTextButtons = {binding.examName, binding.examLocation, binding.examDate, binding.examDuration, binding.examTime, binding.examClassTime, binding.examMode, binding.examStage};
        for (int i = 0; i < 8; i++) {
            materialTextButtons[i].setText(text[i]);
            int finalI = i;
            materialTextButtons[i].setOnClickListener(_ -> {
                params.copy("exam", text[finalI]);
                params.toast(R.string.copy_successfully);
            });
        }
        super.onBindViewHolder(holder, position);
    }
}