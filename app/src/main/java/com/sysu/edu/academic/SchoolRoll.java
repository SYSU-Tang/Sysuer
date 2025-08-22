package com.sysu.edu.academic;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.tabs.TabLayoutMediator;
import com.sysu.edu.R;
import com.sysu.edu.databinding.ActivitySchoolRollBinding;
import com.sysu.edu.extra.LoginActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SchoolRoll extends AppCompatActivity {

    ActivitySchoolRollBinding binding;
    Map<String, List<String>> data;
    String cookie;
    int order = 0;
    OkHttpClient http = new OkHttpClient.Builder().build();
    Handler handler;
    PagerAdapter pagerAdapter;
    int page=1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySchoolRollBinding.inflate(getLayoutInflater());
        cookie = getSharedPreferences("privacy", Context.MODE_PRIVATE).getString("Cookie", "");
        setContentView(binding.getRoot());
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
                "学籍信息", List.of("学院",
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
                "联系方式", List.of("联系电话",
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
                getNextPage(0);
            }
        });
        pagerAdapter = new PagerAdapter(this);
        binding.pager.setAdapter(pagerAdapter);
        new TabLayoutMediator(binding.tabs, binding.pager, (tab, position) -> tab.setText(new String[]{"基本信息", "家庭成员及社会关系", "学历及经历", "交流经历", "异动情况","双专业双学位辅修", "注册状态", "惩处"}[position])).attach();
        binding.tool.setNavigationOnClickListener(v -> supportFinishAfterTransition());
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {

                if (msg.what == -1) {
                    Toast.makeText(SchoolRoll.this, getString(R.string.no_wifi_warning), Toast.LENGTH_LONG).show();
                }else {
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    if (response != null && response.getInteger("code").equals(200)) {
                        JSONObject d = response.getJSONObject("data");
                        if (d != null) {
                            if (msg.what == 0) {
                                data.forEach((title, keyName) -> {
                                    ArrayList<String> values = new ArrayList<>();
                                    int key = List.of("个人基本信息", "学籍信息", "联系方式").indexOf(title);
                                    keys.get(key).forEach(c -> values.add(d.getString(c)));
                                    ((StaggeredFragment) pagerAdapter.getItem(0)).add(title,null, keyName, values);
                                });
                                getNextPage(msg.what+1);
                            } else {
                                int total=d.getInteger("total");
                                d.getJSONArray("rows").forEach(a->{
                                    order++;
                                    ArrayList<String> values = new ArrayList<>();
                                    String[] keyName = new String[][]{
                                            {"称谓","姓名","工作单位","职务","联系电话","出生日期"},
                                            {"学习起始日期","学习终止日期","学习单位","学习地址"},
                                            {"学习起始日期","学习终止日期","派往学校","派往专业","交流状态"},
                                            {"发文日期","文号","异动类别","异动细类","异动原因","异动前学院专业","异动后学院专业"},
                                            {"辅修类别","学院","专业方向","年级","毕业结论",},
                                            {"学年学期","报到状态","注册状态","缴费状态"},
                                            {"违纪日期","违纪简况","违纪类别","处分来源","处分名称","处分原因","处分日期","处分文号","处分撤销日期","处分撤销文号","是否按时毕业","是否获得学位","处分发起单位","处分单位","适用条款","处罚金额","学籍状态","是否在校"}
                                    }[msg.what-1];
                                    for(int i=0;i<keyName.length;i++){
                                        values.add(((JSONObject)a).getString(new String[][]{
                                                {"familyRelationName","familyMemberName","familyWorkUnit","jobName","familyPhone","familyBirthday"},
                                                {"experBeginTime","experEndTime","experStudyUnit","experSite"},
                                                {"startTime","endTime","sendToCollegeName","sentToMajorName","exchangeStatus"},
                                                {"issueDate","issueNumber","moveStyle","changeDetail","moveReason","formerGradeMajorProf","moveAfterGradeMajorProf"},
                                                {"mrollCultureGenreName","mrollCollegeName","mrollMajorFieldName","mrollGrade","minDouDegMajGradName"},
                                                {"academicYearTerm","checkInStatusName","registerStatusName","payedStatusName"},
                                                {"rewPundate","rewPunBriefing","rewPunTypeName","rewPunSourceName","rewPunName","rewPunCause","rewPunTime","rewPunProof","rewPunRepealTime","rewPunRepealProof","rewPunWheGraduate","rewPunWheDegree","rewPunSponDeparName","rewPunDeparName","rewPunAdapt","rewPunMoney","rewPunSchrollState","rewPunWhetherAtsch"}
                                        }[msg.what-1][i]));
                                    }
                                    ((StaggeredFragment) pagerAdapter.getItem(msg.what)).add(SchoolRoll.this,String.valueOf(order), List.of(keyName), values);
                                });
                                if(total/10>page-1){
                                    page++;
                                    getFamily();
                                }else{
                                    page=1;
                                    order=0;
                                    getNextPage(msg.what+1);
                                }
                            }
                        }
                    }
                    else {
                        Toast.makeText(SchoolRoll.this, getString(R.string.login_warning), Toast.LENGTH_LONG).show();
                        launch.launch(new Intent(SchoolRoll.this, LoginActivity.class));
                    }
                }
            }

        };
        getNextPage(0);
        //}
    }
    void getNextPage(int what){
        if(what<8){
            pagerAdapter.add(StaggeredFragment.newInstance(what));
        }
        switch (what){
            case 0:
                getData();
                break;
            case 1:
                getFamily();
                break;
            case 2:
                getExperience();
                break;
            case 3:
                getExchange();
                break;
            case 4:
                getChange();
                break;
            case 5:
                getMin();
                break;
            case 6:
                getRegister();
                break;
            case 7:
                getPunish();
                break;
        }
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
    void getFamily(){
        getWithUrl("https://jwxt.sysu.edu.cn/jwxt/student-status/stuFamily/showStudentFamily",1,page);
    }
    void getExperience(){
        getWithUrl("https://jwxt.sysu.edu.cn/jwxt/student-status/stuExperience/showStudentExperience",2,page);
    }
    void getExchange(){
        getWithUrl("https://jwxt.sysu.edu.cn/jwxt/student-status/abroadInformation/myStulistInformation",3,page);
    }
    void getChange(){
        getWithUrl("https://jwxt.sysu.edu.cn/jwxt/student-status-move/moveStuAgg/showStuChangeRoll",4,page);
    }
    void getMin(){
        getWithUrl("https://jwxt.sysu.edu.cn/jwxt/minor-status/minDouDegMajRoll/queryMinDouDegMajRoll",5,page);
    }
    void getRegister(){
        getWithUrl("https://jwxt.sysu.edu.cn/jwxt/reports-register/stuRegistration/getSelfRegisterList",6,page);
    }
    void getPunish(){
        getWithUrl("https://jwxt.sysu.edu.cn/jwxt/student-status/stuRewPunish/showMyStudentRewPunish",7,page);
    }
    void getWithUrl(String url,int code,int pageNum){
        http.newCall(new Request.Builder().url(url)
                .header("Cookie",cookie)
                .post(RequestBody.create(String.format(Locale.CHINA,"{\"pageNo\":%d,\"pageSize\":10,\"total\":true,\"param\":{}}",pageNum), MediaType.parse("application/json")))
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
                msg.what=code;
                msg.obj=response.body().string();
                handler.sendMessage(msg);
            }
        });
    }
}

class PagerAdapter extends FragmentStateAdapter{

    ArrayList<Fragment> fragments=new ArrayList<>();

    public PagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragments.get(position);
    }

    public PagerAdapter add(Fragment e){
        fragments.add(e);
        notifyItemInserted(getItemCount());
        return this;
    }
    public Fragment getItem(int position){
        return fragments.get(position);
    }
    @Override
    public int getItemCount() {
        return fragments.size();
    }
}
