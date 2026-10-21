package com.fongmi.android.tv.ui.helper;

import androidx.fragment.app.FragmentActivity;

import com.fongmi.android.tv.impl.ConfigListener;
import com.fongmi.android.tv.ui.dialog.MxBoxSourceDialog;

public final class MxBoxSubscriptionActions {

    private MxBoxSubscriptionActions() {
    }

    public static void open(FragmentActivity activity) {
        if (activity instanceof ConfigListener) {
            open(activity, (ConfigListener) activity);
            return;
        }
        MxBoxSourceDialog.create().show(activity);
    }

    public static void open(FragmentActivity activity, ConfigListener listener) {
        if (listener == null) return;
        MxBoxSourceDialog.create().show(activity);
    }
}
