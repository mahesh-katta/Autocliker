// ClickReceiver.java
package com.example.autoclicker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;
import androidx.annotation.RequiresApi;
import static android.content.Context.RECEIVER_EXPORTED;

public class ClickReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("ClickReceiver", "onReceive called");

        if ("com.example.autoclicker.CLICK_COORDINATES".equals(intent.getAction())) {
            float x = intent.getFloatExtra("x", 0f);
            float y = intent.getFloatExtra("y", 0f);

            Log.d("ClickReceiver", "Received coordinates: x = " + x + ", y = " + y);

            Intent serviceIntent = new Intent(context, AutoClickService.class);
            serviceIntent.putExtra("x", x);
            serviceIntent.putExtra("y", y);

            context.startService(serviceIntent);
            Log.d("ClickReceiver", "Started AutoClickService");
        } else {
            Log.w("ClickReceiver", "Invalid action: " + intent.getAction());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public static void registerReceiver(Context context) {
        IntentFilter filter = new IntentFilter("com.example.autoclicker.CLICK_COORDINATES");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.registerReceiver(new ClickReceiver(), filter, RECEIVER_EXPORTED);
            Log.d("ClickReceiver", "Receiver dynamically registered");
        }
    }
}
