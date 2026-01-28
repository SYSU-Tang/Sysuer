package com.sysu.edu.home;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.academic.AgendaActivity;
import com.sysu.edu.academic.CourseDetailActivity;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.SysuerPreferenceManager;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.FragmentDashboardBinding;
import com.sysu.edu.databinding.ItemCourseBinding;
import com.sysu.edu.databinding.ItemExamBinding;
import com.sysu.edu.todo.InitTodo;
import com.sysu.edu.todo.TodoFragment;
import com.sysu.edu.todo.info.TodoInfo;

import org.commonmark.node.Heading;
import org.commonmark.node.Node;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.BiConsumer;

import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.Markwon;
import io.noties.markwon.MarkwonSpansFactory;
import io.noties.markwon.MarkwonVisitor;
import io.noties.markwon.core.CoreProps;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class DashboardFragment extends Fragment {

    final ArrayList<JSONObject> todayCourse = new ArrayList<>();
    final ArrayList<JSONObject> tomorrowCourse = new ArrayList<>();
    final LinkedList<JSONObject> thisWeekExams = new LinkedList<>();
    final LinkedList<JSONObject> nextWeekExams = new LinkedList<>();
    final OkHttpClient http = new OkHttpClient.Builder().build();
    Handler handler;
    String cookie;
    Params params;
    FragmentDashboardBinding binding;
    boolean refresh = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (refresh) {
            binding = FragmentDashboardBinding.inflate(inflater, container, false);
            binding.scan.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName("com.tencent.mm", "com.tencent.mm.ui.LauncherUI"));
                    intent.putExtra("LauncherUI.From.Scaner.Shortcut", true);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setAction("android.intent.action.VIEW");
                    startActivity(intent);
                } catch (ActivityNotFoundException ignored) {
                }
            });
            binding.qrcode.setOnClickListener(v -> {
                String linking = PreferenceManager.getDefaultSharedPreferences(requireContext()).getString("qrcode", "");
                if (!linking.isEmpty()) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(linking)));
                    } catch (ActivityNotFoundException e) {
                        // Toast.makeText(requireContext(), R.string.no_app, Toast.LENGTH_LONG).show();
                    }
                } /*else {
                    //new LaunchMiniProgram(requireActivity()).launchMiniProgram("gh_85575b9f544e");
                }*/
            });
            binding.agenda.setOnClickListener(view -> startActivity(new Intent(getContext(), AgendaActivity.class), ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), view, "miniapp").toBundle()));
            binding.courseList.addItemDecoration(new DividerItemDecoration(requireContext(), 0));
            binding.courseList.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
            binding.examList.addItemDecoration(new DividerItemDecoration(requireContext(), 0));
            binding.examList.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    binding.time.setText(new SimpleDateFormat("hh:mm:ss", Locale.CHINESE).format(new Date()));
                    handler.postDelayed(this, 500);
                }
            });
            params = new Params(requireActivity());
            cookie = params.getCookie();
            params.setCallback(this, () -> {
                cookie = params.getCookie();
                getTerm();
            });
            CourseAdapter courseAdapter = new CourseAdapter(requireActivity());
            courseAdapter.setOnClick((jsonObject, view) -> startActivity(new Intent(getContext(), CourseDetailActivity.class).putExtra("code", jsonObject.getString("courseNum")).putExtra("class", jsonObject.getString("classesNum")), ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), view, "miniapp").toBundle()));
            binding.courseList.setAdapter(courseAdapter);
            ExamAdapter examAdapter = new ExamAdapter(requireActivity());
            binding.examList.setAdapter(examAdapter);
            binding.toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (R.id.today == checkedId) {
                    courseAdapter.set(isChecked ? todayCourse : tomorrowCourse);
                    binding.noClass.setVisibility(courseAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                }
            });
            binding.toggle2.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (R.id.week_18 == checkedId) {
                    examAdapter.set(isChecked ? thisWeekExams : nextWeekExams);
                    binding.noExam.setVisibility(examAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                }
            });
            binding.date.setText(String.format("%s/星期%s", new SimpleDateFormat("M月dd日", Locale.CHINESE).format(new Date()), new String[]{"日", "一", "二", "三", "四", "五", "六"}[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1]));

            handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    if (msg.what == -1) {
                        params.toast(R.string.no_wifi_warning);
                        binding.nextClass.setText(R.string.no_wifi_warning);
                        return;
                    }
                    JSONObject response = JSON.parseObject((String) msg.obj);
                    if (response.get("code").equals(200)) {
                        switch (msg.what) {
                            case 1:
                                ArrayList<JSONObject> beforeArray = new ArrayList<>();
                                ArrayList<JSONObject> afterArray = new ArrayList<>();
                                response.getJSONArray("data").forEach(e -> {
                                    JSONObject jsonObject = (JSONObject) e;
                                    String status = getTimePosition(jsonObject.getString("teachingDate") + " " + jsonObject.getString("startTime"), jsonObject.getString("teachingDate") + " " + jsonObject.getString("endTime"));
                                    jsonObject.put("status", status);
                                    jsonObject.put("time", jsonObject.get("startTime") + "~" + jsonObject.get("endTime"));
                                    jsonObject.put("course", "第" + jsonObject.get("startClassTimes") + "~" + jsonObject.get("endClassTimes") + "节课");
                                    String flag = (String) jsonObject.get("useflag");
                                    if (flag.equals("TD")) {
                                        (Objects.equals(status, "before") ? beforeArray : afterArray).add(jsonObject);
                                    }
                                    (flag.equals("TD") ? todayCourse : tomorrowCourse).add(jsonObject);
//                                    addCourse(flag.equals("TD") ? todayCourse : tomorrowCourse, (String) ((JSONObject) e).get("courseName"), (String) ((JSONObject) e).get("teachingPlace"), ((JSONObject) e).get("startTime") + "~" + ((JSONObject) e).get("endTime")
//                                            , "第" + ((JSONObject) e).get("startClassTimes") + "~" + ((JSONObject) e).get("endClassTimes") + "节课", (String) ((JSONObject) e).get("teacherName"), flag);
                                });
                                binding.progress.setMax(todayCourse.size());
                                binding.progress.setProgress(beforeArray.size());
                                binding.courseList.scrollToPosition(beforeArray.size());
                                Markwon.builder(requireContext()).usePlugin(new AbstractMarkwonPlugin() {
                                    @Override
                                    public void configureSpansFactory(@NonNull MarkwonSpansFactory.Builder builder) {
                                        super.configureSpansFactory(builder);
                                        builder.appendFactory(Heading.class, (heading, configuration) -> {
                                            if (CoreProps.HEADING_LEVEL.require(configuration) == 3) {
                                                return new ForegroundColorSpan(Color.parseColor("#6750a4"));
                                            }
                                            return null;
                                        });
                                    }

                                    @Override
                                    public void configureVisitor(@NonNull MarkwonVisitor.Builder builder) {
                                        super.configureVisitor(builder);
                                        builder.blockHandler(new MarkwonVisitor.BlockHandler() {
                                            @Override
                                            public void blockStart(@NonNull MarkwonVisitor visitor, @NonNull Node node) {

                                            }

                                            @Override
                                            public void blockEnd(@NonNull MarkwonVisitor visitor, @NonNull Node node) {
                                                if (visitor.hasNext(node)) {
                                                    visitor.ensureNewLine();
                                                }
                                            }
                                        });
                                    }
                                }).build().setMarkdown(binding.nextClass, afterArray.isEmpty() ? String.format("### %s\n\n%s：**%s**\n\n%s：**%s**\n\n%s：**%s**",
                                        getString(R.string.noClass),
                                        getString(R.string.next_class),
                                        tomorrowCourse.isEmpty() ? getString(R.string.none) : tomorrowCourse.get(0).getString("courseName"),
                                        getString(R.string.location), tomorrowCourse.isEmpty() ? getString(R.string.none) : tomorrowCourse.get(0).getString("teachingPlace"),
                                        getString(R.string.time), tomorrowCourse.isEmpty() ? getString(R.string.none) : tomorrowCourse.get(0).getString("time")) :
                                        String.format("## %s\n\n%s：**%s**\n\n%s：**%s**\n\n%s：**%s**",
                                                todayCourse.get(beforeArray.size()).getString("courseName"),
                                                getString(R.string.location),
                                                todayCourse.get(beforeArray.size()).getString("teachingPlace"),
                                                getString(R.string.time),
                                                todayCourse.get(beforeArray.size()).getString("time"),
                                                getString(R.string.date),
                                                todayCourse.get(beforeArray.size()).getString("teachingDate")));
                                binding.toggle.clearChecked();
                                binding.toggle.check(R.id.today);
                                break;
                            case 2:
                                JSONArray dataArray = response.getJSONArray("data");
                                if (dataArray.isEmpty()) {
                                    break;
                                }
                                for (int i = 0; i < dataArray.size(); i++) {
                                    JSONObject e = dataArray.getJSONObject(i);
                                    LinkedList<JSONObject> exams = List.of(thisWeekExams, nextWeekExams).get(i);
                                    JSONObject timetable = e.getJSONObject("timetable");
                                    TreeMap<Integer, JSONArray> sortedTimetable = new TreeMap<>();
                                    timetable.forEach((s, t) -> {
                                        if (t != null) {
                                            sortedTimetable.put(Integer.parseInt(s), (JSONArray) t);
                                        }
                                    });
                                    sortedTimetable.forEach((key, value) -> {
                                        if (key.equals(sortedTimetable.firstKey())) {
                                            value.forEach(c -> exams.addFirst((JSONObject) c));
                                        } else {
                                            value.forEach(c -> exams.addLast((JSONObject) c));
                                        }
                                    });
                                }
                                binding.toggle2.clearChecked();
                                binding.toggle2.check(R.id.week_18);
                                break;
                            case 3:
                                String term = response.getJSONObject("data").getString("acadYearSemester");
                                binding.date.setText(String.format("第%s学期\n%s\n星期%s", term, new SimpleDateFormat("M月dd日", Locale.CHINESE).format(new Date()), new String[]{"日", "一", "二", "三", "四", "五", "六"}[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1]));
                                getTodayCourses(term);
                                getExams(term);
                                getWeek(term);
                                refresh = false;
                                break;
                            case 4:
                                String week = response.getJSONArray("data").getJSONObject(0).getString("weekTimes");
                                binding.date.setText(String.format("第%s周\n%s", week, binding.date.getText().toString()));
                                binding.toggle2.check(week.equals("19") ? R.id.week_19 : R.id.week_18);
                                break;
                        }
                    } else {
                        params.gotoLogin(getView(), TargetUrl.JWXT);
                    }
                }
            };
            SysuerPreferenceManager spm = new ViewModelProvider(requireActivity()).get(SysuerPreferenceManager.class);
            spm.setPM(PreferenceManager.getDefaultSharedPreferences(requireActivity()));
            spm.getIsAgreeLiveData().observe(getViewLifecycleOwner(), aBoolean -> {
                if (!aBoolean) {
                    getTerm();
                }
            });
            spm.initLiveData();

            TodoFragment todoFragment = new TodoFragment();
            getParentFragmentManager().beginTransaction().add(R.id.todo_list, todoFragment).commit();
            InitTodo initTodo = new InitTodo(requireActivity(), todoFragment);
            initTodo.filterStatus(todoFragment, TodoInfo.DONE);
            binding.noTodo.setVisibility(initTodo.getCount() != 0 ? View.GONE : View.VISIBLE);
            binding.noTodo.setOnClickListener(v -> initTodo.showTodoAddDialog());
            binding.toggle3.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (checkedId == R.id.filter_todo) {
                    initTodo.filterStatus(todoFragment, isChecked ? TodoInfo.TODO : TodoInfo.DONE);
                    binding.noTodo.setVisibility(initTodo.getCount() != 0 ? View.GONE : View.VISIBLE);
                }
            });
            binding.toggle3.check(R.id.filter_todo);
            spm.getDashboardLiveData().observe(getViewLifecycleOwner(), s -> {
                HashSet<String> visible = new HashSet<>(List.of("0", "1", "2", "3", "4", "5"));
                if (!s.isEmpty()) {
                    visible.removeAll(s);
                }
                visible.forEach(i -> List.of(binding.shortcutGroup, binding.nextClassCard, binding.timeCard, binding.courseGroup, binding.examGroup, binding.todoGroup).get(Integer.parseInt(i)).setVisibility(View.GONE));
            });

        }
        return binding.getRoot();
    }

    void getTerm() {
        http.newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/base-info/acadyearterm/showNewAcadlist")
                .header("Cookie", cookie)
                .header("Referer", "https://jwxt.sysu.edu.cn/jwxt//yd/classSchedule/").build()
        ).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = new Message();
                msg.what = -1;
                handler.sendMessage(msg);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what = 3;
                msg.obj = response.body().string();
                handler.sendMessage(msg);
            }
        });
    }

    void getWeek(String term) {
        http.newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/timetable-search/classTableInfo/getDateWeekly?academicYear=" + term)
                .header("Cookie", cookie)
                .header("Referer", "https://jwxt.sysu.edu.cn/jwxt/yd/index/").build()
        ).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = new Message();
                msg.what = -1;
                handler.sendMessage(msg);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what = 4;
                msg.obj = response.body().string();
                handler.sendMessage(msg);
            }
        });
    }

    void getTodayCourses(String term) {
        new OkHttpClient.Builder().build().newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/timetable-search/classTableInfo/queryTodayStudentClassTable?academicYear=" + term)
                .header("Cookie", cookie)
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message message = new Message();
                message.what = -1;
                handler.sendMessage(message);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what = 1;
                msg.obj = response.body().string();
                handler.sendMessage(msg);
            }
        });
    }

    String getTimePosition(String from, String to) {
        Date now = new Date();
        try {
            Date fromDate = new SimpleDateFormat("yy-MM-dd hh:mm", Locale.getDefault()).parse(from);
            Date toDate = new SimpleDateFormat("yy-MM-dd hh:mm", Locale.getDefault()).parse(to);
            return now.before(fromDate) ? "after" : now.after(toDate) ? "before" : "in";
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    void getExams(String term) {
        new OkHttpClient.Builder().build().newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/examination-manage/classroomResource/queryStuEaxmInfo?code=jwxsd_ksxxck")
                .header("Cookie", cookie)
                .header("Referer", "https://jwxt.sysu.edu.cn/jwxt/mk/")
                .post(RequestBody.create(String.format("{\"acadYear\":\"%s\",\"examWeekId\":\"1928284621349085186\",\"examWeekName\":\"18-19周期末考\",\"examDate\":\"\"}", term), MediaType.parse("application/json")))
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message message = new Message();
                message.what = -1;
                handler.sendMessage(message);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what = 2;
                msg.obj = response.body().string();
                handler.sendMessage(msg);
            }
        });
    }
}

class CourseAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    final Params params;
    final Context context;
    final ArrayList<JSONObject> data = new ArrayList<>();
    BiConsumer<JSONObject, View> onClick;

    public CourseAdapter(Context context) {
        super();
        this.context = context;
        this.params = new Params((FragmentActivity) context);

    }

    public void set(ArrayList<JSONObject> d) {
        clear();
        data.addAll(d);
        notifyItemRangeInserted(0, getItemCount());
    }

    public void clear() {
        int temp = getItemCount();
        data.clear();
        notifyItemRangeRemoved(0, temp);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecyclerView.ViewHolder(ItemCourseBinding.inflate(LayoutInflater.from(context)).getRoot()) {
        };
    }

    public void setOnClick(BiConsumer<JSONObject, View> onClick) {
        this.onClick = onClick;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ItemCourseBinding binding = ItemCourseBinding.bind(holder.itemView);
        BiConsumer<Integer, String> a = (id, s) -> {
            ((TextView) holder.itemView.findViewById(id)).setText(data.get(position).getString(s));
            holder.itemView.findViewById(id).setOnLongClickListener(v -> {
                params.copy(s + "：", data.get(position).getString(s));
                params.toast(R.string.copy_successfully);
                return true;
            });
        };
        holder.itemView.setOnClickListener(v -> onClick.accept(data.get(position), v));
        a.accept(R.id.course_title, "courseName");
        a.accept(R.id.location_container, "teachingPlace");
        a.accept(R.id.time_container, "time");
        a.accept(R.id.teacher, "teacherName");
        a.accept(R.id.course, "course");
        TypedValue colorSurfaceDim = new TypedValue();
        TypedValue colorSurface = new TypedValue();
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurfaceDim, colorSurfaceDim, true);
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurface, colorSurface, true);
        boolean isBefore = Objects.equals(data.get(position).getString("status"), "before");
        binding.courseTitle.setTextAppearance(isBefore ? com.google.android.material.R.style.TextAppearance_Material3_TitleMedium : com.google.android.material.R.style.TextAppearance_Material3_TitleMedium_Emphasized);
        holder.itemView.getBackground().setTint(Objects.equals(data.get(position).getString("status"), "in") ? colorSurfaceDim.data : isBefore ? 0x0 : colorSurface.data);
        //((GradientDrawable) ((RippleDrawable) holder.itemView.getBackground()).getDrawable(1)).setColor(Objects.equals(data.get(position).getString("status"), "in") ? colorSurfaceDim.data : isBefore ? 0x0 : colorSurface.data);
        binding.item.setAlpha(isBefore ? 0.64f : 1.0f);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}

class ExamAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    final Params params;
    final Context context;
    final LinkedList<JSONObject> data = new LinkedList<>();

    public ExamAdapter(Context context) {
        super();
        this.context = context;
        this.params = new Params((FragmentActivity) context);
    }

    public void set(LinkedList<JSONObject> examData) {
        clear();
        data.addAll(examData);
        notifyItemRangeInserted(0, getItemCount());
    }

    public void clear() {
        int temp = getItemCount();
        data.clear();
        notifyItemRangeRemoved(0, temp);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecyclerView.ViewHolder(ItemExamBinding.inflate(LayoutInflater.from(context)).getRoot()) {
        };
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ItemExamBinding binding = ItemExamBinding.bind(holder.itemView);
        holder.itemView.setOnClickListener(v -> {
        });
        JSONObject examData = data.get(position);
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
        TextView[] materialTextButtons = {
                binding.examName,
                binding.examLocation,
                binding.examDate,
                binding.examDuration,
                binding.examTime,
                binding.examClassTime,
                binding.examMode,
                binding.examStage,
        };
        for (int i = 0; i < 8; i++) {
            materialTextButtons[i].setText(text[i]);
            int finalI = i;
            materialTextButtons[i].setOnClickListener(v -> {
                params.copy(finalI + "：", text[finalI]);
                params.toast(R.string.copy_successfully);
            });
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}