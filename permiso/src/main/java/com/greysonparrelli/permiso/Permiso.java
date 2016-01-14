package com.greysonparrelli.permiso;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class to make permission-management easier. Provides methods to conveniently request permissions anywhere in your
 * app.
 */
public class Permiso {

    private static final String TAG = "Permiso";

    /**
     * A map to keep track of our outstanding permission requests. The key is the request code sent when we call
     * {@link ActivityCompat#requestPermissions(Activity, String[], int)}. The value is the {@link Permiso.RequestData}
     * bundle that holds all of the request information.
     */
    private Map<Integer, RequestData> mCodesToRequests;

    /**
     * The active activity. Used to make permissions requests. This must be set by the library-user through
     * {@link Permiso#setActivity(Activity)} or else bad things will happen.
     */
    private WeakReference<Activity> mActivity;

    /**
     * This is just a value we increment to generate new request codes for use with
     * {@link ActivityCompat#requestPermissions(Activity, String[], int)}.
     */
    private int mActiveRequestCode = 1;

    /**
     * The singleton instance.
     */
    private static Permiso sInstance = new Permiso();


    // =====================================================================
    // Creation
    // =====================================================================

    /**
     * @return An instance of {@link Permiso} to help you manage your permissions.
     */
    public static Permiso getInstance() {
        return sInstance;
    }

    /**
     * Implementing a singleton pattern, so this is private.
     */
    private Permiso() {
        mCodesToRequests = new HashMap<>();
    }


    // =====================================================================
    // Public
    // =====================================================================

    /**
     * This method should be invoked in the {@link Activity#onCreate(Bundle)} in every activity that requests
     * permissions. Even if you don't want to use Permiso in your current activity, you should call this method
     * with a null activity to prevent leaking the previously-set activity.
     * <p>
     * <strong>Important: </strong> If your activity subclasses {@link PermisoActivity}, this is already handled for you.
     * @param activity The activity that is currently active.
     */
    public void setActivity(@NonNull Activity activity) {
        mActivity = new WeakReference<>(activity);
    }

    /**
     * Request one or more permissions from the system. Make sure that you are either subclassing {@link PermisoActivity}
     * or that you have set your current activity using {@link Permiso#setActivity(Activity)}!
     * @param callback
     *      A callback that will be triggered when the results of your permission request are available.
     * @param permissions
     *      A list of permission constants that you are requesting. Use constants from
     *      {@link android.Manifest.permission}.
     */
    @MainThread
    public void requestPermissions(@NonNull IOnPermissionResult callback, String... permissions) {
        checkActivity();

        final RequestData requestData = new RequestData(callback, permissions);

        // Mark any permissions that are already granted
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(mActivity.get(), permission) == PackageManager.PERMISSION_GRANTED) {
                requestData.resultSet.grantPermissions(permission);
            }
        }

        // If we had all of them, yay! No need to do anything else.
        if (requestData.resultSet.areAllPermissionsGranted()) {
            requestData.onResultListener.onPermissionResult(requestData.resultSet);
        } else {
            // If we have some unsatisfied ones, let's first see if they can be satisfied by an active request. If it
            // can, we'll re-wire the callback of the active request to also trigger this new one.
            boolean linkedToExisting = linkToExistingRequestIfPossible(requestData);

            // If there was no existing request that can satisfy this one, then let's make a new permission request to
            // the system
            if (!linkedToExisting) {
                // Mark the request as active
                final int requestCode = markRequestAsActive(requestData);

                // First check if there's any permissions for which we need to provide a rationale for using
                String[] permissionsThatNeedRationale = requestData.resultSet.getPermissionsThatNeedRationale(mActivity.get());

                // If there are some that need a rationale, show that rationale, then continue with the request
                if (permissionsThatNeedRationale.length > 0) {
                    requestData.onResultListener.onRationaleRequested(new IOnRationaleProvided() {
                        @Override
                        public void onRationaleProvided() {
                            makePermissionRequest(requestCode);
                        }
                    }, permissionsThatNeedRationale);
                } else {
                    makePermissionRequest(requestCode);
                }
            }
        }
    }

    /**
     * This method needs to be called by your activity's {@link Activity#onRequestPermissionsResult(int, String[], int[])}.
     * Simply forward the results of that method here.
     * <p>
     * <strong>Important: </strong> If your activity subclasses {@link PermisoActivity}, this is already handled for you.
     * @param requestCode
     *      The request code given to you by {@link Activity#onRequestPermissionsResult(int, String[], int[])}.
     * @param permissions
     *      The permissions given to you by {@link Activity#onRequestPermissionsResult(int, String[], int[])}.
     * @param grantResults
     *      The grant results given to you by {@link Activity#onRequestPermissionsResult(int, String[], int[])}.
     */
    @MainThread
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        if (mCodesToRequests.containsKey(requestCode)) {
            RequestData requestData = mCodesToRequests.get(requestCode);
            requestData.resultSet.parsePermissionResults(permissions, grantResults);
            requestData.onResultListener.onPermissionResult(requestData.resultSet);
            mCodesToRequests.remove(requestCode);
        } else {
            Log.w(TAG, "onRequestPermissionResult() was given an unrecognized request code.");
        }
    }

    /**
     * A helper to show your rationale in a {@link android.app.DialogFragment} when implementing
     * {@link IOnRationaleProvided#onRationaleProvided()}. Automatically invokes the rationale callback when the user
     * dismisses the dialog.
     * @param title
     *      The title of the dialog. If null, there will be no title.
     * @param message
     *      The message displayed in the dialog.
     * @param buttonText
     *      The text you want the dismissal button to show. If null, defaults to {@link android.R.string#ok}.
     * @param rationaleCallback
     *      The callback to be trigger
     */
    @MainThread
    public void showRationaleInDialog(@Nullable String title, @NonNull String message, @Nullable String buttonText, @NonNull final IOnRationaleProvided rationaleCallback) {
        checkActivity();

        PermisoDialogFragment dialogFragment = PermisoDialogFragment.newInstance(title, message, buttonText);

        // We show the rationale after the dialog is closed. We use setRetainInstance(true) in the dialog to ensure that
        // it retains the listener after an app rotation.
        dialogFragment.setOnCloseListener(new PermisoDialogFragment.IOnCloseListener() {
            @Override
            public void onClose() {
                rationaleCallback.onRationaleProvided();
            }
        });
        dialogFragment.show(mActivity.get().getFragmentManager(), PermisoDialogFragment.TAG);
    }


    // =====================================================================
    // Private
    // =====================================================================

    /**
     * Checks to see if there are any active requests that are already requesting a superset of the permissions this
     * new request is asking for. If so, this will wire up this new request's callback to be triggered when the
     * existing request is completed and return true. Otherwise, this does nothing and returns false.
     * @param newRequest The new request that is about to be made.
     * @return True if a request was linked, otherwise false.
     */
    private boolean linkToExistingRequestIfPossible(final RequestData newRequest) {
        boolean found = false;

        // Go through all outstanding requests
        for (final RequestData activeRequest : mCodesToRequests.values()) {
            // If we find one that can satisfy all of the new request's permissions, we re-wire the active one's
            // callback to also call this new one's callback
            if (activeRequest.resultSet.containsAllUngrantedPermissions(newRequest.resultSet)) {
                final IOnPermissionResult originalOnResultListener = activeRequest.onResultListener;
                activeRequest.onResultListener = new IOnPermissionResult() {
                    @Override
                    public void onPermissionResult(ResultSet resultSet) {
                        // First, call the active one's callback. It was added before this new one.
                        originalOnResultListener.onPermissionResult(resultSet);

                        // Next, copy over the results to the new one's resultSet
                        String[] unsatisfied = newRequest.resultSet.getUngrantedPermissions();
                        for (String permission : unsatisfied) {
                            newRequest.resultSet.requestResults.put(permission, resultSet.isPermissionGranted(permission));
                        }

                        // Finally, trigger the new one's callback
                        newRequest.onResultListener.onPermissionResult(newRequest.resultSet);
                    }

                    @Override
                    public void onRationaleRequested(IOnRationaleProvided callback, String... permissions) {
                        activeRequest.onResultListener.onRationaleRequested(callback, permissions);
                    }
                };
                found = true;
                break;
            }
        }
        return found;
    }

    /**
     * Puts the RequestData in the map of requests and gives back the request code.
     * @return The request code generated for this request.
     */
    private int markRequestAsActive(RequestData requestData) {
        int requestCode = mActiveRequestCode++;
        mCodesToRequests.put(requestCode, requestData);
        return requestCode;
    }

    /**
     * Makes the permission request for the request that matches the provided request code.
     * @param requestCode The request code of the request you want to run.
     */
    private void makePermissionRequest(int requestCode) {
        RequestData requestData = mCodesToRequests.get(requestCode);
        ActivityCompat.requestPermissions(mActivity.get(), requestData.resultSet.getUngrantedPermissions(), requestCode);
    }

    /**
     * Ensures that our WeakReference to the Activity is still valid. If it isn't, throw an exception saying that the
     * Activity needs to be set.
     */
    private void checkActivity() {
        if (mActivity.get() == null) {
            throw new IllegalStateException("No activity set. Either subclass PermisoActivity or call Permiso.setActivity() in onCreate() and onResume() of your Activity.");
        }
    }


    // =====================================================================
    // Inner Classes
    // =====================================================================

    /**
     * A callback interface for receiving the results of a permission request.
     */
    public interface IOnPermissionResult {
        /**
         * Invoked when the results of your permission request are ready.
         * @param resultSet An object holding the result of your permission request.
         */
        void onPermissionResult(ResultSet resultSet);

        /**
         * Called when the system recommends that you provide a rationale for a permission. This typically happens when
         * a user denies a permission, but they you request it again.
         * @param callback    A callback to be triggered when you are finished showing the user the rationale.
         * @param permissions The list of permissions for which the system recommends you provide a rationale.
         */
        void onRationaleRequested(IOnRationaleProvided callback, String... permissions);
    }

    /**
     * Simple callback to let Permiso know that you have finished providing the user a rationale for a set of permissions.
     * For easy handling of this callback, consider using
     * {@link Permiso#showRationaleInDialog(String, String, String, IOnRationaleProvided)}.
     */
    public interface IOnRationaleProvided {
        /**
         * Invoke this method when you are done providing a rationale to the user in
         * {@link IOnPermissionResult#onRationaleRequested(IOnRationaleProvided, String...)}. The permission request
         * will not be made until this method is invoked.
         */
        void onRationaleProvided();
    }

    private static class RequestData {
        IOnPermissionResult onResultListener;
        ResultSet resultSet;

        public RequestData(@NonNull IOnPermissionResult onResultListener, String... permissions) {
            this.onResultListener = onResultListener;
            resultSet = new ResultSet(permissions);
        }
    }

    /**
     * A class representing the results of a permission request.
     */
    public static class ResultSet {

        private Map<String, Boolean> requestResults;

        private ResultSet(String... permissions) {
            requestResults = new HashMap<>(permissions.length);
            for (String permission : permissions) {
                requestResults.put(permission, false);
            }
        }

        /**
         * Checks if a permission was granted during your permission request.
         * @param permission The permission you are inquiring about. This should be a constant from {@link android.Manifest.permission}.
         * @return True if the permission was granted, otherwise false.
         */
        public boolean isPermissionGranted(String permission) {
            if (requestResults.containsKey(permission)) {
                return requestResults.get(permission);
            }
            return false;
        }

        /**
         * Determines if all permissions in the request were granted.
         * @return True if all permissions in the request were granted, otherwise false.
         */
        public boolean areAllPermissionsGranted() {
            return !requestResults.containsValue(false);
        }

        /**
         * Returns a map representation of this result set. Useful if you'd like to do more complicated operations
         * with the results.
         * @return
         *      A mapping of permission constants to booleans, where true indicates that the permission was granted,
         *      and false indicates that the permission was denied.
         */
        public Map<String, Boolean> toMap() {
            return new HashMap<>(requestResults);
        }

        private void grantPermissions(String... permissions) {
            for (String permission : permissions) {
                requestResults.put(permission, true);
            }
        }

        private void parsePermissionResults(String[] permissions, int[] grantResults) {
            for (int i = 0; i < permissions.length; i++) {
                requestResults.put(permissions[i], grantResults[i] == PackageManager.PERMISSION_GRANTED);
            }
        }

        private String[] getUngrantedPermissions() {
            List<String> ungrantedList = new ArrayList<>(requestResults.size());
            for (String permission : requestResults.keySet()) {
                if (!requestResults.get(permission)) {
                    ungrantedList.add(permission);
                }
            }
            return ungrantedList.toArray(new String[ungrantedList.size()]);
        }

        private boolean containsAllUngrantedPermissions(ResultSet set) {
            List<String> ungranted = Arrays.asList(set.getUngrantedPermissions());
            return requestResults.keySet().containsAll(ungranted);
        }

        private String[] getPermissionsThatNeedRationale(Activity activity) {
            String[] ungranted = getUngrantedPermissions();
            List<String> shouldShowRationale = new ArrayList<>(ungranted.length);
            for (String permission : ungranted) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                    shouldShowRationale.add(permission);
                }
            }
            return shouldShowRationale.toArray(new String[shouldShowRationale.size()]);
        }
    }
}
