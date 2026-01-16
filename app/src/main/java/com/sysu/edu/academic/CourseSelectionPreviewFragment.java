package com.sysu.edu.academic;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.api.CourseSelectionViewModel;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.FragmentCourseSelectionPreviewBinding;
import com.sysu.edu.databinding.ItemEvaluationBinding;

import org.commonmark.node.Node;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.Markwon;
import io.noties.markwon.MarkwonVisitor;
import io.noties.markwon.ext.tables.TablePlugin;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CourseSelectionPreviewFragment extends Fragment {

    Handler handler;
    OkHttpClient http = new OkHttpClient();
    Params params;
    CourseSelectionViewModel vm;
    FragmentCourseSelectionPreviewBinding binding;

    int page = 1;
    MutableLiveData<Integer> type = new MutableLiveData<>();
    Integer total;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (binding == null) {

            binding = FragmentCourseSelectionPreviewBinding.inflate(inflater, container, false);
            params = new Params(requireActivity());
            vm = new ViewModelProvider(requireActivity()).get(CourseSelectionViewModel.class);
            binding.list.recyclerView.setLayoutManager(new StaggeredGridLayoutManager(params.getColumn(), StaggeredGridLayoutManager.VERTICAL));
            CourseSelectionPreviewAdapter previewAdapter = new CourseSelectionPreviewAdapter();
            binding.list.recyclerView.setAdapter(previewAdapter);
            binding.list.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    if (!recyclerView.canScrollVertically(1) && dy > 0 && total / 10.0 > page - 1) {
                        getList(type.getValue() == null ? 1 : type.getValue());
                    }
                }
            });
            binding.type.addOnButtonCheckedListener((group, checkedId, isChecked) -> type.postValue(binding.major.isChecked() ? 1 : binding.publicSelection.isChecked() ? 4 : 2));
            type.observe(getViewLifecycleOwner(), v -> {
                page = 1;
                previewAdapter.clear();
                getList(v);
            });
            handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    super.handleMessage(msg);
                    if (msg.what == -1) {
                        params.toast(R.string.no_wifi_warning);
                    } else {
                        JSONObject response = JSONObject.parse((String) msg.obj);
                        if (response.getInteger("code") == 200) {
                            total = response.getJSONObject("data").getInteger("total");
                            response.getJSONObject("data").getJSONArray("rows").forEach(e -> previewAdapter.add((JSONObject) e));
                        }
                    }
                }
            };
            vm.filterValue.observe(requireActivity(), filter -> {
                page = 1;
                previewAdapter.clear();
                getList(type.getValue() == null ? 1 : type.getValue());
            });
        }
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.addFilter.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.filter_fragment, null, new NavOptions.Builder()
                .setEnterAnim(android.R.animator.fade_in)
                .setExitAnim(android.R.animator.fade_out)
                .build(), new FragmentNavigator.Extras(Map.of(v, "miniapp"))));
    }

    void sendRequest(String url, String data, int what) {
        http.newCall(new Request.Builder().url(url)
                .header("Cookie", params.getCookie())
                .post(RequestBody.create(data, MediaType.parse("application/json")))
                .header("Referer", "https://jwxt.sysu.edu.cn/jwxt/mk/courseSelection/?code=jwxsd_xk&resourceName=%E9%80%89%E8%AF%BE")
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = new Message();
                msg.what = -1;
                handler.sendMessage(msg);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what = what;
                msg.obj = response.body().string();
                handler.sendMessage(msg);
            }
        });
    }

    void getList(int type) {
        sendRequest("https://jwxt.sysu.edu.cn/jwxt/choose-course-front-server/schoolCourse/pageList",
                String.format("{\"pageNo\":%s,\"pageSize\":10,\"total\":false,\"param\":{\"hiddenSelectedStatus\":\"0\",\"type\":\"%s\"%s}}",
                        page++, type, vm.getReturnData()),
                0);
    }

    static class CourseSelectionPreviewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        ArrayList<JSONObject> data = new ArrayList<>();

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemEvaluationBinding binding = ItemEvaluationBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new RecyclerView.ViewHolder(binding.getRoot()) {
            };
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ItemEvaluationBinding binding = ItemEvaluationBinding.bind(holder.itemView);
            binding.title.setText(data.get(position).getString("courseName"));
            String[] key = new String[]{"courseName", "courseCategoryName", "courseUnitName", "scheduleExamTime", "examFormName", "credit", "teachingClassId", "teachingClassNum", "teachingClassName", "courseNum"};
            String[] name = new String[]{"课程名称", "课程类别", "开设学院", "考试时间", "考核方式", "学分", "班级ID", "班级号", "班级名", "课程号"};
            StringBuilder md = new StringBuilder("| 老师 | 时间 | 地点 |\n|:-----:|:----:|:----:|\n|" + data.get(position).getString("teachingTimePlace").replace(";", " | ").replace(",", " |\n| ") + "|\n");
            for (int i = 0; i < key.length; i++) {
                md.append(String.format("\n%s：**%s**\n", name[i], data.get(position).getString(key[i]) == null ? "无" : data.get(position).getString(key[i])));
            }
            View.OnClickListener action = view -> view.getContext().startActivity(new Intent(view.getContext(), CourseDetail.class).putExtra("id", data.get(position).getString("teachingClassId")).putExtra("code", data.get(position).getString("courseNum")).putExtra("class", data.get(position).getString("teachingClassNum")),
                    ActivityOptionsCompat.makeSceneTransitionAnimation((Activity) binding.getRoot().getContext(), binding.title, "miniapp").toBundle());
            binding.getRoot().setOnClickListener(action);
            binding.open.setOnClickListener(action);
            Markwon.builder(binding.getRoot().getContext()).usePlugins(List.of(new AbstractMarkwonPlugin() {
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
            }, TablePlugin.create(binding.getRoot().getContext()))).build().setMarkdown(binding.startTime, md.toString());

        }

        public void add(JSONObject item) {
            data.add(item);
            notifyItemInserted(getItemCount() - 1);
        }

        public void clear() {
            int tmp = getItemCount();
            data.clear();
            notifyItemRangeRemoved(0, tmp);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }
}
