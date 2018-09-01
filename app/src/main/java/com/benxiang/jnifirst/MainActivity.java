package com.benxiang.jnifirst;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android_serialport_api.SerialPort;

public class MainActivity extends AppCompatActivity {

    private Button btn_open, btn_send, btn_close;
    private TextView tv_result;
    private SerialPort serialPort;
    private OutputStream outputStream;
    private InputStream inputStream;
    private static final String TAG = "MainActivity";
    private boolean threadStatus = true;
    byte[] result = new byte[64];
    private boolean isStartCombine = false;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_open = findViewById(R.id.btn_open);
        btn_send = findViewById(R.id.btn_send);
        btn_close = findViewById(R.id.btn_close);
        tv_result = findViewById(R.id.tv_result);

        setOnDataReceiveListener(new OnDataReceiveListener() {
            @Override
            public void onDataReceive(final byte[] buffer, final int size) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        byte[] resultData = new byte[size];
                        System.arraycopy(buffer, 0, resultData, 0, size);
                        tv_result.append(ByteArrToHex(resultData));
                        tv_result.append("\n");
                    }
                });
            }
        });

        btn_open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    serialPort = new SerialPort(new File("dev/ttyS3"), 9600, 0);
                    outputStream = serialPort.getOutputStream();
                    inputStream = serialPort.getInputStream();
                    threadStatus = true;
                    new ReadThread().start();
                    Toast.makeText(MainActivity.this, "串口打开成功", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Toast.makeText(MainActivity.this, "串口打开失败", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        });
        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] sendData = new byte[8];
                sendData[0] = (byte) 0xF2;
                sendData[1] = (byte) 0x45;
                sendData[2] = (byte) 0x00;
                sendData[3] = (byte) 0x40;
                sendData[4] = (byte) 0x0A;
                sendData[5] = (byte) 0x01;
                sendData[6] = (byte) 0x1E;
                sendData[7] = (byte) 0x72;
                try {
                    outputStream.write(sendData);
                    outputStream.flush();
                    Toast.makeText(MainActivity.this, "串口数据发送成功", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        btn_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    outputStream.close();
                    inputStream.close();
                    serialPort.close();
                    threadStatus = false;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    //字节数组转转hex字符串
    public static String ByteArrToHex(byte[] inBytArr) {
        StringBuilder strBuilder = new StringBuilder();
        int j = inBytArr.length;
        for (int i = 0; i < j; i++) {
            strBuilder.append(Byte2Hex(inBytArr[i]));
            strBuilder.append(" ");
        }
        return strBuilder.toString();
    }

    public static String Byte2Hex(Byte inByte)//1字节转2个Hex字符
    {
        return String.format("%02x", inByte).toUpperCase();
    }

    /**
     * 单开一线程，来读数据
     */
    private class ReadThread extends Thread {
        @Override
        public void run() {
            super.run();
            //判断进程是否在运行，更安全的结束进程
            while (threadStatus) {
                Log.d(TAG, "进入线程run");
                //64   1024
                byte[] buffer = new byte[64];
                int size; //读取数据的大小
                try {
                    size = inputStream.read(buffer);
                    if (size > 0) {
//                        Log.d(TAG, "run: 接收到了数据：" + changeTool.ByteArrToHex(buffer));
                        Log.d(TAG, "接收到了数据大小：" + String.valueOf(size));
                        onDataReceiveListener.onDataReceive(buffer, size);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "数据读取异常：" + e.toString());
                }
            }

        }
    }

    OnDataReceiveListener onDataReceiveListener = null;

    interface OnDataReceiveListener {
        void onDataReceive(byte[] buffer, int size);
    }

    public void setOnDataReceiveListener(OnDataReceiveListener dataReceiveListener) {
        onDataReceiveListener = dataReceiveListener;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            outputStream.close();
            inputStream.close();
            serialPort.close();
            threadStatus = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
