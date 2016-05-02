package com.greysonparrelli.permisodemo;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.greysonparrelli.permiso.Permiso;
import com.greysonparrelli.permiso.PermisoActivity;

/**
 * An activity that demonstrates the features of {@link Permiso}. This activity extends {@link PermisoActivity} in order
 * to handle some boilerplate. If you don't want to extend {@link PermisoActivity}, check out
 * {@link NonPermisoActivity}.
 */
public class MainActivity extends PermisoActivity {

    // =====================================================================
    // Overrides
    // =====================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set click listeners
        findViewById(R.id.btn_single).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSingleClick();
            }
        });
        findViewById(R.id.btn_multiple).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onMultipleClick();
            }
        });
        findViewById(R.id.btn_duplicate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDuplicateClick();
            }
        });
        findViewById(R.id.btn_non_permiso).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNonPermisoClick();
            }
        });
    }


    // =====================================================================
    // Click Listeners
    // =====================================================================

    /**
     * Request a single permission and display whether or not it was granted or denied.
     */
    private void onSingleClick() {
        // A request for a single permission
        Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
            @Override
            public void onPermissionResult(Permiso.ResultSet resultSet) {
                if (resultSet.isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(MainActivity.this, R.string.permission_granted, Toast.LENGTH_SHORT).show();
                } else if (resultSet.isPermissionPermanentlyDenied(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                    Toast.makeText(MainActivity.this, R.string.permission_permanently_denied, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                Permiso.getInstance().showRationaleInDialog(getString(R.string.permission_rationale), getString(R.string.needed_for_demo_purposes), null, callback);
            }
        }, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    /**
     * Request multiple permissions and display how many were granted.
     */
    private void onMultipleClick() {
        // A request for two permissions
        Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
            @Override
            public void onPermissionResult(Permiso.ResultSet resultSet) {
                int numGranted = 0;
                if (resultSet.isPermissionGranted(Manifest.permission.READ_CONTACTS)) {
                    numGranted++;
                }
                if (resultSet.isPermissionGranted(Manifest.permission.READ_CALENDAR)) {
                    numGranted++;
                }
                Toast.makeText(MainActivity.this, numGranted + R.string.two_permission_granted, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                Permiso.getInstance().showRationaleInDialog(getString(R.string.permission_rationale), getString(R.string.needed_for_demo_purposes), null, callback);
            }
        }, Manifest.permission.READ_CONTACTS, Manifest.permission.READ_CALENDAR);
    }

    /**
     * Make two simultaneous requests for the same permission. Only one dialog will pop up, and the results from that
     * one request will be given to both callbacks.
     */
    private void onDuplicateClick() {
        // First request
        requestPermissions("1");

        // Second request for the same permission
        requestPermissions("2");
    }

    private void requestPermissions(final String requestNumber) {
        Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
            @Override
            public void onPermissionResult(Permiso.ResultSet resultSet) {
                if (resultSet.areAllPermissionsGranted()) {
                    Toast.makeText(MainActivity.this, R.string.permission_granted + " (" + requestNumber + ")", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, R.string.permission_denied + " (" + requestNumber + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                Permiso.getInstance().showRationaleInDialog(getString(R.string.permission_rationale), getString(R.string.needed_for_demo_purposes), null, callback);
            }
        }, Manifest.permission.CAMERA);
    }

    /**
     * Starts {@link NonPermisoActivity}.
     */
    private void onNonPermisoClick() {
        startActivity(new Intent(this, NonPermisoActivity.class));
    }
}
