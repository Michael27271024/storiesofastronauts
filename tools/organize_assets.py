import os
import shutil
import glob

base = r'A:\Android\AstronautStories\app\src\main\assets'
folders = [
    'images',
    'images/characters',
    'images/backgrounds',
    'images/ui',
    'images/items',
    'audio',
    'videos',
    'styles',
    'scripts',
    'stages',
]

for folder in folders:
    os.makedirs(os.path.join(base, folder), exist_ok=True)

for name in os.listdir(base):
    src = os.path.join(base, name)
    if not os.path.isfile(src):
        continue
    lower = name.lower()
    if lower.endswith(('.png', '.jpg', '.jpeg', '.webp', '.gif', '.svg')):
        shutil.copy2(src, os.path.join(base, 'images', name))
    elif lower.endswith(('.mp3', '.wav', '.ogg')):
        shutil.copy2(src, os.path.join(base, 'audio', name))
    elif lower.endswith('.mp4'):
        shutil.copy2(src, os.path.join(base, 'videos', name))
    elif lower.endswith('.css'):
        shutil.copy2(src, os.path.join(base, 'styles', name))
    elif lower.endswith('.js') and name not in ('asset-loader.js',):
        shutil.copy2(src, os.path.join(base, 'scripts', name))

for path in glob.glob(os.path.join(base, 'stage*.html')):
    name = os.path.basename(path)
    dst = os.path.join(base, 'stages', name)
    if not os.path.exists(dst):
        shutil.copy2(path, dst)

loader_src = os.path.join(base, 'scripts', 'asset-loader.js')
if not os.path.exists(loader_src):
    shutil.copy2(os.path.join(base, 'asset-loader.js'), loader_src)

for path in glob.glob(os.path.join(base, 'stage*.html')):
    with open(path, 'r', encoding='utf-8') as f:
        html = f.read()
    if 'scripts/asset-loader.js' in html or 'asset-loader.js' in html:
        continue
    if '</head>' in html:
        html = html.replace('</head>', '    <script src="scripts/asset-loader.js"></script>\n</head>', 1)
    else:
        html += '\n<script src="scripts/asset-loader.js"></script>'
    with open(path, 'w', encoding='utf-8') as f:
        f.write(html)

print('asset organization complete')
