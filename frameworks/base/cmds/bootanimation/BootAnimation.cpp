/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_NDEBUG 0
#define LOG_TAG "BootAnimation"

#include <stdint.h>
#include <inttypes.h>
#include <sys/inotify.h>
#include <sys/poll.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <math.h>
#include <fcntl.h>
#include <utils/misc.h>
#include <signal.h>
#include <time.h>

#include <cutils/atomic.h>
#include <cutils/properties.h>

#include <androidfw/AssetManager.h>
#include <binder/IPCThreadState.h>
#include <utils/Errors.h>
#include <utils/Log.h>
#include <utils/SystemClock.h>

#include <android-base/properties.h>

#include <ui/PixelFormat.h>
#include <ui/Rect.h>
#include <ui/Region.h>
#include <ui/DisplayInfo.h>

#include <gui/ISurfaceComposer.h>
#include <gui/Surface.h>
#include <gui/SurfaceComposerClient.h>

// TODO: Fix Skia.
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wunused-parameter"
#include <SkBitmap.h>
#include <SkImage.h>
#include <SkStream.h>
#pragma GCC diagnostic pop

#include <GLES/gl.h>
#include <GLES/glext.h>
#include <EGL/eglext.h>

#include "BootAnimation.h"

namespace android {

static const char OEM_BOOTANIMATION_FILE[] = "/oem/media/bootanimation.zip";
static const char PRODUCT_BOOTANIMATION_FILE[] = "/product/media/bootanimation.zip";
static const char SYSTEM_BOOTANIMATION_FILE[] = "/system/media/bootanimation.zip";
static const char PRODUCT_ENCRYPTED_BOOTANIMATION_FILE[] = "/product/media/bootanimation-encrypted.zip";
static const char SYSTEM_ENCRYPTED_BOOTANIMATION_FILE[] = "/system/media/bootanimation-encrypted.zip";
static const char OEM_SHUTDOWNANIMATION_FILE[] = "/oem/media/shutdownanimation.zip";
static const char PRODUCT_SHUTDOWNANIMATION_FILE[] = "/product/media/shutdownanimation.zip";
static const char SYSTEM_SHUTDOWNANIMATION_FILE[] = "/system/media/shutdownanimation.zip";

#ifdef BOOTANIMATION_EXT
static const char PLAY_SOUND_PROP_NAME[] = "persist.sys.bootanim.play_sound";
static const char BOOTREASON_PROP_NAME[] = "ro.boot.bootreason";
static const std::vector<std::string> PLAY_SOUND_BOOTREASON_BLACKLIST {
  "kernel_panic",
  "Panic",
  "Watchdog",
};
/* SPRD: added boot and shutdown animation ,define  path here @{ */
static const char OEM_BOOTSOUND_FILE[] = "/oem/media/bootsound.mp3";
static const char PRODUCT_BOOTSOUND_FILE[] = "/product/media/bootsound.mp3";
static const char SYSTEM_BOOTSOUND_FILE[] = "/system/media/bootsound.mp3";
static const char OEM_SHUTDOWNSOUND_FILE[] = "/oem/media/shutdownsound.mp3";
static const char PRODUCT_SHUTDOWNSOUND_FILE[] = "/product/media/shutdownsound.mp3";
static const char SYSTEM_SHUTDOWNSOUND_FILE[] = "/system/media/shutdownsound.mp3";
#endif

static const char SYSTEM_DATA_DIR_PATH[] = "/data/system";
static const char SYSTEM_TIME_DIR_NAME[] = "time";
static const char SYSTEM_TIME_DIR_PATH[] = "/data/system/time";
static const char CLOCK_FONT_ASSET[] = "images/clock_font.png";
static const char CLOCK_FONT_ZIP_NAME[] = "clock_font.png";
static const char LAST_TIME_CHANGED_FILE_NAME[] = "last_time_change";
static const char LAST_TIME_CHANGED_FILE_PATH[] = "/data/system/time/last_time_change";
static const char ACCURATE_TIME_FLAG_FILE_NAME[] = "time_is_accurate";
static const char ACCURATE_TIME_FLAG_FILE_PATH[] = "/data/system/time/time_is_accurate";
static const char TIME_FORMAT_12_HOUR_FLAG_FILE_PATH[] = "/data/system/time/time_format_12_hour";
// Java timestamp format. Don't show the clock if the date is before 2000-01-01 00:00:00.
static const long long ACCURATE_TIME_EPOCH = 946684800000;
static constexpr char FONT_BEGIN_CHAR = ' ';
static constexpr char FONT_END_CHAR = '~' + 1;
static constexpr size_t FONT_NUM_CHARS = FONT_END_CHAR - FONT_BEGIN_CHAR + 1;
static constexpr size_t FONT_NUM_COLS = 16;
static constexpr size_t FONT_NUM_ROWS = FONT_NUM_CHARS / FONT_NUM_COLS;
static const int TEXT_CENTER_VALUE = INT_MAX;
static const int TEXT_MISSING_VALUE = INT_MIN;
static const char EXIT_PROP_NAME[] = "service.bootanim.exit";
static const int ANIM_ENTRY_NAME_MAX = 256;
static constexpr size_t TEXT_POS_LEN_MAX = 16;

// ---------------------------------------------------------------------------
#ifdef BOOTANIMATION_EXT
class MPlayerListener : public MediaPlayerListener
{
    void notify(int msg, int /*ext1*/, int /*ext2*/, const Parcel * /*obj*/)
    {
        switch (msg) {
        case MEDIA_PLAYBACK_COMPLETE:
            ALOGD("shutdown media finished, quit");
            property_set("service.bootanim.end", "1");
            break;
        default:
            break;
        }
    }
};
#endif

BootAnimation::BootAnimation(sp<Callbacks> callbacks) {
    BootAnimation(callbacks, false);
}

BootAnimation::BootAnimation(sp<Callbacks> callbacks, bool systemCalled)
        : Thread(false), mClockEnabled(true), mTimeIsAccurate(false),
        mTimeFormat12Hour(false), mTimeCheckThread(NULL), mCallbacks(callbacks) {
    mSession = new SurfaceComposerClient();

    std::string powerCtl = android::base::GetProperty("sys.powerctl", "");
    if (powerCtl.empty()) {
        mShuttingDown = false;
    } else {
        mShuttingDown = true;
    }
    ALOGD("System Called %d ", systemCalled);
#ifdef BOOTANIMATION_EXT
    mSystemCalled = systemCalled;
    mfd = -1;
    if (!mShuttingDown) {
        mShuttingDown = android::base::GetProperty("service.bootanim.shutdown", "").compare("1") == 0;
    }
    if (mShuttingDown) {
        mWaitForComplete = android::base::GetBoolProperty("service.wait_for_bootanim", false);
    }
#endif
}

#ifdef BOOTANIMATION_EXT
BootAnimation::~BootAnimation() {
    // SPRD: add shutdown animation
    if(mfd != -1){ close(mfd); }
}
#endif

void BootAnimation::onFirstRef() {
    status_t err = mSession->linkToComposerDeath(this);
    ALOGE_IF(err, "linkToComposerDeath failed (%s) ", strerror(-err));
    if (err == NO_ERROR) {
        run("BootAnimation", PRIORITY_DISPLAY);
    }
}

sp<SurfaceComposerClient> BootAnimation::session() const {
    return mSession;
}


void BootAnimation::binderDied(const wp<IBinder>&)
{
    // woah, surfaceflinger died!
    ALOGD("SurfaceFlinger died, exiting...");

    // calling requestExit() is not enough here because the Surface code
    // might be blocked on a condition variable that will never be updated.
    kill( getpid(), SIGKILL );
    requestExit();
}

status_t BootAnimation::initTexture(Texture* texture, AssetManager& assets,
        const char* name) {
    Asset* asset = assets.open(name, Asset::ACCESS_BUFFER);
    if (asset == NULL)
        return NO_INIT;
    SkBitmap bitmap;
    sk_sp<SkData> data = SkData::MakeWithoutCopy(asset->getBuffer(false),
            asset->getLength());
    sk_sp<SkImage> image = SkImage::MakeFromEncoded(data);
    image->asLegacyBitmap(&bitmap, SkImage::kRO_LegacyBitmapMode);
    asset->close();
    delete asset;

    const int w = bitmap.width();
    const int h = bitmap.height();
    const void* p = bitmap.getPixels();

    GLint crop[4] = { 0, h, w, -h };
    texture->w = w;
    texture->h = h;

    glGenTextures(1, &texture->name);
    glBindTexture(GL_TEXTURE_2D, texture->name);

    switch (bitmap.colorType()) {
        case kAlpha_8_SkColorType:
            glTexImage2D(GL_TEXTURE_2D, 0, GL_ALPHA, w, h, 0, GL_ALPHA,
                    GL_UNSIGNED_BYTE, p);
            break;
        case kARGB_4444_SkColorType:
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA,
                    GL_UNSIGNED_SHORT_4_4_4_4, p);
            break;
        case kN32_SkColorType:
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA,
                    GL_UNSIGNED_BYTE, p);
            break;
        case kRGB_565_SkColorType:
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, w, h, 0, GL_RGB,
                    GL_UNSIGNED_SHORT_5_6_5, p);
            break;
        default:
            break;
    }

    glTexParameteriv(GL_TEXTURE_2D, GL_TEXTURE_CROP_RECT_OES, crop);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

    return NO_ERROR;
}

status_t BootAnimation::initTexture(FileMap* map, int* width, int* height)
{
    SkBitmap bitmap;
    sk_sp<SkData> data = SkData::MakeWithoutCopy(map->getDataPtr(),
            map->getDataLength());
    sk_sp<SkImage> image = SkImage::MakeFromEncoded(data);
    image->asLegacyBitmap(&bitmap, SkImage::kRO_LegacyBitmapMode);

    // FileMap memory is never released until application exit.
    // Release it now as the texture is already loaded and the memory used for
    // the packed resource can be released.
    delete map;

    const int w = bitmap.width();
    const int h = bitmap.height();
    const void* p = bitmap.getPixels();

    GLint crop[4] = { 0, h, w, -h };
    int tw = 1 << (31 - __builtin_clz(w));
    int th = 1 << (31 - __builtin_clz(h));
    if (tw < w) tw <<= 1;
    if (th < h) th <<= 1;

    switch (bitmap.colorType()) {
        case kN32_SkColorType:
            if (!mUseNpotTextures && (tw != w || th != h)) {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, tw, th, 0, GL_RGBA,
                        GL_UNSIGNED_BYTE, 0);
                glTexSubImage2D(GL_TEXTURE_2D, 0,
                        0, 0, w, h, GL_RGBA, GL_UNSIGNED_BYTE, p);
            } else {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA,
                        GL_UNSIGNED_BYTE, p);
            }
            break;

        case kRGB_565_SkColorType:
            if (!mUseNpotTextures && (tw != w || th != h)) {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, tw, th, 0, GL_RGB,
                        GL_UNSIGNED_SHORT_5_6_5, 0);
                glTexSubImage2D(GL_TEXTURE_2D, 0,
                        0, 0, w, h, GL_RGB, GL_UNSIGNED_SHORT_5_6_5, p);
            } else {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, w, h, 0, GL_RGB,
                        GL_UNSIGNED_SHORT_5_6_5, p);
            }
            break;
        default:
            break;
    }

    glTexParameteriv(GL_TEXTURE_2D, GL_TEXTURE_CROP_RECT_OES, crop);

    *width = w;
    *height = h;

    return NO_ERROR;
}

status_t BootAnimation::readyToRun() {
    mAssets.addDefaultAssets();

    sp<IBinder> dtoken(SurfaceComposerClient::getBuiltInDisplay(
            ISurfaceComposer::eDisplayIdMain));
    DisplayInfo dinfo;
    status_t status = SurfaceComposerClient::getDisplayInfo(dtoken, &dinfo);
    if (status)
        return -1;

    // create the native surface
    sp<SurfaceControl> control = session()->createSurface(String8("BootAnimation"),
            dinfo.w, dinfo.h, PIXEL_FORMAT_RGB_565);

    SurfaceComposerClient::Transaction t;
    t.setLayer(control, 0x40000000)
        .apply();

    sp<Surface> s = control->getSurface();

    // initialize opengl and egl
    const EGLint attribs[] = {
            EGL_RED_SIZE,   8,
            EGL_GREEN_SIZE, 8,
            EGL_BLUE_SIZE,  8,
            EGL_DEPTH_SIZE, 0,
            EGL_NONE
    };
    EGLint w, h;
    EGLint numConfigs;
    EGLConfig config;
    EGLSurface surface;
    EGLContext context;

    EGLDisplay display = eglGetDisplay(EGL_DEFAULT_DISPLAY);

    eglInitialize(display, 0, 0);
    eglChooseConfig(display, attribs, &config, 1, &numConfigs);
    surface = eglCreateWindowSurface(display, config, s.get(), NULL);
    context = eglCreateContext(display, config, NULL, NULL);
    eglQuerySurface(display, surface, EGL_WIDTH, &w);
    eglQuerySurface(display, surface, EGL_HEIGHT, &h);

    if (eglMakeCurrent(display, surface, surface, context) == EGL_FALSE)
        return NO_INIT;

    mDisplay = display;
    mContext = context;
    mSurface = surface;
    mWidth = w;
    mHeight = h;
    mFlingerSurfaceControl = control;
    mFlingerSurface = s;

    // If the device has encryption turned on or is in process
    // of being encrypted we show the encrypted boot animation.
    char decrypt[PROPERTY_VALUE_MAX];
    property_get("vold.decrypt", decrypt, "");

    bool encryptedAnimation = atoi(decrypt) != 0 ||
        !strcmp("trigger_restart_min_framework", decrypt);

    if (!mShuttingDown && encryptedAnimation) {
        static const char* encryptedBootFiles[] =
            {PRODUCT_ENCRYPTED_BOOTANIMATION_FILE, SYSTEM_ENCRYPTED_BOOTANIMATION_FILE};
        for (const char* f : encryptedBootFiles) {
            if (access(f, R_OK) == 0) {
                mZipFileName = f;
                return NO_ERROR;
            }
        }
    }
    static const char* bootFiles[] =
        {PRODUCT_BOOTANIMATION_FILE, OEM_BOOTANIMATION_FILE, SYSTEM_BOOTANIMATION_FILE};
    static const char* shutdownFiles[] =
        {PRODUCT_SHUTDOWNANIMATION_FILE, OEM_SHUTDOWNANIMATION_FILE, SYSTEM_SHUTDOWNANIMATION_FILE};

    for (const char* f : (!mShuttingDown ? bootFiles : shutdownFiles)) {
        if (access(f, R_OK) == 0) {
            mZipFileName = f;
#ifdef BOOTANIMATION_EXT
            break;
#else
            return NO_ERROR;
#endif
        }
    }

#ifdef BOOTANIMATION_EXT
    static const char* bootSoundFiles[] =
        {PRODUCT_BOOTSOUND_FILE, OEM_BOOTSOUND_FILE, SYSTEM_BOOTSOUND_FILE};
    static const char* shutdownSoundFiles[] =
        {PRODUCT_SHUTDOWNSOUND_FILE, OEM_SHUTDOWNSOUND_FILE, SYSTEM_SHUTDOWNSOUND_FILE};
    for (const char* f : (!mShuttingDown ? bootSoundFiles : shutdownSoundFiles)) {
        if (access(f, R_OK) == 0) {
            mSoundFileName = f;
            return NO_ERROR;
        }
    }
    mSoundFileName = "";
#endif

    return NO_ERROR;
}

bool BootAnimation::threadLoop()
{
    bool r;
    // We have no bootanimation file, so we use the stock android logo
    // animation.
    if (mZipFileName.isEmpty()) {
        r = android();
    } else {
        r = movie();
    }

    eglMakeCurrent(mDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
    eglDestroyContext(mDisplay, mContext);
    eglDestroySurface(mDisplay, mSurface);
    mFlingerSurface.clear();
    mFlingerSurfaceControl.clear();
    eglTerminate(mDisplay);
    eglReleaseThread();
    IPCThreadState::self()->stopProcess();
    return r;
}

bool BootAnimation::android()
{
    ALOGD("%sAnimationShownTiming start time: %" PRId64 "ms", mShuttingDown ? "Shutdown" : "Boot",
            elapsedRealtime());
#ifdef BOOTANIMATION_EXT
    // SPRD: use small logo pictures on small screen devices @{
    if (mWidth <= 160) {
        initTexture(&mAndroid[0], mAssets, "images/android-logo-mask-minimum.png");
        initTexture(&mAndroid[1], mAssets, "images/android-logo-shine-minimum.png");
    } else if (mWidth <= 240) {
        initTexture(&mAndroid[0], mAssets, "images/android-logo-mask-little.png");
        initTexture(&mAndroid[1], mAssets, "images/android-logo-shine-little.png");
    } else if (mWidth <= 480) {
        initTexture(&mAndroid[0], mAssets, "images/android-logo-mask-small.png");
        initTexture(&mAndroid[1], mAssets, "images/android-logo-shine-small.png");
    } else {
        initTexture(&mAndroid[0], mAssets, "images/android-logo-mask.png");
        initTexture(&mAndroid[1], mAssets, "images/android-logo-shine.png");
    }
    // @}
#else
    initTexture(&mAndroid[0], mAssets, "images/android-logo-mask.png");
    initTexture(&mAndroid[1], mAssets, "images/android-logo-shine.png");
#endif

    mCallbacks->init({});

    // clear screen
    glShadeModel(GL_FLAT);
    glDisable(GL_DITHER);
    glDisable(GL_SCISSOR_TEST);
    glClearColor(0,0,0,1);
    glClear(GL_COLOR_BUFFER_BIT);
    eglSwapBuffers(mDisplay, mSurface);

    glEnable(GL_TEXTURE_2D);
    glTexEnvx(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);

    const GLint xc = (mWidth  - mAndroid[0].w) / 2;
    const GLint yc = (mHeight - mAndroid[0].h) / 2;
    const Rect updateRect(xc, yc, xc + mAndroid[0].w, yc + mAndroid[0].h);

    glScissor(updateRect.left, mHeight - updateRect.bottom, updateRect.width(),
            updateRect.height());

    // Blend state
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glTexEnvx(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);

    const nsecs_t startTime = systemTime();
    do {
        nsecs_t now = systemTime();
        double time = now - startTime;
        float t = 4.0f * float(time / us2ns(16667)) / mAndroid[1].w;
        GLint offset = (1 - (t - floorf(t))) * mAndroid[1].w;
        GLint x = xc - offset;

        glDisable(GL_SCISSOR_TEST);
        glClear(GL_COLOR_BUFFER_BIT);

        glEnable(GL_SCISSOR_TEST);
        glDisable(GL_BLEND);
        glBindTexture(GL_TEXTURE_2D, mAndroid[1].name);
        glDrawTexiOES(x,                 yc, 0, mAndroid[1].w, mAndroid[1].h);
        glDrawTexiOES(x + mAndroid[1].w, yc, 0, mAndroid[1].w, mAndroid[1].h);

        glEnable(GL_BLEND);
        glBindTexture(GL_TEXTURE_2D, mAndroid[0].name);
        glDrawTexiOES(xc, yc, 0, mAndroid[0].w, mAndroid[0].h);

        EGLBoolean res = eglSwapBuffers(mDisplay, mSurface);
        if (res == EGL_FALSE)
            break;

        // 12fps: don't animate too fast to preserve CPU
        const nsecs_t sleepTime = 83333 - ns2us(systemTime() - now);
        if (sleepTime > 0)
            usleep(sleepTime);

        checkExit();
    } while (!exitPending());

    glDeleteTextures(1, &mAndroid[0].name);
    glDeleteTextures(1, &mAndroid[1].name);
    return false;
}

void BootAnimation::checkExit() {
    // Allow surface flinger to gracefully request shutdown
    char value[PROPERTY_VALUE_MAX];
    property_get(EXIT_PROP_NAME, value, "0");
    int exitnow = atoi(value);
    if (exitnow) {
        requestExit();
        mCallbacks->shutdown();
    }
}

bool BootAnimation::validClock(const Animation::Part& part) {
    return part.clockPosX != TEXT_MISSING_VALUE && part.clockPosY != TEXT_MISSING_VALUE;
}

bool parseTextCoord(const char* str, int* dest) {
    if (strcmp("c", str) == 0) {
        *dest = TEXT_CENTER_VALUE;
        return true;
    }

    char* end;
    int val = (int) strtol(str, &end, 0);
    if (end == str || *end != '\0' || val == INT_MAX || val == INT_MIN) {
        return false;
    }
    *dest = val;
    return true;
}

// Parse two position coordinates. If only string is non-empty, treat it as the y value.
void parsePosition(const char* str1, const char* str2, int* x, int* y) {
    bool success = false;
    if (strlen(str1) == 0) {  // No values were specified
        // success = false
    } else if (strlen(str2) == 0) {  // we have only one value
        if (parseTextCoord(str1, y)) {
            *x = TEXT_CENTER_VALUE;
            success = true;
        }
    } else {
        if (parseTextCoord(str1, x) && parseTextCoord(str2, y)) {
            success = true;
        }
    }

    if (!success) {
        *x = TEXT_MISSING_VALUE;
        *y = TEXT_MISSING_VALUE;
    }
}

// Parse a color represented as an HTML-style 'RRGGBB' string: each pair of
// characters in str is a hex number in [0, 255], which are converted to
// floating point values in the range [0.0, 1.0] and placed in the
// corresponding elements of color.
//
// If the input string isn't valid, parseColor returns false and color is
// left unchanged.
static bool parseColor(const char str[7], float color[3]) {
    float tmpColor[3];
    for (int i = 0; i < 3; i++) {
        int val = 0;
        for (int j = 0; j < 2; j++) {
            val *= 16;
            char c = str[2*i + j];
            if      (c >= '0' && c <= '9') val += c - '0';
            else if (c >= 'A' && c <= 'F') val += (c - 'A') + 10;
            else if (c >= 'a' && c <= 'f') val += (c - 'a') + 10;
            else                           return false;
        }
        tmpColor[i] = static_cast<float>(val) / 255.0f;
    }
    memcpy(color, tmpColor, sizeof(tmpColor));
    return true;
}


static bool readFile(ZipFileRO* zip, const char* name, String8& outString)
{
    ZipEntryRO entry = zip->findEntryByName(name);
    ALOGE_IF(!entry, "couldn't find %s", name);
    if (!entry) {
        return false;
    }

    FileMap* entryMap = zip->createEntryFileMap(entry);
    zip->releaseEntry(entry);
    ALOGE_IF(!entryMap, "entryMap is null");
    if (!entryMap) {
        return false;
    }

    outString.setTo((char const*)entryMap->getDataPtr(), entryMap->getDataLength());
    delete entryMap;
    return true;
}

// The font image should be a 96x2 array of character images.  The
// columns are the printable ASCII characters 0x20 - 0x7f.  The
// top row is regular text; the bottom row is bold.
status_t BootAnimation::initFont(Font* font, const char* fallback) {
    status_t status = NO_ERROR;

    if (font->map != nullptr) {
        glGenTextures(1, &font->texture.name);
        glBindTexture(GL_TEXTURE_2D, font->texture.name);

        status = initTexture(font->map, &font->texture.w, &font->texture.h);

        glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
    } else if (fallback != nullptr) {
        status = initTexture(&font->texture, mAssets, fallback);
    } else {
        return NO_INIT;
    }

    if (status == NO_ERROR) {
        font->char_width = font->texture.w / FONT_NUM_COLS;
        font->char_height = font->texture.h / FONT_NUM_ROWS / 2;  // There are bold and regular rows
    }

    return status;
}

void BootAnimation::drawText(const char* str, const Font& font, bool bold, int* x, int* y) {
    glEnable(GL_BLEND);  // Allow us to draw on top of the animation
    glBindTexture(GL_TEXTURE_2D, font.texture.name);

    const int len = strlen(str);
    const int strWidth = font.char_width * len;

    if (*x == TEXT_CENTER_VALUE) {
        *x = (mWidth - strWidth) / 2;
    } else if (*x < 0) {
        *x = mWidth + *x - strWidth;
    }
    if (*y == TEXT_CENTER_VALUE) {
        *y = (mHeight - font.char_height) / 2;
    } else if (*y < 0) {
        *y = mHeight + *y - font.char_height;
    }

    int cropRect[4] = { 0, 0, font.char_width, -font.char_height };

    for (int i = 0; i < len; i++) {
        char c = str[i];

        if (c < FONT_BEGIN_CHAR || c > FONT_END_CHAR) {
            c = '?';
        }

        // Crop the texture to only the pixels in the current glyph
        const int charPos = (c - FONT_BEGIN_CHAR);  // Position in the list of valid characters
        const int row = charPos / FONT_NUM_COLS;
        const int col = charPos % FONT_NUM_COLS;
        cropRect[0] = col * font.char_width;  // Left of column
        cropRect[1] = row * font.char_height * 2; // Top of row
        // Move down to bottom of regular (one char_heigh) or bold (two char_heigh) line
        cropRect[1] += bold ? 2 * font.char_height : font.char_height;
        glTexParameteriv(GL_TEXTURE_2D, GL_TEXTURE_CROP_RECT_OES, cropRect);

        glDrawTexiOES(*x, *y, 0, font.char_width, font.char_height);

        *x += font.char_width;
    }

    glDisable(GL_BLEND);  // Return to the animation's default behaviour
    glBindTexture(GL_TEXTURE_2D, 0);
}

// We render 12 or 24 hour time.
void BootAnimation::drawClock(const Font& font, const int xPos, const int yPos) {
    static constexpr char TIME_FORMAT_12[] = "%l:%M";
    static constexpr char TIME_FORMAT_24[] = "%H:%M";
    static constexpr int TIME_LENGTH = 6;

    time_t rawtime;
    time(&rawtime);
    struct tm* timeInfo = localtime(&rawtime);

    char timeBuff[TIME_LENGTH];
    const char* timeFormat = mTimeFormat12Hour ? TIME_FORMAT_12 : TIME_FORMAT_24;
    size_t length = strftime(timeBuff, TIME_LENGTH, timeFormat, timeInfo);

    if (length != TIME_LENGTH - 1) {
        ALOGE("Couldn't format time; abandoning boot animation clock");
        mClockEnabled = false;
        return;
    }

    char* out = timeBuff[0] == ' ' ? &timeBuff[1] : &timeBuff[0];
    int x = xPos;
    int y = yPos;
    drawText(out, font, false, &x, &y);
}

bool BootAnimation::parseAnimationDesc(Animation& animation)
{
    String8 desString;

    if (!readFile(animation.zip, "desc.txt", desString)) {
        return false;
    }
    char const* s = desString.string();

#ifdef BOOTANIMATION_EXT
    ALOGE("before allowed, soundplay");
    if (playSoundsAllowed()) {
        ALOGE("soundplay called");
        soundplay();
    }
#endif

    // Parse the description file
    for (;;) {
        const char* endl = strstr(s, "\n");
        if (endl == NULL) break;
        String8 line(s, endl - s);
        const char* l = line.string();
        int fps = 0;
        int width = 0;
        int height = 0;
        int count = 0;
        int pause = 0;
        char path[ANIM_ENTRY_NAME_MAX];
        char color[7] = "000000"; // default to black if unspecified
        char clockPos1[TEXT_POS_LEN_MAX + 1] = "";
        char clockPos2[TEXT_POS_LEN_MAX + 1] = "";

        char pathType;
        if (sscanf(l, "%d %d %d", &width, &height, &fps) == 3) {
            // ALOGD("> w=%d, h=%d, fps=%d", width, height, fps);
            if (property_get_bool("ro.sprd.superresolution", 0)) {
                animation.width = mWidth;
                animation.height = mHeight;
            } else {
                animation.width = width;
                animation.height = height;
            }
            animation.fps = fps;
        } else if (sscanf(l, " %c %d %d %s #%6s %16s %16s",
                          &pathType, &count, &pause, path, color, clockPos1, clockPos2) >= 4) {
            //ALOGD("> type=%c, count=%d, pause=%d, path=%s, color=%s, clockPos1=%s, clockPos2=%s",
            //    pathType, count, pause, path, color, clockPos1, clockPos2);
            Animation::Part part;
            part.playUntilComplete = pathType == 'c';
            part.count = count;
            part.pause = pause;
            part.path = path;
            part.audioData = NULL;
            part.animation = NULL;
            if (!parseColor(color, part.backgroundColor)) {
                ALOGE("> invalid color '#%s'", color);
                part.backgroundColor[0] = 0.0f;
                part.backgroundColor[1] = 0.0f;
                part.backgroundColor[2] = 0.0f;
            }
            parsePosition(clockPos1, clockPos2, &part.clockPosX, &part.clockPosY);
            animation.parts.add(part);
        }
        else if (strcmp(l, "$SYSTEM") == 0) {
            // ALOGD("> SYSTEM");
            Animation::Part part;
            part.playUntilComplete = false;
            part.count = 1;
            part.pause = 0;
            part.audioData = NULL;
            part.animation = loadAnimation(String8(SYSTEM_BOOTANIMATION_FILE));
            if (part.animation != NULL)
                animation.parts.add(part);
        }
        s = ++endl;
    }

    return true;
}

bool BootAnimation::preloadZip(Animation& animation)
{
    // read all the data structures
    const size_t pcount = animation.parts.size();
    void *cookie = NULL;
    ZipFileRO* zip = animation.zip;
    if (!zip->startIteration(&cookie)) {
        return false;
    }

    ZipEntryRO entry;
    char name[ANIM_ENTRY_NAME_MAX];
    while ((entry = zip->nextEntry(cookie)) != NULL) {
        const int foundEntryName = zip->getEntryFileName(entry, name, ANIM_ENTRY_NAME_MAX);
        if (foundEntryName > ANIM_ENTRY_NAME_MAX || foundEntryName == -1) {
            ALOGE("Error fetching entry file name");
            continue;
        }

        const String8 entryName(name);
        const String8 path(entryName.getPathDir());
        const String8 leaf(entryName.getPathLeaf());
        if (leaf.size() > 0) {
            if (entryName == CLOCK_FONT_ZIP_NAME) {
                FileMap* map = zip->createEntryFileMap(entry);
                if (map) {
                    animation.clockFont.map = map;
                }
                continue;
            }

            for (size_t j = 0; j < pcount; j++) {
                if (path == animation.parts[j].path) {
                    uint16_t method;
                    // supports only stored png files
                    if (zip->getEntryInfo(entry, &method, NULL, NULL, NULL, NULL, NULL)) {
                        if (method == ZipFileRO::kCompressStored) {
                            FileMap* map = zip->createEntryFileMap(entry);
                            if (map) {
                                Animation::Part& part(animation.parts.editItemAt(j));
                                if (leaf == "audio.wav") {
                                    // a part may have at most one audio file
                                    part.audioData = (uint8_t *)map->getDataPtr();
                                    part.audioLength = map->getDataLength();
                                } else if (leaf == "trim.txt") {
                                    part.trimData.setTo((char const*)map->getDataPtr(),
                                                        map->getDataLength());
                                } else {
                                    Animation::Frame frame;
                                    frame.name = leaf;
                                    frame.map = map;
                                    frame.trimWidth = animation.width;
                                    frame.trimHeight = animation.height;
                                    frame.trimX = 0;
                                    frame.trimY = 0;
                                    part.frames.add(frame);
                                }
                            }
                        } else {
                            ALOGE("bootanimation.zip is compressed; must be only stored");
                        }
                    }
                }
            }
        }
    }

    // If there is trimData present, override the positioning defaults.
    for (Animation::Part& part : animation.parts) {
        const char* trimDataStr = part.trimData.string();
        for (size_t frameIdx = 0; frameIdx < part.frames.size(); frameIdx++) {
            const char* endl = strstr(trimDataStr, "\n");
            // No more trimData for this part.
            if (endl == NULL) {
                break;
            }
            String8 line(trimDataStr, endl - trimDataStr);
            const char* lineStr = line.string();
            trimDataStr = ++endl;
            int width = 0, height = 0, x = 0, y = 0;
            if (sscanf(lineStr, "%dx%d+%d+%d", &width, &height, &x, &y) == 4) {
                Animation::Frame& frame(part.frames.editItemAt(frameIdx));
                frame.trimWidth = width;
                frame.trimHeight = height;
                frame.trimX = x;
                frame.trimY = y;
            } else {
                ALOGE("Error parsing trim.txt, line: %s", lineStr);
                break;
            }
        }
    }

    mCallbacks->init(animation.parts);

    zip->endIteration(cookie);

    return true;
}

bool BootAnimation::movie()
{
    Animation* animation = loadAnimation(mZipFileName);
    if (animation == NULL)
        return false;

    bool anyPartHasClock = false;
    for (size_t i=0; i < animation->parts.size(); i++) {
        if(validClock(animation->parts[i])) {
            anyPartHasClock = true;
            break;
        }
    }
    if (!anyPartHasClock) {
        mClockEnabled = false;
    }

    // Check if npot textures are supported
    mUseNpotTextures = false;
    String8 gl_extensions;
    const char* exts = reinterpret_cast<const char*>(glGetString(GL_EXTENSIONS));
    if (!exts) {
        glGetError();
    } else {
        gl_extensions.setTo(exts);
        if ((gl_extensions.find("GL_ARB_texture_non_power_of_two") != -1) ||
            (gl_extensions.find("GL_OES_texture_npot") != -1)) {
            mUseNpotTextures = true;
        }
    }

    // Blend required to draw time on top of animation frames.
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glShadeModel(GL_FLAT);
    glDisable(GL_DITHER);
    glDisable(GL_SCISSOR_TEST);
    glDisable(GL_BLEND);

    glBindTexture(GL_TEXTURE_2D, 0);
    glEnable(GL_TEXTURE_2D);
    glTexEnvx(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

    bool clockFontInitialized = false;
    if (mClockEnabled) {
        clockFontInitialized =
            (initFont(&animation->clockFont, CLOCK_FONT_ASSET) == NO_ERROR);
        mClockEnabled = clockFontInitialized;
    }

    if (mClockEnabled && !updateIsTimeAccurate()) {
        mTimeCheckThread = new TimeCheckThread(this);
        mTimeCheckThread->run("BootAnimation::TimeCheckThread", PRIORITY_NORMAL);
    }

    playAnimation(*animation);

    if (mTimeCheckThread != nullptr) {
        mTimeCheckThread->requestExit();
        mTimeCheckThread = nullptr;
    }

    releaseAnimation(animation);

    if (clockFontInitialized) {
        glDeleteTextures(1, &animation->clockFont.texture.name);
    }

    return false;
}

bool BootAnimation::playAnimation(const Animation& animation)
{
    const size_t pcount = animation.parts.size();
    nsecs_t frameDuration = s2ns(1) / animation.fps;
    const int animationX = (mWidth - animation.width) / 2;
    const int animationY = (mHeight - animation.height) / 2;

    ALOGD("%sAnimationShownTiming start time: %" PRId64 "ms", mShuttingDown ? "Shutdown" : "Boot",
            elapsedRealtime());
#ifdef BOOTANIMATION_EXT
    scaleSurfaceIfNeeded();
#endif
    for (size_t i=0 ; i<pcount ; i++) {
        const Animation::Part& part(animation.parts[i]);
        const size_t fcount = part.frames.size();
        glBindTexture(GL_TEXTURE_2D, 0);

        // Handle animation package
        if (part.animation != NULL) {
            playAnimation(*part.animation);
            if (exitPending())
                break;
            continue; //to next part
        }

        for (int r=0 ; !part.count || r<part.count ; r++) {
            // Exit any non playuntil complete parts immediately
            if(exitPending() && !part.playUntilComplete)
                break;

            mCallbacks->playPart(i, part, r);

            glClearColor(
                    part.backgroundColor[0],
                    part.backgroundColor[1],
                    part.backgroundColor[2],
                    1.0f);

            for (size_t j=0 ; j<fcount && (!exitPending() || part.playUntilComplete) ; j++) {
                const Animation::Frame& frame(part.frames[j]);
                nsecs_t lastFrame = systemTime();

                if (r > 0) {
                    glBindTexture(GL_TEXTURE_2D, frame.tid);
                } else {
                    if (part.count != 1) {
                        glGenTextures(1, &frame.tid);
                        glBindTexture(GL_TEXTURE_2D, frame.tid);
                        glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                        glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                    }
                    int w, h;
                    initTexture(frame.map, &w, &h);
                }

                const int xc = animationX + frame.trimX;
                const int yc = animationY + frame.trimY;
                Region clearReg(Rect(mWidth, mHeight));
                clearReg.subtractSelf(Rect(xc, yc, xc+frame.trimWidth, yc+frame.trimHeight));
                if (!clearReg.isEmpty()) {
                    Region::const_iterator head(clearReg.begin());
                    Region::const_iterator tail(clearReg.end());
                    glEnable(GL_SCISSOR_TEST);
                    while (head != tail) {
                        const Rect& r2(*head++);
                        glScissor(r2.left, mHeight - r2.bottom, r2.width(), r2.height());
                        glClear(GL_COLOR_BUFFER_BIT);
                    }
                    glDisable(GL_SCISSOR_TEST);
                }
                // specify the y center as ceiling((mHeight - frame.trimHeight) / 2)
                // which is equivalent to mHeight - (yc + frame.trimHeight)
                glDrawTexiOES(xc, mHeight - (yc + frame.trimHeight),
                              0, frame.trimWidth, frame.trimHeight);
                if (mClockEnabled && mTimeIsAccurate && validClock(part)) {
                    drawClock(animation.clockFont, part.clockPosX, part.clockPosY);
                }

                eglSwapBuffers(mDisplay, mSurface);

                nsecs_t now = systemTime();
                nsecs_t delay = frameDuration - (now - lastFrame);
                //ALOGD("%lld, %lld", ns2ms(now - lastFrame), ns2ms(delay));
                lastFrame = now;

                if (delay > 0) {
                    struct timespec spec;
                    spec.tv_sec  = (now + delay) / 1000000000;
                    spec.tv_nsec = (now + delay) % 1000000000;
                    int err;
                    do {
                        err = clock_nanosleep(CLOCK_MONOTONIC, TIMER_ABSTIME, &spec, NULL);
                    } while (err<0 && errno == EINTR);
                }

                checkExit();
            }

            usleep(part.pause * ns2us(frameDuration));

            // For infinite parts, we've now played them at least once, so perhaps exit
            if(exitPending() && !part.count)
                break;
        }
#ifdef BOOTANIMATION_EXT
        if (mShuttingDown && mfd == -1 && mWaitForComplete) {
            ALOGD("shutdown animation part1 finished, quit");
            property_set("service.bootanim.end", "1");
        }
#endif
    }

    // Free textures created for looping parts now that the animation is done.
    for (const Animation::Part& part : animation.parts) {
        if (part.count != 1) {
            const size_t fcount = part.frames.size();
            for (size_t j = 0; j < fcount; j++) {
                const Animation::Frame& frame(part.frames[j]);
                glDeleteTextures(1, &frame.tid);
            }
        }
    }
#ifdef BOOTANIMATION_EXT
    // SPRD: stop soundplay
    if (playSoundsAllowed()) {
        soundstop();
    }
#endif
    return true;
}

void BootAnimation::releaseAnimation(Animation* animation) const
{
    for (Vector<Animation::Part>::iterator it = animation->parts.begin(),
         e = animation->parts.end(); it != e; ++it) {
        if (it->animation)
            releaseAnimation(it->animation);
    }
    if (animation->zip)
        delete animation->zip;
    delete animation;
}

BootAnimation::Animation* BootAnimation::loadAnimation(const String8& fn)
{
    if (mLoadedFiles.indexOf(fn) >= 0) {
        ALOGE("File \"%s\" is already loaded. Cyclic ref is not allowed",
            fn.string());
        return NULL;
    }
    ZipFileRO *zip = ZipFileRO::open(fn);
    if (zip == NULL) {
        ALOGE("Failed to open animation zip \"%s\": %s",
            fn.string(), strerror(errno));
        return NULL;
    }

    Animation *animation =  new Animation;
    animation->fileName = fn;
    animation->zip = zip;
    animation->clockFont.map = nullptr;
    mLoadedFiles.add(animation->fileName);

    parseAnimationDesc(*animation);
    if (!preloadZip(*animation)) {
        return NULL;
    }


    mLoadedFiles.remove(fn);
    return animation;
}

bool BootAnimation::updateIsTimeAccurate() {
    static constexpr long long MAX_TIME_IN_PAST =   60000LL * 60LL * 24LL * 30LL;  // 30 days
    static constexpr long long MAX_TIME_IN_FUTURE = 60000LL * 90LL;  // 90 minutes

    if (mTimeIsAccurate) {
        return true;
    }
    if (mShuttingDown) return true;
    struct stat statResult;

    if(stat(TIME_FORMAT_12_HOUR_FLAG_FILE_PATH, &statResult) == 0) {
        mTimeFormat12Hour = true;
    }

    if(stat(ACCURATE_TIME_FLAG_FILE_PATH, &statResult) == 0) {
        mTimeIsAccurate = true;
        return true;
    }

    FILE* file = fopen(LAST_TIME_CHANGED_FILE_PATH, "r");
    if (file != NULL) {
      long long lastChangedTime = 0;
      fscanf(file, "%lld", &lastChangedTime);
      fclose(file);
      if (lastChangedTime > 0) {
        struct timespec now;
        clock_gettime(CLOCK_REALTIME, &now);
        // Match the Java timestamp format
        long long rtcNow = (now.tv_sec * 1000LL) + (now.tv_nsec / 1000000LL);
        if (ACCURATE_TIME_EPOCH < rtcNow
            && lastChangedTime > (rtcNow - MAX_TIME_IN_PAST)
            && lastChangedTime < (rtcNow + MAX_TIME_IN_FUTURE)) {
            mTimeIsAccurate = true;
        }
      }
    }

    return mTimeIsAccurate;
}

BootAnimation::TimeCheckThread::TimeCheckThread(BootAnimation* bootAnimation) : Thread(false),
    mInotifyFd(-1), mSystemWd(-1), mTimeWd(-1), mBootAnimation(bootAnimation) {}

BootAnimation::TimeCheckThread::~TimeCheckThread() {
    // mInotifyFd may be -1 but that's ok since we're not at risk of attempting to close a valid FD.
    close(mInotifyFd);
}

bool BootAnimation::TimeCheckThread::threadLoop() {
    bool shouldLoop = doThreadLoop() && !mBootAnimation->mTimeIsAccurate
        && mBootAnimation->mClockEnabled;
    if (!shouldLoop) {
        close(mInotifyFd);
        mInotifyFd = -1;
    }
    return shouldLoop;
}

bool BootAnimation::TimeCheckThread::doThreadLoop() {
    static constexpr int BUFF_LEN (10 * (sizeof(struct inotify_event) + NAME_MAX + 1));

    // Poll instead of doing a blocking read so the Thread can exit if requested.
    struct pollfd pfd = { mInotifyFd, POLLIN, 0 };
    ssize_t pollResult = poll(&pfd, 1, 1000);

    if (pollResult == 0) {
        return true;
    } else if (pollResult < 0) {
        ALOGE("Could not poll inotify events");
        return false;
    }

    char buff[BUFF_LEN] __attribute__ ((aligned(__alignof__(struct inotify_event))));;
    ssize_t length = read(mInotifyFd, buff, BUFF_LEN);
    if (length == 0) {
        return true;
    } else if (length < 0) {
        ALOGE("Could not read inotify events");
        return false;
    }

    const struct inotify_event *event;
    for (char* ptr = buff; ptr < buff + length; ptr += sizeof(struct inotify_event) + event->len) {
        event = (const struct inotify_event *) ptr;
        if (event->wd == mSystemWd && strcmp(SYSTEM_TIME_DIR_NAME, event->name) == 0) {
            addTimeDirWatch();
        } else if (event->wd == mTimeWd && (strcmp(LAST_TIME_CHANGED_FILE_NAME, event->name) == 0
                || strcmp(ACCURATE_TIME_FLAG_FILE_NAME, event->name) == 0)) {
            return !mBootAnimation->updateIsTimeAccurate();
        }
    }

    return true;
}

void BootAnimation::TimeCheckThread::addTimeDirWatch() {
        mTimeWd = inotify_add_watch(mInotifyFd, SYSTEM_TIME_DIR_PATH,
                IN_CLOSE_WRITE | IN_MOVED_TO | IN_ATTRIB);
        if (mTimeWd > 0) {
            // No need to watch for the time directory to be created if it already exists
            inotify_rm_watch(mInotifyFd, mSystemWd);
            mSystemWd = -1;
        }
}

status_t BootAnimation::TimeCheckThread::readyToRun() {
    mInotifyFd = inotify_init();
    if (mInotifyFd < 0) {
        ALOGE("Could not initialize inotify fd");
        return NO_INIT;
    }

    mSystemWd = inotify_add_watch(mInotifyFd, SYSTEM_DATA_DIR_PATH, IN_CREATE | IN_ATTRIB);
    if (mSystemWd < 0) {
        close(mInotifyFd);
        mInotifyFd = -1;
        ALOGE("Could not add watch for %s", SYSTEM_DATA_DIR_PATH);
        return NO_INIT;
    }

    addTimeDirWatch();

    if (mBootAnimation->updateIsTimeAccurate()) {
        close(mInotifyFd);
        mInotifyFd = -1;
        return ALREADY_EXISTS;
    }

    return NO_ERROR;
}

#ifdef BOOTANIMATION_EXT
bool BootAnimation::soundplay() {
    mp = NULL;
    if (mSoundFileName.length() == 0) {
        __android_log_print(ANDROID_LOG_WARN, LOG_TAG, "no bootanimation sound resource....");
        return false;
    }
    mfd = open(mSoundFileName.string(), O_RDONLY);
    if(mfd == -1){
        __android_log_print(ANDROID_LOG_WARN, LOG_TAG, "can not find bootanimation sound resource....");
        return false;
    }
    mp = new MediaPlayer();
    if (mShuttingDown && mWaitForComplete) {
        sp<MPlayerListener> mListener = new MPlayerListener();
        mp->setListener(mListener);
    }
    mp->setDataSource(mfd, 0, 0x7ffffffffffffffLL);
    mp->setAudioStreamType(/*AUDIO_STREAM_MUSIC*/AUDIO_STREAM_SYSTEM);
    mp->prepare();
    mp->start();
    return false;
}

bool BootAnimation::soundstop()
{
    if (mSoundFileName.length() == 0) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "no sound resource ");
        return false;
    }

    if (mp != NULL)mp->stop();
    return false;
}

bool BootAnimation::playSoundsAllowed() {
    // Compatible with Android Things
    // only system called will play sounds in system/media/
    if (!mSystemCalled) {
        return false;
    }

    // Read the system property to see if we should play the sound.
    // If it's not present, default to allowed.
    if (!property_get_bool(PLAY_SOUND_PROP_NAME, 1)) {
        return false;
    }

    // Don't play sounds if this is a reboot due to an error.
    char bootreason[PROPERTY_VALUE_MAX];
    if (property_get(BOOTREASON_PROP_NAME, bootreason, nullptr) > 0) {
        for (const auto& str : PLAY_SOUND_BOOTREASON_BLACKLIST) {
            if (strcasecmp(str.c_str(), bootreason) == 0) {
                return false;
            }
        }
    }
    return true;
}

void BootAnimation::scaleSurfaceIfNeeded() {
    if (mShuttingDown) {
        sp<IBinder> dtoken(SurfaceComposerClient::getBuiltInDisplay(
                ISurfaceComposer::eDisplayIdMain));
        DisplayInfo dinfo;
        SurfaceComposerClient::getDisplayInfo(dtoken, &dinfo);
        SurfaceComposerClient::Transaction t;
        if (((int)dinfo.orientation + 4) % 4 == 1) {
            t.setMatrix(mFlingerSurfaceControl, 0,-1,1,0);
            t.setPosition(mFlingerSurfaceControl, 0,(int)dinfo.w);
            t.apply();
        } else if (((int)dinfo.orientation + 4) % 4 == 3) {
            t.setMatrix(mFlingerSurfaceControl, 0,1,-1,0);
            t.setPosition(mFlingerSurfaceControl, (int)dinfo.h,0);
            t.apply();
        } else if (((int)dinfo.orientation + 4) % 4 == 2) {
            t.setMatrix(mFlingerSurfaceControl, -1,0,0,-1);
            t.setPosition(mFlingerSurfaceControl, (int)dinfo.w,(int)dinfo.h);
            t.apply();
        }

    }
}
#endif
// ---------------------------------------------------------------------------

}
; // namespace android
