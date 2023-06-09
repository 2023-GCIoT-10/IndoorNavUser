package com.example.indoorlocalization;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;

import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;

import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import android.media.MediaPlayer;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA_PERMISSION = 100;

    private WifiManager wifiManager;
    private MediaPlayer mediaPlayer;
    Button btn_start;
    Spinner floor_sp, room_sp;
    TextView destination, startLoc;
    String start, dest = "";

    //    String[] floorList = {"4층", "5층"};
//    Resources res = getResources();
    String[] roomList = {"강의실 선택", "401호", "402호", "403호", "404호", "405호", "406호",
            "407호", "408호", "409호", "410호", "411호", "412호", "413호",
            "414호", "415호", "416호", "417호", "418호", "419호", "420호",
            "421호", "422호", "423호", "424호", "425호",
            "426호", "427호", "428호", "429호", "430호", "431호", "432호", "433호", "434호", "435호",
            "501호", "502호", "503호", "504호", "505호", "506호",
            "507호", "508호", "509호", "510호", "511호", "512호", "513호",
            "514호", "515호", "516호", "517호", "518호", "519호", "520호",
            "521호", "522호", "523호", "524호", "525호",
            "526호", "527호", "528호", "529호", "530호", "531호", "532호", "533호", "534호", "535호"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inside onCreate() or a relevant initialization method
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            // Continue with camera setup
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);//와이파이 신호를 받아오기위한 wifi메니저 인스턴스

        startLoc = (TextView) findViewById(R.id.home_now_location_tv);
        String tmp = startLoc.getText().toString();
        start = tmp.substring(7);

        //spinner
        room_sp = (Spinner) findViewById(R.id.home_destination_sp);
        destination = (TextView) findViewById(R.id.home_destination_text_tv);

//        ArrayAdapter<String> fAdapter = new ArrayAdapter<String>(
//                this, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, floorList
//        );
//        floor_sp.setAdapter(fAdapter);
        ArrayAdapter<String> rAdapter = new ArrayAdapter<String>(
                this, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, roomList
        );
        room_sp.setAdapter(rAdapter);

        room_sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != 0) {
                    dest = roomList[position];
                    destination.setText("선택한 목적지: " + dest);
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                destination.setText("");
            }
        });

        /* wifi 정보 수집 */
        //fetchDataFromServer(); //test
        try {
            scanWifiData();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        // 시작 버튼 클릭
//      selected_point = findViewById(R.id.selected_destination);
        btn_start = findViewById(R.id.home_start_btn_tv);
        btn_start.setOnClickListener(v -> {
            if (dest == "") {
                Toast.makeText(this, "목적지를 선택해주세요", Toast.LENGTH_SHORT).show();
            } else {
                /* intent 정보 보내기 */
                Intent intent = new Intent(getApplicationContext(), NavigationActivity.class);
                Bundle toNavigation = new Bundle();

                toNavigation.putString("start", start); // 출발지 위치 - 현재 위치 바꾸기
                toNavigation.putString("dest", dest); // 목적지 위치 - 선택한 위치 바꾸기
                intent.putExtras(toNavigation);

                playSoundEffect();
                startActivity(intent);

                finish();
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, continue with camera setup
            } else {
                // Permission denied, handle accordingly (e.g., show a message or disable camera functionality)
            }
        }
    }

    // API 연결 확인용 - 데이터 요청
    /*
    public void fetchDataFromServer() {
        String serverUrl = "http://aeong.pythonanywhere.com";
        String endpoint = serverUrl + "/";

        // OkHttp Request 객체 생성
        Request request = new Request.Builder()
                .url(endpoint)
                .build();
        OkHttpClient client = new OkHttpClient();
        // 비동기적으로 요청을 보내고 응답 처리를 위한 콜백 등록
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    // 응답 데이터 처리
                    // TODO: 응답 데이터를 파싱하거나 필요한 처리를 수행하세요.
                    Log.d("API test", responseData);
                    // TextView의 텍스트 변경
                    String tmp = "현재 위치 : " + responseData;
                    runOnUiThread(() -> {
                        startLoc.setText(tmp);
                    });
                } else {
                    // 응답이 실패한 경우 처리
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                // 요청 실패 처리
            }
        });
    }

    *  */

    // scan wifi data in here!
    private void scanWifiData() throws JSONException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // 권한이 거부되었을 경우 종료
            return;
        }
        JSONObject jsonData = new JSONObject();
        List<ScanResult> results = wifiManager.getScanResults();

        JSONArray wifiArray = new JSONArray();
        for (ScanResult result : results){
            String bssid = result.BSSID;
            int signalStrength = result.level;

            JSONObject wifi= new JSONObject();
            try{
                wifi.put("BSSID",bssid);
                wifi.put("RSSI",signalStrength);
                wifiArray.put(wifi);
            }catch(JSONException e){
                e.printStackTrace();
            }
        }
        try{
            jsonData.put("wifi",wifiArray);
            sendJsonData(jsonData);
        }catch(JSONException e){
            e.printStackTrace();
        }

}
    // JSON 데이터 전송 메서드 정의 -> 수집한 와이파이 정보 보내기
    private void sendJsonData(JSONObject jsonData) {
        String serverUrl = "http://aeong.pythonanywhere.com";
        String endpoint = serverUrl + '/' + "location"; // 실제 엔드포인트 경로를 추가합니다

        // OkHttp 클라이언트 인스턴스 생성
        OkHttpClient client = new OkHttpClient();
        //JSONObject msg = new JSONObject();

        // JSON 요청 본문 생성
        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                jsonData.toString()
        );

        // OkHttp Request 객체 생성
        Request request = new Request.Builder()
                .url(endpoint)
                .post(requestBody) // POST 요청으로 설정
                .build();

        // 비동기적으로 요청을 보내고 응답 처리를 위한 콜백 등록
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d("API why", response.toString());
                String location;
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    JSONObject jObject = null;
                    try {
                        jObject = new JSONObject(responseData);
                        location = jObject.getString("msg");

                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }

                    // 응답 데이터 처리
                    // 출발 위치 응답받아 넣기, 출발 위치 설정하기
                    // TextView의 텍스트 변경
                    start = String.valueOf(changeToNode(location));
                    location=changeToRoom(changeToNode(location));
                    String tmp = "현재 위치 : " + location;
                    runOnUiThread(() -> {
                        startLoc.setText(tmp);

                    });
                    // start = responseData;
                    Log.d("API success ", tmp + responseData);
                } else {
                    // 응답이 실패한 경우 처리
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {

                // 요청 실패 처리
                Log.e("API failure", e.getMessage());
            }
        });
    }
    private void playSoundEffect() {
        mediaPlayer = MediaPlayer.create(this, R.raw.sfx_success);
        mediaPlayer.start();

        // 효과음 재생이 끝나면 MediaPlayer를 해제합니다.
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mediaPlayer.release();
                mediaPlayer = null;
            }
        });
    }

    int changeToNode(String dest_point){
        dest_point=dest_point.trim();
        dest_point=dest_point.replaceAll("[층,/,호, ]","");
        if(dest_point.equals("NOINFO") || dest_point.equals("")|| dest_point.equals(" ")){
//            return changeToNode(start_point);
            return 0;
        }else{
            String stair= String.valueOf(dest_point.charAt(0));
            int room= Integer.parseInt(dest_point.substring(dest_point.length()-2,dest_point.length()));
            int node=0;

            if(Integer.parseInt(dest_point)<100){
                stair="t";
            }

            if(stair.equals("4")){
                node=(room-1);
            }else if(stair.equals("5")){
                node=(room-1);
                node+=50;
            }else if(stair.equals("t")){
                if(room==1){
                    node=40;
                }else if(room==2){
                    node=42;
                }else if(room==3){
                    node=41;
                }else if(room==4){
                    node=43;
                }else if(room==5){
                    node=35;
                }else if(room==6){
                    node=39;
                }else if(room==7){
                    node=36;
                }else if(room==8){
                    node=38;
                }else if(room==9){
                    node=37;
                }else if(room==10){
                    node=88;
                }else if(room==11){
                    node=86;
                }else if(room==12){
                    node=87;
                }else if(room==13){
                    node=89;
                }else if(room==14){
                    node=85;
                }else if(room==15){
                    node=92;
                }else if(room==16){
                    node=90;
                }else if(room==17){
                    node=91;
                }else if(room==18){
                    node=93;
                }else if(room==19){
                    node=44;
                }else if(room==20){
                    node=47;
                }else if(room==21){
                    node=45;
                }else if(room==22){
                    node=48;
                }else if(room==23){
                    node=46;
                }else if(room==24){
                    node=94;
                }else if(room==25){
                    node=97;
                }else if(room==26){
                    node=95;
                }else if(room==27){
                    node=98;
                }else if(room==28){
                    node=96;
                }else if(room==29){
                    //존재하지 않음
                }else if(room==30){
                    node=49;
                }else if(room==31){
                    node=99;
                }
            }

            return node;
        }

    }

    String changeToRoom(int node){
        String floor="";
        int flag=0;
        String info = "";
        if(node<50){//4th fllor
            floor="4";
        }else if(node<100){//5th floor
            floor="5";
            node-=50;
        }

        if(node<35){//room
            node+=1;
            if(node<10)
                info="0"+node+"호";
            else
                info= node+"호";

            if(node==10)
                info="11호 & 계단";
        }else if(node<50){//not room
            flag=1;
            if(node==35)
                info="우측 엘레베이터 복도5";
            else if(node==36)
                info="우측 엘레베이터 복도1";
            else if(node==37)
                info="우측 엘레베이터 복도2";
            else if(node==38)
                info="우측 엘레베이터 복도3";
            else if(node==39)
                info="우측 엘레베이터 복도4";
            else if(node==40)
                info="중간 엘레베이터 복도1";
            else if(node==41)
                info="중간 엘레베이터 복도2";
            else if(node==42)
                info="중간 엘레베이터 복도3";
            else if(node==43)
                info="좌측 엘레베이터 공간";
            else if(node==44)
                info="좌측 계단";
            else if(node==45)
                info="중간 엘레베이터 앞";
            else if(node==46)
                info="중간 계단 위쪽";
            else if(node==47)
                info="중간 계단 아래쪽";
            else if(node==48)
                info="우측 엘레베이터 앞";
            else if(node==49)
                info="08호 앞 계단";


        }

        if(flag==0){
            return floor+info;
        }else if(flag==1){
            return floor+"층 "+info;
        }

        return node+"";
    }
}
