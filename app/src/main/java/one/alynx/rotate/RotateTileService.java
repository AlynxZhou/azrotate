package one.alynx.rotate;

import android.content.ContentResolver;
import android.content.Intent;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

public class RotateTileService extends TileService {
    private final static String TAG = "RotateTileService";

    @Override
    public void onStartListening() {
        super.onStartListening();

        Tile qsTile = getQsTile();
        qsTile.setState(Tile.STATE_ACTIVE);
        qsTile.updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();

        Log.d(TAG, "Tile clicked!\n");

        ContentResolver resolver = getContentResolver();
        int rotation = 0;

        try {
            rotation = Settings.System.getInt(resolver, Settings.System.USER_ROTATION);
        } catch (Settings.SettingNotFoundException e) {
            // Are there really Android devices without USER_ROTATION?
            Toast toast = Toast.makeText(this, R.string.no_user_rotation, Toast.LENGTH_LONG);
            toast.show();
            e.printStackTrace();
            return;
        }
        Log.d(TAG, String.format("Get screen rotation %d %s.\n", rotation * 90,
            rotation == 0 ? "degree" : "degrees"));

        boolean canChangeSettings = Settings.System.canWrite(this);
        if (!canChangeSettings) {
            Log.d(TAG, "Request permission to change system settings.\n");
            Toast toast = Toast.makeText(this, R.string.grant_permission, Toast.LENGTH_LONG);
            toast.show();
            // There is no way to collapse notification panel, using broadcast is deprecated in
            // Android 12.
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return;
        }

        // USER_ROTATION only works when auto rotate is disabled.
        boolean autoRotateEnabled = true;
        try {
            autoRotateEnabled = Settings.System.getInt(resolver,
                Settings.System.ACCELEROMETER_ROTATION) == 1;
        } catch (Settings.SettingNotFoundException e) {
            // Are there really Android devices without ACCELEROMETER_ROTATION?
            Toast toast = Toast.makeText(this, R.string.no_accelerometer_rotation,
                Toast.LENGTH_LONG);
            toast.show();
            e.printStackTrace();
            return;
        }
        if (autoRotateEnabled) {
            Log.d(TAG, "Disable auto rotate.\n");
            // On my Galaxy Tab S8 with Android 12, it seems only when user click the auto rotate
            // button to disable auto rotate will it sync USER_ROTATION with current screen
            // orientation. Disable auto rotate programmatically won't sync it, so if we first
            // disable auto rotate, and update USER_ROTATION, and enable auto rotate then rotate
            // screen, we will get wrong value on next updating USER_ROTATION. So better to let user
            // disable auto rotate.
            Toast toast = Toast.makeText(this, R.string.disable_auto_rotate, Toast.LENGTH_LONG);
            toast.show();
            return;
            // Settings.System.putInt(resolver, Settings.System.ACCELEROMETER_ROTATION, 0);
        }

        // We don't want to rotate a circle, it's very hard to click 3 times while rotating from 90
        // to 0. Mostly we just want a switch between landscape and portrait.
        switch (rotation) {
            case Surface.ROTATION_0:
                rotation = Surface.ROTATION_90;
                break;
            case Surface.ROTATION_90:
                rotation = Surface.ROTATION_0;
                break;
            case Surface.ROTATION_180:
                rotation = Surface.ROTATION_270;
                break;
            case Surface.ROTATION_270:
                rotation = Surface.ROTATION_180;
                break;
            default:
                break;
        }

        Settings.System.putInt(resolver, Settings.System.USER_ROTATION, rotation);
        Log.d(TAG, String.format("Set screen rotation to %d %s.\n", rotation * 90,
            rotation == 0 ? "degree" : "degrees"));
    }
}
