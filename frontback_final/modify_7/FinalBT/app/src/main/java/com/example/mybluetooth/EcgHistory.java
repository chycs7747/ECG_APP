package com.example.mybluetooth;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.Button;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;

public class EcgHistory extends AppCompatActivity {

    private String username; // 유저네임을 저장할 변수 선언
    private Button mClearBtn; // 클리어 버튼을 저장할 변수 선언
    private Button mSearchBtn; // 클리어 버튼을 저장할 변수 선언
    private ListView ecg_list_view; // ecg데이터 목록을 저장할 리스트뷰 변수 선언
    private ImageButton mReturnBtn; // 메인 액티비티로 돌아가는 이미지버튼 변수 선언
    private String weather;
    private String micro_dust;
    private String tMicro_dust;
    private String ultraViolet;

    ArrayList<Post> postlist = new ArrayList<>();
    ArrayList<String> list_name = new ArrayList<>(); // 리스트뷰에 띄울 이름 목록들을 저장할 문자열 어레이리스트



    Retrofit retrofit = new Retrofit.Builder() // Builder객체를 이용한 몇가지 설정
            .baseUrl("http://ec2-13-124-219-134.ap-northeast-2.compute.amazonaws.com:8080/") // 어떤 서버로 네트워크 통신을 요청할 것인지에 대한 설정
            .addConverterFactory(GsonConverterFactory.create()) // GsonConverterFactory를 이용하여 데이터를 파싱
            .build(); // 객체에 설정한 정보를 이용하여 실질적으로 Retrofit 객체를 만들어 반환

    EcgApi ecgApi = retrofit.create(EcgApi.class); // 미리 정의해둔 Interface를 이용하여 실질적으로 사용할 클라이언트 객체를 생성

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ecg_history);


        Spinner SP_weather = findViewById(R.id.txt_question_type1);
        ArrayList<String> weather_name = new ArrayList<>();
        weather_name.add("날씨");
        weather_name.add("맑음");
        weather_name.add("흐림");
        weather_name.add("비");
        weather_name.add("눈");
        weather_name.add("소나기");
        weather_name.add("구름많음");
        weather_name.add("구름조금");
        weather_name.add("천둥번개");

        Spinner SP_microDust = findViewById(R.id.txt_question_type2);
        ArrayList<String> microDust_name = new ArrayList<>();
        microDust_name.add("미세먼지");
        microDust_name.add("좋음");
        microDust_name.add("보통");
        microDust_name.add("나쁨");
        microDust_name.add("매우나쁨");

        Spinner SP_tMicroDust = findViewById(R.id.txt_question_type3);
        ArrayList<String> tMicroDust_name = new ArrayList<>();
        tMicroDust_name.add("초미세먼지");
        tMicroDust_name.add("좋음");
        tMicroDust_name.add("보통");
        tMicroDust_name.add("나쁨");
        tMicroDust_name.add("매우나쁨");

        Spinner SP_ultraViolet = findViewById(R.id.txt_question_type4);
        ArrayList<String> ultraViolet_name = new ArrayList<>();
        ultraViolet_name.add("자외선");
        ultraViolet_name.add("좋음");
        ultraViolet_name.add("보통");
        ultraViolet_name.add("나쁨");
        ultraViolet_name.add("매우나쁨");

        ArrayAdapter<String> weatherAdapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, weather_name);
        weatherAdapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        SP_weather.setAdapter(weatherAdapter);

        ArrayAdapter<String> microDustAdapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, microDust_name);
        microDustAdapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        SP_microDust.setAdapter(microDustAdapter);

        ArrayAdapter<String> tMicroDustAdapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, tMicroDust_name);
        tMicroDustAdapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        SP_tMicroDust.setAdapter(tMicroDustAdapter);

        ArrayAdapter<String> ultraVioletAdapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, ultraViolet_name);
        ultraVioletAdapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        SP_ultraViolet.setAdapter(ultraVioletAdapter);

        mSearchBtn = (Button)findViewById(R.id.search_button);
        mClearBtn = (Button)findViewById(R.id.clear_button); // 기록을 clear 시키는 버튼 선언
        mReturnBtn = (ImageButton)findViewById(R.id.Return_from_history_to_main_button); // 메인화면으로 돌아가는 버튼 선언
        ecg_list_view = (ListView)findViewById(R.id.ecg_history_listview); // 기록 목록 리스트뷰 선언
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list_name); // 안드로이드가 미리 만들어 놓은 레이아웃 중에 simple_list_item_1.xml 파일을 읽어와라. 그리고 list name의 요소를 담을것임
        ecg_list_view.setAdapter(adapter); // 어답터 업데이트

        mReturnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { onRtnBtnClicked(v); }
        });

        Intent intent = getIntent(); // 메인엑티비티로부터 정보를 받음
        username = intent.getStringExtra("Username"); // username을 메인엑티비티로부터 받음


        SP_weather.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                weather = weather_name.get(i);
                System.out.println(weather);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                weather = "";
            }
        });

        SP_microDust.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                micro_dust = microDust_name.get(i);
                System.out.println(micro_dust);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                micro_dust = "";
            }
        });

        SP_tMicroDust.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                tMicro_dust = tMicroDust_name.get(i);
                System.out.println(tMicro_dust);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                tMicro_dust = "";
            }
        });

        SP_ultraViolet.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ultraViolet = ultraViolet_name.get(i);
                System.out.println(ultraViolet);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                ultraViolet = "";
            }
        });


        mSearchBtn.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View view) { // 클리어 버튼을 눌렀을 때 발생하는 call back 메소드
                postlist.clear(); // postlist 즉, 서버로부터 저장된 post데이터를 저장한 공간을 비운다.
                list_name.clear(); // list_name 즉, 리스트뷰에 저장될 이름목록들을 저장한 공간을 비운다.
                Map<String, String> query = new HashMap<>();
                query.put("user",username);
                if(!(weather.equals("날씨"))){
                    System.out.println("1");
                    query.put("weather",weather);
                }
                if(!(micro_dust.equals("미세먼지"))){
                    System.out.println("2");
                    query.put("micro_dust",micro_dust);
                }
                if(!(tMicro_dust.equals("초미세먼지"))){
                    System.out.println("3");
                    query.put("tmicro_dust",tMicro_dust);
                }
                if(!(ultraViolet.equals("자외선"))){
                    System.out.println("4");
                    query.put("uv_ray",ultraViolet);
                }



                Call<List<Post>> call = ecgApi.getecg(query);
                call.enqueue(new Callback<List<Post>>() {
                    @Override
                    public void onResponse(Call<List<Post>> call, Response<List<Post>> response) {
                        if (!response.isSuccessful()){
                            Log.d("TEST", "Response fail");
                            return;
                        }
                        List<Post> posts = response.body();
                        for (Post post : posts){
                            System.out.println(post.getEcg()); //Log.d("TEST", post.getEcg())
                            System.out.println(post.getCreated_date());
                            postlist.add(post);
                            String tmp_name = post.getCreated_date().split("\\+")[0].substring(0,16).replace("T"," | ");
                            String weather_tmp = post.getWeather();
                            if(weather_tmp.equals("맑음")){
                                tmp_name += " ☀";
                            }
                            else if(weather_tmp.equals("흐림")){
                                tmp_name += " ☁";
                            }
                            else if(weather_tmp.equals("비")){
                                tmp_name += " \uD83C\uDF27";
                            }
                            else if(weather_tmp.equals("눈")){
                                tmp_name += " ❄";
                            }
                            else if(weather_tmp.equals("구름많음")){
                                tmp_name += " \uD83C\uDF25";
                            }
                            else if(weather_tmp.equals("구름조금")){
                                tmp_name += " \uD83C\uDF24";
                            }
                            else if(weather_tmp.equals("천둥번개")){
                                tmp_name += " ⚡";
                            }
                            else if(weather_tmp.equals("소나기")){
                                tmp_name += " ☔";
                            }
                            else{
                                tmp_name += " ?";
                            }
                            list_name.add(tmp_name);
                        }

                        // 어답터를 업데이트 시킴 -> ecg_list_view가 어답터를 통해 업데이트됨 (ecg_list_view 는 list_name에서 요소를 받아옴)
                        adapter.notifyDataSetChanged(); // 어답터 업데이트 => (History + (i+1)의 이름의 ecg_list_view (기록 목록 리스트뷰)를 업데이트
                    }

                    @Override
                    public void onFailure(Call<List<Post>> call, Throwable t) {
                        Log.d("TEST", "Connection fail");
                        return;
                    }
                });

                adapter.notifyDataSetChanged(); // 어답터를 업데이트 시킨다.
            }
        });

        ecg_list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) { // ecg리스트 목록에서 저장된 정보를 누를때 작동하는 call back 메소드
                Intent intent = new Intent(getApplicationContext(), ECGshowHistoryGraph.class); // 새 intent 생성
                intent.putExtra("postlist", postlist.get(position));
                startActivity(intent); // intent 실행

            }
        });

        mClearBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) { // 클리어 버튼을 눌렀을 때 발생하는 call back 메소드
                postlist.clear();
                list_name.clear(); // list_name 즉, 리스트뷰에 저장될 이름목록들을 저장한 공간을 비운다.
                adapter.notifyDataSetChanged(); // 어답터를 업데이트 시킨다.
            }
        }) ;
    }
    
    public void onRtnBtnClicked(View v) { // 돌아가기 버튼을 클릭했을 때 발생하는 call back 메소드
        finish(); // 현재 엑티비티를 종료하고 이전 엑티비티 (메인 엑티비티) 로 돌아간다.
    }
}