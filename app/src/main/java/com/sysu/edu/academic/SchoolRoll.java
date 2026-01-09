package com.sysu.edu.academic;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.tabs.TabLayoutMediator;
import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.ActivityPagerBinding;

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

    final OkHttpClient http = new OkHttpClient.Builder().build();
    ActivityPagerBinding binding;
    Map<String, List<String>> data;
    String cookie;
    int order = 0;
    Handler handler;
    Pager2Adapter pager2Adapter;
    int page = 1;
    Params params;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPagerBinding.inflate(getLayoutInflater());
        params = new Params(this);
        params.setCallback(() -> {
            cookie = params.getCookie();
            addNextPage(0);
        });
        cookie = params.getCookie();
        setContentView(binding.getRoot());

        data = Map.of(
                getString(R.string.schoolroll_personal_info), List.of(
                        getString(R.string.schoolroll_student_number),
                        getString(R.string.schoolroll_name),
                        getString(R.string.schoolroll_english_name),
                        getString(R.string.schoolroll_name_pinyin),
                        getString(R.string.schoolroll_chinese_name),
                        getString(R.string.schoolroll_former_name),
                        getString(R.string.schoolroll_country),
                        getString(R.string.schoolroll_id_type),
                        getString(R.string.schoolroll_id_number),
                        getString(R.string.schoolroll_former_id_type),
                        getString(R.string.schoolroll_former_id_number),
                        getString(R.string.schoolroll_gender),
                        getString(R.string.schoolroll_birthday),
                        getString(R.string.schoolroll_marital_status),
                        getString(R.string.schoolroll_health_status),
                        getString(R.string.schoolroll_religion),
                        getString(R.string.schoolroll_blood_type),
                        getString(R.string.schoolroll_id_validity),
                        getString(R.string.schoolroll_birthplace),
                        getString(R.string.schoolroll_ethnicity),
                        getString(R.string.schoolroll_political_status),
                        getString(R.string.schoolroll_hometown),
                        getString(R.string.schoolroll_hk_macao_taiwan),
                        getString(R.string.schoolroll_hobby),
                        getString(R.string.schoolroll_hk_passport),
                        getString(R.string.schoolroll_exam_number)
                ),
                getString(R.string.schoolroll_roll_info), List.of(
                        getString(R.string.schoolroll_college),
                        getString(R.string.schoolroll_department),
                        getString(R.string.schoolroll_grade),
                        getString(R.string.schoolroll_grade_direction),
                        getString(R.string.schoolroll_campus),
                        getString(R.string.schoolroll_grade_category),
                        getString(R.string.schoolroll_major_category),
                        getString(R.string.schoolroll_major_direction),
                        getString(R.string.schoolroll_standard_major),
                        getString(R.string.schoolroll_cross_college),
                        getString(R.string.schoolroll_education_system),
                        getString(R.string.schoolroll_student_type),
                        getString(R.string.schoolroll_discipline),
                        getString(R.string.schoolroll_degree_type),
                        getString(R.string.schoolroll_credit_system),
                        getString(R.string.schoolroll_need_confirm),
                        getString(R.string.schoolroll_min_study_years),
                        getString(R.string.schoolroll_max_study_years),
                        getString(R.string.schoolroll_class),
                        getString(R.string.schoolroll_status),
                        getString(R.string.schoolroll_in_school),
                        getString(R.string.schoolroll_study_form),
                        getString(R.string.schoolroll_education_level),
                        getString(R.string.schoolroll_training_method),
                        getString(R.string.schoolroll_admission_method),
                        getString(R.string.schoolroll_admission_date),
                        getString(R.string.schoolroll_expected_graduation),
                        getString(R.string.schoolroll_charge_grade),
                        getString(R.string.schoolroll_graduation_date),
                        getString(R.string.schoolroll_degree_category),
                        getString(R.string.schoolroll_certificate_date),
                        getString(R.string.schoolroll_certificate_number),
                        getString(R.string.schoolroll_principal),
                        getString(R.string.schoolroll_degree_date),
                        getString(R.string.schoolroll_degree_number),
                        getString(R.string.schoolroll_international_type),
                        getString(R.string.schoolroll_funding_source),
                        getString(R.string.schoolroll_csc_number),
                        getString(R.string.schoolroll_teaching_language),
                        getString(R.string.schoolroll_origin),
                        getString(R.string.schoolroll_exam_type),
                        getString(R.string.schoolroll_graduation_type),
                        getString(R.string.schoolroll_high_school),
                        getString(R.string.schoolroll_gaokao_score),
                        getString(R.string.schoolroll_admission_score),
                        getString(R.string.schoolroll_province_enroll),
                        getString(R.string.schoolroll_province_rank),
                        getString(R.string.schoolroll_province_rank_percent),
                        getString(R.string.schoolroll_top_rank),
                        getString(R.string.schoolroll_chinese),
                        getString(R.string.schoolroll_math),
                        getString(R.string.schoolroll_english),
                        getString(R.string.schoolroll_comprehensive),
                        getString(R.string.schoolroll_physics),
                        getString(R.string.schoolroll_chemistry),
                        getString(R.string.schoolroll_biology),
                        getString(R.string.schoolroll_politics),
                        getString(R.string.schoolroll_history),
                        getString(R.string.schoolroll_geography),
                        getString(R.string.schoolroll_graduation_evaluation),
                        getString(R.string.schoolroll_exam_characteristics)
                ),
                getString(R.string.schoolroll_contact_info), List.of(
                        getString(R.string.schoolroll_phone),
                        getString(R.string.schoolroll_email),
                        getString(R.string.schoolroll_train_station),
                        getString(R.string.schoolroll_qq_wechat),
                        getString(R.string.schoolroll_postal_code),
                        getString(R.string.schoolroll_home_phone),
                        getString(R.string.schoolroll_address),
                        getString(R.string.schoolroll_home_address)
                )
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
        pager2Adapter = new Pager2Adapter(this);
        binding.pager.setAdapter(pager2Adapter);
        binding.toolbar.setTitle(R.string.school_enroll);
        new TabLayoutMediator(binding.tabs, binding.pager, (tab, position) -> tab.setText(new String[]{
                getString(R.string.schoolroll_basic_info),
                getString(R.string.schoolroll_family_info),
                getString(R.string.schoolroll_education_info),
                getString(R.string.schoolroll_exchange_info),
                getString(R.string.schoolroll_change_info),
                getString(R.string.schoolroll_major_info),
                getString(R.string.schoolroll_register_info),
                getString(R.string.schoolroll_punish_info)
        }[position])).attach();
        binding.toolbar.setNavigationOnClickListener(v -> supportFinishAfterTransition());
        handler = new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {

                if (msg.what == -1) {
                    params.toast(R.string.no_wifi_warning);
                } else {
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    if (response != null && response.getInteger("code").equals(200)) {
                        JSONObject d = response.getJSONObject("data");
                        if (d != null) {
                            if (msg.what == 0) {
                                data.forEach((title, keyName) -> {
                                    ArrayList<String> values = new ArrayList<>();
                                    int key = List.of(
                                            getString(R.string.schoolroll_personal_info),
                                            getString(R.string.schoolroll_roll_info),
                                            getString(R.string.schoolroll_contact_info)
                                    ).indexOf(title);
                                    keys.get(key).forEach(c -> values.add(d.getString(c)));
                                    ((StaggeredFragment) pager2Adapter.getItem(0)).add(title, null, keyName, values);
                                });
                                addNextPage(msg.what + 1);
                            } else {
                                int total = d.getInteger("total");
                                d.getJSONArray("rows").forEach(a -> {
                                    order++;
                                    ArrayList<String> values = new ArrayList<>();
                                    String[] keyName = new String[][]{
                                            {getString(R.string.schoolroll_relation),
                                                    getString(R.string.schoolroll_family_name),
                                                    getString(R.string.schoolroll_work_unit),
                                                    getString(R.string.schoolroll_position),
                                                    getString(R.string.schoolroll_family_phone),
                                                    getString(R.string.schoolroll_family_birthday)},
                                            {getString(R.string.schoolroll_study_start),
                                                    getString(R.string.schoolroll_study_end),
                                                    getString(R.string.schoolroll_study_unit),
                                                    getString(R.string.schoolroll_study_address)},
                                            {getString(R.string.schoolroll_exchange_start),
                                                    getString(R.string.schoolroll_exchange_end),
                                                    getString(R.string.schoolroll_sent_school),
                                                    getString(R.string.schoolroll_sent_major),
                                                    getString(R.string.schoolroll_exchange_status)},
                                            {getString(R.string.schoolroll_issue_date),
                                                    getString(R.string.schoolroll_issue_number),
                                                    getString(R.string.schoolroll_move_type),
                                                    getString(R.string.schoolroll_change_detail),
                                                    getString(R.string.schoolroll_move_reason),
                                                    getString(R.string.schoolroll_former_major),
                                                    getString(R.string.schoolroll_after_major)},
                                            {getString(R.string.schoolroll_minor_type),
                                                    getString(R.string.schoolroll_minor_college),
                                                    getString(R.string.schoolroll_minor_major),
                                                    getString(R.string.schoolroll_minor_grade),
                                                    getString(R.string.schoolroll_minor_graduation)},
                                            {getString(R.string.schoolroll_academic_year),
                                                    getString(R.string.schoolroll_checkin_status),
                                                    getString(R.string.schoolroll_register_status),
                                                    getString(R.string.schoolroll_payment_status)},
                                            {getString(R.string.schoolroll_punish_date),
                                                    getString(R.string.schoolroll_punish_brief),
                                                    getString(R.string.schoolroll_punish_type),
                                                    getString(R.string.schoolroll_punish_source),
                                                    getString(R.string.schoolroll_punish_name),
                                                    getString(R.string.schoolroll_punish_reason),
                                                    getString(R.string.schoolroll_punish_time),
                                                    getString(R.string.schoolroll_punish_proof),
                                                    getString(R.string.schoolroll_punish_repeal_time),
                                                    getString(R.string.schoolroll_punish_repeal_proof),
                                                    getString(R.string.schoolroll_punish_graduate),
                                                    getString(R.string.schoolroll_punish_degree),
                                                    getString(R.string.schoolroll_punish_sponsor),
                                                    getString(R.string.schoolroll_punish_department),
                                                    getString(R.string.schoolroll_punish_clause),
                                                    getString(R.string.schoolroll_punish_money),
                                                    getString(R.string.schoolroll_punish_status),
                                                    getString(R.string.schoolroll_punish_in_school)}
                                    }[msg.what - 1];
                                    for (int i = 0; i < keyName.length; i++) {
                                        values.add(((JSONObject) a).getString(new String[][]{
                                                {"familyRelationName", "familyMemberName", "familyWorkUnit", "jobName", "familyPhone", "familyBirthday"},
                                                {"experBeginTime", "experEndTime", "experStudyUnit", "experSite"},
                                                {"startTime", "endTime", "sendToCollegeName", "sentToMajorName", "exchangeStatus"},
                                                {"issueDate", "issueNumber", "moveStyle", "changeDetail", "moveReason", "formerGradeMajorProf", "moveAfterGradeMajorProf"},
                                                {"mrollCultureGenreName", "mrollCollegeName", "mrollMajorFieldName", "mrollGrade", "minDouDegMajGradName"},
                                                {"academicYearTerm", "checkInStatusName", "registerStatusName", "payedStatusName"},
                                                {"rewPundate", "rewPunBriefing", "rewPunTypeName", "rewPunSourceName", "rewPunName", "rewPunCause", "rewPunTime", "rewPunProof", "rewPunRepealTime", "rewPunRepealProof", "rewPunWheGraduate", "rewPunWheDegree", "rewPunSponDeparName", "rewPunDeparName", "rewPunAdapt", "rewPunMoney", "rewPunSchrollState", "rewPunWhetherAtsch"}
                                        }[msg.what - 1][i]));
                                    }
                                    ((StaggeredFragment) pager2Adapter.getItem(msg.what)).add(SchoolRoll.this, String.valueOf(order), List.of(keyName), values);
                                });
                                if (total / 10 > page - 1) {
                                    page++;
                                    getFamily();
                                } else {
                                    page = 1;
                                    order = 0;
                                    addNextPage(msg.what + 1);
                                }
                            }
                        }
                    } else {
                        params.toast(R.string.login_warning);
                        params.gotoLogin(binding.toolbar, TargetUrl.JWXT);
                    }
                }
            }

        };
        addNextPage(0);
    }

    // ... existing code ...


    void addNextPage(int what) {
        if (what >= 8 || pager2Adapter.getItemCount() > what) {
            return;
        }
        pager2Adapter.add(StaggeredFragment.newInstance(what));
        new Runnable[]{
                this::getData,
                this::getFamily,
                this::getExperience,
                this::getExchange,
                this::getChange,
                this::getMin,
                this::getRegister,
                this::getPunish
        }[what].run();
    }

    void getData() {
        http.newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/student-status/countrystu/studentRollView")
                .header("Cookie", cookie)
                .header("Referer", "https://jwxt.sysu.edu.cn/jwxt/mk/studentWeb/")
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
                msg.what = 0;
                msg.obj = response.body().string();
                handler.sendMessage(msg);
            }
        });
    }

    void getFamily() {
        getWithUrl("https://jwxt.sysu.edu.cn/jwxt/student-status/stuFamily/showStudentFamily", 1, page);
    }

    void getExperience() {
        getWithUrl("https://jwxt.sysu.edu.cn/jwxt/student-status/stuExperience/showStudentExperience", 2, page);
    }

    void getExchange() {
        getWithUrl("https://jwxt.sysu.edu.cn/jwxt/student-status/abroadInformation/myStulistInformation", 3, page);
    }

    void getChange() {
        getWithUrl("https://jwxt.sysu.edu.cn/jwxt/student-status-move/moveStuAgg/showStuChangeRoll", 4, page);
    }

    void getMin() {
        getWithUrl("https://jwxt.sysu.edu.cn/jwxt/minor-status/minDouDegMajRoll/queryMinDouDegMajRoll", 5, page);
    }

    void getRegister() {
        getWithUrl("https://jwxt.sysu.edu.cn/jwxt/reports-register/stuRegistration/getSelfRegisterList", 6, page);
    }

    void getPunish() {
        getWithUrl("https://jwxt.sysu.edu.cn/jwxt/student-status/stuRewPunish/showMyStudentRewPunish", 7, page);
    }

    void getWithUrl(String url, int code, int pageNum) {
        http.newCall(new Request.Builder().url(url)
                .header("Cookie", cookie)
                .post(RequestBody.create(String.format(Locale.getDefault(), "{\"pageNo\":%d,\"pageSize\":10,\"total\":true,\"param\":{}}", pageNum), MediaType.parse("application/json")))
                .header("Referer", "https://jwxt.sysu.edu.cn/jwxt/mk/studentWeb/")
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
                msg.what = code;
                msg.obj = response.body().string();
                handler.sendMessage(msg);
            }
        });
    }
}

