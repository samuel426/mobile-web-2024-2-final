from blog.models import Post
from rest_framework import serializers
from django.contrib.auth.models import User
from rest_framework.fields import DateTimeField

class PostSerializer(serializers.ModelSerializer):
    author = serializers.PrimaryKeyRelatedField(read_only=True)  # 읽기 전용
    created_date = serializers.DateTimeField(read_only=True, format="%Y-%m-%dT%H:%M:%S.%fZ")
    published_date = serializers.DateTimeField(read_only=True, format="%Y-%m-%dT%H:%M:%S.%fZ")

    class Meta:
        model = Post
        fields = ('id', 'author', 'title', 'text', 'created_date', 'published_date', 'image')
