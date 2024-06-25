package com.goofish.emm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

public class VolumeButtonReceiver extends BroadcastReceiver {
    public static final String VOLUME_UP_ACTION = "com.example.VOLUME_UP_ACTION";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_MEDIA_BUTTON)) {
            KeyEvent keyEvent = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (keyEvent != null && keyEvent.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                Intent volumeUpIntent = new Intent(VOLUME_UP_ACTION);
                context.sendBroadcast(volumeUpIntent);
            }
        }
    }
}