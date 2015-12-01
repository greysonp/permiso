package com.greysonparrelli.permisodemo;

import android.Manifest;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.greysonparrelli.permiso.Permiso;

/**
 * Created to demonstrate how to use Permiso without extending PermisoActivity.
 */
public class NonPermisoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_non_permiso);

        //
        // First, tell Permiso that you're using this activity
        //
        Permiso.getInstance().setActivity(this);

        findViewById(R.id.btn_request).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRequestClick();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        //
        // Second, we also have to set the activity here to handle transitioning between activities
        //
        Permiso.getInstance().setActivity(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        //
        // Third, forward the results of this method to Permiso
        //
        Permiso.getInstance().onRequestPermissionResult(requestCode, permissions, grantResults);
    }

    private void onRequestClick() {
        //
        // And that's it! Now you can make permission requests as usual
        //
        Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
            @Override
            public void onPermissionResult(Permiso.ResultSet resultSet) {
                if (resultSet.areAllPermissionsGranted()) {
                    Toast.makeText(NonPermisoActivity.this, "Permission Granted!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(NonPermisoActivity.this, "Permission Denied.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                Permiso.getInstance().showRationaleInDialog("Permission Rationale", "Needed for demo purposes.", null, callback);
            }
        }, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

}
