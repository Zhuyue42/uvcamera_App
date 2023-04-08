package com.vinda.ucvcamera;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.yuan.camera_test.R;

public class activity_Profile extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        findViewById(R.id.homeicon).setOnClickListener(v -> {
            //跳转
            Intent intent = new Intent(activity_Profile.this, activity_Home.class);
            startActivity(intent);
        });
        findViewById(R.id.playbutton).setOnClickListener(v -> {
            //跳转
            Intent intent = new Intent(activity_Profile.this, MainActivity.class);
            startActivity(intent);
        });
    }
}