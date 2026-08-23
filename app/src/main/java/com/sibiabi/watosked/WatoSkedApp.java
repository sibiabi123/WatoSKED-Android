package com.sibiabi.watosked;

import android.content.Intent;
import android.os.Build;

import androidx.multidex.MultiDexApplication;

import com.sibiabi.watosked.service.WatoForegroundService;

public class WatoSkedApp extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        // Start foreground service to keep app alive
        Intent serviceIntent = new Intent(this, WatoForegroundService.class);
        serviceIntent.setAction(WatoForegroundService.ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }
}