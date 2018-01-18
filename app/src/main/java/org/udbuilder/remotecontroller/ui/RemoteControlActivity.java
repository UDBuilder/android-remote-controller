package org.udbuilder.remotecontroller.ui;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import org.udbuilder.remotecontroller.R;

public class RemoteControlActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote_control);
    }

    public void onLaunchSender(View view) {
        Intent intent = new Intent(this, SenderActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    public void onLaunchReceiver(View view) {
        Intent intent = new Intent(this, ReceiverActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
