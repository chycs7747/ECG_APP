package com.example.mybluetooth;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

import android.graphics.Color;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ECGShowGraph extends AppCompatActivity {
    private LineChart lineChart; // 선그래프 선언
    private String username; // 유저이름 선언
    private ImageView mRtnBtn; //
    ArrayList<Integer> data = new ArrayList<>(512); //ecg 데이터를 저장할 공간 선언

    Retrofit retrofit = new Retrofit.Builder() // Builder객체를 이용한 몇가지 설정
            .baseUrl("http://ec2-13-124-219-134.ap-northeast-2.compute.amazonaws.com:8080/") // 어떤 서버로 네트워크 통신을 요청할 것인지에 대한 설정
            .addConverterFactory(GsonConverterFactory.create()) // GsonConverterFactory를 이용하여 데이터를 파싱
            .build(); // 객체에 설정한 정보를 이용하여 실질적으로 Retrofit 객체를 만들어 반환

    EcgApi ecgApi = retrofit.create(EcgApi.class); // 미리 정의해둔 Interface를 이용하여 실질적으로 사용할 클라이언트 객체를 생성

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ecgshow_graph);

        mRtnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { onBackBtnClicked(v); }
        });

        Intent intent = getIntent(); // 이전 엑티비티 (메인엑티비티)에서 정보를 넘겨받음 (ecg IntegerArrayList)
        data = intent.getIntegerArrayListExtra("ECG Data1");  // ecg데이터를 받아와서 data1에 저장
        username = intent.getStringExtra("Username"); // 유저 이름 정보를 받아와서 username에 저장
        ArrayList<Entry> entry_chart1 = new ArrayList<>(); // 그래프 데이터를 저장할 공간 선언
        lineChart = (LineChart) findViewById(R.id.chart); // 선그래프 선언
        LineData chartData = new LineData(); // 차트에 담길 데이터

        for(int i=0; i<512; i++) {
            int tmp = data.get(i);
            if(tmp!=0)
                entry_chart1.add(new Entry(i+1, tmp)); // ecg데이터 한묶음을 하나씩 저장
        }

        LineDataSet lineDataSet1 = new LineDataSet(entry_chart1, "LineGraph1"); // 데이터가 담긴 Arraylist 를 LineDataSet 으로 변환한다.
        lineDataSet1.setColor(Color.RED); // 해당 LineDataSet의 색 설정 :: 각 Line 과 관련된 세팅은 여기서 설정한다.
        lineDataSet1.setDrawValues(!lineDataSet1.isDrawValuesEnabled()); //그래프에 y값 표시 제거
        lineDataSet1.setDrawCircles(false); // 그래프 극점에 동그라미표시 제거

        chartData.addDataSet(lineDataSet1); // 해당 LineDataSet 을 적용될 차트에 들어갈 DataSet 에 넣는다.

        lineChart.setData(chartData); // 그래프에 DataSet을 넣는다.
        lineChart.invalidate(); // 그래능 업데이트
        lineChart.setVisibleXRange(50,150); // 그래프에 한번에 표시되는 x값 (최소값, 최대값)
        lineChart.setPinchZoom(true); //확대, 축소 가능
    }

    public void onSaveBtnClicked(View v) {
        HashMap<String, Object> input = new HashMap<>(); // tag, val을 담을 string, object hashpmap선언
        input.put("ecg", data.toString()); // "ecg", ecg데이터를 input에 넣음
        input.put("ecg_user", username); // "ecg_uaer", username을 input에 넣음
        Log.d("TEST", data.toString()); // 로그 테스트

        ecgApi.postecg(input).enqueue(new Callback<Post>() {
            @Override
            public void onResponse(Call<Post> call, Response<Post> response) {
                if (!response.isSuccessful()){
                    Log.d("TEST", "Response fail");
                    return;
                }
                Post data = response.body();
                Log.d("TEST", "Send Success");
                Log.d("TEST", data.getEcg_user());

                Toast.makeText(getApplicationContext(), "Send Success!", Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onFailure(Call<Post> call, Throwable t) {
                Log.d("TEST", "Connection fail");
                return;
            }
        });
    }

    public void onBackBtnClicked(View v) {
        finish(); // 뒤로가기 버튼 클릭시 ECGShowGraph 엑티비티 종료
    }
}