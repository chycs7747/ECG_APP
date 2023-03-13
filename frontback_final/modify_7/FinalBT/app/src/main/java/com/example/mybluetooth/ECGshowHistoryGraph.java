package com.example.mybluetooth;

import androidx.appcompat.app.AppCompatActivity;

import android.media.Image;
import android.util.Log;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
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

public class ECGshowHistoryGraph extends AppCompatActivity {

    //선 그래프
    private LineChart lineChart; // 선그래프 선언
    private String username; // 유저이름 선언
    private String date; // 날짜
    private String weather; // 날씨
    private String temp; // 온도
    private String micro_dust; // 미세먼지
    private String tMicro_dust; // 초미세먼지
    private String uv_ray; // 자외선
    private ImageButton mRtnBtn;
    private TextView title;
    private TextView additional;
    ArrayList<Integer> data = new ArrayList<>(512); //ecg 데이터를 저장할 공간 선언


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ecgshow_history_graph);
        title = findViewById(R.id.ecg_chart2);
        additional = findViewById(R.id.additional_data);
        mRtnBtn = (ImageButton)findViewById(R.id.Return_from_graph_to_history_button);

        mRtnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { onBackBtnClicked2(v); }
        });

        Intent intent = getIntent();
        Post post = (Post)intent.getSerializableExtra("postlist");

        String[] ecgData = post.getEcg().replaceAll(" ","").replaceAll("\\[","").replaceAll("]","").split(",");
        for (int i = 0; i<512; i++){
            try {
                data.add(Integer.parseInt(ecgData[i]));
            } catch (Exception e) {
                System.out.println("Unable to parse string to int: " + e.getMessage());
            }
        }
        date = post.getCreated_date().split("\\+")[0].substring(0,16).replace("T"," | ");
        title.setText(title.getText().toString()+"\n"+date);
        String dataFiled = "";
        weather = post.getWeather();
        dataFiled = dataFiled + "Weather : " + weather;
        if(weather.equals("맑음")){
            dataFiled += " ☀";
        }
        else if(weather.equals("흐림")){
            dataFiled += " ☁";
        }
        else if(weather.equals("비")){
            dataFiled += " \uD83C\uDF27";
        }
        else if(weather.equals("눈")){
            dataFiled += " ❄";
        }
        else if(weather.equals("구름많음")){
            dataFiled += " \uD83C\uDF25";
        }
        else if(weather.equals("구름조금")){
            dataFiled += " \uD83C\uDF24";
        }
        else if(weather.equals("천둥번개")){
            dataFiled += " ⚡";
        }
        else if(weather.equals("소나기")){
            dataFiled += " ☔";
        }
        else{
            dataFiled += " ?";
        }
        dataFiled += "  Location : " + post.getLocation() +"\n";
        temp = post.getTemperature();
        dataFiled = dataFiled + "Temperature : " + temp + "\n";
        micro_dust = post.getMicro_dust();
        dataFiled = dataFiled + "Micro dust : " + micro_dust + "\n";
        tMicro_dust = post.getTmicro_dust();
        dataFiled = dataFiled + "Tiny micro dust : " + tMicro_dust + "\n";
        uv_ray = post.getUv_ray();
        dataFiled = dataFiled + "Ultra violet ray : " + uv_ray + "\n";

        additional.setText(dataFiled);

        username = post.getEcg_user(); // 유저 이름 정보를 받아와서 username에 저장
        ArrayList<Entry> entry_chart1 = new ArrayList<>(); // 데이터를 담을 Arraylist
        lineChart = (LineChart) findViewById(R.id.chart2); // return HttpResponse("안녕하세요 pybo에 오신것을 환영합니다.")
        LineData chartData = new LineData(); // 차트에 담길 데이터


        for(int i=0; i<512; i++) {
            int tmp = data.get(i);
            if(tmp!=0)
                entry_chart1.add(new Entry(i+1, tmp)); // ecg데이터 한묶음을 하나씩 저장
        }



        LineDataSet lineDataSet1 = new LineDataSet(entry_chart1, "LineGraph1"); // 데이터가 담긴 Arraylist 를 LineDataSet 으로 변환한다.
        lineDataSet1.setColor(Color.RED); // 해당 LineDataSet의 색 설정 :: 각 Line 과 관련된 세팅은 여기서 설정한다.
        lineDataSet1.setDrawValues(!lineDataSet1.isDrawValuesEnabled()); // 그래프에 y값 제거
        lineDataSet1.setDrawCircles(false); // 그래프 극점프 동그라미 표시 제거
        lineDataSet1.setLabel("ECG");
        chartData.addDataSet(lineDataSet1); // 해당 LineDataSet 을 적용될 차트에 들어갈 DataSet 에 넣는다.
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.setData(chartData); // 그래프에 DataSet을 넣는다.
        lineChart.invalidate(); // 그래프 업데이트
        lineChart.setVisibleXRange(50,150); // 그래프에 한번에 표시되는 x값 (최소값, 최대값)
        lineChart.setPinchZoom(true); // 확대,축소 가능
    }

    public void onBackBtnClicked2(View v) {
        finish(); // 뒤로가기 버튼 클릭시 ECGShowHistoryGraph 엑티비티 종료
    }
}