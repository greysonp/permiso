package com.greysonparrelli.permiso;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * An Activity that handles the small amount of boilerplate that {@link Permiso} requires to run. If you'd rather not
 * use this as your base activity class, simply remember to do the following in each of your activities:
 * <ul>
 *     <li>Call {@link Permiso#setActivity(Activity)} in {@link Activity#onCreate(Bundle)}</li>
 *     <li>Call {@link Permiso#onRequestPermissionResult(int, String[], int[])} in
 *      {@link Activity#onRequestPermissionsResult(int, String[], int[])}</li>
 * </ul>
 */
public class PermisoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Permiso.getInstance().setActivity(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Permiso.getInstance().onRequestPermissionResult(requestCode, permissions, grantResults);
    }
}