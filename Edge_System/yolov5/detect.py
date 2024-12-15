# detect.py
# Ultralytics YOLOv5 🚀, AGPL-3.0 license
"""
Run YOLOv5 detection inference on images, videos, directories, globs, YouTube, webcam, streams, etc.
"""

import argparse
import os
import sys
from pathlib import Path
import tkinter as tk
from tkinter import simpledialog
from datetime import datetime

import numpy as np
import torch
import cv2  # Ensure OpenCV is imported

FILE = Path(__file__).resolve()
ROOT = FILE.parents[0]  # YOLOv5 root directory
if str(ROOT) not in sys.path:
    sys.path.append(str(ROOT))  # add ROOT to PATH
ROOT = Path(os.path.relpath(ROOT, Path.cwd()))  # relative

from ultralytics.utils.plotting import Annotator, colors, save_one_box
from models.common import DetectMultiBackend
from utils.dataloaders import LoadImages, LoadStreams
from utils.general import (
    check_img_size,
    check_requirements,  # Added import
    increment_path,
    non_max_suppression,
    print_args,
)
from utils.torch_utils import select_device, smart_inference_mode

from changedetection import ChangeDetection


class InputDialog(simpledialog.Dialog):
    def body(self, master):
        tk.Label(master, text="Title:").grid(row=0)
        tk.Label(master, text="Text:").grid(row=1)

        self.title_entry = tk.Entry(master)
        self.text_entry = tk.Entry(master)

        self.title_entry.grid(row=0, column=1)
        self.text_entry.grid(row=1, column=1)
        return self.title_entry  # initial focus

    def apply(self):
        self.result = {
            'title': self.title_entry.get(),
            'text': self.text_entry.get()
        }


def show_input_dialog():
    """
    사용자로부터 Title과 Text를 입력받는 다이얼로그를 표시합니다.
    """
    print("Showing input dialog")  # Debugging output
    root = tk.Tk()
    root.withdraw()  # Hide the main window
    dialog = InputDialog(root)
    root.destroy()
    if hasattr(dialog, 'result'):
        print("User input received")  # Debugging output
        return dialog.result.get('title'), dialog.result.get('text')
    else:
        print("No user input")  # Debugging output
        return None, None


def save_image(image, save_dir):
    """
    탐지된 이미지를 지정된 디렉토리에 저장하고 경로를 반환합니다.
    """
    now = datetime.now()
    today = now.date()

    save_path = Path(save_dir) / 'detected' / str(today.year) / str(today.month) / str(today.day)
    save_path.mkdir(parents=True, exist_ok=True)

    file_name = f"{now.strftime('%H-%M-%S')}-{now.microsecond}.jpg"
    full_path = save_path / file_name

    # 이미지 저장
    cv2.imwrite(str(full_path), image)

    return str(full_path)


@smart_inference_mode()
def run(
    weights=ROOT / "yolov5s.pt",  # model path or triton URL
    source=ROOT / "data/images",  # file/dir/URL/glob/screen/0(webcam)
    data=ROOT / "data/coco128.yaml",  # dataset.yaml path
    imgsz=(640, 640),  # inference size (height, width)
    conf_thres=0.25,  # confidence threshold
    iou_thres=0.45,  # NMS IOU threshold
    max_det=1000,  # maximum detections per image
    device="",  # cuda device, i.e. 0 or 0,1,2,3 or cpu
    view_img=False,  # show results
    save_txt=False,  # save results to *.txt
    save_format=0,  # save boxes coordinates in YOLO format or Pascal-VOC format (0 for YOLO and 1 for Pascal-VOC)
    save_csv=False,  # save results in CSV format
    save_conf=False,  # save confidences in --save-txt labels
    save_crop=False,  # save cropped prediction boxes
    nosave=False,  # do not save images/videos
    classes=None,  # filter by class: --class 0, or --class 0 2 3
    agnostic_nms=False,  # class-agnostic NMS
    augment=False,  # augmented inference
    visualize=False,  # visualize features
    update=False,  # update all models
    project=ROOT / "runs/detect",  # save results to project/name
    name="exp",  # save results to project/name
    exist_ok=False,  # existing project/name ok, do not increment
    line_thickness=3,  # bounding box thickness (pixels)
    hide_labels=False,  # hide labels
    hide_conf=False,  # hide confidences
    half=False,  # use FP16 half-precision inference
    dnn=False,  # use OpenCV DNN for ONNX inference
    vid_stride=1,  # video frame-rate stride
):
    # Initialize ChangeDetection
    cd = ChangeDetection()
    save_dir = increment_path(Path(project) / name, exist_ok=exist_ok)
    (save_dir / "labels").mkdir(parents=True, exist_ok=True)

    # Initialize device and model
    device = select_device(device)
    model = DetectMultiBackend(weights, device=device, data=data)
    stride, names, pt = model.stride, model.names, model.pt
    imgsz = check_img_size(imgsz, s=stride)

    # Load dataset
    dataset = LoadStreams(source, img_size=imgsz, stride=stride, auto=pt) if source.isnumeric() else LoadImages(source, img_size=imgsz, stride=stride)

    # Flag to prevent multiple dialogs
    is_waiting_for_input = False

    for path, im, im0s, vid_cap, s in dataset:
        if isinstance(im0s, list):
            im0s = im0s[0]

        if not isinstance(im0s, np.ndarray):
            try:
                im0s = np.array(im0s)
            except Exception as e:
                print(f"Error converting im0s to NumPy array: {e}")
                continue

        # Preprocess image
        im = torch.from_numpy(im).to(device)
        im = im.half() if model.fp16 else im.float()
        im /= 255.0  # normalize to [0,1]
        if len(im.shape) == 3:
            im = im[None]  # add batch dimension

        # Inference
        pred = model(im)
        pred = non_max_suppression(pred, conf_thres, iou_thres, max_det=max_det)

        detected_classes = [0] * len(names)
        labels_to_draw = []

        for det in pred:
            labels_to_draw.clear()
            if len(det):
                for *xyxy, conf, cls in det:
                    detected_classes[int(cls)] += 1
                    labels_to_draw.append((names[int(cls)], xyxy))

        # Aggregate detected class names and counts
        detected_class_names_list = [names[idx] for idx, count in enumerate(detected_classes) if count > 0]
        detected_class_names = ', '.join(detected_class_names_list)
        detected_count = sum(detected_classes)

        # Print detected classes
        print(f"Detected Classes: {detected_class_names}")
        print(f"Detected Count: {detected_count}")

        # Check if both 'person' and 'book' are detected and not already waiting for input
        if not is_waiting_for_input and "person" in detected_class_names_list and "book" in detected_class_names_list:
            print("Person and Book detected. Entering wait state.")
            is_waiting_for_input = True
            # Save the detected image
            save_image_path = save_image(im0s, save_dir)
            # Display UI and get user input
            title, text = show_input_dialog()
            if title and text:
                # Send POST request with user input
                cd.send_with_user_input(save_dir, save_image_path, title, text)
                # Print sent content
                print(f"Posted Data - Title: {title}, Text: {text}")
                # Terminate the program after successful POST
                print("POST request completed. Exiting the program.")
                # Release resources before exiting
                if view_img:
                    cv2.destroyAllWindows()
                sys.exit(0)  # 정상 종료
            else:
                print("User cancelled input.")
            # Reset the flag
            is_waiting_for_input = False

        if view_img:
            cv2.imshow(str(path), im0s)
            if cv2.waitKey(1) & 0xFF == ord('q'):
                print("Exit key pressed. Exiting the program.")
                break  # 사용자가 'q' 키를 누르면 루프 종료

    # 자원 해제
    cap = getattr(dataset, 'cap', None)
    if cap:
        cap.release()
    cv2.destroyAllWindows()

    # 프로그램 종료
    sys.exit(0)


def parse_opt():
    parser = argparse.ArgumentParser()
    parser.add_argument("--weights", nargs="+", type=str, default=ROOT / "yolov5s.pt", help="model path or triton URL")
    parser.add_argument("--source", type=str, default=ROOT / "data/images", help="file/dir/URL/glob/screen/0(webcam)")
    parser.add_argument("--data", type=str, default=ROOT / "data/coco128.yaml", help="(optional) dataset.yaml path")
    parser.add_argument("--imgsz", "--img", "--img-size", nargs="+", type=int, default=[640], help="inference size h,w")
    parser.add_argument("--conf-thres", type=float, default=0.25, help="confidence threshold")
    parser.add_argument("--iou-thres", type=float, default=0.45, help="NMS IOU threshold")
    parser.add_argument("--max-det", type=int, default=1000, help="maximum detections per image")
    parser.add_argument("--device", default="", help="cuda device, i.e. 0 or 0,1,2,3 or cpu")
    parser.add_argument("--view-img", action="store_true", help="show results")
    parser.add_argument("--save-txt", action="store_true", help="save results to *.txt")
    parser.add_argument(
        "--save-format",
        type=int,
        default=0,
        help="whether to save boxes coordinates in YOLO format or Pascal-VOC format when save-txt is True, 0 for YOLO and 1 for Pascal-VOC",
    )
    parser.add_argument("--save-csv", action="store_true", help="save results in CSV format")
    parser.add_argument("--save-conf", action="store_true", help="save confidences in --save-txt labels")
    parser.add_argument("--save-crop", action="store_true", help="save cropped prediction boxes")
    parser.add_argument("--nosave", action="store_true", help="do not save images/videos")
    parser.add_argument("--classes", nargs="+", type=int, help="filter by class: --classes 0, or --classes 0 2 3")
    parser.add_argument("--agnostic-nms", action="store_true", help="class-agnostic NMS")
    parser.add_argument("--augment", action="store_true", help="augmented inference")
    parser.add_argument("--visualize", action="store_true", help="visualize features")
    parser.add_argument("--update", action="store_true", help="update all models")
    parser.add_argument("--project", default=ROOT / "runs/detect", help="save results to project/name")
    parser.add_argument("--name", default="exp", help="save results to project/name")
    parser.add_argument("--exist-ok", action="store_true", help="existing project/name ok, do not increment")
    parser.add_argument("--line-thickness", default=3, type=int, help="bounding box thickness (pixels)")
    parser.add_argument("--hide-labels", default=False, action="store_true", help="hide labels")
    parser.add_argument("--hide-conf", default=False, action="store_true", help="hide confidences")
    parser.add_argument("--half", action="store_true", help="use FP16 half-precision inference")
    parser.add_argument("--dnn", action="store_true", help="use OpenCV DNN for ONNX inference")
    parser.add_argument("--vid-stride", type=int, default=1, help="video frame-rate stride")
    opt = parser.parse_args()
    opt.imgsz *= 2 if len(opt.imgsz) == 1 else 1  # expand
    print_args(vars(opt))
    return opt


def main(opt):
    check_requirements(ROOT / "requirements.txt", exclude=("tensorboard", "thop"))
    run(**vars(opt))


if __name__ == "__main__":
    opt = parse_opt()
    main(opt)
