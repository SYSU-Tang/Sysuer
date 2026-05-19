package com.sysu.edu.academic;

import static com.sysu.edu.api.CommonUtil.extractValue;
import static com.sysu.edu.api.CommonUtil.toStringOrDefault;

import android.os.Bundle;
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
import com.sysu.edu.model.XgxtModel;
import com.sysu.edu.view.AdapterListener;
import com.sysu.edu.view.RecyclerAdapter;
import com.sysu.edu.view.StaggeredFragment;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class LeaveReturnRegistrationFragment extends StaggeredFragment {
    final MutableLiveData<Long> leaveDate = new MutableLiveData<>();
    final MutableLiveData<Long> returnDate = new MutableLiveData<>();
    final ArrayList<String> leaveKeys = new ArrayList<>(List.of("假期去向", "预计离校时间", "预计返校时间", "去向类型", "交通工具", "外出地"));
    final ArrayList<String> stayKeys = new ArrayList<>(List.of("假期去向", "留校原因"));
    View view;
    JSONArray transportation;
    JSONArray destination;
    String country = "";
    String province = "";
    String city = "";
    String isStay = "";
    ArrayList<String> leave;
    ArrayList<String> stay;
    String id;
    XgxtModel model;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (view == null) {
            view = super.onCreateView(inflater, container, savedInstanceState);
            id = requireArguments().getString("Id");
            model = new XgxtModel(requireContext());
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
            model.getMessage().observe(requireActivity(), message -> {
                JSONObject response = message.getSecond();
                if (response != null && response.getInteger("code") == 200) {
                    switch (message.getFirst()) {
                        case 0 -> {
                            clear();
                            JSONObject data = response.getJSONObject("data");
                            add("基本信息", List.of("姓名", "学号", "年级", "培养层次", "专业", "学院", "联系电话", "宿舍地址", "紧急联系人", "紧急联系人联系电话", "节假日名称", "节假日时间", "返校报到时间段")
                                    , extractValue(data,new String[]{"xm", "xh", "nj", "pycc", "zymc", "bmmc", "lxdh", "jjlxr", "jjlxrdh", "ssdz", "jjrmc", "jjrrq", "fxbdsj"}));
                            isStay = data.getString("sflx");
                            leaveDate.postValue(LocalDate.parse(toStringOrDefault(data.getString("yjlxsj"))).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
                            returnDate.postValue(LocalDate.parse(toStringOrDefault(data.getString("yjfxsj"))).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
                            country = data.getString("wcdgj");
                            province = data.getString("wcdsf");
                            city = data.getString("wcdcs");
                            String reason = data.getString("lxyy");
                            leave = new ArrayList<>(List.of("离校", data.getString("yjlxsj"), data.getString("yjfxsj"),
                                    data.getString("qxlx"), data.getString("jtgj"), country + " " + province + " " + city));
                            stay = new ArrayList<>(List.of("留校", toStringOrDefault(reason)));
                            if ("0".equals(isStay)) add(getString(R.string.registration), leaveKeys, leave);
                            else add(getString(R.string.registration), stayKeys, stay);
                            getDestination();
                            getTransportation();
                            getCountry();
                        }
                        case 1 -> transportation = response.getJSONArray("data");
                        case 2 -> destination = response.getJSONArray("data");
                        case 3 -> {
                            countryAdapter.clear();
                            response.getJSONArray("data").forEach(e -> countryAdapter.add(((JSONObject) e).getString("label")));
                            countryAdapter.setAction(pos -> {
                                country = response.getJSONArray("data").getJSONObject(pos).getString("value");
                                if ("中国".equals(country)) getProvince();
                                else {
                                    city = "";
                                    province = "";
                                }
                            });
                            countryAdapter.setResult(country);
                            if ("中国".equals(country)) getProvince();
                            // dialogRegionBinding.regionList.setAdapter(new TwoColumnsAdapter(destination));
                        }
                        case 4 -> {
                            provinceAdapter.clear();
                            response.getJSONArray("data").forEach(e -> provinceAdapter.add(((JSONObject) e).getString("label")));
                            provinceAdapter.setAction(pos -> {
                                province = response.getJSONArray("data").getJSONObject(pos).getString("value");
                                getCity(province);
                            });
                            getCity(province);
                            provinceAdapter.setResult(province);
                        }
                        case 5 -> {
                            cityAdapter.clear();
                            response.getJSONArray("data").forEach(e -> cityAdapter.add(((JSONObject) e).getString("label")));
                            cityAdapter.setAction(pos -> city = response.getJSONArray("data").getJSONObject(pos).getString("value"));
                            cityAdapter.setResult(city);
                        }
                        default -> params.toast(response.getString("message"));
                    }
                }
            });
            setListener(new AdapterListener() {
                @Override
                public void onBind(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, RecyclerView.ViewHolder holder, int position) {
                    staggeredAdapter.getTwoColumnsAdapter(position).setListener(new AdapterListener() {
                        @Override
                        public void onBind(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, RecyclerView.ViewHolder holder, int pos) {
                            
                            holder.itemView.setOnClickListener(_ -> {
                                if (position == 1) {
                                    if (pos == 0) {
                                        PopupMenu menu = new PopupMenu(requireContext(), holder.itemView);
                                        List.of("离校", "留校").forEach(i -> menu.getMenu().add(i).setOnMenuItemClickListener(_ -> {
                                            //value.set(pos, i);
                                            isStay = "离校".equals(i) ? "0" : "1";
                                            ((TwoColumnsAdapter) adapter).setValue("离校".equals(i) ? leave : stay);
                                            ((TwoColumnsAdapter) adapter).setKey("离校".equals(i) ? leaveKeys : stayKeys);
                                            return true;
                                        }));
                                        menu.show();
                                    }
                                    if (adapter.getItemCount() == 6) {
                                        if (pos == 3 || pos == 4) {
                                            PopupMenu menu = new PopupMenu(requireContext(), holder.itemView);
                                            (pos == 4 ? transportation : destination).forEach(e -> menu.getMenu().add(((JSONObject) e).getString("label")).setOnMenuItemClickListener(_ -> {
                                                leave.set(pos, ((JSONObject) e).getString("label"));
                                                ((TwoColumnsAdapter) adapter).setValue(leave);
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
                                                ((TwoColumnsAdapter) adapter).setValue(leave);
                                                (pos == 2 ? returnDate : leaveDate).setValue(aLong);
                                            });
                                        } else if (pos == 5) {
                                            regionDialog.show();
                                            dialogRegionBinding.confirm.setOnClickListener(_ -> {
                                                leave.set(pos, countryAdapter.getResult() + " " + provinceAdapter.getResult() + " " + cityAdapter.getResult());
                                                ((TwoColumnsAdapter) adapter).setValue(leave);
                                                regionDialog.dismiss();
                                            });
                                        }
                                    } else if (adapter.getItemCount() == 2) {
                                        if (pos == 1) {
                                            PopupMenu menu = new PopupMenu(requireContext(), holder.itemView);
                                            List.of(getResources().getStringArray(R.array.registration_info_keys)).forEach(i -> menu.getMenu().add(i).setOnMenuItemClickListener(_ -> {
                                                stay.set(pos, i);
                                                ((TwoColumnsAdapter) adapter).setValue(stay);
                                                return true;
                                            }));
                                            menu.show();
                                        }
                                    }
                                }
                            });
                        }
                        
                        @Override
                        public void onCreate(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, ViewBinding binding) {
                            
                        }
                    });
                    
                    holder.itemView.findViewById(R.id.button).setVisibility(position == 0 ? View.GONE : View.VISIBLE);
                }
                
                @Override
                public void onCreate(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, ViewBinding binding) {
                    MaterialButton button = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonTonalStyle);
                    button.setId(R.id.button);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    lp.gravity = Gravity.END;
                    lp.setMargins(0, 0, params.dpToPx(16), params.dpToPx(16));
                    button.setLayoutParams(lp);
                    button.setOnClickListener(_ -> {
                        if ("0".equals(isStay))
                            save(id, isStay, leaveDate.getValue() == null ? "" : Instant.ofEpochMilli(leaveDate.getValue()).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), returnDate.getValue() == null ? "" : Instant.ofEpochMilli(returnDate.getValue()).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), leave.get(3), leave.get(4), country, province, city);
                        else save(id, isStay, stay.get(1));
                    });
                    button.setText(R.string.save);
                    ((ItemCardBinding) binding).getRoot().addView(button);
                }
            });
            getInfo(id);
        }
        return view;
    }
    
    void save(String id, String isStay, String leaveTime, String returnTime, String leaveType, String transportation, String country, String province, String city) {
        model.addAndNext("jjrlfx/api/sm-jjrlfx/student/register",
                String.format(
                        "{\"cjlfxgzId\":\"%s\",\"sflx\":\"%s\",\"yjlxsj\":\"%s\",\"yjfxsj\":\"%s\",\"qxlx\":\"%s\",\"jtgj\":\"%s\",\"wcd\":{\"gj\":\"%s\",\"sf\":\"%s\",\"cs\":\"%s\"},\"wcdgj\":\"%s\",\"wcdsf\":\"%s\",\"wcdcs\":\"%s\"}\n",
                        id, isStay, leaveTime, returnTime, leaveType, transportation, country, province, city, country, province, city
                ), 6);
    }
    
    void save(String id, String isStay, String reason) {
        model.addAndNext("jjrlfx/api/sm-jjrlfx/student/register",
                String.format("{\"cjlfxgzId\":\"%s\",\"sflx\":\"%s\",\"lxyy\":\"%s\"}", id, isStay, reason), 6);
    }
    
    void getInfo(String id) {
        model.addAndNext("jjrlfx/api/sm-jjrlfx/student/" + id + "/info", 0);
    }
    
    void getTransportation() {
        model.addAndNext("jjrlfx/api/sm-jjrlfx/student/transport", 1);
    }
    
    void getDestination() {
        model.addAndNext("jjrlfx/api/sm-jjrlfx/student/destination-type", 2);
    }
    
    void getCountry() {
        model.addAndNext("jjrlfx/api/sm-jjrlfx/student/country/drop", 3);
    }
    
    void getProvince() {
        model.addAndNext("jjrlfx/api/sm-jjrlfx/student/province/drop?0=%E4%B8%AD&1=%E5%9B%BD", 4);
    }
    
    void getCity(String province) {
        model.addAndNext("jjrlfx/api/sm-jjrlfx/student/city/drop?fdm=" + province, 5);
    }
    
    static class OneColumnAdapter extends RecyclerAdapter<String> {
        
        Consumer<? super Integer> action;
        int selection = -1;
        
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(ItemTitleBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false).getRoot()) {
            };
        }
        
        public void setAction(Consumer<? super Integer> action) {
            this.action = action;
        }
        
        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int pos) {
            int position = holder.getBindingAdapterPosition();
            ItemTitleBinding binding = ItemTitleBinding.bind(holder.itemView);
            binding.title.setText(get(position));
            binding.getRoot().setBackgroundResource(position == selection ? R.drawable.bg_selected : R.drawable.box_background);
            binding.getRoot().setOnClickListener(_ -> {
                if (action != null)
                    action.accept(position);
                selection = position;
                notifyItemRangeChanged(0, getItemCount());
            });
            super.onBindViewHolder(holder, pos);
        }
        
        public String getResult() {
            return selection == -1 ? "" : get(selection);
        }
        
        public void setResult(String result) {
            if (data.contains(result)) selection = data.indexOf(result);
        }
    }
}
