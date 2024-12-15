// src/main/java/com/example/myapplication/MainActivity.java
package com.example.myapplication;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Comparator;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ImageAdapter adapter;
    private List<Post> postList = new ArrayList<>();
    private static final String API_URL = "https://samuel26.pythonanywhere.com/api_root/Post/";
    private static final String AUTH_TOKEN = "Token 641ab83796b2582d4ff26009cbad288ace518e69";
    private Button btnWrite, btnRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // View 초기화
        recyclerView = findViewById(R.id.recyclerView);
        btnWrite = findViewById(R.id.btnWrite);
        btnRefresh = findViewById(R.id.btnRefresh); // 새로고침 버튼 초기화

        // Adapter 초기화
        adapter = new ImageAdapter(this, postList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // 글쓰기 버튼 클릭 리스너 설정
        btnWrite.setOnClickListener(v -> {
            // 글쓰기 화면1로 이동
            Intent intent = new Intent(MainActivity.this, WriteActivity1.class);
            startActivity(intent);
        });

        // 새로고침 버튼 클릭 리스너 설정
        btnRefresh.setOnClickListener(v -> {
            // 새로고침 버튼 클릭 시 데이터 다시 로드
            loadPosts();
        });

        // RecyclerView 아이템 클릭 리스너 설정
        adapter.setOnItemClickListener(new ImageAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Post post) {
                Intent intent = new Intent(MainActivity.this, EditActivity.class);
                intent.putExtra("post", post); // Parcelable 객체 전달
                startActivity(intent);
            }
        });

        loadPosts(); // 초기 데이터 로드
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPosts(); // 화면이 다시 활성화될 때 데이터 새로고침
    }

    private void loadPosts() {
        new GetPostsTask().execute(API_URL);
    }

    private class GetPostsTask extends AsyncTask<String, Void, List<Post>> {

        @Override
        protected List<Post> doInBackground(String... urls) {
            String urlString = urls[0];
            List<Post> posts = new ArrayList<>();

            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("Authorization", AUTH_TOKEN);
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONArray jsonArray = new JSONArray(response.toString());
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject postJson = jsonArray.getJSONObject(i);
                        int id = postJson.getInt("id"); // id 필드 추가
                        int author = postJson.getInt("author");
                        String title = postJson.getString("title");
                        String text = postJson.getString("text");
                        String imageUrl = postJson.getString("image");
                        String createdDate = postJson.getString("created_date");
                        String publishedDate = postJson.getString("published_date");

                        posts.add(new Post(id, author, title, text, createdDate, publishedDate, imageUrl));
                    }
                } else {
                    Log.e("GetPostsTask", "Response Code: " + responseCode);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("GetPostsTask", "Exception: " + e.getMessage());
            }
            return posts;
        }

        @Override
        protected void onPostExecute(List<Post> posts) {
            if (posts.isEmpty()) {
                Toast.makeText(MainActivity.this, "데이터를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
            } else {
                postList.clear();
                postList.addAll(posts);

                // 날짜 파싱을 위한 여러 패턴 시도
                Collections.sort(postList, new Comparator<Post>() {
                    @Override
                    public int compare(Post p1, Post p2) {
                        Date date1 = parseDate(p1.getCreatedDate());
                        Date date2 = parseDate(p2.getCreatedDate());

                        if (date1 != null && date2 != null) {
                            return date2.compareTo(date1); // 최신순 (내림차순)
                        } else if (date1 == null && date2 != null) {
                            return 1; // date2가 더 최신
                        } else if (date1 != null && date2 == null) {
                            return -1; // date1이 더 최신
                        } else {
                            return p2.getCreatedDate().compareTo(p1.getCreatedDate()); // 문자열 비교
                        }
                    }
                });

                adapter.notifyDataSetChanged();
                Toast.makeText(MainActivity.this, "데이터를 새로고침했습니다.", Toast.LENGTH_SHORT).show();
            }
        }

        /**
         * 여러 날짜 형식을 시도하여 Date 객체로 변환하는 메서드
         * @param dateStr 서버에서 받은 날짜 문자열
         * @return Date 객체 또는 null
         */
        private Date parseDate(String dateStr) {
            String[] dateFormats = {
                    "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", // 마이크로초 포함
                    "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",    // 밀리초 포함
                    "yyyy-MM-dd'T'HH:mm:ssXXX"         // 초까지만
            };

            for (String format : dateFormats) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
                    return sdf.parse(dateStr);
                } catch (ParseException e) {
                    // 로그 추가: 어떤 포맷이 실패했는지 확인
                    Log.e("GetPostsTask", "Date parse failed for format: " + format + ", dateStr: " + dateStr);
                }
            }
            return null; // 모든 포맷이 실패한 경우
        }
    }
}
