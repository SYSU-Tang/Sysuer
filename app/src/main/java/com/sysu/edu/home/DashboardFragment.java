package com.sysu.edu.home;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Html;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.MainActivity;
import com.sysu.edu.R;
import com.sysu.edu.academic.AgendaActivity;
import com.sysu.edu.academic.CourseDetail;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.FragmentDashboardBinding;
import com.sysu.edu.databinding.ItemCourseBinding;
import com.sysu.edu.databinding.ItemExamBinding;
import com.sysu.edu.extra.LaunchMiniProgram;
import com.sysu.edu.extra.LoginActivity;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.BiConsumer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class DashboardFragment extends Fragment {

    Handler handler;
    String cookie;
    ArrayList<JSONObject> todayCourse = new ArrayList<>();
    ArrayList<JSONObject> tomorrowCourse = new ArrayList<>();
    LinkedList<JSONObject> thisWeekExams = new LinkedList<>();
    LinkedList<JSONObject> nextWeekExams = new LinkedList<>();
    Params params;
    FragmentDashboardBinding binding;
    OkHttpClient http = new OkHttpClient.Builder().build();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (binding == null) {
            binding = FragmentDashboardBinding.inflate(inflater);
            ActivityResultLauncher<Intent> launch = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
                if (o.getResultCode() == Activity.RESULT_OK) {
                    cookie = params.getCookie();
                    getTerm();
                }
            });
            binding.scan.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName("com.tencent.mm", "com.tencent.mm.ui.LauncherUI"));
                    intent.putExtra("LauncherUI.From.Scaner.Shortcut", true);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setAction("android.intent.action.VIEW");
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    //e.printStackTrace();
                }
            });
            binding.qrcode.setOnClickListener(v -> {
                String linking = PreferenceManager.getDefaultSharedPreferences(requireContext()).getString("qrcode", "");
                if (linking.isEmpty()) {
                    new LaunchMiniProgram(requireActivity()).launchMiniProgram("gh_85575b9f544e");
                } else {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(linking)));
                    } catch (ActivityNotFoundException e) {
                        // Toast.makeText(requireContext(), R.string.no_app, Toast.LENGTH_LONG).show();
                    }
                }
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
                    handler.postDelayed(this, 1000);
                }
            });
            params = new Params(requireActivity());
            cookie = params.getCookie();

            CourseAdp courseAdp = new CourseAdp(requireActivity());
            binding.courseList.setAdapter(courseAdp);
            ExamAdp examAdp = new ExamAdp(requireActivity());
            binding.examList.setAdapter(examAdp);
            binding.toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (group.getCheckedButtonId() == checkedId) {
                    courseAdp.set(checkedId == R.id.today ? todayCourse : tomorrowCourse);
                    binding.noClass.setVisibility(courseAdp.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                }
            });
            binding.toggle2.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (group.getCheckedButtonId() == checkedId) {
                    examAdp.set(checkedId == R.id.this_week ? thisWeekExams : nextWeekExams);
                    binding.noExam.setVisibility(examAdp.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                }
            });
            binding.date.setText(String.format("%s/星期%s", new SimpleDateFormat("M月dd日", Locale.CHINESE).format(new Date()), new String[]{"日", "一", "二", "三", "四", "五", "六"}[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1]));

            handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    JSONObject response = JSON.parseObject((String) msg.obj);
                    if (response.get("code").equals(200)) {
                        switch (msg.what) {
                            case 1:
                                ArrayList<JSONObject> beforeArray = new ArrayList<>();
                                ArrayList<JSONObject> afterArray = new ArrayList<>();
                                response.getJSONArray("data").forEach(e -> {
                                    String status = getTimePosition(((JSONObject) e).getString("teachingDate") + " " + ((JSONObject) e).getString("startTime"), ((JSONObject) e).getString("teachingDate") + " " + ((JSONObject) e).getString("endTime"));
                                    ((JSONObject) e).put("status", status);
                                    ((JSONObject) e).put("time", ((JSONObject) e).get("startTime") + "~" + ((JSONObject) e).get("endTime"));
                                    ((JSONObject) e).put("course", "第" + ((JSONObject) e).get("startClassTimes") + "~" + ((JSONObject) e).get("endClassTimes") + "节课");
                                    String flag = (String) ((JSONObject) e).get("useflag");
                                    if (flag.equals("TD")) {
                                        (Objects.equals(status, "before") ? beforeArray : afterArray).add((JSONObject) e);
                                    }
                                    (flag.equals("TD") ? todayCourse : tomorrowCourse).add((JSONObject) e);
//                                    addCourse(flag.equals("TD") ? todayCourse : tomorrowCourse, (String) ((JSONObject) e).get("courseName"), (String) ((JSONObject) e).get("teachingPlace"), ((JSONObject) e).get("startTime") + "~" + ((JSONObject) e).get("endTime")
//                                            , "第" + ((JSONObject) e).get("startClassTimes") + "~" + ((JSONObject) e).get("endClassTimes") + "节课", (String) ((JSONObject) e).get("teacherName"), flag);
                                });
                                binding.progress.setMax(todayCourse.size());
                                binding.progress.setProgress(beforeArray.size());
                                binding.courseList.scrollToPosition(beforeArray.size());
                                binding.nextClass.setText(Html.fromHtml(afterArray.isEmpty() ? String.format("<h4><font color=\"#6750a4\">今天没课</font></h4>下一节：<b>%s</b><br/>地点：<b>自习室</b><br/>时间：<b>自主安排</b>", tomorrowCourse.get(0).getString("courseName")) : String.format("<h4><font color=\"#6750a4\">%s</font></h4>地点：<b>%s</b><br/>时间：<b>%s</b><br/>日期：<b>%s</b>", todayCourse.get(beforeArray.size()).getString("courseName"), todayCourse.get(beforeArray.size()).getString("teachingPlace"), todayCourse.get(beforeArray.size()).getString("time"), todayCourse.get(beforeArray.size()).getString("teachingDate")), Html.FROM_HTML_MODE_COMPACT));
                                binding.toggle.check(R.id.today);
                                break;

                            case 2:
                                if (response.getJSONArray("data").isEmpty()) {
                                    break;
                                }
                                JSONArray dataArray = response.getJSONArray("data");
                                for (int i = 0; i < dataArray.size(); i++) {
                                    JSONObject e = dataArray.getJSONObject(i);
                                    LinkedList<JSONObject> exams = List.of(thisWeekExams, nextWeekExams).get(i);
                                    JSONObject timetable = e.getJSONObject("timetable");
                                    TreeMap<Integer, JSONArray> sortedTimetable = new TreeMap<>();
                                    timetable.forEach((s,t) -> {
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
                                binding.toggle2.check(R.id.this_week);
                                break;
                            case 3:
                                String term = response.getJSONObject("data").getString("acadYearSemester");
                                binding.date.setText(String.format("第%s学期\n%s\n星期%s", term, new SimpleDateFormat("M月dd日", Locale.CHINESE).format(new Date()), new String[]{"日", "一", "二", "三", "四", "五", "六"}[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1]));
                                getTodayCourses(term);
                                getExams(term);
                                break;
                            case -1: {
                                Toast.makeText(requireActivity(), getString(R.string.no_wifi_warning), Toast.LENGTH_LONG).show();
                                break;
                            }
                        }
                    } else {
                        launch.launch(new Intent(getContext(), LoginActivity.class).putExtra("url", TargetUrl.JWXT));
                    }
                }
            };
            getTerm();
        }
        return binding.getRoot();
    }

    void getTerm() {
        http.newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/base-info/acadyearterm/showNewAcadlist")
                .header("Cookie", cookie)
                //.header("Accept-Language", Language.getLanguageCode(requireContext()))
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

    public void getTodayCourses(String term) {
        new OkHttpClient.Builder().build().newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/timetable-search/classTableInfo/queryTodayStudentClassTable?academicYear=" + term)
                .header("Cookie", cookie)
                //.header("Accept-Language", Language.getLanguageCode(requireContext()))
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

    public String getTimePosition(String from, String to) {
        Date now = new Date();
        try {
            Date fromDate = new SimpleDateFormat("yy-MM-dd hh:mm", Locale.getDefault()).parse(from);
            Date toDate = new SimpleDateFormat("yy-MM-dd hh:mm", Locale.getDefault()).parse(to);
            return now.before(fromDate) ? "after" : now.after(toDate) ? "before" : "in";
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public void getExams(String term) {
        new OkHttpClient.Builder().build().newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/examination-manage/classroomResource/queryStuEaxmInfo?code=jwxsd_ksxxck")
                .header("Cookie", cookie)
                //.header("Accept-Language", Language.getLanguageCode(requireContext()))
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

class CourseAdp extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    Context context;
    ArrayList<JSONObject> data = new ArrayList<>();

    public CourseAdp(Context context) {
        super();
        this.context = context;
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

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        BiConsumer<Integer, String> a = (id, s) -> ((TextView) holder.itemView.findViewById(id)).setText(data.get(position).getString(s));
        holder.itemView.setOnClickListener(v -> ((MainActivity) context).launch().launch(new Intent(context, CourseDetail.class).putExtra("code", data.get(position).getString("courseNum")).putExtra("class", data.get(position).getString("classesNum")), ActivityOptionsCompat.makeSceneTransitionAnimation((Activity) context, holder.itemView, "miniapp")));
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
        ((TextView) holder.itemView.findViewById(R.id.course_title)).setTextAppearance(isBefore ? com.google.android.material.R.style.TextAppearance_Material3_BodyMedium : com.google.android.material.R.style.TextAppearance_Material3_TitleMedium_Emphasized);
        ((GradientDrawable) ((RippleDrawable) holder.itemView.getBackground()).getDrawable(1)).setColor(Objects.equals(data.get(position).getString("status"), "in") ? colorSurfaceDim.data : isBefore ? 0x0 : colorSurface.data);
        holder.itemView.findViewById(R.id.item).setAlpha(isBefore ? 0.64f : 1.0f);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}

class ExamAdp extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    Context context;
    LinkedList<JSONObject> data = new LinkedList<>();

    public ExamAdp(Context context) {
        super();
        this.context = context;
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

        binding.examName.setText(examData.getString("examSubjectName"));
        binding.examLocation.setText(examData.getString("classroomNumber"));
        binding.examDate.setText(examData.getString("examDate"));
        binding.examDuration.setText(String.format("%s%s", examData.getString("duration"), context.getString(R.string.minute)));
        binding.examTime.setText(examData.getString("durationTime"));
        binding.examClassTime.setText(String.format(context.getString(R.string.section_range), startClassTimes, endClassTimes));
        binding.examMode.setText(String.format("%s：%s", context.getString(R.string.exam_mode), examData.getString("examMode")));
        binding.examStage.setText(String.format("%s：%s", context.getString(R.string.exam_stage), examData.getString("examStage")));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}