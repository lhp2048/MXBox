package com.fongmi.android.tv.ui.helper;

import android.content.Context;
import android.text.TextUtils;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.MxBoxSourceCatalog;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Site;

public final class MxBoxHomeTitle {

    private MxBoxHomeTitle() {
    }

    public static String text(Context context) {
        Site home = VodConfig.get().getHome();
        String configDesc = MxBoxSourceCatalog.getDisplayName(VodConfig.get().getConfig());
        if (!home.isEmpty() && !TextUtils.isEmpty(home.getName())) {
            if (!TextUtils.isEmpty(configDesc)) {
                return context.getString(R.string.mx_home_title, home.getName(), configDesc);
            }
            return home.getName();
        }
        if (!TextUtils.isEmpty(configDesc)) return configDesc;
        return context.getString(R.string.app_name);
    }
}
