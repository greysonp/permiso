package com.greysonparrelli.permiso;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Permiso {

    private Map<Integer, RequestData> mCodesToRequests;
    private Activity mActivity;
    private int mActiveRequestCode = 1;


    // =====================================================================
    // Creation
    // =====================================================================

    /**
     * <p>
     *     Create a new instance of Permiso to help you manage permission requests.
     * </p>
     * <p>
     *     You should only create one instance of Permiso per activity. Be very careful not to leak your activity by
     *     giving this instance to objects whose lifecycle exists outside of your activity!
     * </p>
     * @param activity The activity that will be requesting permissions.
     */
    public Permiso(@NonNull Activity activity) {
        mActivity = activity;
        mCodesToRequests = new HashMap<>();
    }


    // =====================================================================
    // Public
    // =====================================================================

    /**
     * Request one or more permissions from the system.
     * @param callback    A callback that will be triggered when the results of your permission request are available.
     * @param permissions A list of permission constants that you are requesting. Use constants from {@link android.Manifest.permission}.
     */
    public void requestPermissions(@NonNull IOnPermissionResult callback, String... permissions) {
        final RequestData requestData = new RequestData(callback, permissions);

        // Mark any permissions that are already granted
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(mActivity, permission) == PackageManager.PERMISSION_GRANTED) {
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
                // First check if there's any permissions for which we need to provide a rationale for using
                String[] permissionsThatNeedRationale = requestData.resultSet.getPermissionsThatNeedRationale(mActivity);

                // If there are some that need a rationale, show that rationale, then continue with the request
                if (permissionsThatNeedRationale.length > 0) {
                    requestData.onResultListener.onRationaleRequested(new IOnRationaleProvided() {
                        @Override
                        public void onRationaleProvided() {
                            makePermissionRequest(requestData);
                        }
                    }, permissionsThatNeedRationale);
                } else {
                    makePermissionRequest(requestData);
                }
            }
        }
    }

    /**
     * This method needs to be called by your activity's {@link Activity#onRequestPermissionsResult(int, String[], int[])}.
     * Simply forward the results of that method here.
     * @param requestCode  The request code given to you by {@link Activity#onRequestPermissionsResult(int, String[], int[])}.
     * @param permissions  The permissions given to you by {@link Activity#onRequestPermissionsResult(int, String[], int[])}.
     * @param grantResults The grant results given to you by {@link Activity#onRequestPermissionsResult(int, String[], int[])}.
     */
    public void onPermissionResultsRetrieved(int requestCode, String[] permissions, int[] grantResults) {
        if (mCodesToRequests.containsKey(requestCode)) {
            RequestData requestData = mCodesToRequests.get(requestCode);
            requestData.resultSet.parsePermissionResults(permissions, grantResults);
            requestData.onResultListener.onPermissionResult(requestData.resultSet);
            mCodesToRequests.remove(requestCode);
        }
    }


    // =====================================================================
    // Private
    // =====================================================================

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

    private void makePermissionRequest(RequestData requestData) {
        int requestCode = mActiveRequestCode++;
        mCodesToRequests.put(requestCode, requestData);
        ActivityCompat.requestPermissions(mActivity, requestData.resultSet.getUngrantedPermissions(), requestCode);
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
     */
    public interface IOnRationaleProvided {
        /**
         * Invoke this method when you are done providing a rationale to the user in
         * {@link IOnPermissionResult#onRationaleRequested(IOnRationaleProvided, String...)}
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
