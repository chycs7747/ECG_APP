package com.example.mybluetooth;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SignUp extends AppCompatActivity {
    //GUI COMPONENTS
    private Button mRegisterButton;
    private EditText mEmail;
    private EditText mPassword;
    private EditText mConfirmPassword;

    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("http://ec2-13-124-219-134.ap-northeast-2.compute.amazonaws.com:8080/")
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    LoginApi loginApi = retrofit.create(LoginApi.class);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        ImageButton mReturnBtn = findViewById(R.id.Return_to_Login_button);
        mRegisterButton = findViewById(R.id.register_button);
        mEmail = findViewById(R.id.input_signup_address);
        mPassword = findViewById(R.id.input_signup_password);
        mConfirmPassword = findViewById(R.id.input_signup_confirm_password);
        mReturnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { onReturnButtonClicked(v); }
        });
    }

    public void onReturnButtonClicked(View v) { // 돌아가기 버튼을 클릭했을때
        finish();
    }

    public void onRegisterButtonClicked(View v) {
        if (mPassword.getText().toString().equals(mConfirmPassword.getText().toString())) {
            HashMap<String, Object> input = new HashMap<>();
            input.put("email", mEmail.getText().toString());
            input.put("password", mPassword.getText().toString());

            Call<List<Post>> call1 = loginApi.getdata(mEmail.getText().toString());
            call1.enqueue(new Callback<List<Post>>() {
                @Override
                public void onResponse(Call<List<Post>> call, Response<List<Post>> response) {
                    if (!response.isSuccessful()) {
                        Toast.makeText(getApplicationContext(), "Fail to get data", Toast.LENGTH_LONG).show();
                        return;
                    }
                    List<Post> posts = response.body();
                    try {
                        Log.d("TEST", "try");
                        if (posts.get(0).getEmail().equals(mEmail.getText().toString())) {
                            Toast.makeText(getApplicationContext(), "Same Email already exist", Toast.LENGTH_LONG).show();
                            Log.d("TEST", "if");
                            return;
                        }
                        Log.d("TEST", "try nd");
                        Toast.makeText(getApplicationContext(), "Fill in the email", Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Log.d("TEST", "catch");
                        loginApi.postdata(input).enqueue(new Callback<Post>() {
                            @Override
                            public void onResponse(Call<Post> call, Response<Post> response) {
                                if (!response.isSuccessful()) {
                                    Log.d("TEST", "POST Not Success");
                                    Toast.makeText(getApplicationContext(), "something is wrong", Toast.LENGTH_LONG).show();
                                    return;
                                }
                                Post data = response.body();
                                Log.d("TEST", "POST Success");
                                Log.d("TEST", data.getEmail());

                                Toast.makeText(getApplicationContext(), "Register Success!", Toast.LENGTH_LONG).show();
                                finish();
                            }

                            @Override
                            public void onFailure(Call<Post> call, Throwable t) {
                                Toast.makeText(getApplicationContext(), "Internet connection..", Toast.LENGTH_LONG).show();
                                Log.d("TEST", "POST Not Success ERR");
                                return;
                            }
                        });
                    }
                }

                @Override
                public void onFailure(Call<List<Post>> call, Throwable t) {
                    Toast.makeText(getApplicationContext(), "Internet connection..", Toast.LENGTH_LONG).show();
                    Log.d("TEST", "POST Not Success ERR");

                    return;
                }
            });

        } else {
            Toast.makeText(getApplicationContext(), "Please recheck your password", Toast.LENGTH_LONG).show();
        }
    }
}