package com.fongmi.android.tv.bili;

public class BiliQn {

    private final int value;
    private final String label;

    public BiliQn(int value, String label) {
        this.value = value;
        this.label = label == null || label.isEmpty() ? fallback(value) : label;
    }

    public int getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public static String fallback(int qn) {
        return switch (qn) {
            case 6 -> "240P";
            case 16 -> "360P";
            case 32 -> "480P";
            case 64 -> "720P";
            case 74 -> "720P60";
            case 80 -> "1080P";
            case 112 -> "1080P+";
            case 116 -> "1080P60";
            case 120 -> "4K";
            case 125 -> "HDR";
            case 126 -> "杜比";
            case 127 -> "8K";
            default -> qn + "P";
        };
    }

    /** 未选过时 wanted 为 0，取最高档。没有同一档时先取最近的更低档，否则取更高档。 */
    public static int pick(int wanted, java.util.List<Integer> available) {
        if (available == null || available.isEmpty()) return wanted > 0 ? wanted : 80;
        if (wanted <= 0) {
            int max = available.get(0);
            for (int qn : available) if (qn > max) max = qn;
            return max;
        }
        if (available.contains(wanted)) return wanted;
        int lower = -1;
        int higher = Integer.MAX_VALUE;
        for (int qn : available) {
            if (qn < wanted && qn > lower) lower = qn;
            if (qn > wanted && qn < higher) higher = qn;
        }
        if (lower > 0) return lower;
        if (higher != Integer.MAX_VALUE) return higher;
        return available.get(0);
    }
}
