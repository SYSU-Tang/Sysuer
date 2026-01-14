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

public class SchoolEnrollmentActivity extends AppCompatActivity {

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
                getString(R.string.school_enrollment_personal_info), List.of(
                        getString(R.string.school_enrollment_student_number),
                        getString(R.string.school_enrollment_name),
                        getString(R.string.school_enrollment_english_name),
                        getString(R.string.school_enrollment_name_pinyin),
                        getString(R.string.school_enrollment_chinese_name),
                        getString(R.string.school_enrollment_former_name),
                        getString(R.string.school_enrollment_country),
                        getString(R.string.school_enrollment_id_type),
                        getString(R.string.school_enrollment_id_number),
                        getString(R.string.school_enrollment_former_id_type),
                        getString(R.string.school_enrollment_former_id_number),
                        getString(R.string.school_enrollment_gender),
                        getString(R.string.school_enrollment_birthday),
                        getString(R.string.school_enrollment_marital_status),
                        getString(R.string.school_enrollment_health_status),
                        getString(R.string.school_enrollment_religion),
                        getString(R.string.school_enrollment_blood_type),
                        getString(R.string.school_enrollment_id_validity),
                        getString(R.string.school_enrollment_birthplace),
                        getString(R.string.school_enrollment_ethnicity),
                        getString(R.string.school_enrollment_political_status),
                        getString(R.string.school_enrollment_hometown),
                        getString(R.string.school_enrollment_hk_macao_taiwan),
                        getString(R.string.school_enrollment_hobby),
                        getString(R.string.school_enrollment_hk_passport),
                        getString(R.string.school_enrollment_exam_number)
                ),
                getString(R.string.school_enrollment_roll_info), List.of(
                        getString(R.string.school_enrollment_college),
                        getString(R.string.school_enrollment_department),
                        getString(R.string.school_enrollment_grade),
                        getString(R.string.school_enrollment_grade_direction),
                        getString(R.string.school_enrollment_campus),
                        getString(R.string.school_enrollment_grade_category),
                        getString(R.string.school_enrollment_major_category),
                        getString(R.string.school_enrollment_major_direction),
                        getString(R.string.school_enrollment_standard_major),
                        getString(R.string.school_enrollment_cross_college),
                        getString(R.string.school_enrollment_education_system),
                        getString(R.string.school_enrollment_student_type),
                        getString(R.string.school_enrollment_discipline),
                        getString(R.string.school_enrollment_degree_type),
                        getString(R.string.school_enrollment_credit_system),
                        getString(R.string.school_enrollment_need_confirm),
                        getString(R.string.school_enrollment_min_study_years),
                        getString(R.string.school_enrollment_max_study_years),
                        getString(R.string.school_enrollment_class),
                        getString(R.string.school_enrollment_status),
                        getString(R.string.school_enrollment_in_school),
                        getString(R.string.school_enrollment_study_form),
                        getString(R.string.school_enrollment_education_level),
                        getString(R.string.school_enrollment_training_method),
                        getString(R.string.school_enrollment_admission_method),
                        getString(R.string.school_enrollment_admission_date),
                        getString(R.string.school_enrollment_expected_graduation),
                        getString(R.string.school_enrollment_charge_grade),
                        getString(R.string.school_enrollment_graduation_date),
                        getString(R.string.school_enrollment_degree_category),
                        getString(R.string.school_enrollment_certificate_date),
                        getString(R.string.school_enrollment_certificate_number),
                        getString(R.string.school_enrollment_principal),
                        getString(R.string.school_enrollment_degree_date),
                        getString(R.string.school_enrollment_degree_number),
                        getString(R.string.school_enrollment_international_type),
                        getString(R.string.school_enrollment_funding_source),
                        getString(R.string.school_enrollment_csc_number),
                        getString(R.string.school_enrollment_teaching_language),
                        getString(R.string.school_enrollment_origin),
                        getString(R.string.school_enrollment_exam_type),
                        getString(R.string.school_enrollment_graduation_type),
                        getString(R.string.school_enrollment_high_school),
                        getString(R.string.school_enrollment_gaokao_score),
                        getString(R.string.school_enrollment_admission_score),
                        getString(R.string.school_enrollment_province_enroll),
                        getString(R.string.school_enrollment_province_rank),
                        getString(R.string.school_enrollment_province_rank_percent),
                        getString(R.string.school_enrollment_top_rank),
                        getString(R.string.school_enrollment_chinese),
                        getString(R.string.school_enrollment_math),
                        getString(R.string.school_enrollment_english),
                        getString(R.string.school_enrollment_comprehensive),
                        getString(R.string.school_enrollment_physics),
                        getString(R.string.school_enrollment_chemistry),
                        getString(R.string.school_enrollment_biology),
                        getString(R.string.school_enrollment_politics),
                        getString(R.string.school_enrollment_history),
                        getString(R.string.school_enrollment_geography),
                        getString(R.string.school_enrollment_graduation_evaluation),
                        getString(R.string.school_enrollment_exam_characteristics)
                ),
                getString(R.string.school_enrollment_contact_info), List.of(
                        getString(R.string.school_enrollment_phone),
                        getString(R.string.school_enrollment_email),
                        getString(R.string.school_enrollment_train_station),
                        getString(R.string.school_enrollment_qq_wechat),
                        getString(R.string.school_enrollment_postal_code),
                        getString(R.string.school_enrollment_home_phone),
                        getString(R.string.school_enrollment_address),
                        getString(R.string.school_enrollment_home_address)
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
                getString(R.string.school_enrollment_basic_info),
                getString(R.string.school_enrollment_family_info),
                getString(R.string.school_enrollment_education_info),
                getString(R.string.school_enrollment_exchange_info),
                getString(R.string.school_enrollment_change_info),
                getString(R.string.school_enrollment_major_info),
                getString(R.string.school_enrollment_register_info),
                getString(R.string.school_enrollment_punish_info)
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
                                            getString(R.string.school_enrollment_personal_info),
                                            getString(R.string.school_enrollment_roll_info),
                                            getString(R.string.school_enrollment_contact_info)
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
                                            {getString(R.string.school_enrollment_relation),
                                                    getString(R.string.school_enrollment_family_name),
                                                    getString(R.string.school_enrollment_work_unit),
                                                    getString(R.string.school_enrollment_position),
                                                    getString(R.string.school_enrollment_family_phone),
                                                    getString(R.string.school_enrollment_family_birthday)},
                                            {getString(R.string.school_enrollment_study_start),
                                                    getString(R.string.school_enrollment_study_end),
                                                    getString(R.string.school_enrollment_study_unit),
                                                    getString(R.string.school_enrollment_study_address)},
                                            {getString(R.string.school_enrollment_exchange_start),
                                                    getString(R.string.school_enrollment_exchange_end),
                                                    getString(R.string.school_enrollment_sent_school),
                                                    getString(R.string.school_enrollment_sent_major),
                                                    getString(R.string.school_enrollment_exchange_status)},
                                            {getString(R.string.school_enrollment_issue_date),
                                                    getString(R.string.school_enrollment_issue_number),
                                                    getString(R.string.school_enrollment_move_type),
                                                    getString(R.string.school_enrollment_change_detail),
                                                    getString(R.string.school_enrollment_move_reason),
                                                    getString(R.string.school_enrollment_former_major),
                                                    getString(R.string.school_enrollment_after_major)},
                                            {getString(R.string.school_enrollment_minor_type),
                                                    getString(R.string.school_enrollment_minor_college),
                                                    getString(R.string.school_enrollment_minor_major),
                                                    getString(R.string.school_enrollment_minor_grade),
                                                    getString(R.string.school_enrollment_minor_graduation)},
                                            {getString(R.string.school_enrollment_academic_year),
                                                    getString(R.string.school_enrollment_checkin_status),
                                                    getString(R.string.school_enrollment_register_status),
                                                    getString(R.string.school_enrollment_payment_status)},
                                            {getString(R.string.school_enrollment_punish_date),
                                                    getString(R.string.school_enrollment_punish_brief),
                                                    getString(R.string.school_enrollment_punish_type),
                                                    getString(R.string.school_enrollment_punish_source),
                                                    getString(R.string.school_enrollment_punish_name),
                                                    getString(R.string.school_enrollment_punish_reason),
                                                    getString(R.string.school_enrollment_punish_time),
                                                    getString(R.string.school_enrollment_punish_proof),
                                                    getString(R.string.school_enrollment_punish_repeal_time),
                                                    getString(R.string.school_enrollment_punish_repeal_proof),
                                                    getString(R.string.school_enrollment_punish_graduate),
                                                    getString(R.string.school_enrollment_punish_degree),
                                                    getString(R.string.school_enrollment_punish_sponsor),
                                                    getString(R.string.school_enrollment_punish_department),
                                                    getString(R.string.school_enrollment_punish_clause),
                                                    getString(R.string.school_enrollment_punish_money),
                                                    getString(R.string.school_enrollment_punish_status),
                                                    getString(R.string.school_enrollment_punish_in_school)}
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
                                    ((StaggeredFragment) pager2Adapter.getItem(msg.what)).add(SchoolEnrollmentActivity.this, String.valueOf(order), List.of(keyName), values);
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

