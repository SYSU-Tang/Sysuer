package com.sysu.edu.academic;

import android.content.Context;

public class FilterWidget {

    static final int FILTER_TYPE_SINGLE_CHOICE_SWITCH = 0;
    static final int FILTER_TYPE_SINGLE_CHOICE_TEXT_INPUT = 1;
    static final int FILTER_TYPE_SINGLE_CHOICE_RADIO = 2;
    static final int FILTER_TYPE_SINGLE_CHOICE_SLIDER = 3;
    static final int FILTER_TYPE_MULTI_CHOICE_CHIP = 10;
    static final int FILTER_TYPE_MULTI_CHOICE_MENU = 11;
    static final int FILTER_TYPE_MULTI_CHOICE_CHECKBOX = 13;
    static final int FILTER_TYPE_MULTI_CHOICE_SLIDER = 14;
    final Context context;

    public FilterWidget(Context context) {
        this.context = context;
    }

    public void setKey(String key) {
        // 键
    }
    public void setName(String name) {
        // 名称
    }

    public void setDefaultValue(String defaultValue) {
        // 默认值
    }

    public void setFilterType(int filterType) {
        switch (filterType) {
            case 0:

                // 单选（开关）
                break;
            case 1:
                // 填空
                break;
            case 2:
                // 单选（单选按钮）
                break;
            case 3:
                // 滑动条
                break;
            case 10:
                // 多选（芯片）
                break;
            case 11:
                // 多选（菜单）
                break;
            case 12:
                // 多选（单选）
                break;
            case 13:
                // 多选（复选框）
                break;
            case 14:
                // 多选（滑动条）
                break;
            default:
                break;
        }
    }
}
