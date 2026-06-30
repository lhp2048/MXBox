from PIL import Image
import os

src = r"D:\Users\mx\Desktop\MXBox\bg.png"
targets = [
    r"D:\Users\mx\Desktop\MXBox\app\src\main\res\drawable-nodpi\wallpaper_1.webp",
]

img = Image.open(src).convert("RGB")
w, h = img.size
target_ratio = 16 / 9
current_ratio = w / h
if current_ratio > target_ratio:
    new_w = int(h * target_ratio)
    left = (w - new_w) // 2
    img = img.crop((left, 0, left + new_w, h))
else:
    new_h = int(w / target_ratio)
    top = (h - new_h) // 2
    img = img.crop((0, top, w, top + new_h))

img = img.resize((1920, 1080), Image.LANCZOS)

for path in targets:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path, "WEBP", quality=88, method=6)
    print("Saved", path)

# sample theme color from upper-left grass area
sample = img.resize((64, 64), Image.LANCZOS)
pixels = list(sample.getdata())
r = sum(p[0] for p in pixels) // len(pixels)
g = sum(p[1] for p in pixels) // len(pixels)
b = sum(p[2] for p in pixels) // len(pixels)
print(f"Theme color: 0xFF{r:02X}{g:02X}{b:02X}")
