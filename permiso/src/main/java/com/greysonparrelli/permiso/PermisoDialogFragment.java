package com.greysonparrelli.permiso;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;

public class PermisoDialogFragment extends DialogFragment {

    public static String TAG = "PermisoDialogFragment";

    private static final String KEY_TITLE = "title";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_BUTTON_TEXT = "button_text";

    private String mTitle;
    private String mMessage;
    private String mButtonText;

    private IOnDismissListener mOnDismissListener;

    public static PermisoDialogFragment newInstance(@Nullable String title, @NonNull String message, @Nullable String buttonText) {
        PermisoDialogFragment dialogFragment = new PermisoDialogFragment();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putString(KEY_TITLE, title);
        args.putString(KEY_MESSAGE, message);
        args.putString(KEY_BUTTON_TEXT, buttonText);
        dialogFragment.setArguments(args);

        return dialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTitle = getArguments().getString(KEY_TITLE);
        mMessage = getArguments().getString(KEY_MESSAGE);
        mButtonText = getArguments().getString(KEY_BUTTON_TEXT);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        if (mTitle != null) {
            builder.setTitle(mTitle);
        }
        if (mMessage != null) {
            builder.setMessage(mMessage);
        }
        if (mButtonText != null) {
            builder.setPositiveButton(mButtonText, null);
        } else {
            builder.setPositiveButton(android.R.string.ok, null);
        }
        return builder.create();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mOnDismissListener != null) {
            mOnDismissListener.onDismiss();
        }
    }

    public void setOnDismissListener(IOnDismissListener listener) {
        mOnDismissListener = listener;
    }

    public interface IOnDismissListener {
        void onDismiss();
    }
}
