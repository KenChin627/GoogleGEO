package fcu.iecs.googlemapgeo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import android.os.AsyncTask;

public class MainActivity extends AppCompatActivity {

    private static final String API_KEY = "AIzaSyDrI8lB_NjDLXdemix8uANrrd909HvmJb8"; // 替換為你的 API 密鑰
    private LocationManager locationManager;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        textView = findViewById(R.id.result_text_view);

        // 初始化 LocationManager
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // 檢查位置權限
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        // 設置 LocationListener
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                new GeocodingTask().execute(latitude, longitude);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        });
    }

    private class GeocodingTask extends AsyncTask<Double, Void, String> {

        @Override
        protected String doInBackground(Double... params) {
            double latitude = params[0];
            double longitude = params[1];
            String result = null;
            try {
                String urlString = String.format(
                        "https://maps.googleapis.com/maps/api/geocode/json?latlng=%f,%f&key=%s&language=zh-TW",
                        latitude, longitude, API_KEY
                );

                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder resultBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    resultBuilder.append(line);
                }
                reader.close();

                result = resultBuilder.toString(); // 返回 JSON 響應

                Log.d("GeocodingTask", "Response: " + result); // 輸出響應內容

            } catch (Exception e) {
                Log.e("GeocodingTask", "Error fetching geocoding data", e);
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    JSONArray results = jsonObject.getJSONArray("results");
                    if (results.length() > 0) {
                        JSONObject firstResult = results.getJSONObject(0);
                        JSONArray addressComponents = firstResult.getJSONArray("address_components");

                        // 使用 StringBuilder 來構建分開顯示的地址信息
                        StringBuilder addressBuilder = new StringBuilder();
                        String country = "";
                        String administrativeArea = "";
                        String district = "";
                        String street = "";
                        String streetNumber = "";

                        for (int i = 0; i < addressComponents.length(); i++) {
                            JSONObject component = addressComponents.getJSONObject(i);
                            String longName = component.getString("long_name");
                            JSONArray types = component.getJSONArray("types");

                            // 記錄組件類型以進行調試
                            Log.d("GeocodingTask", "Component: " + longName + " Types: " + types.toString());

                            for (int j = 0; j < types.length(); j++) {
                                String type = types.getString(j);
                                switch (type) {
                                    case "country":
                                        country = longName; // 國家
                                        break;
                                    case "administrative_area_level_1":
                                        administrativeArea = longName; // 省份/直轄市
                                        break;
                                    case "administrative_area_level_2":
                                        district = longName; // 區域/行政區
                                        break;
                                    case "route":
                                        street = longName; // 街道/鄉鎮
                                        break;
                                    case "street_number":
                                        streetNumber = longName; // 門牌號碼
                                        break;
                                }
                            }
                        }

                        // 將地址各部分添加到 StringBuilder 中
                        addressBuilder.append("國家: ").append(country).append("\n");
                        addressBuilder.append("省份/直轄市: ").append(administrativeArea).append("\n");
                        addressBuilder.append("區域/行政區: ").append(district).append("\n");
                        addressBuilder.append("街道/鄉鎮: ").append(street).append("\n");
                        addressBuilder.append("門牌號碼: ").append(streetNumber).append("\n");

                        textView.setText(addressBuilder.toString());
                    } else {
                        textView.setText("未找到地址");
                    }
                } catch (Exception e) {
                    Log.e("GeocodingTask", "解析 JSON 錯誤", e);
                    textView.setText("解析地址錯誤");
                }
            } else {
                textView.setText("獲取地址錯誤");
            }
        }
    }
}
