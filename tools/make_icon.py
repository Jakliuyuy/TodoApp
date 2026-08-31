#!/usr/bin/env python3
"""生成 TodoApp 的启动图标：紫色渐变圆角方块 + 白色对勾。

用法: python3 tools/make_icon.py
输出: app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.png
"""
import os
from PIL import Image, ImageDraw

SIZES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

BG_TOP = (0x8A, 0x6A, 0xE8)
BG_BOTTOM = (0x5B, 0x3F, 0xB0)
CHECK = (255, 255, 255, 255)
SUPER = 4  # 超采样倍数，保证缩放后边缘平滑


def make_base(size: int) -> Image.Image:
    s = size * SUPER
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # 竖向渐变背景（正方形，留出安全边距以免被系统遮罩裁掉内容）
    pad = int(s * 0.06)
    box_s = s - pad * 2
    radius = int(box_s * 0.22)
    gradient = Image.new("RGBA", (box_s, box_s))
    gd = ImageDraw.Draw(gradient)
    for y in range(box_s):
        t = y / max(box_s - 1, 1)
        color = tuple(int(BG_TOP[i] + (BG_BOTTOM[i] - BG_TOP[i]) * t) for i in range(3)) + (255,)
        gd.line([(0, y), (box_s, y)], fill=color)

    # 圆角遮罩
    mask = Image.new("L", (box_s, box_s), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, box_s - 1, box_s - 1], radius=radius, fill=255)
    img.paste(gradient, (pad, pad), mask)

    # 白色对勾
    d = ImageDraw.Draw(img)
    w = max(int(s * 0.095), 2)
    p1 = (int(s * 0.25), int(s * 0.47))
    p2 = (int(s * 0.42), int(s * 0.64))
    p3 = (int(s * 0.76), int(s * 0.30))
    d.line([p1, p2], fill=CHECK, width=w, joint="curve")
    d.line([p2, p3], fill=CHECK, width=w, joint="curve")
    # 补齐折角
    d.ellipse([p2[0] - w // 2, p2[1] - w // 2, p2[0] + w // 2, p2[1] + w // 2], fill=CHECK)

    return img.resize((size, size), Image.LANCZOS)


def main():
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    base = make_base(512)
    for folder, size in SIZES.items():
        out_dir = os.path.join(root, "app", "src", "main", "res", folder)
        os.makedirs(out_dir, exist_ok=True)
        out = os.path.join(out_dir, "ic_launcher.png")
        base.resize((size, size), Image.LANCZOS).save(out, "PNG")
        print("生成", out, f"{size}x{size}")


if __name__ == "__main__":
    main()
