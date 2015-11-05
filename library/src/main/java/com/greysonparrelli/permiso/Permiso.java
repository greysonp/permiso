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

    public Permiso(@NonNull Activity activity) {
        mActivity = activity;
        mCodesToRequests = new HashMap<>();
    }


    // =====================================================================
    // Public
    // =====================================================================

    public void requestPermission(@NonNull IOnPermissionResult listener, String... permissions) {
        final RequestData requestData = new RequestData(listener, permissions);

        // Mark any permissions that are already granted
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(mActivity, permission) == PackageManager.PERMISSION_GRANTED) {
                requestData.resultSet.satisfyPermissions(permission);
            }
        }

        // If we had all of them, yay! No need to do anything else.
        if (requestData.resultSet.areAllPermissionsSatisfied()) {
            requestData.onResultListener.onPermissionResult(requestData.resultSet);
        } else {
            // If we have some unsatisfied ones, let's first see if they can be satisfied by an active request. If it
            // can, we'll re-wire the callback of the active request to also trigger this new one.
            boolean linkedToExisting = linkToExistingRequestIfPossible(requestData);

            // If there was no existing request that can satisfy this one, then let's make a new permission request to
            // the system
            if (!linkedToExisting) {
                int requestCode = mActiveRequestCode++;
                mCodesToRequests.put(requestCode, requestData);
                ActivityCompat.requestPermissions(mActivity, requestData.resultSet.getUnsatisfiedPermissions(), requestCode);
            }
        }
    }

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
            if (activeRequest.resultSet.containsAllUnsatisfiedPermissions(newRequest.resultSet)) {
                final IOnPermissionResult originalOnResultListener = activeRequest.onResultListener;
                activeRequest.onResultListener = new IOnPermissionResult() {
                    @Override
                    public void onPermissionResult(ResultSet resultSet) {
                        // First, call the active one's callback. It was added before this new one.
                        originalOnResultListener.onPermissionResult(resultSet);

                        // Next, copy over the results to the new one's resultSet
                        String[] unsatisfied = newRequest.resultSet.getUnsatisfiedPermissions();
                        for (String permission : unsatisfied) {
                            newRequest.resultSet.requestResults.put(permission, resultSet.isPermissionSatisfied(permission));
                        }

                        // Finally, trigger the new one's callback
                        newRequest.onResultListener.onPermissionResult(newRequest.resultSet);
                    }
                };
                found = true;
                break;
            }
        }
        return found;
    }


    // =====================================================================
    // Inner Classes
    // =====================================================================

    public interface IOnPermissionResult {
        void onPermissionResult(ResultSet resultSet);
    }

    private static class RequestData {
        IOnPermissionResult onResultListener;
        ResultSet resultSet;

        public RequestData(@NonNull IOnPermissionResult onResultListener, String... permissions) {
            this.onResultListener = onResultListener;
            resultSet = new ResultSet(permissions);
        }
    }

    public static class ResultSet {

        private Map<String, Boolean> requestResults;

        public ResultSet(String... permissions) {
            requestResults = new HashMap<>(permissions.length);
            for (String permission : permissions) {
                requestResults.put(permission, false);
            }
        }

        public boolean isPermissionSatisfied(String permission) {
            if (requestResults.containsKey(permission)) {
                return requestResults.get(permission);
            }
            return false;
        }

        public boolean areAllPermissionsSatisfied() {
            return !requestResults.containsValue(false);
        }

        public Map<String, Boolean> toMap() {
            return new HashMap<>(requestResults);
        }

        private void satisfyPermissions(String... permissions) {
            for (String permission : permissions) {
                requestResults.put(permission, true);
            }
        }

        private void parsePermissionResults(String[] permissions, int[] grantResults) {
            for (int i = 0; i < permissions.length; i++) {
                requestResults.put(permissions[i], grantResults[i] == PackageManager.PERMISSION_GRANTED);
            }
        }

        private String[] getUnsatisfiedPermissions() {
            List<String> unsatisfiedList = new ArrayList<>(requestResults.size());
            for (String permission : requestResults.keySet()) {
                if (!requestResults.get(permission)) {
                    unsatisfiedList.add(permission);
                }
            }
            return unsatisfiedList.toArray(new String[unsatisfiedList.size()]);
        }

        private boolean containsAllUnsatisfiedPermissions(ResultSet set) {
            List<String> unsatisfied = Arrays.asList(set.getUnsatisfiedPermissions());
            return requestResults.keySet().containsAll(unsatisfied);
        }
    }
}
