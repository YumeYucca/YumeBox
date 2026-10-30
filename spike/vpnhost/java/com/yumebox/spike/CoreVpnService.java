package com.yumebox.spike;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * Builds a real VpnService tun and runs the sing-box core inside this process, handing it the fd.
 *
 * In-process instead of fork/exec because Android's Java ProcessBuilder spawns with
 * CLOEXEC_DEFAULT: an arbitrary fd cannot survive it without YumeBox's own native fork/exec bridge,
 * which is out of scope for a probe. The fd therefore stays valid, and platform.OpenTun consumes it
 * exactly like the production path will.
 *
 * The app's own uid is excluded from the tunnel, so the core's egress never loops back into the tun
 * and no VpnService.protect RPC is needed here.
 */
public final class CoreVpnService extends VpnService {
    private static final String TAG = "YumeBoxSpike";
    private static final String CHANNEL_ID = "spike";
    private static final int NOTIFICATION_ID = 1;

    private static volatile Sink sink;

    private ParcelFileDescriptor tun;
    private Thread coreThread;
    private File configFile;

    /** Receives status lines for the activity. */
    public interface Sink {
        void onLine(String line);
    }

    static void setSink(Sink value) {
        sink = value;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (coreThread != null) {
            return Service.START_STICKY;
        }
        startForeground(NOTIFICATION_ID, buildNotification());
        try {
            startCore();
        } catch (Exception error) {
            Log.e(TAG, "start failed", error);
            emitLine("start failed: " + error);
            stopSelf();
            return Service.START_NOT_STICKY;
        }
        return Service.START_STICKY;
    }

    private void startCore() throws Exception {
        Builder builder = new Builder();
        builder.setSession("YumeBox Spike");
        builder.addAddress("172.19.0.1", 30);
        builder.addDnsServer("172.19.0.2");
        builder.setMtu(1500);
        builder.addRoute("0.0.0.0", 0);
        builder.addDisallowedApplication(getPackageName());
        tun = builder.establish();
        if (tun == null) {
            throw new IllegalStateException("VpnService.Builder.establish returned null");
        }
        final int fd = tun.getFd();
        // Belt and braces: the fd no longer crosses an exec, but keeping it non-inheritable is the
        // posture the production path needs anyway.
        Os.fcntlInt(tun.getFileDescriptor(), OsConstants.F_SETFD, 0);
        emitLine("tun established, fd=" + fd);

        File directory = getFilesDir();
        configFile = new File(directory, "spike.json");
        writeConfig(configFile);

        final String library = getApplicationInfo().nativeLibraryDir + "/libyumecore.so";
        final String arguments = String.join("\n",
                "yumecore",
                "--mode", "tun",
                "--home", directory.getAbsolutePath(),
                "--config", configFile.getAbsolutePath(),
                "--tun-fd", String.valueOf(fd),
                "--tun-name", "tun0",
                "--stack", "gvisor",
                "--api-addr", "127.0.0.1:9090");

        coreThread = new Thread(new Runnable() {
            @Override
            public void run() {
                emitLine("starting core from " + library);
                int code = CoreBridge.run(library, arguments);
                emitLine("core exited with " + code);
            }
        }, "singbox-core");
        coreThread.start();
    }

    /**
     * A tun inbound with no proxies: enough to prove the fd path. The DNS server is mandatory
     * because sing-box falls back to [::1]:53 on Android when the config has no dns block.
     */
    private void writeConfig(File config) throws Exception {
        String json = "{"
                + "\"log\":{\"level\":\"debug\"},"
                + "\"dns\":{\"servers\":[{\"type\":\"udp\",\"tag\":\"ali\",\"server\":\"223.5.5.5\"}],"
                + "\"final\":\"ali\"},"
                + "\"inbounds\":[{\"type\":\"tun\",\"tag\":\"tun-in\",\"address\":[\"172.19.0.1/30\"],"
                + "\"mtu\":1500,\"auto_route\":false,\"stack\":\"gvisor\"}],"
                + "\"outbounds\":[{\"type\":\"direct\",\"tag\":\"direct\"}],"
                + "\"route\":{\"default_domain_resolver\":\"ali\","
                + "\"rules\":[{\"action\":\"sniff\"},{\"protocol\":\"dns\",\"action\":\"hijack-dns\"}]," + "\"final\":\"direct\"}"
                + "}";
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(config), "UTF-8")) {
            writer.write(json);
        }
    }

    private Notification buildNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(new NotificationChannel(
                        CHANNEL_ID, "Spike core", NotificationManager.IMPORTANCE_LOW));
            }
        }
        Intent launch = new Intent(this, MainActivity.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pending = PendingIntent.getActivity(this, 0, launch, flags);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return builder
                .setContentTitle("YumeBox Spike")
                .setContentText("sing-box core running")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pending)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        if (tun != null) {
            try {
                tun.close();
            } catch (Exception error) {
                Log.w(TAG, "close tun", error);
            }
            tun = null;
        }
        super.onDestroy();
    }

    private static void emitLine(String line) {
        Log.i(TAG, line);
        Sink current = sink;
        if (current != null) {
            current.onLine(line);
        }
    }
}
