package com.greysonparrelli.permiso;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.FragmentActivity;

/**
 * An Activity that handles the small amount of boilerplate that {@link Permiso} requires to run. If you'd rather not
 * use this as your base activity class, simply remember to do the following in each of your activities:
 * <ul>
 *     <li>Call {@link Permiso#setActivity(FragmentActivity)} in {@link Activity#onCreate(Bundle)} and {@link Activity#onResume()}</li>
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
    protected void onResume() {
        super.onResume();
        Permiso.getInstance().setActivity(this);
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // If we don't do this, android.support.v4.app.DialogFragment can throw IllegalStateException. See bug:
        // https://code.google.com/p/android/issues/detail?id=190966
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                Permiso.getInstance().onRequestPermissionResult(requestCode, permissions, grantResults);
            }
        });
    }
}