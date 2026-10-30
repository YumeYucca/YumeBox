package com.yumebox.spike;

import android.app.Activity;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** One screen: start / stop the spike VPN, and show the core's log stream. */
public final class MainActivity extends Activity implements CoreVpnService.Sink {
    private static final int REQUEST_VPN = 1;

    private TextView output;

    /** Named instead of anonymous on purpose: the d8 shipped with build-tools 34 trips over
     *  javac 21's anonymous-class attributes. */
    private final class StartClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            startVpn();
        }
    }

    private final class StopClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            stopService(new Intent(MainActivity.this, CoreVpnService.class));
            if (output != null) {
                output.setText("");
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 48, 32, 32);

        Button start = new Button(this);
        start.setText("Start core VPN");
        start.setOnClickListener(new StartClickListener());
        root.addView(start);

        Button stop = new Button(this);
        stop.setText("Stop");
        stop.setOnClickListener(new StopClickListener());
        root.addView(stop);

        output = new TextView(this);
        output.setGravity(Gravity.START);
        output.setTextIsSelectable(true);
        output.setTextSize(11);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(output);
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
        CoreVpnService.setSink(this);
    }

    private void startVpn() {
        Intent prepare = VpnService.prepare(this);
        if (prepare != null) {
            startActivityForResult(prepare, REQUEST_VPN);
            return;
        }
        startService(new Intent(this, CoreVpnService.class));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_VPN && resultCode == RESULT_OK) {
            startService(new Intent(this, CoreVpnService.class));
        }
    }

    @Override
    public void onLine(final String line) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (output != null) {
                    output.append(line);
                    output.append("\n");
                }
            }
        });
    }
}
