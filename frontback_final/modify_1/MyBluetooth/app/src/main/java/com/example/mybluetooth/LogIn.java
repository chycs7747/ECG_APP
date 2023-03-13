package com.example.mybluetooth;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

public class LogIn extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);
    }

    public void onSignUpButtonClicked(View v) {
        Toast.makeText(getApplicationContext(), "Goto SignUp Page..", Toast.LENGTH_LONG
        ).show();

        Intent intent = new Intent(getApplicationContext(), SignUp.class);
        startActivity(intent);
    }
}