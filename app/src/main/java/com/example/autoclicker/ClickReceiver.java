
// ClickReceiver.java
package com.example.autoclicker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;
import androidx.annotation.RequiresApi;

public class ClickReceiver extends BroadcastReceiver {
    private static final String TAG = "ClickReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive called");

        if ("com.example.autoclicker.CLICK_COORDINATES".equals(intent.getAction())) {
            float x = intent.getFloatExtra("x", 0f);
            float y = intent.getFloatExtra("y", 0f);

            Log.d(TAG, "Received coordinates: x = " + x + ", y = " + y);

            AutoClickService service = AutoClickService.getInstance();
            if (service != null) {
                service.performClick(x, y);
                Log.d(TAG, "Click performed through AutoClickService");
            } else {
                Log.e(TAG, "AutoClickService instance is null");
            }
        } else {
            Log.w(TAG, "Invalid action: " + intent.getAction());
        }
    }

    public static void registerReceiver(Context context) {
        IntentFilter filter = new IntentFilter("com.example.autoclicker.CLICK_COORDINATES");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(new ClickReceiver(), filter, Context.RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(new ClickReceiver(), filter);
        }
        Log.d(TAG, "Receiver registered");
    }
}
