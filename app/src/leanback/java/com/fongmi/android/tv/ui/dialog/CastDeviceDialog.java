package com.fongmi.android.tv.ui.dialog;

import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.databinding.DialogCastDeviceBinding;
import com.fongmi.android.tv.setting.CastSetting;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class CastDeviceDialog extends BaseAlertDialog {

    public interface Listener {
        void onCastDeviceName(String name);
    }

    private DialogCastDeviceBinding binding;

    public static void show(FragmentActivity activity) {
        new CastDeviceDialog().show(activity.getSupportFragmentManager(), null);
    }

    @Override
    protected ViewBinding getBinding() {
        return binding = DialogCastDeviceBinding.inflate(getLayoutInflater());
    }

    @Override
    protected MaterialAlertDialogBuilder getBuilder() {
        return builder().setView(getBinding().getRoot());
    }

    @Override
    protected void initView() {
        String text = CastSetting.getDeviceName();
        binding.text.setText(text);
        binding.text.setSelection(TextUtils.isEmpty(text) ? 0 : text.length());
    }

    @Override
    protected void initEvent() {
        binding.positive.setOnClickListener(this::onPositive);
        binding.negative.setOnClickListener(this::onNegative);
        binding.text.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) binding.positive.performClick();
            return true;
        });
    }

    private void onPositive(View view) {
        if (requireActivity() instanceof Listener listener) {
            listener.onCastDeviceName(binding.text.getText().toString().trim());
        }
        dismiss();
    }

    private void onNegative(View view) {
        dismiss();
    }

    @Override
    public void onStart() {
        super.onStart();
        setWidth(0.45f);
    }
}
