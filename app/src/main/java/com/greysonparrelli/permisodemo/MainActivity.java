package com.greysonparrelli.permisodemo;

import android.Manifest;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.greysonparrelli.permiso.Permiso;

public class MainActivity extends AppCompatActivity {

    private Permiso mPermiso;


    // =====================================================================
    // Overrides
    // =====================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Instantiate Permiso
        mPermiso = new Permiso(this);

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
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mPermiso.onPermissionResultsRetrieved(requestCode, permissions, grantResults);
    }

    // =====================================================================
    // Click Listeners
    // =====================================================================

    /**
     * Request a single permission and display whether or not it was granted or denied.
     */
    private void onSingleClick() {
        mPermiso.requestPermissions(new Permiso.IOnPermissionResult() {
            @Override
            public void onPermissionResult(Permiso.ResultSet resultSet) {
                if (resultSet.areAllPermissionsGranted()) {
                    Toast.makeText(MainActivity.this, "Permission Granted!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Permission Denied.", Toast.LENGTH_SHORT).show();
                }
            }
        }, Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    /**
     * Request multiple permissions and display how many were granted.
     */
    private void onMultipleClick() {
        mPermiso.requestPermissions(new Permiso.IOnPermissionResult() {
            @Override
            public void onPermissionResult(Permiso.ResultSet resultSet) {
                int numGranted = 0;
                if (resultSet.isPermissionGranted(Manifest.permission.READ_CONTACTS)) {
                    numGranted++;
                }
                if (resultSet.isPermissionGranted(Manifest.permission.READ_CALENDAR)) {
                    numGranted++;
                }
                Toast.makeText(MainActivity.this, numGranted + "/2 Permissions Granted.", Toast.LENGTH_SHORT).show();
            }
        }, Manifest.permission.READ_CONTACTS, Manifest.permission.READ_CALENDAR);
    }

    /**
     * Make two simultaneous requests for the same permission. Only one dialog will pop up, and the results from that
     * one request will be given to both callbacks.
     */
    private void onDuplicateClick() {
        mPermiso.requestPermissions(new Permiso.IOnPermissionResult() {
            @Override
            public void onPermissionResult(Permiso.ResultSet resultSet) {
                if (resultSet.areAllPermissionsGranted()) {
                    Toast.makeText(MainActivity.this, "Permission Granted! (1)", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Permission Denied. (1)", Toast.LENGTH_SHORT).show();
                }
            }
        }, Manifest.permission.CAMERA);

        mPermiso.requestPermissions(new Permiso.IOnPermissionResult() {
            @Override
            public void onPermissionResult(Permiso.ResultSet resultSet) {
                if (resultSet.areAllPermissionsGranted()) {
                    Toast.makeText(MainActivity.this, "Permission Granted! (2)", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Permission Denied. (2)", Toast.LENGTH_SHORT).show();
                }
            }
        }, Manifest.permission.CAMERA);
    }
}
