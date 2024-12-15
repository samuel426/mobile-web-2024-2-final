# changedetection.py
import os
import cv2
import pathlib
import requests
from datetime import datetime


class ChangeDetection:
    HOST = 'https://samuel26.pythonanywhere.com'
    username = 'admin'
    password = '1234'
    token = ''  # 초기 토큰 비워두기
    author = 1

    coco_classes = [
        'person', 'bicycle', 'car', 'motorcycle', 'airplane', 'bus', 'train', 'truck', 'boat', 'traffic light',
        'fire hydrant', 'N/A', 'stop sign', 'parking meter', 'bench', 'bird', 'cat', 'dog', 'horse', 'sheep',
        'cow', 'elephant', 'bear', 'zebra', 'giraffe', 'N/A', 'backpack', 'umbrella', 'handbag', 'tie', 'suitcase',
        'frisbee', 'skis', 'snowboard', 'sports ball', 'kite', 'baseball bat', 'baseball glove', 'skateboard',
        'surfboard', 'tennis racket', 'bottle', 'N/A', 'wine glass', 'cup', 'fork', 'knife', 'spoon', 'bowl', 'banana',
        'apple', 'sandwich', 'orange', 'broccoli', 'carrot', 'hot dog', 'pizza', 'donut', 'cake', 'chair', 'couch',
        'potted plant', 'bed', 'N/A', 'dining table', 'N/A', 'toilet', 'N/A', 'tv', 'laptop', 'mouse', 'remote', 'keyboard',
        'cell phone', 'microwave', 'oven', 'toaster', 'sink', 'refrigerator', 'book', 'clock', 'vase', 'scissors', 'teddy bear',
        'hair drier', 'toothbrush'
    ]

    def __init__(self):
        # 초기 토큰 가져오기
        self.refresh_token()

    def refresh_token(self):
        """
        토큰을 갱신하는 메서드입니다.
        """
        try:
            res = requests.post(f'{self.HOST}/api-token-auth/', {
                'username': self.username,
                'password': self.password,
            })
            res.raise_for_status()
            self.token = res.json()['token']
            print("Token refreshed:", self.token)
        except requests.RequestException as e:
            print(f"Failed to refresh token: {e}")

    def send_with_user_input(self, save_dir, image_path, title, text):
        """
        사용자 입력을 받아 서버로 데이터를 전송하는 메서드입니다.
        """
        now = datetime.now()

        headers = {
            'Authorization': f'Token {self.token}',  # 변경된 부분
            'Accept': 'application/json'
        }

        data = {
            'title': title,
            'text': text,
            # 'author': self.author,  # 클라이언트에서 author를 보내지 않도록 수정 (서버에서 설정)
            # 'created_date': now.isoformat(),  # 서버에서 자동 설정
            # 'published_date': now.isoformat()  # 서버에서 자동 설정
        }

        try:
            with open(image_path, 'rb') as image_file:
                files = {'image': image_file}
                res = requests.post(f'{self.HOST}/api_root/Post/', data=data, files=files, headers=headers)
                if res.status_code == 401:  # Unauthorized, 토큰 만료 시
                    print("Token expired. Refreshing token...")
                    self.refresh_token()
                    headers['Authorization'] = f'Token {self.token}'  # 토큰 갱신 후 헤더 수정
                    res = requests.post(f'{self.HOST}/api_root/Post/', data=data, files=files, headers=headers)
                print(f"Response: {res.status_code}, {res.text}")
        except requests.RequestException as e:
            print(f"Failed to send data: {e}")
        except Exception as e:
            print(f"Error opening image file: {e}")

        # 임계값 체크 (if needed)
        self.check_detection_threshold(detection_count=1)  # Adjust as needed

    def check_detection_threshold(self, detection_count):
        """
        특정 임계값을 초과하면 경고 메시지를 출력합니다.
        """
        threshold = 10  # 임계값 설정
        if detection_count > threshold:
            print(f"Warning: Detected object count exceeds the threshold! ({detection_count} > {threshold})")
            # 추가 작업을 수행할 수 있음 (예: 알림, 로그 저장 등)
