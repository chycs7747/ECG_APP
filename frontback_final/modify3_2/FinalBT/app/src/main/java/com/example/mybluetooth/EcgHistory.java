package com.example.mybluetooth;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

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
    private ListView ecg_list_view; // ecg데이터 목록을 저장할 리스트뷰 변수 선언
    private ImageButton mReturnBtn; // 메인 액티비티로 돌아가는 이미지버튼 변수 선언

    ArrayList<ArrayList<Integer>> datalist = new ArrayList<>(); // 서버로부터 받는 데이터를 저장할 공간 (각 인덱스당 한번 측정한 ecg데이터를 추가함) >> 이중 정수 어레이리스트
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

        mClearBtn = (Button)findViewById(R.id.clear_button); // 기록을 clear 시키는 버튼 선언
        ecg_list_view = (ListView)findViewById(R.id.ecg_history_listview); // 기록 목록 리스트뷰 선언
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list_name); // 안드로이드가 미리 만들어 놓은 레이아웃 중에 simple_list_item_1.xml 파일을 읽어와라. 그리고 list name의 요소를 담을것임
        ecg_list_view.setAdapter(adapter); // 어답터 업데이트

        mReturnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { onRtnBtnClicked(v); }
        });

        Intent intent = getIntent(); // 메인엑티비티로부터 정보를 받음
        username = intent.getStringExtra("Username"); // username을 메인엑티비티로부터 받음

        Call<List<Post>> call = ecgApi.getecg(username);
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
                    String[] tmp = post.getEcg().replaceAll(" ","").replaceAll("\\[","").replaceAll("]","").split(",");
                    ArrayList<Integer> arrlst = new ArrayList<>(512);
                    for (int i = 0; i<512; i++){
                        try {
                            arrlst.add(Integer.parseInt(tmp[i]));
                        } catch (Exception e) {
                            System.out.println("Unable to parse string to int: " + e.getMessage());
                        }
                    }
                    /*
                    이 저장부분을 onResponse 밖에서 실행하면 onResponse 메소드보다 먼저 실행되므로
                    datalist 는 arrlist를 전달받지 못한다.
                     */
                    datalist.add(arrlst); // 서버로부터 받읕 arrlist를 datalist에 저장한다.

                }
                for(int i=0; i<datalist.size(); i++) {
                    //이부분 또한 doResponse 안에서 실행해야 한다.
                    list_name.add("History "+(i+1)); // "History + (i+1)"의 이름으로 list_name에 datalist의 개수(i개)만큼 요소를 추가한다.
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


        ecg_list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) { // ecg리스트 목록에서 저장된 정보를 누를때 작동하는 call back 메소드
                Intent intent = new Intent(getApplicationContext(), ECGshowHistoryGraph.class); // 새 intent 생성
                intent.putExtra("ECG DATA", datalist.get(position)); // Adapter 속의 index와 지역변수 position은 같으므로, position을 통한 index처리
                startActivity(intent); // intent 실행

            }
        });

        mClearBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) { // 클리어 버튼을 눌렀을 때 발생하는 call back 메소드
                datalist.clear(); // datalist 즉, 서버로부터 저장된 ecg데이터를 저장한 공간을 비운다.
                list_name.clear(); // list_name 즉, 리스트뷰에 저장될 이름목록들을 저장한 공간을 비운다.
                adapter.notifyDataSetChanged(); // 어답터를 업데이트 시킨다.
            }
        }) ;
    }
    
    public void onRtnBtnClicked(View v) { // 돌아가기 버튼을 클릭했을 때 발생하는 call back 메소드
        finish(); // 현재 엑티비티를 종료하고 이전 엑티비티 (메인 엑티비티) 로 돌아간다.
    }
}