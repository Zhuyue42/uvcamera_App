package com.vinda.ucvcamera;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.yuan.camera_test.R;

public class activity_Home extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        findViewById(R.id.profileicon).setOnClickListener(v -> {
            //跳转
            Intent intent = new Intent(activity_Home.this, activity_Profile.class);
            startActivity(intent);
        });
        findViewById(R.id.playbutton).setOnClickListener(v -> {
            //跳转
            Intent intent = new Intent(activity_Home.this, MainActivity.class);
            startActivity(intent);
        });
    }
}