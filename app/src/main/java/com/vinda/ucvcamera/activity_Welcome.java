package com.vinda.ucvcamera;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.yuan.camera_test.R;

public class activity_Welcome extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        findViewById(R.id.btLogin).setOnClickListener(v -> {
            //跳转
            Intent intent = new Intent(activity_Welcome.this, activity_Login.class);
            startActivity(intent);
        });
        findViewById(R.id.btRegister).setOnClickListener(v -> {
            //跳转
            Intent intent = new Intent(activity_Welcome.this, activity_Register.class);
            startActivity(intent);
        });
    }
}