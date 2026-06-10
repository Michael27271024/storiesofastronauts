/**
 * asset-loader.js
 * Handles all asset path corrections for Android WebView
 * Fixes: images, videos, audio paths automatically
 */
(function(global) {
    const AssetLoader = {
        // Correct base paths for Android WebView
        basePath: './',
        audioPath: 'sounds/',
        imagePath: 'images/',
        videoPath: 'videos/',
        
        // Initialize and fix all paths
        init: function() {
            this.fixImagePaths();
            this.fixVideoPaths();
            this.fixAudioPaths();
            this.setupFallbacks();
            console.log('[AssetLoader] Initialized - all paths corrected');
        },
        
        // Fix image src attributes
        fixImagePaths: function() {
            document.querySelectorAll('img').forEach(img => {
                if (img.src) {
                    const oldSrc = img.src;
                    img.src = this.getAssetPath('images', img.src);
                    console.log(`[AssetLoader] Image: ${oldSrc.substring(oldSrc.lastIndexOf('/') + 1)} → ${img.src}`);
                    
                    if (!img.onerror) {
                        img.onerror = function() {
                            console.warn(`[AssetLoader] Failed to load: ${this.src}`);
                            this.src = 'data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 width=%22150%22 height=%22150%22%3E%3Crect fill=%22%23222%22 width=%22150%22 height=%22150%22/%3E%3Ctext x=%2250%25%22 y=%2250%25%22 dominant-baseline=%22middle%22 text-anchor=%22middle%22 font-size=%2250%22 fill=%22%23666%22%3E?%3C/text%3E%3C/svg%3E';
                        };
                    }
                }
            });
        },
        
        // Fix video sources
        fixVideoPaths: function() {
            document.querySelectorAll('video, source').forEach(el => {
                if (el.src) {
                    const oldSrc = el.src;
                    el.src = this.getAssetPath('videos', el.src);
                    console.log(`[AssetLoader] Video: ${oldSrc.substring(oldSrc.lastIndexOf('/') + 1)} → ${el.src}`);
                }
            });
        },
        
        // Fix audio paths
        fixAudioPaths: function() {
            document.querySelectorAll('audio, audio source').forEach(el => {
                if (el.src) {
                    const oldSrc = el.src;
                    el.src = this.getAssetPath('sounds', el.src);
                    console.log(`[AssetLoader] Audio: ${oldSrc.substring(oldSrc.lastIndexOf('/') + 1)} → ${el.src}`);
                }
            });
        },
        
        // Setup image fallbacks
        setupFallbacks: function() {
            document.querySelectorAll('img').forEach(img => {
                const original = img.src;
                let errorCount = 0;
                
                img.addEventListener('error', function() {
                    errorCount++;
                    if (errorCount > 3) return; // Prevent infinite loop
                    
                    console.warn(`[AssetLoader] Image error (attempt ${errorCount}): ${this.src}`);
                    
                    // Try different extensions
                    const paths = [
                        original,
                        original.replace(/\.png$/i, '.svg'),
                        original.replace(/\.jpg$/i, '.svg'),
                        original.replace(/\.svg$/i, '.png'),
                    ];
                    
                    for (const path of paths) {
                        if (path !== this.src && errorCount <= 2) {
                            this.src = path;
                            return;
                        }
                    }
                    
                    // Last resort: use placeholder
                    this.src = 'data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 width=%22150%22 height=%22150%22%3E%3Crect fill=%22%23333%22 width=%22150%22 height=%22150%22 rx=%225%22/%3E%3Ctext x=%2250%25%22 y=%2250%25%22 dominant-baseline=%22middle%22 text-anchor=%22middle%22 font-size=%2260%22 fill=%22%23999%22%3E👨‍🚀%3C/text%3E%3C/svg%3E';
                    console.error(`[AssetLoader] Using fallback for: ${original}`);
                });
            });
        },
        
        // Get corrected asset path
        getAssetPath: function(type, src) {
            if (!src) return '';
            
            // Remove old android_asset prefix if present
            src = src.replace('file:///android_asset/', '');
            src = src.replace(/^(sounds|audio|images|videos)\//, '');
            
            // Clean up the path
            src = src.trim();
            
            const typeMap = {
                'images': this.imagePath,
                'sounds': this.audioPath,
                'audio': this.audioPath,
                'videos': this.videoPath
            };
            
            const pathPrefix = typeMap[type] || '';
            const fullPath = this.basePath + pathPrefix + src;
            return fullPath;
        },
        
        // Get audio path for audio-manager
        getAudioPath: function(filename) {
            return this.getAssetPath('audio', filename);
        },
        
        // Get image path
        getImagePath: function(filename) {
            return this.getAssetPath('images', filename);
        },
        
        // Get video path
        getVideoPath: function(filename) {
            return this.getAssetPath('videos', filename);
        }
    };
    
    // Auto-init on load
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => AssetLoader.init());
    } else {
        AssetLoader.init();
    }
    
    // Expose globally
    global.AssetLoader = AssetLoader;
})(window);
