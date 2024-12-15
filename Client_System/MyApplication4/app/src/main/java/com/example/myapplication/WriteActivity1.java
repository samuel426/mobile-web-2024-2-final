package com.example.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

// WriteActivity1: 사진 선택 -> YOLOv5 분석 -> person+book 검출 시 WriteActivity2로 이동
public class WriteActivity1 extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView imgViewSelected;
    private TextView txtDetectionResult;
    private Button btnSelectImage, btnBack;
    private Bitmap selectedImageBitmap;
    private boolean isPersonDetected = false;
    private boolean isBookDetected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write1);

        // View 초기화
        imgViewSelected = findViewById(R.id.imgViewSelected);
        txtDetectionResult = findViewById(R.id.txtDetectionResult);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnBack = findViewById(R.id.btnBack); // 뒤로가기 버튼 추가

        // 버튼 클릭 리스너 설정
        btnSelectImage.setOnClickListener(v -> selectImage());
        btnBack.setOnClickListener(v -> finish()); // 뒤로가기 버튼 누르면 이 Activity 종료
    }

    // 이미지 선택을 위한 인텐트 시작
    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    // 이미지 선택 결과 처리
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try (InputStream inputStream = getContentResolver().openInputStream(imageUri)) {
                selectedImageBitmap = BitmapFactory.decodeStream(inputStream);
                imgViewSelected.setImageBitmap(selectedImageBitmap);

                // 이미지 분석 AsyncTask 실행
                new AnalyzeImageTask().execute(selectedImageBitmap);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "이미지를 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // AsyncTask를 사용하여 이미지 분석 수행
    private class AnalyzeImageTask extends AsyncTask<Bitmap, Void, String> {
        private Module module;

        // COCO 클래스 목록 (실제 모델에 맞게 수정 필요)
        private String[] cocoClasses = {
                "person","bicycle","car","motorcycle","airplane","bus","train","truck","boat","traffic light","fire hydrant",
                "stop sign","parking meter","bench","bird","cat","dog","horse","sheep","cow","elephant","bear","zebra","giraffe",
                "backpack","umbrella","handbag","tie","suitcase","frisbee","skis","snowboard","sports ball","kite","baseball bat",
                "baseball glove","skateboard","surfboard","tennis racket","bottle","wine glass","cup","fork","knife","spoon","bowl",
                "banana","apple","sandwich","orange","broccoli","carrot","hot dog","pizza","donut","cake","chair","couch","potted plant",
                "bed","dining table","toilet","tv","laptop","mouse","remote","keyboard","cell phone","microwave","oven","toaster","sink",
                "refrigerator","book","clock","vase","scissors","teddy bear","hair drier","toothbrush"
        };

        private ArrayList<String> detectedObjects = new ArrayList<>();

        // 모델 입력 파라미터 (YOLOv5 기본값)
        int inputWidth = 640;
        int inputHeight = 640;
        float[] mean = {0.485f, 0.456f, 0.406f};
        float[] std = {0.229f, 0.224f, 0.225f};

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            try {
                module = Module.load(assetFilePath("yolov5s.torchscript.pt"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        protected String doInBackground(Bitmap... bitmaps) {
            if (module == null) {
                return "Model not loaded";
            }

            Bitmap bitmap = bitmaps[0];
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true);
            int[] pixels = new int[inputWidth * inputHeight];
            resizedBitmap.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight);

            float[] inputData = new float[3 * inputWidth * inputHeight];
            int pixelCount = inputWidth * inputHeight;

            // RGB -> Float 배열로 변환 + 정규화
            for (int i = 0; i < pixelCount; i++) {
                int px = pixels[i];
                int r = (px >> 16) & 0xFF;
                int g = (px >> 8) & 0xFF;
                int b = px & 0xFF;

                float rf = r / 255.0f;
                float gf = g / 255.0f;
                float bf = b / 255.0f;

                rf = (rf - mean[0]) / std[0];
                gf = (gf - mean[1]) / std[1];
                bf = (bf - mean[2]) / std[2];

                inputData[i] = rf;
                inputData[i + pixelCount] = gf;
                inputData[i + 2 * pixelCount] = bf;
            }

            // 텐서 생성
            Tensor inputTensor = Tensor.fromBlob(inputData, new long[]{1, 3, inputHeight, inputWidth});
            IValue outputs = module.forward(IValue.from(inputTensor));

            // 모델 출력이 튜플 형태임을 확인
            Log.d("ModelOutput", "isTensor: " + outputs.isTensor());
            Log.d("ModelOutput", "isTuple: " + outputs.isTuple());
            Log.d("ModelOutput", "isList: " + outputs.isList());
            Log.d("ModelOutput", "isBool: " + outputs.isBool());
            Log.d("ModelOutput", "isDouble: " + outputs.isDouble());
            Log.d("ModelOutput", "isLong: " + outputs.isLong());
            Log.d("ModelOutput", "isBoolList: " + outputs.isBoolList());
            Log.d("ModelOutput", "isTensorList: " + outputs.isTensorList());

            // 모델 출력 파싱
            IValue[] tupleOutputs;
            try {
                tupleOutputs = outputs.toTuple();
            } catch (Exception e) {
                e.printStackTrace();
                return "Model output parsing error: " + e.getMessage();
            }

            if (tupleOutputs.length == 0) {
                return "Model returned empty tuple";
            }

            IValue firstElement = tupleOutputs[0];
            if (firstElement.isTensor()) {
                Tensor outputTensor = firstElement.toTensor();
                float[] outputDataArray = outputTensor.getDataAsFloatArray();
                int detectionSize = 6;
                isPersonDetected = false;
                isBookDetected = false;
                detectedObjects.clear();

                Log.d("ModelOutput", "outputDataArray length: " + outputDataArray.length);

                // YOLOv5 가정: [x1, y1, x2, y2, conf, cls]
                for (int i = 0; i < outputDataArray.length; i += detectionSize) {
                    if (i + detectionSize > outputDataArray.length) {
                        break; // 배열 범위 초과 방지
                    }
                    float conf = outputDataArray[i + 4];
                    float cls = outputDataArray[i + 5];
                    int clsIdx = (int) cls;

                    String clsName = (clsIdx >= 0 && clsIdx < cocoClasses.length) ? cocoClasses[clsIdx] : "unknown";

                    Log.d("ModelOutput", "Detection " + (i / detectionSize + 1) + ": conf=" + conf + ", cls=" + cls + " (" + clsName + ")");

                    // Confidence threshold 설정 (예: 0.5 이상)
                    if (conf < 0.5f) {
                        continue;
                    }

                    if (clsName.equals("person")) {
                        isPersonDetected = true;
                        Log.d("ModelOutput", "Person detected.");
                    } else if (clsName.equals("book")) {
                        isBookDetected = true;
                        Log.d("ModelOutput", "Book detected.");
                    }

                    detectedObjects.add(clsName);

                    // person과 book 둘 다 검출되면 더 이상 파싱하지 않음
                    if (isPersonDetected && isBookDetected) {
                        Log.d("ModelOutput", "Both person and book detected, breaking loop.");
                        break;
                    }
                }

                // 검출된 객체들을 문자열로 합치기 (최대 10개)
                StringBuilder sb = new StringBuilder("Detected Classes: ");
                if (detectedObjects.isEmpty()) {
                    sb.append("None");
                } else {
                    int maxDisplay = 10;
                    for (int j = 0; j < detectedObjects.size() && j < maxDisplay; j++) {
                        sb.append(detectedObjects.get(j));
                        if (j < detectedObjects.size() - 1 && j < maxDisplay -1 ) sb.append(", ");
                    }
                    if (detectedObjects.size() > maxDisplay) {
                        sb.append(", ...");
                    }
                }

                // person, book 동시 검출 상태 추가
                if (isPersonDetected && isBookDetected) {
                    sb.append("\nCondition Met: person and book detected.");
                } else {
                    sb.append("\nCondition Not Met: person and book not both detected.");
                }

                return sb.toString();
            } else {
                return "Model output is not a tensor";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            txtDetectionResult.setText(result);

            if (result.equals("Model not loaded")) {
                Toast.makeText(WriteActivity1.this, "모델 로딩 실패. 모델 파일 경로를 확인하세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (result.contains("Condition Met: person and book detected.")) {
                Toast.makeText(WriteActivity1.this, "객체 검출 성공! 글쓰기 화면2로 이동합니다.", Toast.LENGTH_SHORT).show();
                navigateToWriteActivity2();
            } else {
                // 필수 객체 미검출 시 화면 전환 없음
                Toast.makeText(WriteActivity1.this, "필수 객체(person, book)가 모두 검출되지 않았습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Bitmap을 캐시에 저장하고 URI를 반환하는 메서드
    private Uri saveBitmapToCache(Bitmap bitmap) {
        try {
            File cacheDir = getCacheDir();
            File tempFile = new File(cacheDir, "selected_image.jpg");
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            }
            return Uri.fromFile(tempFile);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // WriteActivity2로 이동하는 메서드
    private void navigateToWriteActivity2() {
        Uri imageUri = saveBitmapToCache(selectedImageBitmap);
        if (imageUri == null) {
            Toast.makeText(this, "이미지 URI 생성 실패", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(WriteActivity1.this, WriteActivity2.class);
        intent.putExtra("imageUri", imageUri.toString());
        startActivity(intent);
    }

    // asset 폴더에서 파일을 복사하여 앱 내부 파일 경로를 반환하는 메서드
    private String assetFilePath(String assetName) throws IOException {
        File file = new File(getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }
        try (InputStream is = getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }
}
