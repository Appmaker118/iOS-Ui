package com.replica.iosui.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class VolumeKeyReceiver extends BroadcastReceiver {

    public interface VolumeChangeListener {
        void onVolumeChanged();
    }

    private final VolumeChangeListener listener;

    public VolumeKeyReceiver(VolumeChangeListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.media.VOLUME_CHANGED_ACTION".equals(intent.getAction())) {
            if (listener != null) {
                listener.onVolumeChanged();
            }
        }
    }
}
