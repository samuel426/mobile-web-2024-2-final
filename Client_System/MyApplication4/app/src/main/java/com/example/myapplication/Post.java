// src/main/java/com/example/myapplication/Post.java
package com.example.myapplication;

import android.os.Parcel;
import android.os.Parcelable;

public class Post implements Parcelable {
    private int id; // pk 필드 추가
    private int author;
    private String title;
    private String text;
    private String createdDate;
    private String publishedDate;
    private String imageUrl;

    // 생성자
    public Post(int id, int author, String title, String text, String createdDate, String publishedDate, String imageUrl) {
        this.id = id;
        this.author = author;
        this.title = title;
        this.text = text;
        this.createdDate = createdDate;
        this.publishedDate = publishedDate;
        this.imageUrl = imageUrl;
    }

    // Parcelable 구현
    protected Post(Parcel in) {
        id = in.readInt();
        author = in.readInt();
        title = in.readString();
        text = in.readString();
        createdDate = in.readString();
        publishedDate = in.readString();
        imageUrl = in.readString();
    }

    public static final Creator<Post> CREATOR = new Creator<Post>() {
        @Override
        public Post createFromParcel(Parcel in) {
            return new Post(in);
        }

        @Override
        public Post[] newArray(int size) {
            return new Post[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeInt(author);
        parcel.writeString(title);
        parcel.writeString(text);
        parcel.writeString(createdDate);
        parcel.writeString(publishedDate);
        parcel.writeString(imageUrl);
    }

    // Getter 메서드
    public int getId() {
        return id;
    }

    public int getAuthor() {
        return author;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public String getPublishedDate() {
        return publishedDate;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    // Setter 메서드 (필요 시 추가)
    public void setTitle(String title) {
        this.title = title;
    }

    public void setText(String text) {
        this.text = text;
    }
}
