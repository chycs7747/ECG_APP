package com.example.mybluetooth;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {
    /*
    선언부 (Declaration)
     */
    // flag
    // sum != 0 으로 초기화시켜야 블루투스를 연결하지 않은 상황에서
    // ECG START 버튼을 아무리 눌러도 작동이 되지 않는다.

    private int sum = 10;
    // GUI Components
    private ImageButton mlogOut;
    private ImageView btImg;                        // 블루투스 온/오프 이미지
    private Button mListPairedDevicesBtn;           // 페어링 가능한 기기 목록 보여주기 버튼
    private Button mECGStartBtn;                    // ECG 키트 측정 시작 버튼
    private Button mECGHistoryBtn;                  // ECG History 페이지 전환 버튼
    private Button mShowGraphBtn;                   // 그래프 페이지 전환 버튼
    private BluetoothAdapter mBTAdapter;            // 블루투스 어댑터
    private Set<BluetoothDevice> mPairedDevices;    // 페어링 가능 기기 목록 담는 자료구조
    private ArrayAdapter<String> mBTArrayAdapter;   //
    private ListView mDevicesListView;              // 페어링 가능 기기 목록 보여주는 리스트
    private String username;                        // 유저 이름

    private Handler mHandler;                       // Our main handler that will receive callback notifications
    private ConnectedThread mConnectedThread;       // bluetooth background worker thread to send and receive data
    private BluetoothSocket mBTSocket = null;       // bi-directional client-to-client data path


    // 블루투스 모드를 위한 고유 UUID 번호 ("random" unique identifier)
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


    // 블루투스 상태 표시 (#defines for identifying shared types between calling functions)
    private final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names
    private final static int MESSAGE_READ = 2;      // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status
    private int PAIR_STATUS = 0;

    // 데이터 전송 현황 보여주는 ProgressBar
    private ProgressBar mProgressBar;
    private TextView mProgressData;
    ArrayList<Integer> final_data = new ArrayList<>(512); // 측정 데이터 담는 ArrayList

    // GPS
    private GpsTracker gpsTracker;
    private double latitude = 0;
    private double longitude = 0;
    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    String[] REQUIRED_PERMISSIONS  = {android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

    /*
    메인 페이지 (선언한 기능에 대한 구현부)
    Main Page (Implementation of the declared functions)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        username = intent.getStringExtra("Login_user");             // 로그인한 유저 이름 가져오기
        // BluetoothAdapter.ACTION_STATE_CHANGED에 대한 intent를 받고 blReceiver에 filter을 적용시켜 레지스터를 등록한다.
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(blReceiver, filter);

        mlogOut = (ImageButton) findViewById(R.id.logOut);
        btImg = (ImageView) findViewById(R.id.btOff_img);
        mECGStartBtn = (Button) findViewById(R.id.start_ecg_btn);
        mListPairedDevicesBtn = (Button) findViewById(R.id.PairedBtn);
        mECGHistoryBtn = (Button) findViewById(R.id.ecgHistoryBtn);
        mShowGraphBtn = (Button) findViewById(R.id.showGraphBtn);

        mBTArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        // 블루투스 어댑터에 핸들러 활성화 (get a handle on the bluetooth radio)
        mBTAdapter = BluetoothAdapter.getDefaultAdapter();

        // 페어링 가능 기기 목록 제시 및 선택된 기기 페어링 수행
        mDevicesListView = (ListView) findViewById(R.id.devicesListView);
        mDevicesListView.setAdapter(mBTArrayAdapter); // assign model to view
        mDevicesListView.setOnItemClickListener(mDeviceClickListener);

        // ProgressBar
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mProgressData = (TextView) findViewById(R.id.progress_data);

        if(mBTAdapter.isEnabled()) {
            btImg.setImageResource(R.drawable.bton);
        }
        else {
            btImg.setImageResource(R.drawable.btoff);
        }
        if (!checkLocationServicesStatus()) {
            showDialogForLocationServiceSetting();
        }else {
            checkRunTimePermission();
        }


        // 블루투스 핸들러 동작 알고리즘 (ECG 키트에서 안드로이드 앱으로 데이터 수신하는 알고리즘)
        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                // START ECG 버튼을 누르면 키트로부터 데이터를 읽어온다
                if (msg.what == MESSAGE_READ) {
                    // Back GettingData to START ECG
                    mECGStartBtn.setText("START ECG");
                    mECGStartBtn.setBackgroundResource(R.drawable.btn_blue);
                    // 측정 데이터 가져오기 전 초기화 (Initialize Data Variable of ECG)
                    int tmpNum = 0, tmpNum1 = 0, tmpNum2 = 0;

                    /*
                    측정 데이터 값의 범위가 0 ~ 1000 이다.
                    하지만 블루투스를 통해서 데이터를 넘기기 위해서는 Byte 단위로 전달해야하는데,
                    1 byte = 8 bit 이므로, 0 ~ 255 까지만 표현이 가능하다.
                    따라서 256 ~ 1000 까지의 데이터 또한 표현 가능하도록 해야 측정 데이터의 그래프 표현이 가능해지므로
                    1 byte 두 부분을 받아서 값을 원래 측정된 값으로 다시 변환하여 앱 상 데이터 목록에 추가한다.
                     */
                    for (int i = 0; i < 512; i++) {
                        tmpNum1 = ((byte[]) msg.obj)[i * 2] & 0xff;
                        tmpNum2 = ((byte[]) msg.obj)[i * 2 + 1] & 0xff;
                        if (tmpNum1 != 0) {
                            tmpNum = ((tmpNum1 << 8) & 0xff00) + (tmpNum2 & 0x00ff);
                            final_data.add(tmpNum - 256);
                        } else {
                            sum += i;
                            System.out.println(sum);
                            mProgressBar.setProgress(sum);
                            mProgressData.setText(Integer.toString(sum * 100 / 512) + "%");
                            mShowGraphBtn.setClickable(true);
                            break;
                        }
                    }
                    // 전송 완료
                    if (sum == 512) {
                        System.out.println("finished");
                        sum = 0;
                    }
                }
                // 페어링 기기 연결 여부에 따른 화면 출력
                if (msg.what == CONNECTING_STATUS) {
                    if (msg.arg1 == 1)
                        btImg.setImageResource(R.drawable.link);
                    else
                        Toast.makeText(getApplicationContext(), "Connection Failed", Toast.LENGTH_SHORT).show();
                }
            }
        };

        // 블루투스 연결 가능한 기기가 없음을 표현
        if (mBTArrayAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(getApplicationContext(),"Bluetooth device not found!",Toast.LENGTH_SHORT).show();
        }
        // 기기 페어링 완료 후 ECG START 버튼 눌렀을 때, 값을 가져올 수 있게 만들도록 함
        else {
            mECGStartBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    if(mConnectedThread != null) { //First check to make sure thread created
                        final_data.clear();
                    }
                }
            });

            // 페어링 가능 기기 목록 클릭 활성화 버튼
            mListPairedDevicesBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v){
                    //Toast.makeText(getApplicationContext(),"paired btn clicked", Toast.LENGTH_SHORT).show();
                    listPairedDevices(v);
                }
            });
            // ECG START 버튼
            mECGStartBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    // sum != 0으로 처음에 초기화 해주는 이유
                    gpsTracker = new GpsTracker(MainActivity.this);

                    latitude = gpsTracker.getLatitude();
                    longitude = gpsTracker.getLongitude();

                    String address = getCurrentAddress(latitude, longitude);
                    System.out.print("위도: ");
                    System.out.println(latitude);
                    System.out.print("걍도: ");
                    System.out.println(longitude);

                    if (sum==0) {
                        if(PAIR_STATUS != 0) { // 블루투스가 연결되어 있지만, 페어링 안된 상태에서 데이터 수신방지
                            // ECG START 버튼이 눌려지고 데이터를 받아오고 있음을 UI 상에 표현되게 만듬
                            mECGStartBtn.setText("Getting Data...");
                            mECGStartBtn.setBackgroundResource(R.drawable.btn_green);
                            final_data.clear();
                            mProgressBar.setProgress(0);
                            mProgressData.setText("0%");
                            mConnectedThread.write("1");
                        }
                    }
                    else {
                        Toast.makeText(getApplicationContext(),"Pair your device first!", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        // 로그아웃 버튼 클릭
        mlogOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { logoutButtonClicked(v); }
        });


    }

    // 블루투스 연결 가능한 기기를 발견하면 리스트에 페어링 가능 기기로 추가한다
    final BroadcastReceiver blReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // add the name to the list
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                mBTArrayAdapter.notifyDataSetChanged();
            }

            if(action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                if (mBTAdapter.getState() == BluetoothAdapter.STATE_ON) {
                    Toast.makeText(getApplicationContext(),"Bluetooth on",Toast.LENGTH_SHORT).show();
                    btImg.setImageResource(R.drawable.bton);
                }
                if (mBTAdapter.getState() == BluetoothAdapter.STATE_OFF) {
                    btImg.setImageResource(R.drawable.btoff);
                    mECGStartBtn.setText("START ECG"); // ecg데이터 수신 중 블루투스를 끊었을 때 스타트 버튼 텍스트 원래대로 변경
                    mECGStartBtn.setBackgroundResource(R.drawable.btn_blue); // ecg데이터 수신 중 블루투스를 끊었을 때 스타트 버튼 배경색 원래대로 변경
                    Toast.makeText(getApplicationContext(),"Bluetooth offed",Toast.LENGTH_SHORT).show();
                    // The user bluetooth is already disabled.
                    return;
                }
            }
        }
    };

    // 페어링 가능한 기기 목록을 보여준다
    private void listPairedDevices(View view){
        mPairedDevices = mBTAdapter.getBondedDevices();
        if(mBTAdapter.isEnabled()) {
            // put it's one to the adapter
            for (BluetoothDevice device : mPairedDevices)
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());

            Toast.makeText(getApplicationContext(), "Show Paired Devices", Toast.LENGTH_SHORT).show();
        }
        else
            Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
    }

    // 페어링 가능 기기 목록에서 연결하고자 하는 기기를 선택했을 때
    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // 현재 기기의 블루투스가 활성화 되어있지 않음
            if(!mBTAdapter.isEnabled()) {
                Toast.makeText(getBaseContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
                return;
            }
            // 현재 기기와 페어링 시도하는 기기 사이의 연결 시도
            sum=0;
            // 페어링 시도하는 기기의 MAC 주소를 가져온다 (MAC 주소 : 리스트뷰 상에서 마지막 17글자)
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            final String address = info.substring(info.length() - 17);
            final String name = info.substring(0,info.length() - 17);

            // Spawn a new thread to avoid blocking the GUI one
            new Thread()
            {
                public void run() {
                    boolean fail = false;

                    BluetoothDevice device = mBTAdapter.getRemoteDevice(address);

                    try {
                        mBTSocket = createBluetoothSocket(device);
                    } catch (IOException e) {
                        fail = true;
                        Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                    }
                    // 블루투스 소켓 연결 활성화 시키기
                    // Establish the Bluetooth socket connection.
                    try {
                        mBTSocket.connect();
                    } catch (IOException e) {
                        try {
                            fail = true;
                            PAIR_STATUS = 0; // 현재 블루투스 페어링이 성공적이지 않은 상태
                            btImg.setImageResource(R.drawable.bton); // 페어링을 끊었을 때 블루투스 켜짐 이미지로 바꾸어줌
                            mECGStartBtn.setText("START ECG"); // ecg데이터 수신 중 페어링을 끊었을 때 스타트 버튼 텍스트 원래대로 변경
                            mECGStartBtn.setBackgroundResource(R.drawable.btn_blue); // ecg데이터 수신 중 페어링을 끊었을 때 스타트 버튼 배경색 원래대로 변경
                            mBTSocket.close();
                            mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
                                    .sendToTarget();
                        } catch (IOException e2) {
                            //insert code to deal with this
                            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                    if(fail == false) {
                        mConnectedThread = new ConnectedThread(mBTSocket);
                        mConnectedThread.start();
                        PAIR_STATUS = 1; // 현재 블루투스 페어링이 성공적인 상태
                        mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, name)
                                .sendToTarget();
                    }
                }
            }.start();
        }
    };

    // 블루투스 소켓 생성
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connection with BT device using UUID
    }

    // 그래프 페이지로 전환
    public void onShowGraphBtnClicked(View v) {
        if(final_data.size()>=512) {
            Intent intent = new Intent(getApplicationContext(), ECGShowGraph.class);
            intent.putExtra("ECG Data1", final_data);
            intent.putExtra("Username", username);
            intent.putExtra("n", latitude);
            intent.putExtra("e", longitude);

            startActivity(intent);
        }
        // 기기 상에 데이터가 존재하지 않을 경우, 측정 먼저 하라는 토스트 메시지를 띄운다
        else {
            Toast.makeText(getApplicationContext(), "Start ECG Button first!", Toast.LENGTH_SHORT
            ).show();
        }
    }

    // Username 전달하면서 ECG History 페이지로 전환
    public void ecgHistoryButtonClicked(View v) {
        Intent intent = new Intent(getApplicationContext(), EcgHistory.class);
        intent.putExtra("Username", username);
        startActivity(intent);
    }

    // Logout 버튼 클릭시 로그아웃 토스트 메시지 띄우면서 로그인 페이지로 전황
    public void logoutButtonClicked(View v) {
        Toast.makeText(getApplicationContext(),"Logout Complete!", Toast.LENGTH_SHORT).show();
        finish();
    }

    //gps start
    /*
     * ActivityCompat.requestPermissions를 사용한 퍼미션 요청의 결과를 리턴받는 메소드입니다.
     */
    @Override
    public void onRequestPermissionsResult(int permsRequestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grandResults) {

        if ( permsRequestCode == PERMISSIONS_REQUEST_CODE && grandResults.length == REQUIRED_PERMISSIONS.length) {

            // 요청 코드가 PERMISSIONS_REQUEST_CODE 이고, 요청한 퍼미션 개수만큼 수신되었다면

            boolean check_result = true;


            // 모든 퍼미션을 허용했는지 체크합니다.

            for (int result : grandResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false;
                    break;
                }
            }


            if ( check_result ) {

                //위치 값을 가져올 수 있음
                ;
            }
            else {
                // 거부한 퍼미션이 있다면 앱을 사용할 수 없는 이유를 설명해주고 앱을 종료합니다.2 가지 경우가 있습니다.

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1])) {

                    Toast.makeText(MainActivity.this, "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요.", Toast.LENGTH_LONG).show();
                    finish();


                }else {

                    Toast.makeText(MainActivity.this, "퍼미션이 거부되었습니다. 설정(앱 정보)에서 퍼미션을 허용해야 합니다. ", Toast.LENGTH_LONG).show();

                }
            }

        }
    }

    void checkRunTimePermission(){

        //런타임 퍼미션 처리
        // 1. 위치 퍼미션을 가지고 있는지 체크합니다.
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION);


        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {

            // 2. 이미 퍼미션을 가지고 있다면
            // ( 안드로이드 6.0 이하 버전은 런타임 퍼미션이 필요없기 때문에 이미 허용된 걸로 인식합니다.)


            // 3.  위치 값을 가져올 수 있음



        } else {  //2. 퍼미션 요청을 허용한 적이 없다면 퍼미션 요청이 필요합니다. 2가지 경우(3-1, 4-1)가 있습니다.

            // 3-1. 사용자가 퍼미션 거부를 한 적이 있는 경우에는
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, REQUIRED_PERMISSIONS[0])) {

                // 3-2. 요청을 진행하기 전에 사용자가에게 퍼미션이 필요한 이유를 설명해줄 필요가 있습니다.
                Toast.makeText(MainActivity.this, "이 앱을 실행하려면 위치 접근 권한이 필요합니다.", Toast.LENGTH_LONG).show();
                // 3-3. 사용자게에 퍼미션 요청을 합니다. 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);


            } else {
                // 4-1. 사용자가 퍼미션 거부를 한 적이 없는 경우에는 퍼미션 요청을 바로 합니다.
                // 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            }

        }

    }


    public String getCurrentAddress( double latitude, double longitude) {

        //지오코더... GPS를 주소로 변환
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        List<Address> addresses;

        try {

            addresses = geocoder.getFromLocation(
                    latitude,
                    longitude,
                    7);
        } catch (IOException ioException) {
            //네트워크 문제
            Toast.makeText(this, "지오코더 서비스 사용불가", Toast.LENGTH_LONG).show();
            return "지오코더 서비스 사용불가";
        } catch (IllegalArgumentException illegalArgumentException) {
            Toast.makeText(this, "잘못된 GPS 좌표", Toast.LENGTH_LONG).show();
            return "잘못된 GPS 좌표";

        }



        if (addresses == null || addresses.size() == 0) {
            Toast.makeText(this, "주소 미발견", Toast.LENGTH_LONG).show();
            return "주소 미발견";

        }

        Address address = addresses.get(0);
        return address.getAddressLine(0).toString()+"\n";

    }


    //여기부터는 GPS 활성화를 위한 메소드들
    private void showDialogForLocationServiceSetting() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("위치 서비스 비활성화");
        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n"
                + "위치 설정을 수정하실래요?");
        builder.setCancelable(true);
        builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case GPS_ENABLE_REQUEST_CODE:

                //사용자가 GPS 활성 시켰는지 검사
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {

                        Log.d("@@@", "onActivityResult : GPS 활성화 되있음");
                        checkRunTimePermission();
                        return;
                    }
                }

                break;
        }
    }
    //gps end
    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // 입출력 흐름이 모두 final로 선언되어있으므로 직접적 변경이 불가하여
            // 임시 object로 입출력 흐름 데이터를 받는다.
            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        // 실제 블루투스로 측정값을 가져오는 인터페이스 부분
        public void run() {
            // 넘어오는 측정값을 받기 위한 버퍼 (buffer store for the stream)
            byte[] buffer = new byte[1024];
            int bytes; // bytes returned from read()
            // 예외 발생할 때까지 넘어오는 측정값을 받을 준비를 하고 있는다
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.available();

                    if(bytes != 0) {
                        // 나머지 데이터를 받기 위해 잠시 멈추고 기다린다 (데이터 전송 속도에 따라 조절하면 된다)
                        // pause and wait for rest of data. Adjust this depending on your sending speed.
                        SystemClock.sleep(100);
                        // 몇 byte를 읽어올 수 있는지 확인
                        // how many bytes are ready to be read?
                        bytes = mmInStream.available();
                        mmInStream.read(buffer, 0, bytes); // record how many bytes we actually read
                        for(int i=bytes; i<1024; i++) {
                            buffer[i] = 0;
                        }

                        mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                                .sendToTarget(); // Send the obtained bytes to the UI activity
                    }
                } catch (IOException e) {
                    e.printStackTrace();

                    break;
                }
            }
        }
        // 현재 연결된 기기로 데이터를 보낸다
        public void write(String input) {
            byte[] bytes = input.getBytes();           //문자열을 바이트로 형변환시킨다
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        // 블루투스 소켓을 닫아서 연결을 해제한다
        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
}