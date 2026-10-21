package com.fongmi.android.tv.ui.helper;

import android.content.Context;
import android.view.View;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import android.widget.TextView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.event.DepotLoadEvent;

public final class SiteDialogDepotHelper {

    private SiteDialogDepotHelper() {
    }

    public static void bind(View panel, LinearProgressIndicator bar, TextView text, Context context) {
        if (panel == null || bar == null || text == null || context == null) return;
        if (!DepotLoadEvent.isLoading()) {
            panel.setVisibility(View.GONE);
            return;
        }
        panel.setVisibility(View.VISIBLE);
        int total = Math.max(DepotLoadEvent.getTotal(), 1);
        bar.setMax(total);
        bar.setProgress(Math.min(DepotLoadEvent.getCurrent(), total));
        bar.setIndeterminate(DepotLoadEvent.getCurrent() <= 0);
        text.setText(context.getString(
                R.string.mx_depot_loading,
                DepotLoadEvent.getCurrent(),
                total,
                DepotLoadEvent.getDepotName(),
                DepotLoadEvent.getSiteCount()
        ));
    }
}
