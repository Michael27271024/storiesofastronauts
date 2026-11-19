function normalizeAssetName(path) {
    if (!path) return path;

    let normalized = path
      .replace(/^file:\/\/\/android_asset\//i, '')
      .replace(/^android_asset\//i, '')
      .replace(/^assets\//i, '')
      .replace(/^file:\/\/.*?\/assets\//i, '')
      .replace(/^[\\/]+/, '');

    const fileName = normalized.split('/').pop();
    if (!fileName) return normalized;

    const lower = fileName.toLowerCase();

    // ابتدا بررسی ویدیوها (حتی اگر کلمه logo در اسمشان باشد)
    if (/\.(mp4|webm|mov)$/i.test(lower)) {
      return 'videos/' + fileName;
    }
    if (/\.(mp3|wav|ogg|m4a)$/i.test(lower)) {
      return 'audio/' + fileName;
    }
    if (/\.(css)$/i.test(lower)) {
      return 'styles/' + fileName;
    }
    if (/\.(js)$/i.test(lower)) {
      return 'scripts/' + fileName;
    }
    if (/\.(png|jpg|jpeg|gif|webp|svg|bmp)$/i.test(lower)) {
      let folder = 'images';
      if (/(background|bg|planet|earth|space|mercury|mars|saturn|jupiter|inside_the_ship|rocket_interior|station)/i.test(lower)) {
        folder = 'images/backgrounds';
      } else if (/(ammo|battery|health|rocket|part|beaker|pack|powerful)/i.test(lower)) {
        folder = 'images/items';
      } else if (/(player|astronaut|alien|zombie|flyer|splitter|soldier|boss|hostage|ship|gun)/i.test(lower)) {
        folder = 'images/characters';
      } else if (/(logo|flag|agency|icon|ui)/i.test(lower)) {
        folder = 'images/ui';
      }
      return folder + '/' + fileName;
    }

    return normalized;
  }