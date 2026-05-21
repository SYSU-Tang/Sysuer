package com.sysu.edu.life;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.sysu.edu.R;
import com.sysu.edu.api.AuthorizationJar;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.DialogGymReservationBinding;
import com.sysu.edu.databinding.FragmentGymDetailBinding;
import com.sysu.edu.databinding.ItemDateBinding;
import com.sysu.edu.databinding.ItemFieldDetailBinding;
import com.sysu.edu.view.RecyclerAdapter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.IntStream;


public class GymDetailFragment extends Fragment {

    final HashMap<String, JSONObject> fee = new HashMap<>();
    HttpManager http;
    GymReservationViewModel viewModel;
    String id;
    String hash;
    DateAdapter date;
    String userId;
    String type;
    FragmentGymDetailBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentGymDetailBinding.inflate(inflater, container, false);
        DialogGymReservationBinding dialogBinding = DialogGymReservationBinding.inflate(inflater, container, false);
        binding.date.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        date = new DateAdapter();

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(dialogBinding.getRoot());
        initReservationDialog(dialogBinding);
        viewModel = new ViewModelProvider(requireActivity()).get(GymReservationViewModel.class);
        date.setAction(viewModel.position::setValue);
        Params params = new Params(this);
        binding.date.recyclerView.setAdapter(date);
        binding.date.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!recyclerView.canScrollHorizontally(1) && dx > 0) date.offset(7);
            }
        });
        GridLayoutManager gridLayoutManager = new GridLayoutManager(requireContext(), 4, GridLayoutManager.HORIZONTAL, false);
        binding.field.recyclerView.setLayoutManager(gridLayoutManager);
        FieldAdapter field = new FieldAdapter();
        binding.field.recyclerView.setAdapter(field);
        id = requireArguments().getString("id");
        field.setAction(_ -> {
            viewModel.selected.setValue(field.getSelected());
//                updateReservationDialog(dialogBinding, p, studentFee);
//            dialog.show();
        });
        if (viewModel.position.getValue() == null) {
            viewModel.position.postValue(0);
        }
        viewModel.position.observe(getViewLifecycleOwner(), p -> {
            if (p != null) getInfo();
        });
        http = new HttpManager(new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (msg.what == -1) params.toast(R.string.no_net_connected);
                else {
                    String response = (String) msg.obj;
                    if (msg.getData().getBoolean("isJSON")) {
                        switch (msg.what) {
                            case 0:
                                reset(field);
                                hash = md5(response);
                                AtomicInteger availableCapacity = new AtomicInteger();
                                AtomicInteger rows = new AtomicInteger(-1);
                                MutableLiveData<Boolean> name = new MutableLiveData<>(false);
                                JSONArray.parse(response).forEach(e -> {
                                    JSONObject item = (JSONObject) e;
                                    JSONArray timeslots = item.getJSONArray("Timeslots");
                                    if (timeslots != null) {
                                        if (rows.get() == -1) {
                                            field.add(JSONObject.of("Name", getString(R.string.time), "Type", 2));
                                            timeslots.forEach(o -> field.add(JSONObject.of("Name", String.format("%s\n%s", ((JSONObject) o).getString("Start"), ((JSONObject) o).getString("End")), "Type", 2)));
                                            name.setValue(true);
                                            rows.set(timeslots.size() + 1);
                                            gridLayoutManager.setSpanCount(rows.get());
                                        }// 第一列
                                        String fieldName = Pattern.compile("(.+)-").matcher(item.getString("VenueName")).replaceAll(""); // 第一行
                                        field.add(new JSONObject().fluentPut("VenueName", fieldName).fluentPut("Type", 0));
                                        timeslots.forEach(o -> {
                                            JSONObject jsonObject = (JSONObject) o;
                                            field.add(jsonObject.clone().fluentPut("VenueBooking", item.clone().fluentPut("Timeslots", JSONArray.of(jsonObject))).fluentPut("Type", 1).fluentPut("Venue", fieldName).fluentPut("Duration", String.format(Locale.getDefault(), "%s~%s", jsonObject.getString("Start"), jsonObject.getString("End"))));
                                            int capacity = jsonObject.getInteger("AvailableCapacity");
                                            if (capacity > 0)
                                                availableCapacity.addAndGet(capacity);
                                        });
                                        if (field.getItemCount() % rows.get() != 0)
                                            IntStream.range(0, rows.get() - field.getItemCount() % rows.get()).forEach(_ -> field.add(JSONObject.of("Type", 3)));

                                    }
                                });
                                if (viewModel.position.getValue() != null)
                                    date.setAvailableCapacity(viewModel.position.getValue(), availableCapacity.get());
                                getFee(id);
                                break;
                            case 1:
                                fee.clear();
                                JSONArray feeTemplates = JSONArray.parse(response);
                                if (feeTemplates != null)
                                    feeTemplates.forEach(e -> fee.put(((JSONObject) e).getString("UserRole"), (JSONObject) e));
                                getMe();
                                break;
                            case 2:
                                JSONArray mes = JSONArray.parseArray(response);
                                if (!mes.isEmpty()) {
                                    JSONObject me = mes.getJSONObject(0);
                                    userId = me.getString("UserId");
                                }
                                getType(id);
                                break;
                            case 3:
                                JSONArray types = JSONArray.parseArray(response);
                                if (!types.isEmpty()) {
                                    type = types.getJSONObject(0).getString("TypeIdentity");
                                }
                                break;
                            case 4:
                                JSONObject result = JSONObject.parse(response);
                                if (result.getInteger("Code") == 200) {
                                    params.toast(R.string.reserve_success);
                                } else {
                                    params.toast(result.getString("Result"));
                                }
                                // 订单编号
                                /*{"Code":200,"Result":{"Identity":"6ae40ca4-392b-4ebc-8ba2-8ec1126f54bc","Name":"tangxb6","BookingId":"#RB-44U4HTRRQ4","UserId":"tangxb6","HostKey":"24308152","VenueTypeId":"802fbfda-f9f6-41c8-9c72-685247f07c73","VenueBookings":[{"VenueId":"d2c4c59a-1d00-4c44-9a6a-24f26390227e","VenueName":"东校园游泳池","TimeSlots":[{"Date":"2026-03-18T00:00:00","Start":"19:30","End":"21:00"}]}],"Participants":[],"Status":"Accepted","Description":"东校园游泳池","Charge":-5,"IsCash":false,"CreatedAt":"2026-03-17T21:30:19.8154563+08:00","UpdatedAt":"2026-03-17T13:30:19.611245Z","ActionedBy":"tangxb6","Token":null}}*/
                                break;
                        }
                    } else if (!viewModel.authorizationManager.isAuthorized(response)) {
                        params.toast(R.string.login_warning);
                        params.gotoLogin( viewModel.authorizationManager.isAccessible() ? TargetUrl.GYM : TargetUrl.GYM_WEBVPN);
                    } else if (Pattern.compile("人机识别检测").matcher(response).find()) {
                        params.gotoLogin(viewModel.authorizationManager.isAccessible() ? TargetUrl.GYM : TargetUrl.GYM_WEBVPN);
                    } else if (!viewModel.authorizationManager.isAccessible(response)) {
                        params.toast(R.string.educational_wifi_warning);
                        getInfo();
                    }

                }
            }
        });
        http.setParams(params);
        http.setUA(viewModel.ua);
        http.setHeader(Map.of("Accept", "application/json, text/plain, */*"));
        http.setAuthorizationRequired(true);
        http.setAuthorizationJar(new AuthorizationJar(requireContext()));
        date.select(viewModel.position.getValue() == null ? 0 : viewModel.position.getValue());
        viewModel.selected.observe(getViewLifecycleOwner(), selected -> {
            field.setSelected(selected);
            JSONObject studentFee = fee.get("学生");
            if (studentFee != null) {
                binding.submit.setEnabled(!field.getSelected().isEmpty());
                if (field.getSelected().isEmpty()) {
                    binding.info.setText(getString(R.string.unselected));
                } else {
                    StringBuilder info = new StringBuilder();
                    JSONArray items = new JSONArray();
                    field.getSelected().forEach(e -> {
                        info.append(field.get(e).getString("Venue")).append(" ").append(field.get(e).getString("Duration")).append("+");
                        items.add(field.get(e).getJSONObject("VenueBooking"));
                    });
                    String venueName = field.get(new ArrayList<>(field.getSelected()).get(0)).getString("VenueName");
                    int creditFee = studentFee.getInteger("CreditFee") * field.getSelected().size();
                    binding.submit.setOnClickListener(_ -> reserve(items, venueName, creditFee));
                    binding.info.setText(String.format(Locale.getDefault(), "%s=%d元", info.deleteCharAt(info.length() - 1), creditFee));
                }
            }
        });
        return binding.getRoot();
    }

    void reset(FieldAdapter field) {
        field.clear();
        field.clearSelected();
        viewModel.selected.setValue(new HashSet<>());
    }

    void getInfo() {
        Integer positionValue = viewModel.position.getValue();
        if (positionValue != null)
            getInfo(id, date.getFormattedDate(positionValue), date.getFormattedDate(positionValue));
    }

    void getInfo(String id, String from, String to) {
        http.getRequest(viewModel.authorizationManager.getBaseUrl() + String.format("api/venue/available-slots/range?venueTypeId=%s&start=%s&end=%s", id, from, to), 0);
    }

    void getFee(String id) {
        http.getRequest(viewModel.authorizationManager.getBaseUrl() + String.format("api/venuetype/%s/feetemplates", id), 1);
    }

    void getMe() {
        http.getRequest(viewModel.authorizationManager.getBaseUrl() + "api/swimmer/me", 2);
    }

    void getType(String id) {
        http.getRequest(viewModel.authorizationManager.getBaseUrl() + "api/venue/type/" + id, 3);
    }

    public void reserve(String payload) {
        http.postRequest(viewModel.authorizationManager.getBaseUrl() + "api/BookingRequestVenue", payload, 4);
    }

    /**
     * 生成 UUID
     *
     * @return 生成的 UUID
     *
     */
    String generateUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * 生成 Token
     *
     * @param hash 哈希值
     * @return 生成的 Token
     *
     */
    String genToken(String uuid, String hash) {
        long timestamp = System.currentTimeMillis() / 1000L;
        return md5("SYSUBOOKING-" + uuid + timestamp) + "." + timestamp + "." + hash;
    }

    /**
     * 计算 MD5 哈希值
     *
     * @param input 输入字符串
     * @return 计算得到的 MD5 哈希值（十六进制小写字符串）
     *
     */
    String md5(String input) {
        try {
            StringBuilder hexString = new StringBuilder();
            for (byte b : MessageDigest.getInstance("MD5").digest(input.getBytes(StandardCharsets.UTF_8))) {
                String hex = Integer.toHexString(0xff & b);
                hexString.append(hex.length() == 1 ? "0" : hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    void initReservationDialog(DialogGymReservationBinding binding) {
        binding.field.key.setText(R.string.field);
        binding.date.key.setText(R.string.date);
        binding.time.key.setText(R.string.time);
        binding.fee.key.setText(R.string.fee);
        binding.type.key.setText(R.string.type);
    }

//    void updateReservationDialog(DialogGymReservationBinding binding, JSONObject item, JSONObject studentFee) {
//        binding.field.value.setText(item.getString("Venue"));
//        binding.date.value.setText(item.getString("Date"));
//        binding.time.value.setText(item.getString("Duration"));
//        Integer creditFee = studentFee.getInteger("CreditFee");
//        Integer cashFee = studentFee.getInteger("CashFee");
//        binding.fee.value.setText(String.format(Locale.getDefault(), "运动时￥%d或现金￥%d", creditFee, cashFee));
//        binding.type.value.setText(item.getString("Type"));
//        binding.reserve.setOnClickListener(_ -> reserve(item.getJSONArray("VenueBooking"), item.getString("VenueName"), creditFee));
//    }

    private void reserve(JSONArray items, String venueName, Integer creditFee) {
        String uuid = generateUUID();
        String time = LocalDateTime.now().atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).toString();
//            {"Identity":"561e14e8-1f6d-44d4-89e9-e9cbe0b5bb81","BookingId":"e59ec78d1b4b06f72f6860d10e0fe7d2.1773708652.b268e04b7b07d45673fdf4ad2f14c914","VenueTypeId":"802fbfda-f9f6-41c8-9c72-685247f07c73","VenueBookings":[{"VenueId":"d2c4c59a-1d00-4c44-9a6a-24f26390227e","VenueName":"东校园游泳池","TimeSlots":[{"Date":"2026-03-17T00:00:00.000Z","Start":"16:30","End":"18:00"}]}],"Participants":[],"Status":"Accepted","Description":"东校园游泳池","CreatedAt":"2026-03-17T00:50:52.552Z","UpdatedAt":"2026-03-17T00:50:52.552Z","ActionedBy":"tangxb6","IsCash":false,"Charge":5}
        JSONObject payload = JSONObject.of(
                "Identity", uuid,
                "BookingId", genToken(uuid, hash),
                "VenueTypeId", type,
                "VenueBookings", items,
                "Participants", JSONArray.of(),
                "Status", "Accepted",
                "Description", venueName,
                "CreatedAt", time,
                "UpdatedAt", time,
                "ActionedBy", userId,/*NetID*/
                "IsCash", false,
                "Charge", creditFee
        );
//        System.out.println(payload);
        reserve(payload.toJSONString());
    }


    static class DateAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        final HashMap<Integer, Integer> availableCapacity = new HashMap<>();
        Consumer<? super Integer> action;
        int page = 7;
        int selected = -1;

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(ItemDateBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false).getRoot()) {
            };
        }

        void select(int position) {
            int tmp = selected;
            selected = position;
            action.accept(position);
            notifyItemChanged(position);
            notifyItemChanged(tmp);
        }

        public void setAction(Consumer<? super Integer> action) {
            this.action = action;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ItemDateBinding binding = ItemDateBinding.bind(holder.itemView);
            Context context = holder.itemView.getContext();
            binding.date.setText(getDate(position));
            binding.week.setText(getWeek(context, position));
            binding.getRoot().setOnClickListener(_ -> select(position));
            Integer capacity = availableCapacity.getOrDefault(position, -1);
            binding.getRoot().setChecked(position == selected);
            binding.availableCapacity.setText(capacity != null && capacity >= 0 ? String.format(Locale.getDefault(), "%d", capacity) : "");
        }

        public void setAvailableCapacity(int position, int i) {
            availableCapacity.put(position, i);
            notifyItemChanged(position);
        }

        @Override
        public int getItemCount() {
            return page;
        }

        public void offset(int offset) {
            page += offset;
            notifyItemRangeInserted(page - 1, offset);
        }

        private String getDate(int distanceDay, String pattern) {
            return LocalDate.now().plusDays(distanceDay).format(DateTimeFormatter.ofPattern(pattern));
        }

        public String getDate(int distanceDay) {
            return getDate(distanceDay, "M月dd日");
        }

        public String getFormattedDate(int distanceDay) {
            return getDate(distanceDay, "M-dd");
        }

        String getWeek(Context context, int distanceDay) {
            int week = LocalDate.now().plusDays(distanceDay).getDayOfWeek().getValue();
            return context.getResources().getStringArray(R.array.weeks)[week - 1];
        }

    }

    static class FieldAdapter extends RecyclerAdapter<JSONObject> {

        Consumer<? super Integer> action;
        HashSet<Integer> selected = new HashSet<>();

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(ItemFieldDetailBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false).getRoot()) {
            };
        }

        void setAction(Consumer<? super Integer> action) {
            this.action = action;
        }

        void clearSelected() {
            HashSet<Integer> s = new HashSet<>(selected);
            selected.clear();
            s.forEach(this::notifyItemChanged);
        }

        HashSet<Integer> getSelected() {
            return selected;
        }

        public void setSelected(HashSet<Integer> selected) {
            if (!this.selected.equals(selected)) {
                this.selected = selected;
                selected.forEach(this::notifyItemChanged);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int pos) {
            int position = holder.getBindingAdapterPosition();
            ItemFieldDetailBinding binding = ItemFieldDetailBinding.bind(holder.itemView);
            binding.fieldDetail.setAlpha(1.0f);
            JSONObject item = get(position);
            Context context = holder.itemView.getContext();
            binding.getRoot().setOnClickListener(_ -> {
                if (item.getInteger("Type") == 1 && item.getInteger("AvailableCapacity") > 0) {
                    if (selected.contains(position)) {
                        selected.remove(position);
                    } else {
                        selected.add(position);
                    }
                    action.accept(position);
                    notifyItemChanged(position);
                }
            });
            switch (item.getInteger("Type")) {
                case 0 ->
                        binding.fieldDetail.setText(String.format(Locale.getDefault(), "%s", item.getString("VenueName")));
                case 2 ->
                        binding.fieldDetail.setText(String.format(Locale.getDefault(), "%s", item.getString("Name")));
                case 1 -> {
                    if (item.getInteger("AvailableCapacity") > 0) {
                        binding.fieldDetail.setText(context.getString(R.string.reservable));
                    } else {
                        binding.fieldDetail.setText(context.getString(R.string.reserved));
                        binding.fieldDetail.setAlpha(0.5f);
                    }
                    binding.getRoot().setChecked(selected.contains(position));
                }
                default -> binding.fieldDetail.setText("");
            }
        }
    }
}