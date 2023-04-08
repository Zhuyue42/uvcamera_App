package com.vinda.ucvcamera;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.yuan.camera_test.R;

public class activity_Login extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        findViewById(R.id.inlogin).setOnClickListener(v -> {
            //跳转
            Intent intent = new Intent(activity_Login.this, activity_Register.class);
            startActivity(intent);
        });
        findViewById(R.id.btn_login).setOnClickListener(v -> {
            //跳转
            Intent intent = new Intent(activity_Login.this, activity_Home.class);
            startActivity(intent);
        });
    }
}