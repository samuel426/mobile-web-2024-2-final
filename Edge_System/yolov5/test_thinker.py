# test_tkinter.py
import tkinter as tk
from tkinter import simpledialog

def test_dialog():
    root = tk.Tk()
    root.withdraw()  # 메인 윈도우 숨기기
    title = simpledialog.askstring("Input", "Title:")
    text = simpledialog.askstring("Input", "Text:")
    root.destroy()
    print(f"Title: {title}, Text: {text}")

if __name__ == "__main__":
    test_dialog()
