// src/main/java/com/example/myapplication/EditActivity.java
package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log; // 로그를 사용하기 위해 추가
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.squareup.picasso.Picasso;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class EditActivity extends AppCompatActivity {

    private ImageView imgViewPost;
    private EditText editTextTitle, editTextText;
    private Button btnSave, btnCancel;
    private Post post;
    private static final String API_URL = "https://samuel26.pythonanywhere.com/api_root/Post/"; // 수정 필요
    private static final String AUTH_TOKEN = "Token 641ab83796b2582d4ff26009cbad288ace518e69";

    // 새로운 변수 추가: 새로운 이미지 URI를 저장
    private Uri newImageUri = null;

    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        imgViewPost = findViewById(R.id.imgViewPost);
        editTextTitle = findViewById(R.id.editTextTitle);
        editTextText = findViewById(R.id.editTextText);
        btnSave = findViewById(R.id.btnSaveEdit);
        btnCancel = findViewById(R.id.btnCancelEdit);

        // 초기화: newImageUri는 null로 설정
        newImageUri = null;

        // Intent로부터 Post 객체 받기
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("post")) {
            post = intent.getParcelableExtra("post");
            if (post != null) {
                editTextTitle.setText(post.getTitle());
                editTextText.setText(post.getText());

                // Picasso를 사용하여 이미지 로딩
                String imageUrl = post.getImageUrl();
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Picasso.get()
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_launcher_background) // 로딩 중 표시할 이미지
                            .error(R.drawable.ic_launcher_foreground) // 에러 발생 시 표시할 이미지
                            .into(imgViewPost);
                } else {
                    imgViewPost.setImageResource(R.drawable.ic_launcher_background); // 기본 이미지 설정
                }
            }
        }

        // 이미지 클릭 시 갤러리 열기
        imgViewPost.setOnClickListener(v -> openImagePicker());

        btnSave.setOnClickListener(v -> saveEdit());
        btnCancel.setOnClickListener(v -> finish()); // 취소 시 종료
    }

    // 이미지 선택을 위한 메서드
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "이미지 선택"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 이미지 선택 결과 처리
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            newImageUri = data.getData();
            if (newImageUri != null) {
                imgViewPost.setImageURI(newImageUri);
            }
        }
    }

    private void saveEdit() {
        String updatedTitle = editTextTitle.getText().toString().trim();
        String updatedText = editTextText.getText().toString().trim();

        if (TextUtils.isEmpty(updatedTitle) || TextUtils.isEmpty(updatedText)) {
            Toast.makeText(this, "제목과 내용을 모두 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        new UpdatePostTask().execute(updatedTitle, updatedText);
    }

    private class UpdatePostTask extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {
            String updatedTitle = params[0];
            String updatedText = params[1];

            try {
                // 고유 식별자인 pk를 URL에 포함
                int postId = post.getId();
                String requestUrl = API_URL + postId + "/"; // 서버의 API 엔드포인트에 맞게 변경

                URL url = new URL(requestUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("Authorization", AUTH_TOKEN);
                connection.setRequestMethod("PATCH"); // PATCH 메서드로 변경
                connection.setDoOutput(true);

                String boundary = "----Boundary" + System.currentTimeMillis();
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());

                // title
                outputStream.writeBytes("--" + boundary + "\r\n");
                outputStream.writeBytes("Content-Disposition: form-data; name=\"title\"\r\n\r\n");
                outputStream.writeBytes(updatedTitle + "\r\n");

                // text
                outputStream.writeBytes("--" + boundary + "\r\n");
                outputStream.writeBytes("Content-Disposition: form-data; name=\"text\"\r\n\r\n");
                outputStream.writeBytes(updatedText + "\r\n");

                // 이미지가 새로 선택된 경우에만 이미지 필드 포함
                if (newImageUri != null) {
                    InputStream imageStream = getContentResolver().openInputStream(newImageUri);
                    if (imageStream != null) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = imageStream.read(buffer)) != -1) {
                            baos.write(buffer, 0, len);
                        }
                        byte[] imageBytes = baos.toByteArray();

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
                // 로그 추가: 응답 코드 확인
                Log.d("UpdatePostTask", "Response Code: " + responseCode);

                // 응답 내용을 읽어 에러 메시지 확인
                if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_CREATED) {
                    InputStream errorStream = connection.getErrorStream();
                    if (errorStream != null) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
                        StringBuilder errorResponse = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            errorResponse.append(line);
                        }
                        reader.close();
                        Log.e("UpdatePostTask", "Error Response: " + errorResponse.toString());
                    }
                }

                // 성공적으로 업데이트되었는지 확인
                return (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("UpdatePostTask", "Exception: " + e.getMessage());
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Toast.makeText(EditActivity.this, "수정이 완료되었습니다.", Toast.LENGTH_SHORT).show();
                finish(); // 수정 완료 후 종료
            } else {
                Toast.makeText(EditActivity.this, "수정에 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
