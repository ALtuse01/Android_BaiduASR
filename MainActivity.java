package com.example.yuyin;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.baidu.aip.asrwakeup3.core.mini.AutoCheck;
import com.baidu.speech.EventListener;
import com.baidu.speech.EventManager;
import com.baidu.speech.EventManagerFactory;
import com.baidu.speech.asr.SpeechConstant;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import JSON.JSON;

public class MainActivity extends AppCompatActivity implements EventListener{

    private TextView temp_tv2;
    private TextView humidity_tv2;
    private TextView asr_res;
    private Button bt_dht;
    private Button bt_asr;
    private Button asr_stop;
    private RadioGroup radioGroup1;
    private RadioGroup radioGroup2;
    private static Handler handler;
    final String param = "";
    Gson gson = new Gson();
    Timer timer = new Timer();

    private EventManager asr;

    private boolean logTime = false;

    protected boolean enableOffline = false; // 测试离线命令词，需要改成true

    /**
     * 基于SDK集成2.2 发送开始事件
     * 点击开始按钮
     * 测试参数填在这里
     */
    private void start() {
        asr_res.setText("");
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        String event = null;
        event = SpeechConstant.ASR_START; // 替换成测试的event

        if (enableOffline) {
            params.put(SpeechConstant.DECODER, 2);
        }
        // 基于SDK集成2.1 设置识别参数
        params.put(SpeechConstant.ACCEPT_AUDIO_VOLUME, false);

        (new AutoCheck(getApplicationContext(), new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == 100) {
                    AutoCheck autoCheck = (AutoCheck) msg.obj;
                    synchronized (autoCheck) {
                        String message = autoCheck.obtainErrorMessage(); // autoCheck.obtainAllMessage();
                        asr_res.append(message + "\n");
                        ; // 可以用下面一行替代，在logcat中查看代码
                        // Log.w("AutoCheckMessage", message);
                    }
                }
            }
        }, enableOffline)).checkAsr(params);
        String json = null; // 可以替换成自己的json
        json = new JSONObject(params).toString(); // 这里可以替换成你需要测试的json
        asr.send(event, json, null, 0, 0);
        //printLog("输入参数：" + json);
    }

    /**
     * 点击停止按钮
     * 基于SDK集成4.1 发送停止事件
     */
    private void stop() {
        //printLog("停止识别：ASR_STOP");
        asr.send(SpeechConstant.ASR_STOP, null, null, 0, 0); //
    }


    /**
     * enableOffline设为true时，在onCreate中调用
     * 基于SDK离线命令词1.4 加载离线资源(离线时使用)
     */
    private void loadOfflineEngine() {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put(SpeechConstant.DECODER, 2);
        params.put(SpeechConstant.ASR_OFFLINE_ENGINE_GRAMMER_FILE_PATH, "assets://baidu_speech_grammar.bsg");
        asr.send(SpeechConstant.ASR_KWS_LOAD_ENGINE, new JSONObject(params).toString(), null, 0, 0);
    }

    /**
     * enableOffline为true时，在onDestory中调用，与loadOfflineEngine对应
     * 基于SDK集成5.1 卸载离线资源步骤(离线时使用)
     */
    private void unloadOfflineEngine() {
        asr.send(SpeechConstant.ASR_KWS_UNLOAD_ENGINE, null, null, 0, 0); //
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initPermission();
        // 基于sdk集成1.1 初始化EventManager对象
        asr = EventManagerFactory.create(this, "asr");
        // 基于sdk集成1.3 注册自己的输出事件类
        asr.registerListener(this); //  EventListener 中 onEvent方法

        handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                Bundle bundle = msg.getData();
                String Temprature = bundle.getString("Temprature");
                String Humidity = bundle.getString("Humidity");
                temp_tv2.setText(Temprature+"°C");
                humidity_tv2.setText(Humidity+"%");
            }
        };

        bt_asr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start();
                Toast.makeText(MainActivity.this,"打开开关",Toast.LENGTH_SHORT).show();
            }
        });
        asr_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop();
            }
        });
        if (enableOffline) {
            loadOfflineEngine(); // 测试离线命令词请开启, 测试 ASR_OFFLINE_ENGINE_GRAMMER_FILE_PATH 参数时开启
        }

        radioGroup1.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton btn = (RadioButton)findViewById(checkedId);

                Toast.makeText(getApplicationContext(),  btn.getText(), Toast.LENGTH_LONG).show();
                if(btn.getText().toString().equals("红灯亮")){
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            post("led1=on");
                        }
                    }).start();
                }
                else if(btn.getText().toString().equals("红灯灭")){
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            post("led1=off");
                        }
                    }).start();
                }
            }

        });

        radioGroup2.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton green = (RadioButton)findViewById(checkedId);
                Toast.makeText(getApplicationContext(), green.getText(), Toast.LENGTH_LONG).show();
                if(green.getText().toString().equals("绿灯亮")){
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            post("led2=on");
                        }
                    }).start();
                }
                else if(green.getText().toString().equals("绿灯灭")){
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            post("led2=off");
                        }
                    }).start();
                }
            }
        });

        bt_dht.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(bt_dht.getText().toString().equals("开始检测")) {
                    bt_dht.setText("停止检测");

                    TimerTask task = new TimerTask() {
                        @Override
                        public void run() {
                            String res = get();
                            System.out.println(res);
                            if(res.contains("temperature")) {
                                try {
                                    JSON dat = new JSON();
                                    dat = gson.fromJson(res, JSON.class);
                                    Message msg = MainActivity.handler.obtainMessage();
                                    Bundle bundle = new Bundle();
                                    bundle.putString("Temprature", dat.getTemperature().toString());
                                    bundle.putString("Humidity", dat.getHumidity().toString());
                                    msg.setData(bundle);
                                    MainActivity.handler.sendMessage(msg);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    };
                    timer.schedule(task,0,2000);
                }
                else{
                    bt_dht.setText("开始检测");
                    timer.cancel();
                    timer = new Timer();
                }
            }
        });
    }

    public void post(String cmd){
        try {
            URL url = new URL("http://10.128.213.200/led");
            //URL url = new URL("http://www.baidu.com");
            HttpURLConnection Conn = (HttpURLConnection) url.openConnection();
            Conn.setRequestMethod("POST");
            Conn.setDoInput(true);
            Conn.setDoOutput(true);
            Conn.setUseCaches(false);
            Conn.setInstanceFollowRedirects(true);
            Conn.setRequestProperty("Content-type","application/x-www-form-urlencoded");
            String param = cmd;
            DataOutputStream out = new DataOutputStream(Conn.getOutputStream());
            out.writeBytes(param);
            out.flush();
            out.close();

            if(Conn.getResponseCode() == HttpURLConnection.HTTP_OK){
                InputStreamReader in = new InputStreamReader(Conn.getInputStream());
                BufferedReader buffer = new BufferedReader(in);
                String inputLine = null;
//                while((inputLine = buffer.readLine())!=null){
//                    result +=inputLine;
//                }
                in.close();
            }
            Conn.disconnect();
        }catch (MalformedURLException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public String get(){
        String result = "";
        try {
            URL url = new URL("http://10.128.213.200:80/search?keywords=dht");
            HttpURLConnection Conn = (HttpURLConnection)url.openConnection();
            InputStreamReader in = new InputStreamReader(Conn.getInputStream());
            BufferedReader buffer = new BufferedReader(in);
            String inputLine = null;

            while((inputLine = buffer.readLine())!=null){
                result+=inputLine+"\n";
            }
            in.close();
            Conn.disconnect();
        }catch (MalformedURLException e){
            e.printStackTrace();
        }
        catch (IOException e){
            e.printStackTrace();
        }
        return result;
    }


    @Override
    protected void onPause() {
        super.onPause();
        asr.send(SpeechConstant.ASR_CANCEL, "{}", null, 0, 0);
        Log.i("ActivityMiniRecog", "On pause");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 基于SDK集成4.2 发送取消事件
        asr.send(SpeechConstant.ASR_CANCEL, "{}", null, 0, 0);
        if (enableOffline) {
            unloadOfflineEngine(); // 测试离线命令词请开启, 测试 ASR_OFFLINE_ENGINE_GRAMMER_FILE_PATH 参数时开启
        }

        // 基于SDK集成5.2 退出事件管理器
        // 必须与registerListener成对出现，否则可能造成内存泄露
        asr.unregisterListener(this);
    }

    // 基于sdk集成1.2 自定义输出事件类 EventListener 回调方法
    // 基于SDK集成3.1 开始回调事件
    @Override
    public void onEvent(String name, String params, byte[] data, int offset, int length) {
        String logTxt = "";
        String resTxt = "";
        if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_PARTIAL)) {
            // 识别相关的结果都在这里
            if (params == null || params.isEmpty()) {
                return;
            }
            if (params.contains("\"nlu_result\"")) {
                // 一句话的语义解析结果
                if (length > 0 && data.length > 0) {
                  //  logTxt += ", 语义解析结果：" + new String(data, offset, length);
                }
            } else if (params.contains("\"partial_result\"")) {
                // 一句话的临时识别结果
              //  logTxt += ", 临时识别结果：" + params;
            }  else if (params.contains("\"final_result\""))  {
                // 一句话的最终识别结果
               // logTxt += ", 最终识别结果：" + params;
                try {
                    JSONObject obj = new JSONObject(params);
                    logTxt +=obj.get("best_result").toString();
                    resTxt = logTxt.substring(0,logTxt.length()-1);
                    Toast.makeText(MainActivity.this,"Success",Toast.LENGTH_SHORT).show();

                    if (resTxt.contains("打开红灯")){
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                post("led1=on");
                            }
                        }).start();

                    }
                    else if(resTxt.contains("关闭红灯")){
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                post("led1=off");
                            }
                        }).start();
                    }
                    else if(resTxt.contains("打开绿灯")){
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                post("led2=on");
                            }
                        }).start();
                    }
                    else if(resTxt.contains("关闭绿灯")){
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                post("led2=off");
                            }
                        }).start();
                    }
                    asr_res.setText(resTxt);

                }catch (JSONException e){
                    e.printStackTrace();
                }
            }  else {
                // 一般这里不会运行
                //logTxt += " ;params :" + params;
                if (data != null) {
                  //  logTxt += " ;data length=" + data.length;
                }
            }
        } else {
            // 识别开始，结束，音量，音频数据回调
            if (params != null && !params.isEmpty()){
                //logTxt += " ;params :" + params;
            }
            if (data != null) {
                //logTxt += " ;data length=" + data.length;
            }
        }


        //printLog(resTxt);
    }

//    private void printLog(String text) {
//        if (logTime) {
//            text += "  ;time=" + System.currentTimeMillis();
//        }
//        //text += "\n";
//        Log.i(getClass().getName(), text);
//        asr_res.append(text + "\n");
//    }


    private void initView() {
//        txtResult = (TextView) findViewById(com.baidu.aip.asrwakeup3.core.R.id.txtResult);
//        txtLog = (TextView) findViewById(com.baidu.aip.asrwakeup3.core.R.id.txtLog);
//        btn = (Button) findViewById(com.baidu.aip.asrwakeup3.core.R.id.btn);
//        stopBtn = (Button) findViewById(com.baidu.aip.asrwakeup3.core.R.id.btn_stop);
//        txtLog.setText(DESC_TEXT + "\n");

        radioGroup1 = (RadioGroup)findViewById(R.id.Radio_group1);
        radioGroup2 = (RadioGroup)findViewById(R.id.Radio_group2);
//        led_state = (TextView)findViewById(R.id.led_state);
        temp_tv2 = (TextView)findViewById(R.id.temp_tv2);
        humidity_tv2 = (TextView)findViewById(R.id.humidity_tv2);
        bt_dht = (Button)findViewById(R.id.bt_dht);
        bt_asr = (Button)findViewById(R.id.bt_asr);
        asr_stop = (Button)findViewById(R.id.asr_stop);
        asr_res = (TextView)findViewById(R.id.asr_res);
    }

    /**
     * android 6.0 以上需要动态申请权限
     */
    private void initPermission() {
        String permissions[] = {Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        ArrayList<String> toApplyList = new ArrayList<String>();

        for (String perm : permissions) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, perm)) {
                toApplyList.add(perm);
                // 进入到这里代表没有权限.

            }
        }
        String tmpList[] = new String[toApplyList.size()];
        if (!toApplyList.isEmpty()) {
            ActivityCompat.requestPermissions(this, toApplyList.toArray(tmpList), 123);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // 此处为android 6.0以上动态授权的回调，用户自行实现。
    }

}