package com.example.mybluetooth;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;

import static android.text.TextUtils.split;

public class MainActivity extends AppCompatActivity {
    /*
    선언부 (Declaration)
     */
    // flag
    // sum != 0 으로 초기화시켜야 블루투스를 연결하지 않은 상황에서
    // ECG START 버튼을 아무리 눌러도 작동이 되지 않는다.
    private int sum = 10;
    // GUI Components
    private TextView mBluetoothStatus;              // 블루투스 연결 상태 보여주는 텍스트
    private Button mOnBtn;                          // 블루투스 활성화 버튼
    private Button mOffBtn;                         // 블루투스 비활성화 버튼
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

    // 데이터 전송 현황 보여주는 ProgressBar
    private ProgressBar mProgressBar;
    private TextView mProgressData;
    ArrayList<Integer> final_data = new ArrayList<>(512); // 측정 데이터 담는 ArrayList

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

        mBluetoothStatus = (TextView)findViewById(R.id.bluetoothStatus);
        mOnBtn = (Button)findViewById(R.id.on);
        mOffBtn = (Button)findViewById(R.id.off);
        mECGStartBtn = (Button)findViewById(R.id.start_ecg_btn);
        mListPairedDevicesBtn = (Button)findViewById(R.id.PairedBtn);
        mECGHistoryBtn = (Button)findViewById(R.id.ecgHistoryBtn);
        mShowGraphBtn = (Button) findViewById(R.id.showGraphBtn);
        //mShowGraphBtn.setClickable(false);

        mBTArrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
        // 블루투스 어댑터에 핸들러 활성화 (get a handle on the bluetooth radio)
        mBTAdapter = BluetoothAdapter.getDefaultAdapter();

        // 페어링 가능 기기 목록 제시 및 선택된 기기 페어링 수행
        mDevicesListView = (ListView)findViewById(R.id.devicesListView);
        mDevicesListView.setAdapter(mBTArrayAdapter); // assign model to view
        mDevicesListView.setOnItemClickListener(mDeviceClickListener);

        // ProgressBar
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mProgressData = (TextView) findViewById(R.id.progress_data);


        // 블루투스 핸들러 동작 알고리즘 (ECG 키트에서 안드로이드 앱으로 데이터 수신하는 알고리즘)
        mHandler = new Handler(){
            public void handleMessage(Message msg){
                // START ECG 버튼을 누르면 키트로부터 데이터를 읽어온다
                if(msg.what == MESSAGE_READ){
                    // Back GettingData to START ECG
                    mECGStartBtn.setText("START ECG");
                    mECGStartBtn.setBackgroundResource(R.drawable.btn_blue);
                    // 측정 데이터 가져오기 전 초기화 (Initialize Data Variable of ECG)
                    int tmpNum = 0, tmpNum1 = 0, tmpNum2 =0;

                    /*
                    측정 데이터 값의 범위가 0 ~ 1000 이다.
                    하지만 블루투스를 통해서 데이터를 넘기기 위해서는 Byte 단위로 전달해야하는데,
                    1 byte = 8 bit 이므로, 0 ~ 255 까지만 표현이 가능하다.
                    따라서 256 ~ 1000 까지의 데이터 또한 표현 가능하도록 해야 측정 데이터의 그래프 표현이 가능해지므로
                    1 byte 두 부분을 받아서 값을 원래 측정된 값으로 다시 변환하여 앱 상 데이터 목록에 추가한다.
                     */
                    for(int i=0; i<512; i++) {
                        tmpNum1 = ((byte[]) msg.obj)[i*2]&0xff;
                        tmpNum2 = ((byte[]) msg.obj)[i * 2 + 1]&0xff;
                        if(tmpNum1 != 0) {
                            tmpNum = ((tmpNum1 << 8) & 0xff00) + (tmpNum2 & 0x00ff);
                            final_data.add(tmpNum-256);
                        }
                        else {
                            sum+=i;
                            System.out.println(sum);
                            mProgressBar.setProgress(sum);
                            mProgressData.setText(Integer.toString(sum*100/512)+"%");
                            mShowGraphBtn.setClickable(true);
                            break;
                        }
                    }
                    // 전송 완료
                    if(sum==512) {
                        System.out.println("finished");
                        sum = 0;
                    }
                }

                // 페어링 기기 연결 여부에 따른 화면 출력
                if(msg.what == CONNECTING_STATUS){
                    if(msg.arg1 == 1)
                        mBluetoothStatus.setText("Connected to Device: " + (String)(msg.obj));
                    else
                        mBluetoothStatus.setText("Connection Failed");
                }
            }
        };

        // 블루투스 연결 가능한 기기가 없음을 표현
        if (mBTArrayAdapter == null) {
            // Device does not support Bluetooth
            mBluetoothStatus.setText("Status: Bluetooth not found");
            Toast.makeText(getApplicationContext(),"Bluetooth device not found!",Toast.LENGTH_SHORT).show();
        }
        // 기기 페어링 완료 후 ECG START 버튼 눌렀을 때, 값을 가져올 수 있게 만들도록 함
        else {
            mECGStartBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    if(mConnectedThread != null) { //First check to make sure thread created
                        final_data.clear();
                        mConnectedThread.write("1");
                    }
                }
            });
            // 블루투스 활성화 버튼
            mOnBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bluetoothOn(v);
                }
            });
            // 블루투스 비활성화 버튼
            mOffBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    bluetoothOff(v);
                }
            });
            // 페어링 가능 기기 목록 클릭 활성화 버튼
            mListPairedDevicesBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v){
                    listPairedDevices(v);
                }
            });
            // ECG START 버튼
            mECGStartBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    // sum != 0으로 처음에 초기화 해주는 이유
                    if (sum==0) {
                        // ECG START 버튼이 눌려지고 데이터를 받아오고 있음을 UI 상에 표현되게 만듬
                        mECGStartBtn.setText("Getting Data...");
                        final_data.clear();
                        mECGStartBtn.setBackgroundResource(R.drawable.btn_green);
                        mProgressBar.setProgress(0);
                        mProgressData.setText("0%");
                        mConnectedThread.write("1");
                    }
                    else {
                        Toast.makeText(getApplicationContext(),"Pair your device first!", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    // 블루투스가 활성화 버튼이 눌렸을 때
    private void bluetoothOn(View view){
        if (!mBTAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            mBluetoothStatus.setText("Bluetooth enabled");
            Toast.makeText(getApplicationContext(),"Bluetooth turned on",Toast.LENGTH_SHORT).show();

        }
        else{
            Toast.makeText(getApplicationContext(),"Bluetooth is already on", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent Data) {
        // 우리가 반응하고 있는 요청이 무엇인지 확인한다
        // Check which request we're responding to
        super.onActivityResult(requestCode, resultCode, Data);
        if (requestCode == REQUEST_ENABLE_BT) {
            // 블루투스 활성화 요청이 들어왔을 때
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // 블루투스가 활성화되어있다면 Enabled
                // 아니라면 Disabled
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.
                mBluetoothStatus.setText("Enabled");
            } else
                mBluetoothStatus.setText("Disabled");
        }
    }

    // 블루투스 비활성화 버튼이 눌렸을 때
    private void bluetoothOff(View view){
        mBTAdapter.disable(); // turn off
        mBluetoothStatus.setText("Bluetooth disabled");
        Toast.makeText(getApplicationContext(),"Bluetooth turned Off", Toast.LENGTH_SHORT).show();
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
            mBluetoothStatus.setText("Connecting...");
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
    public void ecgLogoutButtonClicked(View v) {
        Toast.makeText(getApplicationContext(),"Logout Complete!", Toast.LENGTH_SHORT).show();
        finish();
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

        // 페어링된 기기로 데이터를 전송한다
        // 없애도 되지 않나.....
        /* Call this from the main activity to send data to the remote device */

        // public void write(String input) {
        //     byte[] bytes = input.getBytes();           //converts entered String into bytes
        //     try {
        //         mmOutStream.write(bytes);
        //         System.out.println("hello");
        //     } catch (IOException e) { }
        // }


        // 블루투스 소켓을 닫아서 연결을 해제한다
        /* Call this from the main activity to shutdown the connection */
        /*
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
        */
    }
}