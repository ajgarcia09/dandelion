package edu.utep.cs.cs4330.dandelion.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class DandelionSyncService extends Service {
    private static final Object sSyncAdapterLock = new Object();
    private static DandelionSyncAdapter sDandelionSyncAdapter = null;

    @Override
    public void onCreate() {
        Log.d("DandelionSyncService", "onCreate - DandelionSyncService");
        synchronized (sSyncAdapterLock) {
            if (sDandelionSyncAdapter == null) {
                sDandelionSyncAdapter = new DandelionSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sDandelionSyncAdapter.getSyncAdapterBinder();
    }
}