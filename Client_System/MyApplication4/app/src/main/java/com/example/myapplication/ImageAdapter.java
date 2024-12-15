// src/main/java/com/example/myapplication/ImageAdapter.java
package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {

    private List<Post> postList;
    private Context context;
    private OnItemClickListener listener;

    // 인터페이스 정의
    public interface OnItemClickListener {
        void onItemClick(Post post);
    }

    // 리스너 설정 메서드
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public ImageAdapter(Context context, List<Post> postList) {
        this.context = context;
        this.postList = postList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 아이템 레이아웃 inflating
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Post post = postList.get(position);
        holder.textTitle.setText(post.getTitle());
        holder.textDate.setText(formatDate(post.getCreatedDate()));

        // Picasso를 사용하여 이미지 로딩
        String imageUrl = post.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_launcher_background) // 로딩 중 표시할 이미지
                    .error(R.drawable.ic_launcher_foreground) // 에러 발생 시 표시할 이미지
                    .into(holder.imageView, new Callback() {
                        @Override
                        public void onSuccess() {
                            // 이미지 로드 성공
                            // Log.d("ImageAdapter", "Image loaded successfully: " + imageUrl);
                        }

                        @Override
                        public void onError(Exception e) {
                            e.printStackTrace();
                            // Log.e("ImageAdapter", "Error loading image: " + imageUrl, e);
                        }
                    });
        } else {
            holder.imageView.setImageResource(R.drawable.ic_launcher_background); // 기본 이미지 설정
        }

        // 클릭 리스너 설정
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(post);
            }
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    // ViewHolder 클래스
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView textTitle, textDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imgPost);
            textTitle = itemView.findViewById(R.id.textTitle);
            textDate = itemView.findViewById(R.id.textDate);
        }
    }

    // 날짜 포맷을 변경하는 메서드
    private String formatDate(String dateStr) {
        // 서버에서 오는 날짜 형식: "2024-12-14T00:36:46.327586+09:00"
        // 원하는 형식으로 변환 (예: "2024-12-14 00:36")
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            Date date = inputFormat.parse(dateStr);
            if (date != null) {
                return outputFormat.format(date);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return dateStr; // 변환 실패 시 원래 문자열 반환
    }
}
