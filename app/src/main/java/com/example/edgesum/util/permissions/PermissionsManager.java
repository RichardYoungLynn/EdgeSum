package com.example.edgesum.util.permissions;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionsManager {

    private PermissionsManager() {

    }

    public static boolean checkIfContextHavePermission(Context context, String permission) {
        if (context == null)
            return false;
        return ContextCompat.checkSelfPermission(context,
                permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestPermissionForActivity(Activity activity,
                                                    String[] permissions, int requestCode) {
        ActivityCompat.requestPermissions(activity,
                permissions,
                requestCode);
    }

}
