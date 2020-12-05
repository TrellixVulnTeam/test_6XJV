
package android.os;

import java.io.File;

import android.os.storage.StorageManager;
import android.text.TextUtils;
import android.util.Log;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import android.os.storage.VolumeInfo;
import android.os.storage.IStorageManager;

/** {@hide} */
public class EnvironmentEx {
    private static final String TAG = "EnvironmentEx";

    private static final String ENV_EMULATED_STORAGE = "EMULATED_STORAGE";
    private static final String ENV_PHYSICAL_STORAGE = "PHYSICAL_STORAGE";


    public static final int STORAGE_PRIMARY_EXTERNAL = 1;
    public static final int STORAGE_PRIMARY_INTERNAL = 2;

    private static final String emulatedPathPrefix = "/storage/emulated";
    private static final String emulatedPathString = getPathString(ENV_EMULATED_STORAGE, "/storage/self/emulated");
    private static final String physicalPathString = getPathString(ENV_PHYSICAL_STORAGE, "/storage/sdcard0");

    static String getPathString(String variableName, String defaultPath) {
        String path = System.getenv(variableName);
        return path == null ? defaultPath : path;
    }

    public static File getSecondaryStorageDirectory() {
        File path = null;
        switch (getStorageType()) {
            case STORAGE_PRIMARY_EXTERNAL:
                path = getInternalStoragePath();
                break;

            case STORAGE_PRIMARY_INTERNAL:
                path = getExternalStoragePath();
                break;
        }
        return path;
    }

    public static File getInternalStoragePath() {
        File internalPath = new File(emulatedPathPrefix + "/" + UserHandle.myUserId());
        return internalPath;
    }

    /**
     * SPRD: add for primary_physical support, need userid for new user when
     * calling from system_server. Apps should not use.
     **/
    public static File getInternalStoragePath(int userId) {
        File internalPath = new File(emulatedPathPrefix + "/" + userId);
        return internalPath;
    }

    public static String getInternalStoragePathState() {
        return Environment.getExternalStorageState(getInternalStoragePath());
    }

    public static File getExternalStoragePath() {
        File externalPath = getStoragePathFormKey("vold.sdcard0.path");
        if (externalPath == null) {
            externalPath = getExternalStorageLinkPath();
        }
        return externalPath;
    }

    public static File getExternalStorageLinkPath() {
        return new File(physicalPathString);
    }

    public static File getEmulatedStoragePath() {
        return new File(emulatedPathString);
    }

    public static File getLegacyExternalStorageDirectory() {
         File tempPath = null;
         switch (getStorageType()) {
                case STORAGE_PRIMARY_EXTERNAL:
                case STORAGE_PRIMARY_INTERNAL:
                     tempPath = getInternalStoragePath();
                     break;
                default:
                     tempPath = Environment.getLegacyExternalStorageDirectory();
        }
        return tempPath;
    }

    /**
     * SPRD: add for primary_physical support, need userid for new user when
     * calling from system_server. Apps should not use.
     **/
    public static File getLegacyExternalStorageDirectory(int userId) {
        File tempPath = null;
        switch (getStorageType()) {
            case STORAGE_PRIMARY_EXTERNAL:
            case STORAGE_PRIMARY_INTERNAL:
                tempPath = getInternalStoragePath(userId);
                break;
            default:
                tempPath = Environment.getLegacyExternalStorageDirectory();
        }
        return tempPath;
    }

    public static String getLegacyExternalStorageState() {
        String state = Environment.MEDIA_REMOVED;
        if (getStorageType() == EnvironmentEx.STORAGE_PRIMARY_EXTERNAL) {
            state = Environment.MEDIA_MOUNTED;
        }
        return state;
    }

    /**
     * only hiddenapi-package-whitelist can call this
     **/
    public static List<VolumeInfo> getUsbdiskVolumes() {
        List<VolumeInfo> vols;
        final ArrayList<VolumeInfo> res = new ArrayList<>();

        final IStorageManager storageManager = IStorageManager.Stub.asInterface(ServiceManager.getService("mount"));

        try {
            vols = Arrays.asList(storageManager.getVolumes(0));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }

        for (VolumeInfo vol : vols) {
            if (vol.disk != null && vol.disk.isUsb()) {
                res.add(vol);
            }
        }

        return res;
    }

    /**
     * only hiddenapi-package-whitelist can call this
     **/
    public static File[] getUsbdiskVolumePaths() {
        int mCount;
        List<VolumeInfo> vols = getUsbdiskVolumes();
        mCount = vols.size();

        final File[] files = new File[mCount];
        for (int i = 0; i < mCount; i++) {
            files[i] = vols.get(i).getPath();
        }

        return files;
    }

    public static File[] getOtgVolumePaths(){
        return getUsbdiskVolumePaths();
    }

    public static String getUsbdiskVolumeState(File path) {
        return Environment.getExternalStorageState(path);
    }

    public static String getExternalStoragePathState() {
        return SystemProperties.get("vold.sdcard0.state", Environment.MEDIA_UNKNOWN);
    }

    public static int getStorageType() {
        return SystemProperties.getInt(StorageManager.PROP_PRIMARY_TYPE, 2);
    }

    private static File getStoragePathFormKey(String key) {
        String path = SystemProperties.get(key);
        if(TextUtils.isEmpty(path)) {
            return null;
        } else {
            return new File(path);
        }
    }
}
