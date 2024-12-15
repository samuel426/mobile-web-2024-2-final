package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WriteActivity2 extends AppCompatActivity {

    private ImageView imgViewPost;
    private EditText editTextTitle, editTextText;
    private Button btnSave, btnCancel;
    private Uri imageUri;
    private static final String API_URL = "https://samuel26.pythonanywhere.com/api_root/Post/";
    private static final String AUTH_TOKEN = "Token 641ab83796b2582d4ff26009cbad288ace518e69";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write2); // 올바른 레이아웃 파일 지정

        // View 초기화
        imgViewPost = findViewById(R.id.imgViewPost); // 레이아웃 파일의 ID와 일치해야 함
        editTextTitle = findViewById(R.id.editTextTitle);
        editTextText = findViewById(R.id.editTextText);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);

        // Intent로부터 imageUri 받기
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("imageUri")) {
            String imageUriString = intent.getStringExtra("imageUri");
            if (imageUriString != null) {
                imageUri = Uri.parse(imageUriString);
                imgViewPost.setImageURI(imageUri);
            }
        }

        // 버튼 클릭 리스너 설정
        btnSave.setOnClickListener(v -> savePost());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void savePost() {
        String title = editTextTitle.getText().toString().trim();
        String text = editTextText.getText().toString().trim();

        if (title.isEmpty() || text.isEmpty()) {
            Toast.makeText(this, "제목과 내용을 모두 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        new CreatePostTask().execute(title, text);
    }

    private class CreatePostTask extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {
            String title = params[0];
            String text = params[1];

            try {
                URL url = new URL(API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("Authorization", AUTH_TOKEN);
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);

                String boundary = "----Boundary" + System.currentTimeMillis();
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());

                // title
                outputStream.writeBytes("--" + boundary + "\r\n");
                outputStream.writeBytes("Content-Disposition: form-data; name=\"title\"\r\n\r\n");
                outputStream.writeBytes(title + "\r\n");

                // text
                outputStream.writeBytes("--" + boundary + "\r\n");
                outputStream.writeBytes("Content-Disposition: form-data; name=\"text\"\r\n\r\n");
                outputStream.writeBytes(text + "\r\n");

                // image (optional)
                if (imageUri != null) {
                    InputStream imageStream = getContentResolver().openInputStream(imageUri);
                    if (imageStream != null) {
                        byte[] imageBytes = getBytesFromInputStream(imageStream);

                        outputStream.writeBytes("--" + boundary + "\r\n");
                        outputStream.writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"image.jpg\"\r\n");
                        outputStream.writeBytes("Content-Type: image/jpeg\r\n\r\n");
                        outputStream.write(imageBytes);
                        outputStream.writeBytes("\r\n");
                        imageStream.close();
                    }
                }

                // 마무리 boundary
                outputStream.writeBytes("--" + boundary + "--\r\n");
                outputStream.flush();
                outputStream.close();

                int responseCode = connection.getResponseCode();
                Log.d("CreatePostTask", "Response Code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
                    return true;
                } else {
                    // 에러 응답 읽기
                    InputStream errorStream = connection.getErrorStream();
                    if (errorStream != null) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
                        StringBuilder errorResponse = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            errorResponse.append(line);
                        }
                        reader.close();
                        Log.e("CreatePostTask", "Error Response: " + errorResponse.toString());
                    }
                    return false;
                }

            } catch (Exception e) {
                e.printStackTrace();
                Log.e("CreatePostTask", "Exception: " + e.getMessage());
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Toast.makeText(WriteActivity2.this, "글이 성공적으로 작성되었습니다.", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(WriteActivity2.this, "저장에 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        }

        private byte[] getBytesFromInputStream(InputStream is) throws IOException {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[16384];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            return buffer.toByteArray();
        }
    }
}
