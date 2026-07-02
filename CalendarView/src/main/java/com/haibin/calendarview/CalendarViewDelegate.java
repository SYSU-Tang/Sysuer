/*
 * Copyright (C) 2016 huanghaibin_dev <huanghaibin_dev@163.com>
 * WebSite https://github.com/MiracleTimes-Dev
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.haibin.calendarview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Google规范化的属性委托,
 * 代码量多，但是不影响阅读性
 */
public final class CalendarViewDelegate {

    /**
     * 周起始：周日
     */
    static final int WEEK_START_WITH_SUN = 1;

    /**
     * 周起始：周一
     */
    static final int WEEK_START_WITH_MON = 2;

    /**
     * 周起始：周六
     */
    static final int WEEK_START_WITH_SAT = 7;

    /**
     * 默认选择日期1号first_day_of_month
     */
    static final int FIRST_DAY_OF_MONTH = 0;

    /**
     * 跟随上个月last_select_day
     */
    static final int LAST_MONTH_VIEW_SELECT_DAY = 1;

    /**
     * 跟随上个月last_select_day_ignore_current忽视今天
     */
    static final int LAST_MONTH_VIEW_SELECT_DAY_IGNORE_CURRENT = 2;
    /**
     * 全部显示
     */
    static final int MODE_ALL_MONTH = 0;
    /**
     * 仅显示当前月份
     */
    static final int MODE_ONLY_CURRENT_MONTH = 1;
    /**
     * 自适应显示，不会多出一行，但是会自动填充
     */
    static final int MODE_FIT_MONTH = 2;
    /**
     * 默认选择模式
     */
    static final int SELECT_MODE_DEFAULT = 0;
    /**
     * 单选模式
     */
    static final int SELECT_MODE_SINGLE = 1;
    /**
     * 范围选择模式
     */
    static final int SELECT_MODE_RANGE = 2;
    /**
     * 多选模式
     */
    static final int SELECT_MODE_MULTI = 3;
    /**
     * 支持转换的最小农历年份
     */
    static final int MIN_YEAR = 1900;
    /**
     * 支持转换的最大农历年份
     */
    private static final int MAX_YEAR = 2099;
    /**
     * 年月视图是否打开
     */
    boolean isShowYearSelectedLayout;
    /**
     * 当前月份和周视图的item位置
     */
    int mCurrentMonthViewItem;
    /**
     * 标记的日期,数量巨大，请使用这个
     */
    Map<String, Calendar> mSchemeDatesMap;
    /**
     * 点击Padding位置事件
     */
    CalendarView.OnClickCalendarPaddingListener mClickCalendarPaddingListener;
    /**
     * 日期拦截事件
     */
    CalendarView.OnCalendarInterceptListener mCalendarInterceptListener;
    /**
     * 日期选中监听
     */
    CalendarView.OnCalendarSelectListener mCalendarSelectListener;
    /**
     * 范围选择
     */
    CalendarView.OnCalendarRangeSelectListener mCalendarRangeSelectListener;
    /**
     * 多选选择事件
     */
    CalendarView.OnCalendarMultiSelectListener mCalendarMultiSelectListener;
    /**
     * 外部日期长按事件
     */
    CalendarView.OnCalendarLongClickListener mCalendarLongClickListener;
    /**
     * 内部日期切换监听，用于内部更新计算
     */
    CalendarView.OnInnerDateSelectedListener mInnerListener;
    /**
     * 快速年份切换
     */
    CalendarView.OnYearChangeListener mYearChangeListener;
    /**
     * 月份切换事件
     */
    CalendarView.OnMonthChangeListener mMonthChangeListener;
    /**
     * 周视图改变事件
     */
    CalendarView.OnWeekChangeListener mWeekChangeListener;
    /**
     * 视图改变事件
     */
    CalendarView.OnViewChangeListener mViewChangeListener;
    /**
     * 年视图改变事件
     */
    CalendarView.OnYearViewChangeListener mYearViewChangeListener;
    /**
     * 保存选中的日期
     */
    Calendar mSelectedCalendar;
    /**
     * 保存标记位置
     */
    Calendar mIndexCalendar;
    /**
     * 多选日历
     */
    final Map<String, Calendar> mSelectedCalendars = new HashMap<>();
    /**
     * 选择范围日历
     */
    Calendar mSelectedStartRangeCalendar, mSelectedEndRangeCalendar;
    private int mCurDayLunarTextColor;
//    private int mCurMonthLunarTextColor;
    private int mWeekTextColor;
    /**
     * 年视图一些padding
     */
    private int mYearViewPadding;
    /**
     * 年视图一些padding
     */
    private int mYearViewMonthPaddingLeft;
    private int mYearViewMonthPaddingRight;
    private int mYearViewMonthPaddingTop;
    private int mYearViewMonthPaddingBottom;
    /**
     * 年视图字体大小
     */
    private int mYearViewMonthTextSize;
    private int mYearViewDayTextSize;
    private int mYearViewWeekTextSize;
    /**
     * 年视图月份高度和周的高度
     */
    private int mYearViewMonthHeight;
    private int mYearViewWeekHeight;
    private int mYearViewSelectTextColor;
    private int mYearViewCurDayTextColor;
    private int mYearViewWeekTextColor;
    /**
     * 星期栏的背景、线的背景、年份背景
     */
    private int mWeekLineBackground;
    private int mYearViewBackground;
    private int mWeekBackground;
    /**
     * 星期栏Line margin
     */
    private int mWeekLineMargin;
    /**
     * 星期栏字体大小
     */
    private int mWeekTextSize;
    /**
     * 自定义的日历路径
     */
    private String mMonthViewClassPath;
    /**
     * 自定义周视图路径
     */
    private String mWeekViewClassPath;
    /**
     * 自定义年视图路径
     */
    private String mYearViewClassPath;
    /**
     * 自定义周栏路径
     */
    private String mWeekBarClassPath;
    /**
     * 日期和农历文本大小
     */
    private int mDayTextSize;
    private int mLunarTextSize;
    /**
     * 是否是全屏日历
     */
    private boolean isFullScreenCalendar;
    /**
     * 星期栏的高度
     */
    private int mWeekBarHeight;
    private int mDefaultCalendarSelectDay;
    /**
     * 周起始
     */
    private int mWeekStart;
    /**
     * 月份显示模式
     */
    private int mMonthViewShowMode;
    /**
     * 选择模式
     */
    private int mSelectMode;
    /**
     * 各种字体颜色，看名字知道对应的地方
     */
    private int mCurDayTextColor;
    private int mSchemeTextColor;
    private int mSchemeLunarTextColor;
    private int mOtherMonthTextColor;
    private int mCurrentMonthTextColor;
    private int mSelectedTextColor;
    private int mSelectedLunarTextColor;
    private int mCurMonthLunarTextColor;
    private int mOtherMonthLunarTextColor;
    private boolean preventLongPressedSelected;
    private int mYearViewPaddingLeft;
    private int mYearViewPaddingRight;
    /**
     * 日历内部左右padding
     */
    private int mCalendarPadding;
    /**
     * 日历内部左padding
     */
    private int mCalendarPaddingLeft;
    /**
     * 日历内部右padding
     */
    private int mCalendarPaddingRight;
    /**
     * 年视图字体和标记颜色
     */
    private int mYearViewMonthTextColor;
    private int mYearViewDayTextColor;
    private int mYearViewSchemeTextColor;
    /**
     * 标记的主题色和选中的主题色
     */
    private int mSchemeThemeColor, mSelectedThemeColor;
    /**
     * 月视图类
     */
    private Class<?> mMonthViewClass;
    /**
     * 周视图类
     */
    private Class<?> mWeekViewClass;
    /**
     * 周视图类
     */
    private Class<?> mYearViewClass;
    /**
     * 自定义周栏
     */
    private Class<?> mWeekBarClass;
    /**
     * 标记文本
     */
    private String mSchemeText;
    /**
     * 最小年份和最大年份
     */
    private int mMinYear, mMaxYear;
    /**
     * 最小年份和最大年份对应最小月份和最大月份
     * when you want set 2015-07 to 2017-08
     */
    private int mMinYearMonth, mMaxYearMonth;
    /**
     * 最小年份和最大年份对应最小天和最大天数
     * when you want set like 2015-07-08 to 2017-08-30
     */
    private int mMinYearDay, mMaxYearDay;
    /**
     * 日历卡的项高度
     */
    private int mCalendarItemHeight;
    /**
     * 今天的日子
     */
    private Calendar mCurrentDate;
    private boolean mMonthViewScrollable,
            mWeekViewScrollable,
            mYearViewScrollable;
    private int mMaxMultiSelectSize;
    private int mMinSelectRange, mMaxSelectRange;

    CalendarViewDelegate(Context context, @Nullable AttributeSet attrs) {
        try (TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.CalendarView)) {

            LunarCalendar.init(context);

            mCalendarPadding = (int) array.getDimension(R.styleable.CalendarView_calendar_padding, 0);
            mCalendarPaddingLeft = (int) array.getDimension(R.styleable.CalendarView_calendar_padding_left, 0);
            mCalendarPaddingRight = (int) array.getDimension(R.styleable.CalendarView_calendar_padding_right, 0);

            if (mCalendarPadding != 0) {
                mCalendarPaddingLeft = mCalendarPadding;
                mCalendarPaddingRight = mCalendarPadding;
            }

            mSchemeTextColor = array.getColor(R.styleable.CalendarView_scheme_text_color, 0xFFFFFFFF);
            mSchemeLunarTextColor = array.getColor(R.styleable.CalendarView_scheme_lunar_text_color, 0xFFe1e1e1);
            mSchemeThemeColor = array.getColor(R.styleable.CalendarView_scheme_theme_color, 0x50CFCFCF);
            mMonthViewClassPath = array.getString(R.styleable.CalendarView_month_view);
            mYearViewClassPath = array.getString(R.styleable.CalendarView_year_view);
            mWeekViewClassPath = array.getString(R.styleable.CalendarView_week_view);
            mWeekBarClassPath = array.getString(R.styleable.CalendarView_week_bar_view);
            mWeekTextSize = array.getDimensionPixelSize(R.styleable.CalendarView_week_text_size,
                    CalendarUtil.dipToPx(context, 12));
            mWeekBarHeight = (int) array.getDimension(R.styleable.CalendarView_week_bar_height,
                    CalendarUtil.dipToPx(context, 40));
            mWeekLineMargin = (int) array.getDimension(R.styleable.CalendarView_week_line_margin,
                    CalendarUtil.dipToPx(context, 0));

            mSchemeText = array.getString(R.styleable.CalendarView_scheme_text);
            if (TextUtils.isEmpty(mSchemeText)) {
                mSchemeText = "记";
            }

            mMonthViewScrollable = array.getBoolean(R.styleable.CalendarView_month_view_scrollable, true);
            mWeekViewScrollable = array.getBoolean(R.styleable.CalendarView_week_view_scrollable, true);
            mYearViewScrollable = array.getBoolean(R.styleable.CalendarView_year_view_scrollable, true);

            mDefaultCalendarSelectDay = array.getInt(R.styleable.CalendarView_month_view_auto_select_day,
                    FIRST_DAY_OF_MONTH);

            mMonthViewShowMode = array.getInt(R.styleable.CalendarView_month_view_show_mode, MODE_ALL_MONTH);
            mWeekStart = array.getInt(R.styleable.CalendarView_week_start_with, WEEK_START_WITH_SUN);
            mSelectMode = array.getInt(R.styleable.CalendarView_select_mode, SELECT_MODE_DEFAULT);
            mMaxMultiSelectSize = array.getInt(R.styleable.CalendarView_max_multi_select_size, Integer.MAX_VALUE);
            mMinSelectRange = array.getInt(R.styleable.CalendarView_min_select_range, -1);
            mMaxSelectRange = array.getInt(R.styleable.CalendarView_max_select_range, -1);
            setSelectRange(mMinSelectRange, mMaxSelectRange);

            mWeekBackground = array.getColor(R.styleable.CalendarView_week_background, Color.WHITE);
            mWeekLineBackground = array.getColor(R.styleable.CalendarView_week_line_background, Color.TRANSPARENT);
            mYearViewBackground = array.getColor(R.styleable.CalendarView_year_view_background, Color.WHITE);
            mWeekTextColor = array.getColor(R.styleable.CalendarView_week_text_color, 0xFF333333);

            mCurDayTextColor = array.getColor(R.styleable.CalendarView_current_day_text_color, Color.RED);
            mCurDayLunarTextColor = array.getColor(R.styleable.CalendarView_current_day_lunar_text_color, Color.RED);

            mSelectedThemeColor = array.getColor(R.styleable.CalendarView_selected_theme_color, 0x50CFCFCF);
            mSelectedTextColor = array.getColor(R.styleable.CalendarView_selected_text_color, 0xFF111111);

            mSelectedLunarTextColor = array.getColor(R.styleable.CalendarView_selected_lunar_text_color, 0xFF111111);
            mCurrentMonthTextColor = array.getColor(R.styleable.CalendarView_current_month_text_color, 0xFF111111);
            mOtherMonthTextColor = array.getColor(R.styleable.CalendarView_other_month_text_color, 0xFFe1e1e1);

            mCurMonthLunarTextColor = array.getColor(R.styleable.CalendarView_current_month_lunar_text_color, 0xffe1e1e1);
            mOtherMonthLunarTextColor = array.getColor(R.styleable.CalendarView_other_month_lunar_text_color, 0xffe1e1e1);
            mMinYear = array.getInt(R.styleable.CalendarView_min_year, 1971);
            mMaxYear = array.getInt(R.styleable.CalendarView_max_year, 2055);
            mMinYearMonth = array.getInt(R.styleable.CalendarView_min_year_month, 1);
            mMaxYearMonth = array.getInt(R.styleable.CalendarView_max_year_month, 12);
            mMinYearDay = array.getInt(R.styleable.CalendarView_min_year_day, 1);
            mMaxYearDay = array.getInt(R.styleable.CalendarView_max_year_day, -1);

            mDayTextSize = array.getDimensionPixelSize(R.styleable.CalendarView_day_text_size,
                    CalendarUtil.dipToPx(context, 16));
            mLunarTextSize = array.getDimensionPixelSize(R.styleable.CalendarView_lunar_text_size,
                    CalendarUtil.dipToPx(context, 10));
            mCalendarItemHeight = (int) array.getDimension(R.styleable.CalendarView_calendar_height,
                    CalendarUtil.dipToPx(context, 56));
            isFullScreenCalendar = array.getBoolean(R.styleable.CalendarView_calendar_match_parent, false);

            //年视图相关
            mYearViewMonthTextSize = array.getDimensionPixelSize(R.styleable.CalendarView_year_view_month_text_size,
                    CalendarUtil.dipToPx(context, 18));
            mYearViewDayTextSize = array.getDimensionPixelSize(R.styleable.CalendarView_year_view_day_text_size,
                    CalendarUtil.dipToPx(context, 7));
            mYearViewMonthTextColor = array.getColor(R.styleable.CalendarView_year_view_month_text_color, 0xFF111111);
            mYearViewDayTextColor = array.getColor(R.styleable.CalendarView_year_view_day_text_color, 0xFF111111);
            mYearViewSchemeTextColor = array.getColor(R.styleable.CalendarView_year_view_scheme_color, mSchemeThemeColor);
            mYearViewWeekTextColor = array.getColor(R.styleable.CalendarView_year_view_week_text_color, 0xFF333333);
            mYearViewCurDayTextColor = array.getColor(R.styleable.CalendarView_year_view_current_day_text_color, mCurDayTextColor);
            mYearViewSelectTextColor = array.getColor(R.styleable.CalendarView_year_view_select_text_color, 0xFF333333);
            mYearViewWeekTextSize = array.getDimensionPixelSize(R.styleable.CalendarView_year_view_week_text_size,
                    CalendarUtil.dipToPx(context, 8));
            mYearViewMonthHeight = array.getDimensionPixelSize(R.styleable.CalendarView_year_view_month_height,
                    CalendarUtil.dipToPx(context, 32));
            mYearViewWeekHeight = array.getDimensionPixelSize(R.styleable.CalendarView_year_view_week_height,
                    CalendarUtil.dipToPx(context, 0));

            mYearViewPadding = (int) array.getDimension(R.styleable.CalendarView_year_view_padding,
                    CalendarUtil.dipToPx(context, 12));
            mYearViewPaddingLeft = (int) array.getDimension(R.styleable.CalendarView_year_view_padding_left,
                    CalendarUtil.dipToPx(context, 12));
            mYearViewPaddingRight = (int) array.getDimension(R.styleable.CalendarView_year_view_padding_right,
                    CalendarUtil.dipToPx(context, 12));

            if (mYearViewPadding != 0) {
                mYearViewPaddingLeft = mYearViewPadding;
                mYearViewPaddingRight = mYearViewPadding;
            }

            mYearViewMonthPaddingTop = (int) array.getDimension(R.styleable.CalendarView_year_view_month_padding_top,
                    CalendarUtil.dipToPx(context, 4));
            mYearViewMonthPaddingBottom = (int) array.getDimension(R.styleable.CalendarView_year_view_month_padding_bottom,
                    CalendarUtil.dipToPx(context, 4));

            mYearViewMonthPaddingLeft = (int) array.getDimension(R.styleable.CalendarView_year_view_month_padding_left,
                    CalendarUtil.dipToPx(context, 4));
            mYearViewMonthPaddingRight = (int) array.getDimension(R.styleable.CalendarView_year_view_month_padding_right,
                    CalendarUtil.dipToPx(context, 4));

            if (mMinYear <= MIN_YEAR) mMinYear = MIN_YEAR;
            if (mMaxYear >= MAX_YEAR) mMaxYear = MAX_YEAR;
            array.recycle();
        } catch (Exception _) {

        }
        init();
    }

    private void init() {
        mCurrentDate = new Calendar();
        Date d = new Date();
        mCurrentDate.setYear(CalendarUtil.getDate("yyyy", d));
        mCurrentDate.setMonth(CalendarUtil.getDate("MM", d));
        mCurrentDate.setDay(CalendarUtil.getDate("dd", d));
        mCurrentDate.setCurrentDay(true);
        LunarCalendar.setupLunarCalendar(mCurrentDate);
        setRange(mMinYear, mMinYearMonth, mMaxYear, mMaxYearMonth);

        try {
            mWeekBarClass = TextUtils.isEmpty(mWeekBarClassPath) ?
                    WeekBar.class : Class.forName(mWeekBarClassPath);
        } catch (Exception _) {
        }

        try {
            mYearViewClass = TextUtils.isEmpty(mYearViewClassPath) ?
                    DefaultYearView.class : Class.forName(mYearViewClassPath);
        } catch (Exception _) {
        }
        try {
            mMonthViewClass = TextUtils.isEmpty(mMonthViewClassPath) ?
                    DefaultMonthView.class : Class.forName(mMonthViewClassPath);
        } catch (Exception _) {
        }
        try {
            mWeekViewClass = TextUtils.isEmpty(mWeekViewClassPath) ?
                    DefaultWeekView.class : Class.forName(mWeekViewClassPath);
        } catch (Exception _) {
        }
    }


    private void setRange(int minYear, int minYearMonth,
                          int maxYear, int maxYearMonth) {
        mMinYear = minYear;
        mMinYearMonth = minYearMonth;
        mMaxYear = maxYear;
        mMaxYearMonth = maxYearMonth;
        if (mMaxYear < mCurrentDate.getYear()) {
            mMaxYear = mCurrentDate.getYear();
        }
        if (mMaxYearDay == -1) {
            mMaxYearDay = CalendarUtil.getMonthDaysCount(mMaxYear, mMaxYearMonth);
        }
        int y = mCurrentDate.getYear() - mMinYear;
        mCurrentMonthViewItem = 12 * y + mCurrentDate.getMonth() - mMinYearMonth;
    }

    void setRange(int minYear, int minYearMonth, int minYearDay,
                  int maxYear, int maxYearMonth, int maxYearDay) {
        mMinYear = minYear;
        mMinYearMonth = minYearMonth;
        mMinYearDay = minYearDay;
        mMaxYear = maxYear;
        mMaxYearMonth = maxYearMonth;
        mMaxYearDay = maxYearDay;
        if (mMaxYearDay == -1)
            mMaxYearDay = CalendarUtil.getMonthDaysCount(mMaxYear, mMaxYearMonth);
        int y = mCurrentDate.getYear() - mMinYear;
        mCurrentMonthViewItem = 12 * y + mCurrentDate.getMonth() - mMinYearMonth;
    }

    String getSchemeText() {
        return mSchemeText;
    }

    int getCurDayTextColor() {
        return mCurDayTextColor;
    }

    int getCurDayLunarTextColor() {
        return mCurDayLunarTextColor;
    }

    int getWeekTextColor() {
        return mWeekTextColor;
    }

    int getSchemeTextColor() {
        return mSchemeTextColor;
    }

    int getSchemeLunarTextColor() {
        return mSchemeLunarTextColor;
    }

    int getOtherMonthTextColor() {
        return mOtherMonthTextColor;
    }

    int getCurrentMonthTextColor() {
        return mCurrentMonthTextColor;
    }

    int getSelectedTextColor() {
        return mSelectedTextColor;
    }

    int getSelectedLunarTextColor() {
        return mSelectedLunarTextColor;
    }

    int getCurrentMonthLunarTextColor() {
        return mCurMonthLunarTextColor;
    }

    int getOtherMonthLunarTextColor() {
        return mOtherMonthLunarTextColor;
    }

    int getSchemeThemeColor() {
        return mSchemeThemeColor;
    }

    int getSelectedThemeColor() {
        return mSelectedThemeColor;
    }

    int getWeekBackground() {
        return mWeekBackground;
    }

    int getYearViewBackground() {
        return mYearViewBackground;
    }

    int getWeekLineBackground() {
        return mWeekLineBackground;
    }

    int getWeekLineMargin() {
        return mWeekLineMargin;
    }

    Class<?> getMonthViewClass() {
        return mMonthViewClass;
    }

    void setMonthViewClass(Class<?> monthViewClass) {
        mMonthViewClass = monthViewClass;
    }

    Class<?> getWeekViewClass() {
        return mWeekViewClass;
    }

    void setWeekViewClass(Class<?> weekViewClass) {
        mWeekViewClass = weekViewClass;
    }

    Class<?> getWeekBarClass() {
        return mWeekBarClass;
    }

    void setWeekBarClass(Class<?> weekBarClass) {
        mWeekBarClass = weekBarClass;
    }

    Class<?> getYearViewClass() {
        return mYearViewClass;
    }

    String getYearViewClassPath() {
        return mYearViewClassPath;
    }

    int getWeekBarHeight() {
        return mWeekBarHeight;
    }

    int getMinYear() {
        return mMinYear;
    }

    int getMaxYear() {
        return mMaxYear;
    }

    int getDayTextSize() {
        return mDayTextSize;
    }

    int getLunarTextSize() {
        return mLunarTextSize;
    }

    int getCalendarItemHeight() {
        return mCalendarItemHeight;
    }

    void setCalendarItemHeight(int height) {
        mCalendarItemHeight = height;
    }

    int getMinYearMonth() {
        return mMinYearMonth;
    }

    int getMaxYearMonth() {
        return mMaxYearMonth;
    }

    int getYearViewMonthTextSize() {
        return mYearViewMonthTextSize;
    }

    int getYearViewMonthTextColor() {
        return mYearViewMonthTextColor;
    }

    int getYearViewWeekTextSize() {
        return mYearViewWeekTextSize;
    }

    int getYearViewWeekTextColor() {
        return mYearViewWeekTextColor;
    }

    int getYearViewSelectTextColor() {
        return mYearViewSelectTextColor;
    }

    int getYearViewCurDayTextColor() {
        return mYearViewCurDayTextColor;
    }

    @SuppressWarnings("unused")
    int getYearViewPadding() {
        return mYearViewPadding;
    }

    int getYearViewPaddingLeft() {
        return mYearViewPaddingLeft;
    }

    int getYearViewPaddingRight() {
        return mYearViewPaddingRight;
    }

    int getYearViewMonthPaddingLeft() {
        return mYearViewMonthPaddingLeft;
    }

    int getYearViewMonthPaddingRight() {
        return mYearViewMonthPaddingRight;
    }

    int getYearViewMonthPaddingTop() {
        return mYearViewMonthPaddingTop;
    }

    int getYearViewMonthPaddingBottom() {
        return mYearViewMonthPaddingBottom;
    }

    int getYearViewWeekHeight() {
        return mYearViewWeekHeight;
    }

    int getYearViewMonthHeight() {
        return mYearViewMonthHeight;
    }

    int getYearViewDayTextColor() {
        return mYearViewDayTextColor;
    }

    int getYearViewDayTextSize() {
        return mYearViewDayTextSize;
    }

    int getYearViewSchemeTextColor() {
        return mYearViewSchemeTextColor;
    }

    int getMonthViewShowMode() {
        return mMonthViewShowMode;
    }

    void setMonthViewShowMode(int monthViewShowMode) {
        mMonthViewShowMode = monthViewShowMode;
    }

    void setTextColor(int curDayTextColor, int curMonthTextColor, int otherMonthTextColor, int curMonthLunarTextColor, int otherMonthLunarTextColor) {
        mCurDayTextColor = curDayTextColor;
        mOtherMonthTextColor = otherMonthTextColor;
        mCurrentMonthTextColor = curMonthTextColor;
        mCurMonthLunarTextColor = curMonthLunarTextColor;
        mOtherMonthLunarTextColor = otherMonthLunarTextColor;
    }

    void setSchemeColor(int schemeColor, int schemeTextColor, int schemeLunarTextColor) {
        mSchemeThemeColor = schemeColor;
        mSchemeTextColor = schemeTextColor;
        mSchemeLunarTextColor = schemeLunarTextColor;
    }

    void setYearViewTextColor(int yearViewMonthTextColor, int yearViewDayTextColor, int yarViewSchemeTextColor) {
        mYearViewMonthTextColor = yearViewMonthTextColor;
        mYearViewDayTextColor = yearViewDayTextColor;
        mYearViewSchemeTextColor = yarViewSchemeTextColor;
    }

    void setSelectColor(int selectedColor, int selectedTextColor, int selectedLunarTextColor) {
        mSelectedThemeColor = selectedColor;
        mSelectedTextColor = selectedTextColor;
        mSelectedLunarTextColor = selectedLunarTextColor;
    }

    void setThemeColor(int selectedThemeColor, int schemeColor) {
        mSelectedThemeColor = selectedThemeColor;
        mSchemeThemeColor = schemeColor;
    }

    boolean isMonthViewScrollable() {
        return mMonthViewScrollable;
    }

    void setMonthViewScrollable(boolean monthViewScrollable) {
        mMonthViewScrollable = monthViewScrollable;
    }

    boolean isWeekViewScrollable() {
        return mWeekViewScrollable;
    }

    void setWeekViewScrollable(boolean weekViewScrollable) {
        mWeekViewScrollable = weekViewScrollable;
    }

    boolean isYearViewScrollable() {
        return mYearViewScrollable;
    }

    void setYearViewScrollable(boolean yearViewScrollable) {
        mYearViewScrollable = yearViewScrollable;
    }

    int getWeekStart() {
        return mWeekStart;
    }

    void setWeekStart(int mWeekStart) {
        this.mWeekStart = mWeekStart;
    }

    int getDefaultCalendarSelectDay() {
        return mDefaultCalendarSelectDay;
    }

    void setDefaultCalendarSelectDay(int defaultCalendarSelect) {
        mDefaultCalendarSelectDay = defaultCalendarSelect;
    }

    int getWeekTextSize() {
        return mWeekTextSize;
    }

    /**
     * 选择模式
     *
     * @return 选择模式
     */
    int getSelectMode() {
        return mSelectMode;
    }

    /**
     * 设置选择模式
     *
     * @param mSelectMode mSelectMode
     */
    void setSelectMode(int mSelectMode) {
        this.mSelectMode = mSelectMode;
    }

    int getMinSelectRange() {
        return mMinSelectRange;
    }

    int getMaxSelectRange() {
        return mMaxSelectRange;
    }

    int getMaxMultiSelectSize() {
        return mMaxMultiSelectSize;
    }

    void setMaxMultiSelectSize(int maxMultiSelectSize) {
        mMaxMultiSelectSize = maxMultiSelectSize;
    }

    void setSelectRange(int minRange, int maxRange) {
        if (minRange > maxRange && maxRange > 0) {
            mMaxSelectRange = minRange;
            mMinSelectRange = minRange;
            return;
        }
        if (minRange <= 0) {
            mMinSelectRange = -1;
        } else {
            mMinSelectRange = minRange;
        }
        if (maxRange <= 0) {
            mMaxSelectRange = -1;
        } else {
            mMaxSelectRange = maxRange;
        }
    }

    Calendar getCurrentDay() {
        return mCurrentDate;
    }

    void updateCurrentDay() {
        Date d = new Date();
        mCurrentDate.setYear(CalendarUtil.getDate("yyyy", d));
        mCurrentDate.setMonth(CalendarUtil.getDate("MM", d));
        mCurrentDate.setDay(CalendarUtil.getDate("dd", d));
        LunarCalendar.setupLunarCalendar(mCurrentDate);
    }

    @SuppressWarnings("unused")
    int getCalendarPadding() {
        return mCalendarPadding;
    }

    void setCalendarPadding(int mCalendarPadding) {
        this.mCalendarPadding = mCalendarPadding;
        mCalendarPaddingLeft = mCalendarPadding;
        mCalendarPaddingRight = mCalendarPadding;
    }

    int getCalendarPaddingLeft() {
        return mCalendarPaddingLeft;
    }

    void setCalendarPaddingLeft(int mCalendarPaddingLeft) {
        this.mCalendarPaddingLeft = mCalendarPaddingLeft;
    }

    int getCalendarPaddingRight() {
        return mCalendarPaddingRight;
    }

    void setCalendarPaddingRight(int mCalendarPaddingRight) {
        this.mCalendarPaddingRight = mCalendarPaddingRight;
    }

    boolean isPreventLongPressedSelected() {
        return preventLongPressedSelected;
    }

    void setPreventLongPressedSelected(boolean preventLongPressedSelected) {
        this.preventLongPressedSelected = preventLongPressedSelected;
    }

    void clearSelectedScheme() {
        mSelectedCalendar.clearScheme();
    }

    int getMinYearDay() {
        return mMinYearDay;
    }

    int getMaxYearDay() {
        return mMaxYearDay;
    }

    boolean isFullScreenCalendar() {
        return isFullScreenCalendar;
    }

    void updateSelectCalendarScheme() {
        if (mSchemeDatesMap != null && !mSchemeDatesMap.isEmpty()) {
            String key = mSelectedCalendar.toString();
            if (mSchemeDatesMap.containsKey(key)) {
                Calendar d = mSchemeDatesMap.get(key);
                mSelectedCalendar.mergeScheme(d, getSchemeText());
            }
        } else {
            clearSelectedScheme();
        }
    }

    void updateCalendarScheme(Calendar targetCalendar) {
        if (targetCalendar == null) {
            return;
        }
        if (mSchemeDatesMap == null || mSchemeDatesMap.isEmpty()) {
            return;
        }
        String key = targetCalendar.toString();
        if (mSchemeDatesMap.containsKey(key)) {
            Calendar d = mSchemeDatesMap.get(key);
            targetCalendar.mergeScheme(d, getSchemeText());
        }
    }

    Calendar createCurrentDate() {
        Calendar calendar = new Calendar();
        calendar.setYear(mCurrentDate.getYear());
        calendar.setWeek(mCurrentDate.getWeek());
        calendar.setMonth(mCurrentDate.getMonth());
        calendar.setDay(mCurrentDate.getDay());
        calendar.setCurrentDay(true);
        LunarCalendar.setupLunarCalendar(calendar);
        return calendar;
    }

    Calendar getMinRangeCalendar() {
        Calendar calendar = new Calendar();
        calendar.setYear(mMinYear);
        calendar.setMonth(mMinYearMonth);
        calendar.setDay(mMinYearDay);
        calendar.setCurrentDay(calendar.equals(mCurrentDate));
        LunarCalendar.setupLunarCalendar(calendar);
        return calendar;
    }

    @SuppressWarnings("unused")
    Calendar getMaxRangeCalendar() {
        Calendar calendar = new Calendar();
        calendar.setYear(mMaxYear);
        calendar.setMonth(mMaxYearMonth);
        calendar.setDay(mMaxYearDay);
        calendar.setCurrentDay(calendar.equals(mCurrentDate));
        LunarCalendar.setupLunarCalendar(calendar);
        return calendar;
    }

    /**
     * 添加事件标记，来自Map
     */
    void addSchemesFromMap(List<Calendar> mItems) {
        if (mSchemeDatesMap == null || mSchemeDatesMap.isEmpty()) {
            return;
        }
        for (Calendar a : mItems) {
            if (mSchemeDatesMap.containsKey(a.toString())) {
                Calendar d = mSchemeDatesMap.get(a.toString());
                if (d == null) {
                    continue;
                }
                a.setScheme(TextUtils.isEmpty(d.getScheme()) ? getSchemeText() : d.getScheme());
                a.setSchemeColor(d.getSchemeColor());
                a.setSchemes(d.getSchemes());
            } else {
                a.setScheme("");
                a.setSchemeColor(0);
                a.setSchemes(null);
            }
        }
    }

    /**
     * 添加数据
     *
     * @param mSchemeDates mSchemeDates
     */
    void addSchemes(Map<String, Calendar> mSchemeDates) {
        if (mSchemeDates == null || mSchemeDates.isEmpty()) {
            return;
        }
        if (mSchemeDatesMap == null) {
            mSchemeDatesMap = new HashMap<>();
        }
        for (String key : mSchemeDates.keySet()) {
            mSchemeDatesMap.remove(key);
            Calendar calendar = mSchemeDates.get(key);
            if (calendar == null) {
                continue;
            }
            mSchemeDatesMap.put(key, calendar);
        }
    }

    /**
     * 清楚选择
     */
    void clearSelectRange() {
        mSelectedStartRangeCalendar = null;
        mSelectedEndRangeCalendar = null;
    }

    /**
     * 获得选中范围
     *
     * @return 选中范围
     */
    List<Calendar> getSelectCalendarRange() {
        if (mSelectMode != SELECT_MODE_RANGE) {
            return null;
        }
        List<Calendar> calendars = new ArrayList<>();
        if (mSelectedStartRangeCalendar == null ||
                mSelectedEndRangeCalendar == null) {
            return calendars;
        }
        final long ONE_DAY = 1000 * 3600 * 24;
        java.util.Calendar date = java.util.Calendar.getInstance();

        date.set(mSelectedStartRangeCalendar.getYear(),
                mSelectedStartRangeCalendar.getMonth() - 1,
                mSelectedStartRangeCalendar.getDay());//

        long startTimeMills = date.getTimeInMillis();//获得起始时间戳


        date.set(mSelectedEndRangeCalendar.getYear(),
                mSelectedEndRangeCalendar.getMonth() - 1,
                mSelectedEndRangeCalendar.getDay());//
        long endTimeMills = date.getTimeInMillis();
        for (long start = startTimeMills; start <= endTimeMills; start += ONE_DAY) {
            date.setTimeInMillis(start);
            Calendar calendar = new Calendar();
            calendar.setYear(date.get(java.util.Calendar.YEAR));
            calendar.setMonth(date.get(java.util.Calendar.MONTH) + 1);
            calendar.setDay(date.get(java.util.Calendar.DAY_OF_MONTH));
            LunarCalendar.setupLunarCalendar(calendar);
            updateCalendarScheme(calendar);
            if (mCalendarInterceptListener != null &&
                    mCalendarInterceptListener.onCalendarIntercept(calendar)) {
                continue;
            }

            calendars.add(calendar);
        }
        addSchemesFromMap(calendars);
        return calendars;
    }
}
