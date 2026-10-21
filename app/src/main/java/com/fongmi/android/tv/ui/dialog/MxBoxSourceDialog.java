package com.fongmi.android.tv.ui.dialog;

import android.text.TextUtils;
import android.view.View;

import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.api.config.MxBoxSourceCatalog;
import com.fongmi.android.tv.api.config.MxBoxSourceProbe;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.databinding.DialogMxSourceBinding;
import com.fongmi.android.tv.event.ConfigEvent;
import com.fongmi.android.tv.event.DepotLoadEvent;
import com.fongmi.android.tv.impl.ConfigListener;
import com.fongmi.android.tv.ui.adapter.MxBoxSourceAdapter;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class MxBoxSourceDialog extends BaseAlertDialog implements MxBoxSourceAdapter.OnClickListener, MxBoxSourceProbe.Listener {

    private DialogMxSourceBinding binding;
    private MxBoxSourceAdapter adapter;

    public static MxBoxSourceDialog create() {
        return new MxBoxSourceDialog();
    }

    public void show(FragmentActivity activity) {
        show(activity.getSupportFragmentManager(), null);
    }

    @Override
    protected ViewBinding getBinding() {
        return binding = DialogMxSourceBinding.inflate(getLayoutInflater());
    }

    @Override
    protected MaterialAlertDialogBuilder getBuilder() {
        return builder().setView(getBinding().getRoot());
    }

    @Override
    protected void initView() {
        adapter = new MxBoxSourceAdapter(this);
        binding.recycler.setItemAnimator(null);
        binding.recycler.setHasFixedSize(false);
        binding.recycler.addItemDecoration(new SpaceItemDecoration(1, 10));
        refreshList();
        refreshProbeUi();
    }

    @Override
    protected void initEvent() {
        binding.test.setOnClickListener(this::onTest);
        binding.addVod.setOnClickListener(this::onAddVod);
        binding.addLive.setOnClickListener(this::onAddLive);
        binding.manage.setOnClickListener(this::onManage);
    }

    private void refreshList() {
        if (adapter == null || binding == null) return;
        binding.recycler.setAdapter(adapter
                .setItems(MxBoxSourceCatalog.getPickerSources())
                .setCurrent(VodConfig.getUrl(), getCurrentLiveUrl()));
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        setWidth(ResUtil.isLand(requireContext()) ? 0.68f : 0.92f);
        MxBoxSourceProbe.addListener(this);
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        MxBoxSourceProbe.removeListener(this);
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        binding = null;
        adapter = null;
        super.onDestroyView();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConfigEvent(ConfigEvent event) {
        refreshList();
        if (!MxBoxSourceProbe.isRunning()) updateStatus(MxBoxSourceProbe.getLastTestTime());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDepotLoadEvent(DepotLoadEvent event) {
        refreshList();
    }

    private String getCurrentLiveUrl() {
        Live home = LiveConfig.get().getHome();
        if (home != null && !home.isEmpty() && !TextUtils.isEmpty(home.getUrl())) return home.getUrl();
        return LiveConfig.getUrl();
    }

    private ConfigListener getConfigListener() {
        return (ConfigListener) requireActivity();
    }

    private void onAddVod(View view) {
        ConfigDialog.create().vod().userSource().show(requireActivity());
    }

    private void onAddLive(View view) {
        ConfigDialog.create().live().userSource().show(requireActivity());
    }

    private void onManage(View view) {
        new MaterialAlertDialogBuilder(requireActivity())
                .setTitle(R.string.mx_source_manage_title)
                .setItems(new CharSequence[]{
                        getString(R.string.mx_source_manage_vod),
                        getString(R.string.mx_source_manage_live)
                }, (dialog, which) -> {
                    if (which == 0) HistoryDialog.create().vod().userOnly().show(requireActivity());
                    else HistoryDialog.create().live().userOnly().show(requireActivity());
                })
                .show();
    }

    private void onTest(View view) {
        if (!isActive()) return;
        if (MxBoxSourceProbe.isRunning()) return;
        binding.test.setEnabled(false);
        binding.test.setText(R.string.mx_source_testing_prepare);
        binding.status.setText(R.string.mx_source_testing_prepare);
        MxBoxSourceProbe.probeAll();
    }

    private void refreshProbeUi() {
        if (!isActive()) return;
        if (MxBoxSourceProbe.isRunning()) {
            MxBoxSourceProbe.Session session = MxBoxSourceProbe.getSession();
            adapter.restoreSession(session);
            if (session != null) {
                binding.test.setText(getString(R.string.mx_source_testing, session.completed.get(), session.total));
                binding.status.setText(getString(R.string.mx_source_testing_parallel, session.completed.get(), session.total, MxBoxSourceProbe.PROBE_PARALLEL));
            }
            binding.test.setEnabled(false);
            return;
        }
        adapter.setResults(MxBoxSourceProbe.loadResults());
        updateStatus(MxBoxSourceProbe.getLastTestTime());
        updateTestButton();
    }

    private boolean isActive() {
        return isAdded() && binding != null && adapter != null;
    }

    private void updateStatus(long testedAt) {
        if (!isActive() || MxBoxSourceProbe.isRunning()) return;
        String current = getString(R.string.mx_source_current_prefix, formatCurrentSources());
        String testPart;
        if (testedAt <= 0) {
            testPart = getString(R.string.mx_source_test_none);
        } else {
            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(testedAt));
            testPart = getString(R.string.mx_source_test_last, time);
        }
        binding.status.setText(current + " · " + testPart);
    }

    private String formatCurrentSources() {
        ArrayList<String> parts = new ArrayList<>();
        Config vodConfig = VodConfig.get().getConfig();
        Config liveConfig = LiveConfig.get().getConfig();
        String vod = MxBoxSourceCatalog.getDisplayName(vodConfig);
        String live = MxBoxSourceCatalog.getDisplayName(liveConfig);
        if (!TextUtils.isEmpty(vod)) {
            parts.add(getString(R.string.mx_source_current_item, vod, MxBoxSourceCatalog.getTypeLabel(0)));
        }
        if (!TextUtils.isEmpty(live)) {
            parts.add(getString(R.string.mx_source_current_item, live, MxBoxSourceCatalog.getTypeLabel(1)));
        }
        if (parts.isEmpty()) return getString(R.string.mx_source_current_none);
        return TextUtils.join(" · ", parts);
    }

    private void updateTestButton() {
        if (!isActive()) return;
        binding.test.setEnabled(!MxBoxSourceProbe.isRunning());
        binding.test.setText(MxBoxSourceProbe.isRunning() ? R.string.mx_source_testing_prepare : R.string.mx_source_test);
    }

    @Override
    public boolean syncSession(MxBoxSourceProbe.Session session) {
        refreshProbeUi();
        return true;
    }

    @Override
    public void onProbeStart(int total) {
        if (!isActive()) return;
        adapter.beginProbe();
        binding.status.setText(getString(R.string.mx_source_testing_parallel, 0, total, MxBoxSourceProbe.PROBE_PARALLEL));
        binding.test.setEnabled(false);
    }

    @Override
    public void onTesting(Config config, int progress, int total) {
        if (!isActive()) return;
        adapter.addTesting(config);
        binding.test.setText(getString(R.string.mx_source_testing, progress, total));
        binding.status.setText(getString(R.string.mx_source_testing_parallel, progress, total, MxBoxSourceProbe.PROBE_PARALLEL));
    }

    @Override
    public void onProgress(int progress, int total, Config config, MxBoxSourceProbe.Result result) {
        if (!isActive()) return;
        adapter.completeProbe(config, result);
        binding.test.setText(getString(R.string.mx_source_testing, progress, total));
        binding.status.setText(getString(R.string.mx_source_testing_parallel, progress, total, MxBoxSourceProbe.PROBE_PARALLEL));
    }

    @Override
    public void onComplete(long testedAt, Map<String, MxBoxSourceProbe.Result> results) {
        if (isActive()) {
            adapter.setResults(results);
            updateStatus(testedAt);
            updateTestButton();
        }
        Notify.show(R.string.mx_source_test_done);
    }

    @Override
    public void onError(String message) {
        if (isActive()) {
            adapter.setResults(MxBoxSourceProbe.loadResults());
            updateStatus(MxBoxSourceProbe.getLastTestTime());
            updateTestButton();
        }
        if (!TextUtils.isEmpty(message)) Notify.show(message);
    }

    @Override
    public void onItemClick(Config item) {
        if (item.getType() == 1) {
            if (TextUtils.equals(item.getUrl(), getCurrentLiveUrl())) {
                dismiss();
                return;
            }
        } else if (TextUtils.equals(item.getUrl(), VodConfig.getUrl())) {
            dismiss();
            return;
        }
        item.save();
        getConfigListener().setConfig(item);
        dismiss();
    }
}
