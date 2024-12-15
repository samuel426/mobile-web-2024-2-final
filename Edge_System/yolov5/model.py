import torch
from models.common import DetectMultiBackend

# 모델 로드
weights_path = 'yolov5s.pt'
model = DetectMultiBackend(weights=weights_path)
model.model.eval()

# ONNX 변환
dummy_input = torch.randn(1, 3, 640, 640)  # 입력 텐서 크기
onnx_path = 'yolov5s.onnx'
torch.onnx.export(
    model.model, 
    dummy_input, 
    onnx_path, 
    input_names=['input'], 
    output_names=['output'], 
    opset_version=12  # ONNX opset 버전
)
print(f"ONNX 모델이 {onnx_path}에 저장되었습니다.")
