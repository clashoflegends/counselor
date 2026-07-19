# -*- coding: utf-8 -*-
# Compose the WiX MSI installer bitmaps from a Clash teaser.
#   dialog.bmp  493x312  (WixUIDialogBmp)  - Welcome/Complete pages; art in the left panel, white right for text
#   banner.bmp  493x58   (WixUIBannerBmp)  - interior page top banner; small art on the right
# Usage: python make_installer_bmp.py [source_image]
import sys
from PIL import Image

SRC = sys.argv[1] if len(sys.argv) > 1 else r"C:\Clash\portraits\Teasers\All3_image.png"
BG = (255, 255, 255)
ACCENT = (150, 20, 24)  # Clash red divider

def cover(img, w, h):
    iw, ih = img.size
    scale = max(w / iw, h / ih)
    img = img.resize((max(1, round(iw * scale)), max(1, round(ih * scale))), Image.LANCZOS)
    iw, ih = img.size
    left = (iw - w) // 2
    top = (ih - h) // 2
    return img.crop((left, top, left + w, top + h))

src = Image.open(SRC).convert("RGB")
W, H = src.size

# --- dialog.bmp: left art panel + white text area ---
DW, DH = 493, 312
PANEL = 175
dlg = Image.new("RGB", (DW, DH), BG)
# left third of the teaser (the White Walker) fills the panel
art = cover(src.crop((int(W * 0.02), 0, int(W * 0.34), H)), PANEL, DH)
dlg.paste(art, (0, 0))
# thin red divider
for x in range(PANEL, PANEL + 3):
    for y in range(DH):
        dlg.putpixel((x, y), ACCENT)
dlg.save(r"C:\Clash\PbmCounselor\jpackage\win-resources\dialog.bmp")

# --- banner.bmp: white with a small teaser crop on the right ---
BW, BH = 493, 58
ban = Image.new("RGB", (BW, BH), BG)
crop = cover(src.crop((int(W * 0.34), 0, int(W * 0.66), H)), BH, BH)  # the dragon (centre)
ban.paste(crop, (BW - BH, 0))
ban.save(r"C:\Clash\PbmCounselor\jpackage\win-resources\banner.bmp")
print("wrote dialog.bmp (493x312) + banner.bmp (493x58)")
