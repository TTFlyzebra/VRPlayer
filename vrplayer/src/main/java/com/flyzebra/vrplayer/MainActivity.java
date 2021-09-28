package com.flyzebra.vrplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.flyzebra.ffplay.GlVideoView;

public class MainActivity extends AppCompatActivity {

    private GlVideoView glVideoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        glVideoView = findViewById(R.id.ac_vlvv01);
        glVideoView.playUrl("rtsp://172.30.16.234:8554");
        //glVideoView.playUrl("/sdcard/camera/GaoQingOK.mp4");
    }
}