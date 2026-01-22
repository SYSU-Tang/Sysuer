package com.sysu.edu.academic;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.sysu.edu.R;
import com.sysu.edu.databinding.DialogRegionBinding;
import com.sysu.edu.databinding.ItemCardBinding;
import com.sysu.edu.databinding.ItemTitleBinding;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LeaveReturnRegistrationFragment extends StaggeredFragment {
    final OkHttpClient http = new OkHttpClient();
    final MutableLiveData<Long> leaveDate = new MutableLiveData<>();
    final MutableLiveData<Long> returnDate = new MutableLiveData<>();
    final ArrayList<String> leaveKeys = new ArrayList<>(List.of("假期去向", "预计离校时间", "预计返校时间", "去向类型", "交通工具", "外出地"));
    final ArrayList<String> stayKeys = new ArrayList<>(List.of("假期去向", "留校原因"));
    View view;
    Handler handler;
    JSONArray transportation;
    JSONArray destination;
    String country = "";
    String province = "";
    String city = "";
    String isStay = "";
    ArrayList<String> leave;
    ArrayList<String> stay;
    String id;
    String baseUrl = "https://xgxt.sysu.edu.cn";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (view == null) {
            view = super.onCreateView(inflater, container, savedInstanceState);
            id = requireArguments().getString("Id");
            BottomSheetDialog regionDialog = new BottomSheetDialog(requireContext());
            DialogRegionBinding dialogRegionBinding = DialogRegionBinding.inflate(inflater, container, false);
            regionDialog.setContentView(dialogRegionBinding.getRoot());
            dialogRegionBinding.country.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            OneColumnAdapter countryAdapter = new OneColumnAdapter();
            dialogRegionBinding.country.recyclerView.setAdapter(countryAdapter);
            dialogRegionBinding.country.recyclerView.setNestedScrollingEnabled(false);
            dialogRegionBinding.country.recyclerView.setOverScrollMode(RecyclerView.OVER_SCROLL_ALWAYS);
            dialogRegionBinding.province.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            OneColumnAdapter provinceAdapter = new OneColumnAdapter();
            dialogRegionBinding.province.recyclerView.setAdapter(provinceAdapter);
            dialogRegionBinding.province.recyclerView.setNestedScrollingEnabled(false);
            dialogRegionBinding.province.recyclerView.setOverScrollMode(RecyclerView.OVER_SCROLL_ALWAYS);
            dialogRegionBinding.county.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            OneColumnAdapter cityAdapter = new OneColumnAdapter();
            dialogRegionBinding.county.recyclerView.setAdapter(cityAdapter);
            dialogRegionBinding.county.recyclerView.setNestedScrollingEnabled(false);
            dialogRegionBinding.county.recyclerView.setOverScrollMode(RecyclerView.OVER_SCROLL_ALWAYS);
            handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    if (msg.what == -1) {
                        params.toast(R.string.no_wifi_warning);
                    } else {
                        int code = msg.getData().getInt("code");
                        if (code == 200) {
                            JSONObject json = JSONObject.parse(msg.getData().getString("response"));
                            if (json != null && json.getInteger("code") == 200) {
                                if (msg.what == 0) {
                                    ArrayList<String> value = new ArrayList<>();
                                    clear();
                                    JSONObject data = json.getJSONObject("data");
                                    for (String i : new String[]{"xm", "xh", "nj", "pycc", "zymc", "bmmc", "lxdh", "jjlxr", "jjlxrdh", "ssdz", "jjrmc", "jjrrq", "fxbdsj"})
                                        value.add(data.getString(i));
                                    add("基本信息", List.of("姓名", "学号", "年级", "培养层次", "专业", "学院", "联系电话", "宿舍地址", "紧急联系人", "紧急联系人联系电话", "节假日名称", "节假日时间", "返校报到时间段"), value);
                                    isStay = data.getString("sflx");
                                    try {
                                        Date leaveTime = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(data.getString("yjlxsj") == null ? "" : data.getString("yjlxsj"));
                                        if (leaveTime != null) {
                                            leaveDate.postValue(leaveTime.getTime());
                                        }
                                        Date returnTime = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(data.getString("yjfxsj") == null ? "" : data.getString("yjfxsj"));
                                        if (returnTime != null) {
                                            returnDate.postValue(returnTime.getTime());
                                        }
                                        returnDate.postValue(Objects.requireNonNull((new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())).parse(data.getString("yjfxsj"))).getTime());
                                    } catch (ParseException e) {
                                        //throw new RuntimeException(e);
                                    }

                                    country = data.getString("wcdgj");
                                    province = data.getString("wcdsf");
                                    city = data.getString("wcdcs");

                                    String reason = data.getString("lxyy");

                                    leave = new ArrayList<>(List.of("离校", data.getString("yjlxsj"), data.getString("yjfxsj"),
                                            data.getString("qxlx"), data.getString("jtgj"), country + " " + province + " " + city));
                                    stay = new ArrayList<>(List.of("留校", reason == null ? "" : reason));
                                    if (Objects.equals(isStay, "0")) {
                                        add("登记", leaveKeys, leave);
                                    } else {
                                        add("登记", stayKeys, stay);
                                    }
                                    getDestination();
                                    getTransportation();
                                    getCountry();
                                } else if (msg.what == 1) {
                                    transportation = json.getJSONArray("data");
                                } else if (msg.what == 2) {
                                    destination = json.getJSONArray("data");
                                } else if (msg.what == 3) {
                                    countryAdapter.clear();
                                    json.getJSONArray("data").forEach(e -> countryAdapter.add(((JSONObject) e).getString("label")));
                                    countryAdapter.setAction(pos -> {
                                        country = json.getJSONArray("data").getJSONObject(pos).getString("value");
                                        if (country.equals("中国")) {
                                            getProvince();
                                        } else {
                                            city = "";
                                            province = "";
                                        }
                                    });
                                    countryAdapter.setResult(country);
                                    if (country.equals("中国")) {
                                        getProvince();
                                    }
                                    // dialogRegionBinding.regionList.setAdapter(new TwoColumnsAdapter(destination));
                                } else if (msg.what == 4) {
                                    provinceAdapter.clear();
                                    json.getJSONArray("data").forEach(e -> provinceAdapter.add(((JSONObject) e).getString("label")));
                                    provinceAdapter.setAction(pos -> {
                                        province = json.getJSONArray("data").getJSONObject(pos).getString("value");
                                        getCity(province);
                                    });
                                    getCity(province);
                                    provinceAdapter.setResult(province);
                                } else if (msg.what == 5) {
                                    cityAdapter.clear();
                                    json.getJSONArray("data").forEach(e -> cityAdapter.add(((JSONObject) e).getString("label")));
                                    cityAdapter.setAction(pos -> city = json.getJSONArray("data").getJSONObject(pos).getString("value"));
                                    cityAdapter.setResult(city);
                                } else if (msg.what == 6) {
                                    params.toast(json.getString("message"));
                                } else {
                                    params.toast(json.getString("message"));
                                }
                            }
                        } else {
                            params.toast(R.string.educational_wifi_warning);
                            baseUrl = "https://xgxt-443.webvpn.sysu.edu.cn";
                            getInfo(id);
                        }
                    }
                }
            };
            staggeredAdapter.setListener(new StaggeredListener() {
                @Override
                public void onBind(RecyclerView.Adapter<RecyclerView.ViewHolder> a, RecyclerView.ViewHolder holder, int position) {
                    staggeredAdapter.getTwoColumnsAdapter(position).setListener(new StaggeredListener() {
                        @Override
                        public void onBind(RecyclerView.Adapter<RecyclerView.ViewHolder> a, RecyclerView.ViewHolder holder, int pos) {

                            holder.itemView.setOnClickListener(v -> {
                                if (position == 1) {
                                    if (pos == 0) {
                                        PopupMenu menu = new PopupMenu(requireContext(), holder.itemView);
                                        List.of("离校", "留校").forEach(i -> menu.getMenu().add(i).setOnMenuItemClickListener(item -> {
                                            //value.set(pos, i);
                                            isStay = i.equals("离校") ? "0" : "1";
                                            ((TwoColumnsAdapter) a).setValue(i.equals("离校") ? leave : stay);
                                            ((TwoColumnsAdapter) a).setKey(i.equals("离校") ? leaveKeys : stayKeys);
                                            return true;
                                        }));
                                        menu.show();
                                    }
                                    if (a.getItemCount() == 6) {
                                        if (pos == 3 || pos == 4) {
                                            PopupMenu menu = new PopupMenu(requireContext(), holder.itemView);
                                            (pos == 4 ? transportation : destination).forEach(e -> menu.getMenu().add(((JSONObject) e).getString("label")).setOnMenuItemClickListener(item -> {
                                                leave.set(pos, ((JSONObject) e).getString("label"));
                                                ((TwoColumnsAdapter) a).setValue(leave);
                                                return true;
                                            }));
                                            menu.show();
                                        } else if (pos == 2 || pos == 1) {
                                            MaterialDatePicker<Long> calendar = MaterialDatePicker.Builder.datePicker()
                                                    .setSelection(pos == 2 ? returnDate.getValue() != null ? returnDate.getValue() : MaterialDatePicker.todayInUtcMilliseconds() : leaveDate.getValue() != null ? leaveDate.getValue() : MaterialDatePicker.todayInUtcMilliseconds())
                                                    .build();
                                            calendar.show(getParentFragmentManager(), "calendar");

                                            calendar.addOnPositiveButtonClickListener(aLong -> {
                                                leave.set(pos, calendar.getHeaderText());
                                                ((TwoColumnsAdapter) a).setValue(leave);
                                                (pos == 2 ? returnDate : leaveDate).setValue(aLong);
                                            });
                                        } else if (pos == 5) {
                                            regionDialog.show();
                                            dialogRegionBinding.confirm.setOnClickListener(view -> {
                                                leave.set(pos, countryAdapter.getResult() + " " + provinceAdapter.getResult() + " " + cityAdapter.getResult());
                                                ((TwoColumnsAdapter) a).setValue(leave);
                                                regionDialog.dismiss();
                                            });
                                        }
                                    } else if (a.getItemCount() == 2) {
                                        if (pos == 1) {
                                            PopupMenu menu = new PopupMenu(requireContext(), holder.itemView);
                                            List.of(getResources().getStringArray(R.array.registration_info_keys)).forEach(i -> menu.getMenu().add(i).setOnMenuItemClickListener(item -> {
                                                stay.set(pos, i);
                                                ((TwoColumnsAdapter) a).setValue(stay);
                                                return true;
                                            }));
                                            menu.show();
                                        }
                                    }
                                }
                            });
                        }

                        @Override
                        public void onCreate(RecyclerView.Adapter<RecyclerView.ViewHolder> a, ViewBinding binding) {

                        }
                    });

                    holder.itemView.findViewById(R.id.button).setVisibility(position == 0 ? View.GONE : View.VISIBLE);
                }

                @Override
                public void onCreate(RecyclerView.Adapter<RecyclerView.ViewHolder> a, ViewBinding binding) {
                    MaterialButton button = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonTonalStyle);
                    button.setId(R.id.button);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    lp.gravity = Gravity.END;
                    lp.setMargins(0, 0, params.dpToPx(16), params.dpToPx(16));
                    button.setLayoutParams(lp);
                    button.setOnClickListener(v -> {
                        if (isStay.equals("0")) {
                            save(id, isStay, leaveDate.getValue() == null ? "" : new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(leaveDate.getValue())), returnDate.getValue() == null ? "" : new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(returnDate.getValue()), leave.get(3), leave.get(4), country, province, city);
                        } else {

                            save(id, isStay, stay.get(1));
                        }
                    });
                    button.setText(R.string.save);
                    ((ItemCardBinding) binding).getRoot().addView(button);
                }
            });
            getInfo(id);
        }
        return view;
    }

    void sendRequest(String url, int what) {
        http.newCall(new Request.Builder().url(baseUrl + url)
                .header("Cookie", params.getCookie())
                .build()).enqueue(new Callback() {

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what = what;
                Bundle bundle = new Bundle();
                bundle.putInt("code", response.code());
                bundle.putString("response", response.body().string());
                msg.setData(bundle);
                handler.sendMessage(msg);
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = new Message();
                msg.what = -1;
                handler.sendMessage(msg);
            }
        });
    }

    void postRequest(String url, String data, int what) {
        http.newCall(new Request.Builder().url(baseUrl + url)
                .header("Cookie", params.getCookie())
                .post(RequestBody.create(data, MediaType.parse("application/json")))
                .build()).enqueue(new Callback() {

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what = what;
                Bundle bundle = new Bundle();
                bundle.putInt("code", response.code());
                bundle.putString("response", response.body().string());
                msg.setData(bundle);
                handler.sendMessage(msg);
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = new Message();
                msg.what = -1;
                handler.sendMessage(msg);
            }
        });
    }

    void save(String id, String isStay, String leaveTime, String returnTime, String leaveType, String transportation, String country, String province, String city) {
        postRequest("/jjrlfx/api/sm-jjrlfx/student/register",
                String.format(
                        "{\"cjlfxgzId\":\"%s\",\"sflx\":\"%s\",\"yjlxsj\":\"%s\",\"yjfxsj\":\"%s\",\"qxlx\":\"%s\",\"jtgj\":\"%s\",\"wcd\":{\"gj\":\"%s\",\"sf\":\"%s\",\"cs\":\"%s\"},\"wcdgj\":\"%s\",\"wcdsf\":\"%s\",\"wcdcs\":\"%s\"}\n",
                        id, isStay, leaveTime, returnTime, leaveType, transportation, country, province, city, country, province, city
                ), 6);
    }

    void save(String id, String isStay, String reason) {
        postRequest("/jjrlfx/api/sm-jjrlfx/student/register",
                String.format("{\"cjlfxgzId\":\"%s\",\"sflx\":\"%s\",\"lxyy\":\"%s\"}", id, isStay, reason), 6);
    }

    void getInfo(String id) {
        sendRequest("/jjrlfx/api/sm-jjrlfx/student/" + id + "/info", 0);
    }

    void getTransportation() {
        sendRequest("/jjrlfx/api/sm-jjrlfx/student/transport", 1);
    }

    void getDestination() {
        sendRequest("/jjrlfx/api/sm-jjrlfx/student/destination-type", 2);
    }

    void getCountry() {
        sendRequest("/jjrlfx/api/sm-jjrlfx/student/country/drop", 3);
    }

    void getProvince() {
        sendRequest("/jjrlfx/api/sm-jjrlfx/student/province/drop?0=%E4%B8%AD&1=%E5%9B%BD", 4);
    }

    void getCity(String province) {
        sendRequest("/jjrlfx/api/sm-jjrlfx/student/city/drop?fdm=" + province, 5);
    }


    static class OneColumnAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        final ArrayList<String> value = new ArrayList<>();
        Consumer<Integer> action;
        int selection = -1;

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(ItemTitleBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false).getRoot()) {
            };
        }

        public void add(String value) {
            this.value.add(value);
            notifyItemInserted(this.value.size() - 1);
        }

        public void clear() {
            int tmp = getItemCount();
            value.clear();
            notifyItemRangeRemoved(0, tmp);
        }

        public void setAction(Consumer<Integer> action) {
            this.action = action;
        }


        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int pos) {
            int position = holder.getBindingAdapterPosition();
            ItemTitleBinding binding = ItemTitleBinding.bind(holder.itemView);
            binding.title.setText(value.get(position));
            binding.getRoot().setBackgroundResource(position == selection ? R.drawable.bg_selected : R.drawable.box_background);
            binding.getRoot().setOnClickListener(v -> {
                if (action != null)
                    action.accept(position);
                selection = position;
                notifyItemRangeChanged(0, getItemCount());
            });
        }

        @Override
        public int getItemCount() {
            return value.size();
        }

        public String getResult() {
            if (selection == -1)
                return "";
            return value.get(selection);
        }

        public void setResult(String result) {
            if (value.contains(result)) {
                selection = value.indexOf(result);
            }
        }
    }
}
