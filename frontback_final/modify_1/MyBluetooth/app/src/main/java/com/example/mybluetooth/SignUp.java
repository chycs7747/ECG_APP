package com.example.mybluetooth;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.widget.Button;
import android.widget.EditText;

public class SignUp extends AppCompatActivity {

    //GUI COMPONENTS
    private Button mRegisterButton;
    private EditText mPassword;
    private EditText mConfirmPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mRegisterButton = (Button)findViewById(R.id.register_button);
    }

    public void onBackButton1Clicked(View v) {
        Toast.makeText(getApplicationContext(),"goto Login Page..", Toast.LENGTH_LONG).show();
        finish();
    }

    public void onRegisterButtonClicked(View v) {
        if(mPassword.getText().toString().equals(mConfirmPassword)) {
            Toast.makeText(getApplicationContext(),"Register Success!", Toast.LENGTH_LONG).show();
            finish();
        }
        else {
            Toast.makeText(getApplicationContext(),"Please recheck your input", Toast.LENGTH_LONG).show();
        }




    }


}