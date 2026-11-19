from pathlib import Path
import re

base = Path(r'A:\Android\AstronautStories\app\src\main\assets')

# Keep these at root intentionally
root_keep = {'index.html', 'level-select.html', 'video.html'}
root_move = {
    'common.css': 'styles/common.css',
    'common.js': 'scripts/common.js',
    'asset-loader.js': 'scripts/asset-loader.js',
    'video-playback-fix.js': 'scripts/video-playback-fix.js',
    'video-playback-fix.nomodule.js': 'scripts/video-playback-fix.nomodule.js',
}

# Categorize images using the actual filenames in the project

def image_category(name: str) -> str:
    lower = name.lower()
    if re.search(r'(background|bg|planet|earth|space|mercury|mars|saturn|jupiter|moon|rocket_interior|inside_the_ship|station|flag|agency|iran|logo)', lower):
        return 'backgrounds'
    if re.search(r'(ammo|battery|health|rocket|part|beaker|pack|powerful)', lower):
        return 'items'
    if re.search(r'(player|astronaut|alien|zombie|flyer|splitter|soldier|boss|hostage|ship|gun)', lower):
        return 'characters'
    if re.search(r'(logo|flag|agency|icon|ui|banner|emblem)', lower):
        return 'ui'
    return 'ui'


# Move files to the proper folders
for p in list(base.iterdir()):
    if not p.is_file():
        continue
    name = p.name.lower()
    if p.name in root_keep:
        continue
    if p.name in root_move:
        dest = base / root_move[p.name]
        dest.parent.mkdir(parents=True, exist_ok=True)
        if not dest.exists():
            p.rename(dest)
        continue
    if p.name.startswith('stage') and p.suffix.lower() == '.html':
        dest = base / 'stages' / p.name
        if not dest.exists():
            p.rename(dest)
        continue
    if p.suffix.lower() in {'.png', '.jpg', '.jpeg', '.gif', '.webp', '.svg', '.bmp'}:
        cat = image_category(p.name)
        dest = base / 'images' / cat / p.name
        if not dest.exists():
            p.rename(dest)
        continue
    if p.suffix.lower() in {'.mp3', '.wav', '.ogg'}:
        dest = base / 'audio' / p.name
        if not dest.exists():
            p.rename(dest)
        continue
    if p.suffix.lower() in {'.mp4', '.mov', '.webm'}:
        dest = base / 'videos' / p.name
        if not dest.exists():
            p.rename(dest)
        continue
    if p.suffix.lower() == '.css':
        dest = base / 'styles' / p.name
        if not dest.exists():
            p.rename(dest)
        continue
    if p.suffix.lower() == '.js':
        dest = base / 'scripts' / p.name
        if not dest.exists():
            p.rename(dest)
        continue

# Ensure critical root files are present and references updated
root_files = [base / 'index.html', base / 'video.html', base / 'level-select.html']
for f in root_files:
    if not f.exists():
        continue
    text = f.read_text(encoding='utf-8', errors='ignore')
    text = text.replace('file:///android_asset/logo.jpg', 'file:///android_asset/images/logo.jpg')
    text = text.replace('file:///android_asset/astronaut.png', 'file:///android_asset/images/characters/astronaut.png')
    text = text.replace('file:///android_asset/army_of_stars_logo.png', 'file:///android_asset/images/ui/army_of_stars_logo.png')
    text = text.replace('file:///android_asset/moon_guardians_logo.png', 'file:///android_asset/images/ui/moon_guardians_logo.png')
    text = text.replace('logo.mp4', 'videos/logo.mp4')
    text = text.replace('common.css', 'styles/common.css')
    text = text.replace('common.js', 'scripts/common.js')
    text = text.replace('styles/common.css', 'styles/common.css')
    f.write_text(text, encoding='utf-8')

# patch stage files in stages folder
for f in (base / 'stages').glob('*.html'):
    text = f.read_text(encoding='utf-8', errors='ignore')
    text = text.replace('href="common.css"', 'href="../styles/common.css"')
    text = text.replace('href="styles/theme.css"', 'href="../styles/theme.css"')
    text = text.replace('src="scripts/asset-loader.js"', 'src="../scripts/asset-loader.js"')
    text = text.replace('src="common.css"', 'src="../styles/common.css"')
    text = text.replace("import { attachVideoHandlers } from './video-playback-fix.js';", "import { attachVideoHandlers } from '../scripts/video-playback-fix.js';")
    text = text.replace('import { attachVideoHandlers } from "./video-playback-fix.js";', 'import { attachVideoHandlers } from "../scripts/video-playback-fix.js";')
    text = text.replace('window.location.href = \'level-select.html\'', 'window.location.href = \'../level-select.html\'')
    text = text.replace('window.location.href=\'level-select.html\'', 'window.location.href=\'../level-select.html\'')
    text = text.replace('window.location.href = "level-select.html"', 'window.location.href = "../level-select.html"')
    text = text.replace('window.location.href = \'index.html\'', 'window.location.href = \'../index.html\'')
    text = text.replace('window.location.href=\'index.html\'', 'window.location.href=\'../index.html\'')
    text = text.replace('window.location.href = "index.html"', 'window.location.href = "../index.html"')
    text = text.replace('file:///android_asset/', 'file:///android_asset/')
    # fix asset loader path references to the new folder layout for known files
    text = text.replace('file:///android_asset/mercury_background.png', 'file:///android_asset/images/backgrounds/mercury_background.png')
    text = text.replace('file:///android_asset/mars_background.png', 'file:///android_asset/images/backgrounds/mars_background.png')
    text = text.replace('file:///android_asset/saturn_background.png', 'file:///android_asset/images/backgrounds/saturn_background.png')
    text = text.replace('file:///android_asset/jupiter_background.png', 'file:///android_asset/images/backgrounds/jupiter_background.png')
    text = text.replace('file:///android_asset/rocket_interior.jpg', 'file:///android_asset/images/backgrounds/rocket_interior.jpg')
    text = text.replace('file:///android_asset/Inside_the_ship.jpg', 'file:///android_asset/images/backgrounds/Inside_the_ship.jpg')
    text = text.replace('file:///android_asset/player_idle_gun.png', 'file:///android_asset/images/characters/player_idle_gun.png')
    text = text.replace('file:///android_asset/player_walk_gun.png', 'file:///android_asset/images/characters/player_walk_gun.png')
    text = text.replace('file:///android_asset/player_jump_gun.png', 'file:///android_asset/images/characters/player_jump_gun.png')
    text = text.replace('file:///android_asset/player_idle_no_gun.png', 'file:///android_asset/images/characters/player_idle_no_gun.png')
    text = text.replace('file:///android_asset/zombie.png', 'file:///android_asset/images/characters/zombie.png')
    text = text.replace('file:///android_asset/flyer.png', 'file:///android_asset/images/characters/flyer.png')
    text = text.replace('file:///android_asset/splitter.png', 'file:///android_asset/images/characters/splitter.png')
    text = text.replace('file:///android_asset/ammo_pack.png', 'file:///android_asset/images/items/ammo_pack.png')
    text = text.replace('file:///android_asset/powerful_battery.png', 'file:///android_asset/images/items/powerful_battery.png')
    text = text.replace('file:///android_asset/rocket_part.png', 'file:///android_asset/images/items/rocket_part.png')
    text = text.replace('file:///android_asset/health_pack.png', 'file:///android_asset/images/items/health_pack.png')
    text = text.replace('file:///android_asset/stage9_betrayal.mp4', 'file:///android_asset/videos/stage9_betrayal.mp4')
    text = text.replace('file:///android_asset/audio/music_mercury.mp3', 'file:///android_asset/audio/music_mercury.mp3')
    text = text.replace('file:///android_asset/audio/shoot.wav', 'file:///android_asset/audio/shoot.wav')
    text = text.replace('file:///android_asset/audio/hit.wav', 'file:///android_asset/audio/hit.wav')
    text = text.replace('file:///android_asset/audio/jump.wav', 'file:///android_asset/audio/jump.wav')
    f.write_text(text, encoding='utf-8')

# patch root scripts to load relative asset folders for root pages
for f in (base / 'scripts').glob('*.js'):
    text = f.read_text(encoding='utf-8', errors='ignore')
    text = text.replace('file:///android_asset/', 'file:///android_asset/')
    f.write_text(text, encoding='utf-8')

# ensure stage file list is in stages folder and root no longer contains active stage files
for p in list(base.iterdir()):
    if p.is_file() and p.name.startswith('stage') and p.suffix.lower() == '.html' and p.parent == base:
        p.unlink()

print('final asset relocation complete')
