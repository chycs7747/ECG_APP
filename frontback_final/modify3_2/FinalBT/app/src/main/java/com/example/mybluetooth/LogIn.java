package com.example.mybluetooth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class LogIn extends AppCompatActivity {

    EditText username;
    EditText password;

    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("http://ec2-13-124-219-134.ap-northeast-2.compute.amazonaws.com:8080/")
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    LoginApi loginApi = retrofit.create(LoginApi.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);

        username = findViewById(R.id.input_login_address);
        password = findViewById(R.id.input_login_password);

        // GIF
        ImageView heart_beating_img = (ImageView)findViewById(R.id.heartbeating_gif_img);
        Glide.with(this).load(R.raw.heartbeating).into(heart_beating_img);
    }

    public void onSignUpButtonClicked(View v) { // signup 버튼을 눌렀을 때 발생하는 event를 처리하는 call back 메소드
        Intent intent = new Intent(getApplicationContext(), SignUp.class); // SignUp 엑티비티에 대한 인텐트 생성
        startActivity(intent); // SignUp 엑티비티 실행
    }

    public void onLoginButtonClicked(View v) {
        Call<List<Post>> call = loginApi.getdata(username.getText().toString());
        call.enqueue(new Callback<List<Post>>(){
            @Override
            public void onResponse(Call<List<Post>> call, Response<List<Post>> response) {
                if (!response.isSuccessful())
                {
                    Log.d("TEST", "POST Not Success");
                    return;
                }
                List<Post> posts = response.body();

                try {
                    if (posts.get(0).getPassword().equals(password.getText().toString())) {

                        Log.d("TEST", "Log in Success");

                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.putExtra("Login_user", username.getText().toString());
                        username.setText("");
                        password.setText("");
                        startActivity(intent);
                    } else {
                        Toast.makeText(getApplicationContext(), "Wrong password..", Toast.LENGTH_LONG).show();
                        Log.d("TEST", posts.get(0).getPassword());
                    }
                }
                catch(Exception e){
                    Toast.makeText(getApplicationContext(), "Wrong E-mail..", Toast.LENGTH_LONG).show();
                }

            }

            @Override
            public void onFailure(Call<List<Post>> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "Internet connection..", Toast.LENGTH_LONG).show();
                Log.d("TEST", "POST Not Success ERR");
                return;
            }
        });
    }
}