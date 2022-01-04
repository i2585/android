package com.example.embedded;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    TextView mTvBluetoothStatus; // 블루투스 상태 표시
    TextView lview, rview; // 왼발, 오른발 좌우 밸런스 표시
    ImageView RTBview, LTBview; // 각 발의 위아래 밸러스 표시
    Button mBtnBluetoothOn; // 블루투스 활성화 버튼
    Button mBtnBluetoothOff; // 블루투스 비활성화 버튼
    Button mBtnConnect; // 블루투스 연결 버튼
    BluetoothAdapter mBluetoothAdapter;
    Set<BluetoothDevice> mPairedDevices; // 블루투스 페어링 장치 표시
    List<String> mListPairedDevices; // 블루투스 페어링 장치 표시

    Handler mBluetoothHandler;
    ConnectedBluetoothThread mThreadConnectedBluetooth;
    BluetoothDevice mBluetoothDevice;
    BluetoothSocket mBluetoothSocket;

    DrawerLayout drawerLayout;
    View drawerView;

    static char[] temp = new char[20]; // 이상 데이터 처리 배열
    static int lens = 0; // 이상 데이터 처리를 위한 길이
    static float lsum = 0;
    static float rsum = 0;
    static float ltsum = 0;
    static float lbsum = 0;
    static float rtsum = 0;
    static float rbsum = 0;
    static float except = 0;
    static int count = 0; // 화면 표시를 위한 카운트
    final static int BT_REQUEST_ENABLE = 1; // 블루투스 이용 가능 상태
    final static int BT_MESSAGE_READ = 2; // 블루투스 데이터 수신 상태
    final static int BT_CONNECTING_STATUS = 3; // 블루투스 연결 상태
    final static UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // 아두이노와 통신

    @SuppressLint({"HandlerLeak", "NewApi"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //drawer.xml id
        drawerLayout = findViewById(R.id.drawer_layout);
        drawerView = findViewById(R.id.drawerView);
        drawerLayout.setDrawerListener(listener);
        mTvBluetoothStatus = findViewById(R.id.tvBluetoothStatus);
        mBtnBluetoothOn = findViewById(R.id.btnBluetoothOn);
        mBtnBluetoothOff = findViewById(R.id.btnBluetoothOff);
        mBtnConnect = findViewById(R.id.btnConnect);


        //activity_main.xml id
        lview = findViewById(R.id.lview);
        rview = findViewById(R.id.rview);
        RTBview = findViewById(R.id.RTBview);
        LTBview = findViewById(R.id.LTBview);

        //해당 디바이스가 블루투스를 지원하지는 파악하는 변수
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // 블루투스 활성화 클릭 이벤트
        mBtnBluetoothOn.setOnClickListener(view -> bluetoothOn());

        // 블루투스 비활성화 클릭 이벤트
        mBtnBluetoothOff.setOnClickListener(view -> bluetoothOff());

        // 블루투스 페어링 된 기기 클릭 이벤트
        mBtnConnect.setOnClickListener(view -> listPairedDevices());

        // 디바이스 화면에 출력
        mBluetoothHandler = new Handler(){

            // 수신 데이터 처리 및 이상 데이터 처리
            public void handleMessage(Message msg) {
                if (msg.what == BT_MESSAGE_READ) {
                    String readMessage;
                    String a;
                    int j = 0;
                    int maxlen = lens + ((byte[]) msg.obj).length;
                    readMessage = new String((byte[]) msg.obj, StandardCharsets.UTF_8);
                    if (readMessage.lastIndexOf("/") != 8) {
                        for (int i = lens; i < maxlen; i++) {
                            temp[i] = readMessage.charAt(j);
                            lens++;
                            j++;
                        }
                        if (String.valueOf(temp).indexOf('/') != -1 && String.valueOf(temp).indexOf('/') < 8) {
                            int k = String.valueOf(temp).indexOf('/') + 1;
                            lens = lens - k;
                            for (int i = 0; i < 9; i++) {
                                temp[i] = temp[k++];
                            }
                            Arrays.fill(temp, lens, 20, '\0');
                        }
                        if (lens > 8) {
                            a = String.valueOf(temp);
                            readMessage = a.substring(0, a.indexOf('/'));
                            print(readMessage);
                            lens = lens - 9;
                            System.arraycopy(temp, 9, temp, 0, 9);
                            Arrays.fill(temp, lens, 20, '\0');
                        }
                    } else {
                        readMessage = readMessage.substring(0, readMessage.indexOf('/'));
                        print(readMessage);
                    }
                }
            }

            // 수신 데이터 처리 후 출력
            @SuppressLint({"DefaultLocale", "SetTextI18n"})
            public void print(String readMessage){
                boolean b = Integer.parseInt(readMessage.substring(2, 4)) > 8 && Integer.parseInt(readMessage.substring(2, 4)) < 16;
                int sensor;
                except += Integer.parseInt(readMessage.substring(5, 8));
                if(Integer.parseInt(readMessage.substring(5, 8))!=0) {
                    sensor = (Integer.parseInt(readMessage.substring(5, 8)) / 50) + 1;
                }
                else {
                    sensor = 0;
                }
                count++;
                if(Integer.parseInt(readMessage.substring(0,1))==1) { // 왼발
                    lsum += sensor;
                    if (Integer.parseInt(readMessage.substring(2, 4)) < 7) {
                        lbsum += sensor;
                    }
                    else if(b){
                        ltsum += sensor;
                    }
                }
                else { // 오른발
                    rsum += sensor;
                    if (Integer.parseInt(readMessage.substring(2, 4)) < 7) {
                        rbsum += sensor;
                    }
                    else if(b){
                        rtsum += sensor;
                    }
                }

                // 디바이스 화면에 출력

                if(count==32) {
                    LTBview.animate().setDuration(320);
                    RTBview.animate().setDuration(320);
                    float outputL = ((((lbsum/(ltsum+lbsum))*100)-((ltsum/(ltsum+lbsum))*100))*36)/10;
                    float outputR = ((((rbsum/(rtsum+rbsum))*100)-((rtsum/(rtsum+rbsum))*100))*36)/10;

                    if(except<=150) {
                        lview.setText("50%");
                        rview.setText("50%");
                    }
                    else{
                        lview.setText(String.format("%.2f", lsum / (lsum + rsum) * 100) + "%");
                        rview.setText(String.format("%.2f", rsum / (lsum + rsum) * 100) + "%");
                    }
                    if(except<=150 || Float.isNaN(outputL) || outputL == 0) {
                        LTBview.animate().y(490).withLayer();
                    }
                    else{
                        LTBview.animate().translationY(outputL).withLayer();
                    }
                    if(except<=150 || Float.isNaN(outputR) || outputR == 0){
                        RTBview.animate().y(490).withLayer();
                    }
                    else{
                        RTBview.animate().translationY(outputR).withLayer();
                    }
                    count = 0; lsum = 0; rsum = 0; lbsum = 0; ltsum = 0; rbsum = 0; rtsum = 0; except = 0;
                    }
            }
        };

    }

    //drawerLayout 이벤트
    DrawerLayout.DrawerListener listener = new DrawerLayout.DrawerListener() {
        @Override
        public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
        }

        @Override
        public void onDrawerOpened(@NonNull View drawerView) {
        }

        @Override
        public void onDrawerClosed(@NonNull View drawerView) {
        }

        @Override
        public void onDrawerStateChanged(int newState) {
        }
    };

    // 블루투스 활성화
    void bluetoothOn() {
        if(mBluetoothAdapter == null) { // 디바이스가 블루투스를 지원하지 않을 경우
            Toast.makeText(getApplicationContext(), "블루투스를 지원하지 않는 기기입니다.", Toast.LENGTH_LONG).show();
        }
        else {
            if (mBluetoothAdapter.isEnabled()) { // 이미 활성화 된 경우
                Toast.makeText(getApplicationContext(), "블루투스가 이미 활성화 되어 있습니다.", Toast.LENGTH_LONG).show();
                mTvBluetoothStatus.setText("활성화");
            }
            else {
                Toast.makeText(getApplicationContext(), "블루투스가 활성화 되어 있지 않습니다.", Toast.LENGTH_LONG).show();
                Intent intentBluetoothEnable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intentBluetoothEnable, BT_REQUEST_ENABLE);
            }
        }
    }

    // 블루투스 비활성화
    void bluetoothOff() {
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.disable();
            Toast.makeText(getApplicationContext(), "블루투스가 비활성화 되었습니다.", Toast.LENGTH_SHORT).show();
            mTvBluetoothStatus.setText("비활성화");
        }
        else {
            Toast.makeText(getApplicationContext(), "블루투스가 이미 비활성화 되어 있습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    //블루투스 비활성화 상태에서 활성화시 뜨는 파업창
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        Arrays.fill(temp,'\0');
        lens=0;
        if (requestCode == BT_REQUEST_ENABLE) {
            if (resultCode == RESULT_OK) { // 블루투스 활성화를 확인을 클릭하였다면
                Toast.makeText(getApplicationContext(), "블루투스 활성화", Toast.LENGTH_LONG).show();
                mTvBluetoothStatus.setText("활성화");
            } else if (resultCode == RESULT_CANCELED) { // 블루투스 활성화를 취소를 클릭하였다면
                Toast.makeText(getApplicationContext(), "취소", Toast.LENGTH_LONG).show();
                mTvBluetoothStatus.setText("비활성화");
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // 페어링 된 기기들 중 에서 연결할 기기를 선택
    void listPairedDevices() {
        if (mBluetoothAdapter.isEnabled()) {
            mPairedDevices = mBluetoothAdapter.getBondedDevices();

            if (mPairedDevices.size() > 0) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("장치 선택");

                mListPairedDevices = new ArrayList<>();
                for (BluetoothDevice device : mPairedDevices) {
                    mListPairedDevices.add(device.getName());
                    mListPairedDevices.add(device.getName() + "\n" + device.getAddress());
                }
                final CharSequence[] items = mListPairedDevices.toArray(new CharSequence[0]);
                mListPairedDevices.toArray(new CharSequence[0]);

                builder.setItems(items, (dialog, item) -> connectSelectedDevice(items[item].toString()));
                AlertDialog alert = builder.create();
                alert.show();
            } else {
                Toast.makeText(getApplicationContext(), "페어링된 장치가 없습니다.", Toast.LENGTH_LONG).show();
            }
        }
        else {
            Toast.makeText(getApplicationContext(), "블루투스가 비활성화 되어 있습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    // 페어링 된 기기와 블루투스 연결 시도
    void connectSelectedDevice(String selectedDeviceName) {
        for(BluetoothDevice tempDevice : mPairedDevices) {
            if (selectedDeviceName.equals(tempDevice.getName())) {
                mBluetoothDevice = tempDevice;
                break;
            }
        }
        try {
            mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(BT_UUID);
            mBluetoothSocket.connect();
            mThreadConnectedBluetooth = new ConnectedBluetoothThread(mBluetoothSocket);
            mThreadConnectedBluetooth.start();
            mBluetoothHandler.obtainMessage(BT_CONNECTING_STATUS, 1, -1).sendToTarget();
            Toast.makeText(getApplicationContext(), "블루투스 연결이 완료 되었습니다.", Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "블루투스 연결 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
        }
    }

    private class ConnectedBluetoothThread extends Thread {
        private final BluetoothSocket mmSocket; // socket
        private final InputStream mmInStream; // 수신 스트림

        public ConnectedBluetoothThread(BluetoothSocket socket) { // ConnectedBluetoothThread 생성자
            mmSocket = socket;
            InputStream tmpIn = null;

            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "소켓 연결 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            }

            mmInStream = tmpIn;
        }
        public synchronized void run() { // 0.01초 간격으로 데이터 읽기
                byte[] buffer = new byte[9];
                int bytes;
                try {
                    Thread.sleep(10);
                    while (true) {
                        bytes = mmInStream.read(buffer);
                        byte[] data = Arrays.copyOf(buffer, bytes);
                        mBluetoothHandler.obtainMessage(BT_MESSAGE_READ, bytes, -1, data).sendToTarget();
                    }
                } catch (IOException | InterruptedException e) {
                    cancel();
                }
        }
        public void cancel() { // socket연결 오류 시 socket close
            try {
                mmSocket.close();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "소켓 해제 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
