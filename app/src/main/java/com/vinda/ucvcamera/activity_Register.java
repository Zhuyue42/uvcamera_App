package com.vinda.ucvcamera;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.yuan.camera_test.R;

public class activity_Register extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        findViewById(R.id.inregister).setOnClickListener(v -> {
            //跳转
            Intent intent = new Intent(activity_Register.this, activity_Login.class);
            startActivity(intent);
        });
        findViewById(R.id.btn_register).setOnClickListener(v -> {
            //跳转
            Intent intent = new Intent(activity_Register.this, activity_Home.class);
            startActivity(intent);
        });
    }
}