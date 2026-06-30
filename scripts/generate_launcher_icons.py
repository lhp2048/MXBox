from PIL import Image
import math
import os

src = r"C:\Users\mx\.cursor\projects\d-Users-mx-Desktop-MXBox\assets\mxbox_app_logo_no_ring.png"
out_dir = r"D:\Users\mx\Desktop\MXBox\app\src\main\res"
assets_dir = r"D:\Users\mx\Desktop\MXBox\app\src\main\assets"
BG = (18, 18, 18, 255)
CANVAS = 1024
# Android adaptive icon + splash visible circle diameter / layer size.
SAFE_DIAMETER_RATIO = 66 / 108
SAFE_MARGIN = 0.90
# In-app toolbar uses a square view; keep glyph smaller for readability.
APP_MAX_FILL = 0.55

os.makedirs(assets_dir, exist_ok=True)


def trim_content(im, threshold=25):
    pixels = im.load()
    w, h = im.size
    min_x, min_y, max_x, max_y = w, h, 0, 0
    for y in range(h):
        for x in range(w):
            r, g, b, a = pixels[x, y]
            if a > 10 and (r > threshold or g > threshold or b > threshold):
                min_x = min(min_x, x)
                min_y = min(min_y, y)
                max_x = max(max_x, x)
                max_y = max(max_y, y)
    if max_x <= min_x:
        return im
    pad = max(8, int(min(max_x - min_x, max_y - min_y) * 0.02))
    return im.crop(
        (
            max(0, min_x - pad),
            max(0, min_y - pad),
            min(w, max_x + pad + 1),
            min(h, max_y + pad + 1),
        )
    )


def fit_on_canvas(content, canvas_size, max_half_radius, bg=BG):
    w, h = content.size
    half_diag = math.hypot(w / 2, h / 2)
    scale = max_half_radius / half_diag if half_diag > 0 else 1
    new_w = max(1, int(w * scale))
    new_h = max(1, int(h * scale))
    resized = content.resize((new_w, new_h), Image.LANCZOS)
    canvas = Image.new("RGBA", (canvas_size, canvas_size), bg)
    canvas.paste(
        resized,
        ((canvas_size - new_w) // 2, (canvas_size - new_h) // 2),
        resized,
    )
    return canvas


def fit_square(content, canvas_size, max_fill, bg=BG):
    w, h = content.size
    scale = min(canvas_size * max_fill / w, canvas_size * max_fill / h)
    new_w = max(1, int(w * scale))
    new_h = max(1, int(h * scale))
    resized = content.resize((new_w, new_h), Image.LANCZOS)
    canvas = Image.new("RGBA", (canvas_size, canvas_size), bg)
    canvas.paste(
        resized,
        ((canvas_size - new_w) // 2, (canvas_size - new_h) // 2),
        resized,
    )
    return canvas


def report(name, image):
    w, h = image.size
    px = image.load()
    min_x, min_y, max_x, max_y = w, h, 0, 0
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a > 10 and (r > 35 or g > 35 or b > 35):
                min_x = min(min_x, x)
                min_y = min(min_y, y)
                max_x = max(max_x, x)
                max_y = max(max_y, y)
    cx, cy = w / 2, h / 2
    corners = [
        math.hypot(min_x - cx, min_y - cy),
        math.hypot(max_x - cx, min_y - cy),
        math.hypot(min_x - cx, max_y - cy),
        math.hypot(max_x - cx, max_y - cy),
    ]
    safe_r = (SAFE_DIAMETER_RATIO * w / 2) * SAFE_MARGIN
    print(
        f"{name}: fill={(max_x - min_x + 1) / w:.1%} "
        f"maxCorner={max(corners):.0f}px safeR={safe_r:.0f}px ok={max(corners) <= safe_r}"
    )


img = Image.open(src).convert("RGBA")
trimmed = trim_content(img)
safe_radius = (SAFE_DIAMETER_RATIO * CANVAS / 2) * SAFE_MARGIN
launcher = fit_on_canvas(trimmed, CANVAS, safe_radius)
app_logo = fit_square(trimmed, CANVAS, APP_MAX_FILL)

master_path = os.path.join(assets_dir, "mxbox_logo.png")
launcher.save(master_path, optimize=True)
report("launcher", launcher)
report("app_logo", app_logo)

mipmap_sizes = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}
drawable_sizes = {
    "drawable-mdpi": 108,
    "drawable-hdpi": 162,
    "drawable-xhdpi": 216,
    "drawable-xxhdpi": 324,
    "drawable-xxxhdpi": 432,
}

for folder, size in mipmap_sizes.items():
    path = os.path.join(out_dir, folder)
    os.makedirs(path, exist_ok=True)
    resized = launcher.resize((size, size), Image.LANCZOS)
    resized.save(os.path.join(path, "ic_launcher.png"), optimize=True)
    resized.save(os.path.join(path, "ic_launcher_round.png"), optimize=True)

for folder, size in drawable_sizes.items():
    path = os.path.join(out_dir, folder)
    os.makedirs(path, exist_ok=True)
    resized = launcher.resize((size, size), Image.LANCZOS)
    resized.save(os.path.join(path, "ic_mx_launcher_foreground.png"), optimize=True)

nodpi = os.path.join(out_dir, "drawable-nodpi")
os.makedirs(nodpi, exist_ok=True)
app_logo.save(os.path.join(nodpi, "ic_logo.png"), optimize=True)
launcher.save(os.path.join(nodpi, "ic_mx_launcher_foreground.png"), optimize=True)
app_logo.save(os.path.join(nodpi, "artwork.png"), optimize=True)
print("Done")
