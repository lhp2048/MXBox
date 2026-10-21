from PIL import Image
import os

logo_path = r'D:\Users\mx\Desktop\MXBox\app\src\main\assets\mxbox_logo.png'
out_dir = r'D:\Users\mx\Desktop\MXBox\app\src\leanback\res'
bg = (18, 18, 18, 255)

banner_sizes = {
    'drawable-mdpi': (320, 180),
    'drawable-hdpi': (480, 270),
    'drawable-xhdpi': (640, 360),
    'drawable-xxhdpi': (960, 540),
    'drawable-xxxhdpi': (1280, 720),
}

logo = Image.open(logo_path).convert('RGBA')

for folder, (w, h) in banner_sizes.items():
    canvas = Image.new('RGBA', (w, h), bg)
    side = int(min(w, h) * 0.55)
    resized = logo.resize((side, side), Image.LANCZOS)
    x = (w - side) // 2
    y = (h - side) // 2
    canvas.paste(resized, (x, y), resized)
    path = os.path.join(out_dir, folder)
    os.makedirs(path, exist_ok=True)
    canvas.save(os.path.join(path, 'ic_banner_foreground.png'), optimize=True)
    print(f'Wrote {folder} {w}x{h}')

nodpi = os.path.join(out_dir, 'drawable-nodpi')
os.makedirs(nodpi, exist_ok=True)
w, h = 1280, 720
canvas = Image.new('RGBA', (w, h), bg)
side = int(min(w, h) * 0.55)
resized = logo.resize((side, side), Image.LANCZOS)
canvas.paste(resized, ((w - side) // 2, (h - side) // 2), resized)
canvas.save(os.path.join(nodpi, 'ic_banner_foreground.png'), optimize=True)
print('Wrote drawable-nodpi fallback')
