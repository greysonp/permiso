package com.greysonparrelli.permiso;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;

/**
 * A DialogFragment created for convenience to show a simple message explaining why a certain permission is being
 * requested and trigger a listener when it has been closed. Intended to be used by
 * {@link Permiso#showRationaleInDialog(String, String, String, Permiso.IOnRationaleProvided)}.
 */
public class PermisoDialogFragment extends DialogFragment {

    public static final String TAG = "PermisoDialogFragment";

    private static final String KEY_TITLE = "title";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_BUTTON_TEXT = "button_text";

    private String mTitle;
    private String mMessage;
    private String mButtonText;

    private IOnCloseListener mOnCloseListener;

    /**
     * Creates a new {@link PermisoDialogFragment}. Only intended to be used by
     * {@link Permiso#showRationaleInDialog(String, String, String, Permiso.IOnRationaleProvided)}.
     * @param title      The title of the dialog. If null, no title will be displayed.
     * @param message    The message to be shown in the dialog.
     * @param buttonText The text to label the dialog button. If null, defaults to {@link android.R.string#ok}.
     * @return A new {@link PermisoDialogFragment}.
     */
    public static PermisoDialogFragment newInstance(@Nullable String title, @NonNull String message, @Nullable String buttonText) {
        PermisoDialogFragment dialogFragment = new PermisoDialogFragment();

        // Build arguments bundle
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

        // Retain instance state so we can keep our listeners registered after a rotation
        setRetainInstance(true);

        mTitle = getArguments().getString(KEY_TITLE);
        mMessage = getArguments().getString(KEY_MESSAGE);
        mButtonText = getArguments().getString(KEY_BUTTON_TEXT);
    }

    @Override
    public void onDestroyView() {
        // If we don't do this, the DialogFragment is not recreated after a rotation. See bug:
        // https://code.google.com/p/android/issues/detail?id=17423
        if (getDialog() != null && getRetainInstance()) {
            getDialog().setDismissMessage(null);
        }
        super.onDestroyView();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Title
        if (mTitle != null) {
            builder.setTitle(mTitle);
        }

        // Message
        if (mMessage != null) {
            builder.setMessage(mMessage);
        }

        // Button text
        String buttonText;
        if (mButtonText != null) {
            buttonText = mButtonText;
        } else {
            buttonText = getString(android.R.string.ok);
        }
        builder.setPositiveButton(buttonText, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mOnCloseListener != null) {
                    mOnCloseListener.onClose();
                }
            }
        });
        return builder.create();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        if (mOnCloseListener != null) {
            mOnCloseListener.onClose();
        }
    }

    /**
     * Sets the listener that will be triggered when this dialog is closed by a user action. This includes clicking
     * the dismissal button as well as clicking in the area outside of the dialog. NOT triggered by rotation.
     * @param listener
     */
    public void setOnCloseListener(IOnCloseListener listener) {
        mOnCloseListener = listener;
    }

    /**
     * A simple listener that will be triggered when this dialog is closed by a user action.
     */
    public interface IOnCloseListener {
        /**
         * Called when the dialog is closed by a user action. This includes clicking the dismissal button as well as
         * clicking in the area outside of the dialog. NOT triggered by rotation.
         */
        void onClose();
    }
}
