package com.sysu.edu.academic;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.ActivitySchoolRollBinding;
import com.sysu.edu.databinding.CardItemBinding;
import com.sysu.edu.databinding.TwoColumnBinding;
import com.sysu.edu.extra.LoginActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SchoolRoll extends AppCompatActivity {

    ActivitySchoolRollBinding binding;
    Map<String, List<String>> data;
    String cookie;

    OkHttpClient http = new OkHttpClient.Builder().build();
    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySchoolRollBinding.inflate(getLayoutInflater());
        Params params = new Params(this);
        cookie = getSharedPreferences("privacy", Context.MODE_PRIVATE).getString("Cookie", "");
        setContentView(binding.getRoot());
        binding.container.setColumnCount(params.getColumn());
        data = Map.of(
                "个人基本信息", List.of("学号",
                        "姓名",
                        "英文姓名",
                        "姓名拼音",
                        "中文姓名",
                        "曾用名",
                        "国家/地区",
                        "身份证件类型",
                        "身份证件号",
                        "曾用身份证件类型",
                        "曾用身份证件号",
                        "性别",
                        "出生日期",
                        "婚姻状况",
                        "健康状况",
                        "信仰宗教",
                        "血型",
                        "身份证件有效期",
                        "出生地",
                        "民族",
                        "政治面貌",
                        "籍贯",
                        "港澳台侨外",
                        "特长或爱好",
                        "港澳台通行证号",
                        "考生号"),
                "学籍信息",List.of("学院",
                        "系部",
                        "年级",
                        "年级专业方向",
                        "当前校区",
                        "所属年级专业大类",
                        "专业大类",
                        "专业方向",
                        "国标专业",
                        "跨院系大类",
                        "学制",
                        "学生类别",
                        "学科门类",
                        "专业授予学位",
                        "是否全面学分制",
                        "是否需要认定",
                        "最短修读年限",
                        "最长修读年限",
                        "班级",
                        "当前学籍状态",
                        "是否在校",
                        "学习形式",
                        "培养层次",
                        "培养方式",
                        "入学方式",
                        "入学日期",
                        "预计毕业日期",
                        "收费年级",
                        "毕业日期",
                        "授予学位类别",
                        "毕业发证日期",
                        "证书编号",
                        "校长名",
                        "学位发证日期",
                        "学位证书编号",
                        "来华留学生类别",
                        "来华留学经费来源",
                        "CSC/CIS编号",
                        "授课语言",
                        "生源地",
                        "考生类别",
                        "毕业类别",
                        "毕业中学",
                        "高考成绩",
                        "投档成绩",
                        "大类内本省录取人数",
                        "大类内本省排名",
                        "大类内本省排名百分比",
                        "是否本省本大类排名第1或前15%",
                        "语文",
                        "数学",
                        "外语",
                        "综合",
                        "物理",
                        "化学",
                        "生物",
                        "政治",
                        "历史",
                        "地理",
                        "毕业鉴定",
                        "考生特征"),
                "联系方式",List.of("联系电话",
                        "邮箱",
                        "火车到达站",
                        "QQ/微信号",
                        "邮政编码",
                        "家庭电话",
                        "通讯地址",
                        "家庭地址")
        );
        List<List<String>> keys = List.of(List.of("studentNumber",
                        "basicName",
                        "basicEngName",
                        "basicNameSpell",
                        "basicChName",
                        "basicOnceName",
                        "basicNationalityNAME",
                        "basicIdentityTypeNAME",
                        "basicIdentityNumber",
                        "basicOnceIdentityNAME",
                        "basicOnceDocumentCode",
                        "basicSexName",
                        "basicBirthday",
                        "basicMarriageNAME",
                        "basicHealthNAME",
                        "basicBeliefNAME",
                        "basicBloodNAME",
                        "basicIdentityValidity",
                        "basicBirthplaceNAME",
                        "basicNationNAME",
                        "basicPoliticsNAME",
                        "basicNativeNAME",
                        "basicOverseasChNAME",
                        "basicHobby",
                        "basicHongKongPassCheck",
                        "basicExaNumber"),
                List.of(
                        "rollCollegeNumNAME",
                        "rollDepartmentNAME",
                        "rollGrade",
                        "rollGradeDirectionNAME",
                        "rollCampusNAME",
                        "rollGradeBroadNAME",
                        "rollBroadNAME",
                        "rollmajorNAME",
                        "rollStandardNAME",
                        "rollFacultyName",
                        "rollEdusys",
                        "rollStuTypeName",
                        "rollStuSubcategory",
                        "rollStuDegcategory",
                        "rollWhetherCreditShow",
                        "rollAffirmShow",
                        "shortest",
                        "longtest",
                        "rollClassNAME",
                        "rollStateNAME",
                        "rollWhetherSchShow",
                        "rollShapeNAME",
                        "rollGradationNAME",
                        "rollWayNAME",
                        "rollEnterWayName",
                        "rollEnterSchDate",
                        "rollPredGradDate",
                        "rollChargeGrade",
                        "gradDate",
                        "gradDegreeName",
                        "gradDetailCertAwardTime",
                        "gradCertNum",
                        "gradPrincipal",
                        "gradDetailDegreeAwardDate",
                        "gradDegreeNum",
                        "generalProvinceRank",
                        "basicOverseasTypeNAME",
                        "basicOverseasCostNAME",
                        "basicCiscode",
                        "basicLanguageNAME",
                        "origins",
                        "originExamType",
                        "originGradType",
                        "originHighSchName",
                        "originExam",
                        "fileGrade",
                        "generalProvinceEnrollNum",
                        "generalProvinceRank",
                        "generalProvinceRankPer",
                        "originChPer",
                        "originMathPer",
                        "originEnglishPer",
                        "originSynthePer",
                        "originPhysicsPer",
                        "originChemistryPer",
                        "originBiologyPer",
                        "originPoliticsPer",
                        "originHistoryPer",
                        "originGeographyPer",
                        "originGradAuthen",
                        "originStuTrait"
                ), List.of(
                        "contaPhone",
                        "contaLetter",
                        "contaArrive",
                        "contaWeChat",
                        "contaPostalCode",
                        "contaFaPhone",
                        "contaEailAddress",
                        "contaFaAddress"
                ));
        ActivityResultLauncher<Intent> launch = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
            if (o.getResultCode() == Activity.RESULT_OK) {
                cookie = getSharedPreferences("privacy", Context.MODE_PRIVATE).getString("Cookie", "");
                getData();
            }
        });
        binding.tool.setNavigationOnClickListener(v->supportFinishAfterTransition());
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                JSONObject response = JSONObject.parseObject((String) msg.obj);
                if (response != null && response.getInteger("code").equals(200)) {
                    switch (msg.what) {
                        case -1:
                            Toast.makeText(SchoolRoll.this, getString(R.string.no_wifi_warning), Toast.LENGTH_LONG).show();
                            break;
                        case 0:
                            JSONObject d = response.getJSONObject("data");
                            if(d!=null){
                                data.forEach((a,b)->{
                                    ArrayList<String>values = new ArrayList<>();
                                    keys.get(List.of("个人基本信息","学籍信息","联系方式").indexOf(a)).forEach(c->{
                                        values.add(d.getString(c));
                                    });
                                    CardItemBinding item = CardItemBinding.inflate(getLayoutInflater(),binding.container,false);
                                    item.title.setText(a);
                                    RecyclerView list = getRecyclerView(b, values);
                                    item.card.addView(list);
                                    binding.container.addView(item.getRoot());
                                });
                            }
                            break;
                    }
                }else{
                    Toast.makeText(SchoolRoll.this, getString(R.string.login_warning), Toast.LENGTH_LONG).show();
                    launch.launch(new Intent(SchoolRoll.this, LoginActivity.class));
                }
            }

            @NonNull
            private RecyclerView getRecyclerView(List<String> b, List<String> values) {
                RecyclerView list = new RecyclerView(SchoolRoll.this);
                list.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                list.setLayoutManager(new LinearLayoutManager(SchoolRoll.this,LinearLayoutManager.VERTICAL,false));
                list.setNestedScrollingEnabled(false);
                ColumnAdp adp = new ColumnAdp(SchoolRoll.this, b, values);
                list.setAdapter(adp);
                return list;
            }
        };
        getData();
    }
    void getData(){
        http.newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/student-status/countrystu/studentRollView")
                .header("Cookie",cookie)
                .header("Referer","https://jwxt.sysu.edu.cn/jwxt/mk/studentWeb/")
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = new Message();
                msg.what=-1;
                handler.sendMessage(msg);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what=0;
                msg.obj=response.body().string();
                handler.sendMessage(msg);
            }
        });
    }
}
class ColumnAdp extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    List<String> value;
    Context context;
    List<String> key;
    public ColumnAdp(Context context,List<String> data,List<String> value){
        super();
        this.key = data;
        this.value = value;
        this.context=context;
    }
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TwoColumnBinding b = TwoColumnBinding.inflate(LayoutInflater.from(context),parent,false);
        b.item.setOnClickListener(view -> {

        });
        return new RecyclerView.ViewHolder(b.getRoot()) {
        };
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((TextView)holder.itemView.findViewById(R.id.key)).setText(key.get(position));
        holder.itemView.setOnClickListener(v->{
            ClipboardManager clip = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            clip.setPrimaryClip(ClipData.newPlainText(key.get(position),value.get(position)));
        });
        if (position< value.size()) {
            ((TextView)holder.itemView.findViewById(R.id.value)).setText(value.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return key.size();
    }
}
