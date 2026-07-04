"""mkdn 应用图标：米黄底 + 深棕 'M' 字母 + 书本折角"""
from PIL import Image, ImageDraw, ImageFont
import os

base = "/mnt/e/project/mkdn/app/src/main/res"
densities = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}

def find_font(size):
    candidates = [
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
        "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf",
        "/usr/share/fonts/TTF/DejaVuSans-Bold.ttf",
    ]
    for c in candidates:
        if os.path.exists(c):
            return ImageFont.truetype(c, size)
    return ImageFont.load_default()

def draw_icon(size):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # 圆角矩形底 - 护眼米黄
    bg = (250, 248, 242, 255)
    d.rounded_rectangle([0, 0, size-1, size-1], radius=int(size*0.18), fill=bg)
    # 左上角折角效果（小三角）
    fold = int(size*0.22)
    d.polygon([(0, 0), (fold, 0), (0, fold)], fill=(220, 213, 195, 255))
    # 主体 "M" 字
    try:
        font = find_font(int(size*0.62))
    except Exception:
        font = ImageFont.load_default()
    text = "M"
    bbox = d.textbbox((0, 0), text, font=font)
    tw, th = bbox[2]-bbox[0], bbox[3]-bbox[1]
    x = (size - tw) // 2 - bbox[0]
    y = (size - th) // 2 - bbox[1]
    d.text((x, y), text, fill=(139, 69, 19, 255), font=font)
    return img

for name, sz in densities.items():
    folder = f"{base}/mipmap-{name}"
    os.makedirs(folder, exist_ok=True)
    ic = draw_icon(sz)
    ic.save(f"{folder}/ic_launcher.png")
    ic.save(f"{folder}/ic_launcher_round.png")
    print("wrote", folder, sz)
print("done")
