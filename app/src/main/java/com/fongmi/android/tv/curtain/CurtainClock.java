package com.fongmi.android.tv.curtain;

import android.icu.util.Calendar;
import android.icu.util.ChineseCalendar;

import java.util.Locale;

public final class CurtainClock {

    public static final class Face {
        public final String time;
        public final String solar;
        public final String lunar;

        Face(String time, String solar, String lunar) {
            this.time = time;
            this.solar = solar;
            this.lunar = lunar;
        }
    }

    private static final String[] WEEK = {"日", "一", "二", "三", "四", "五", "六"};
    private static final String[] STEMS = {"甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"};
    private static final String[] BRANCHES = {"子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"};
    private static final String[] ZODIAC = {"鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪"};
    private static final String[] MONTHS = {"正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊"};
    private static final String[] DAYS = {
            "", "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
            "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
            "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
    };
    private static final String[] TERMS = {
            "小寒", "大寒", "立春", "雨水", "惊蛰", "春分", "清明", "谷雨",
            "立夏", "小满", "芒种", "夏至", "小暑", "大暑", "立秋", "处暑",
            "白露", "秋分", "寒露", "霜降", "立冬", "小雪", "大雪", "冬至"
    };
    private static final long TERM_BASE = termBase();
    private static final int[] TERM_MINUTES = {
            0, 21208, 42467, 63836, 85337, 107014, 128867, 150921,
            173149, 195551, 218072, 240693, 263343, 285989, 308563, 331033,
            353350, 375494, 397447, 419210, 440795, 462224, 483532, 504758
    };

    private CurtainClock() {
    }

    public static Face now() {
        return at(System.currentTimeMillis());
    }

    static Face at(long millis) {
        Calendar solar = Calendar.getInstance();
        solar.setTimeInMillis(millis);
        ChineseCalendar lunar = new ChineseCalendar();
        lunar.setTimeInMillis(millis);
        return new Face(time(solar), date(solar), lunar(lunar, solarTerm(solar)));
    }

    private static String time(Calendar calendar) {
        return String.format(Locale.CHINA, "%02d:%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND));
    }

    private static String date(Calendar calendar) {
        int week = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        String name = week >= 0 && week < WEEK.length ? WEEK[week] : "";
        return String.format(Locale.CHINA, "%d年%d月%d日 星期%s", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH), name);
    }

    private static String lunar(ChineseCalendar calendar, String term) {
        int cycle = Math.floorMod(calendar.get(Calendar.EXTENDED_YEAR) - 1, 60);
        int month = calendar.get(Calendar.MONTH);
        boolean leap = calendar.get(Calendar.IS_LEAP_MONTH) == 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        String monthName = (month >= 0 && month < MONTHS.length ? MONTHS[month] : String.valueOf(month + 1)) + "月";
        if (leap) monthName = "闰" + monthName;
        String dayName = day >= 0 && day < DAYS.length ? DAYS[day] : String.valueOf(day);
        String text = "农历" + STEMS[cycle % 10] + BRANCHES[cycle % 12] + ZODIAC[cycle % 12] + "年 " + monthName + dayName;
        if (!term.isEmpty()) text = text + "  " + term;
        return text;
    }

    private static String solarTerm(Calendar now) {
        int year = now.get(Calendar.YEAR);
        long best = Long.MIN_VALUE;
        String name = "";
        for (int y = year - 1; y <= year; y++) {
            for (int i = 0; i < TERMS.length; i++) {
                long time = termTime(y, i);
                if (time <= now.getTimeInMillis() && time >= best) {
                    best = time;
                    name = TERMS[i];
                }
            }
        }
        return name;
    }

    private static long termTime(int year, int index) {
        return TERM_BASE + (long) (31556925974.7 * (year - 1900) + TERM_MINUTES[index] * 60000L);
    }

    private static long termBase() {
        java.util.Calendar calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
        calendar.clear();
        calendar.set(1900, java.util.Calendar.JANUARY, 6, 2, 5, 0);
        return calendar.getTimeInMillis();
    }
}
