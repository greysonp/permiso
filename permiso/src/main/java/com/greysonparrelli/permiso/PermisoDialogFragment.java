package com.greysonparrelli.permiso;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

/**
 * A DialogFragment created for convenience to show a simple message explaining why a certain permission is being
 * requested and trigger a listener when it has been closed. Intended to be used by
 * {@link Permiso#showRationaleInDialog(String, String, String, Permiso.IOnRationaleProvided)}.
 */
public class PermisoDialogFragment extends DialogFragment {

    public static final String TAG = "PermisoDialogFragment";

    private static final String KEY_TITLE       = "titleId";
    private static final String KEY_MESSAGE     = "message";
    private static final String KEY_BUTTON_TEXT = "button_text";
    private static final String KEY_MESSAGE_ID  = "message_id";
    private static final String KEY_TITLE_ID    = "title_id";
    private static final String KEY_HAS_HTML    = "has_html";
    private static final String KEY_BUTTON_TEXT_ID = "button_text_id";

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

    public static class Builder {
        private int titleId = 0;
        private String title = null;
        private int buttonTextId = 0;
        private String message = null;
        private int messageId = 0;
        // by default, interpret the dialog msg body as string text
        private boolean interpretHtml = false;
        private String buttonText = null;


        private Builder() {}

        public Builder(int titleId, int msgBody, int buttonTextId) {
            this();
            this.titleId = titleId;
            this.messageId = msgBody;
            this.buttonTextId = buttonTextId;
        }


        public int getTitleId() { return titleId; }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public int getMessageId() { return messageId;}
        public int getButtonTextId() { return buttonTextId; }
        public String getButtonText() { return buttonText; }
        public boolean isHtml() { return interpretHtml; }

        public Builder setTitleId(int val) { titleId = val; return this; }
        public Builder setTitle(String val) { title = val; return this; }
        public Builder setMessageId(String val) { message = val; return this; }
        public Builder setMessage(int val)    { messageId = val; return this; }
        public Builder setButtonTextId(int stringId) { buttonTextId = stringId; return this; }
        public Builder setButtonText(String string) { buttonText = string; return this; }
        public Builder setHtmlInterpretation(boolean interpretHtml) { this.interpretHtml = interpretHtml; return this; }


        public PermisoDialogFragment build() {
            return PermisoDialogFragment.getInstance(this);
        }

    }

    private static PermisoDialogFragment getInstance(Builder builder) {
        PermisoDialogFragment frag = new PermisoDialogFragment();
        frag.setArguments(getDialogArgs(builder));
        return frag;
    }

    private static Bundle getDialogArgs(Builder builder) {
        Bundle args = new Bundle();
        args.putBoolean(KEY_HAS_HTML, builder.isHtml());
        if (builder.getMessage() != null) {
            args.putString(KEY_MESSAGE, builder.getMessage());
        } else if (builder.getMessageId() != 0) {
            args.putInt(KEY_MESSAGE_ID, builder.getMessageId());
        }

        if (builder.getTitle() != null) {
            args.putString(KEY_TITLE, builder.getTitle());
        } else if (builder.getTitleId() != 0) {
            args.putInt(KEY_TITLE_ID, builder.getTitleId());
        }

        if (builder.getButtonTextId() != 0) {
            args.putInt(KEY_BUTTON_TEXT_ID, builder.getButtonTextId());
        } else if (builder.getButtonText() != null) {
            args.putString(KEY_BUTTON_TEXT, builder.getButtonText());
        }

        return args;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retain instance state so we can keep our listeners registered after a rotation
        setRetainInstance(true);

        if (getArguments().containsKey(KEY_TITLE_ID)) {
            mTitle = getActivity().getString(getArguments().getInt(KEY_TITLE_ID));
        } else if (getArguments().containsKey(KEY_TITLE)) {
            mTitle = getArguments().getString(KEY_TITLE);
        }
        if (getArguments().containsKey(KEY_MESSAGE_ID)) {
            mMessage = getActivity().getString(getArguments().getInt(KEY_MESSAGE_ID));
        } else if (getArguments().containsKey(KEY_MESSAGE)) {
            mMessage = getArguments().getString(KEY_MESSAGE);
        }
        if (getArguments().containsKey(KEY_BUTTON_TEXT_ID)) {
            mButtonText = getActivity().getString(getArguments().getInt(KEY_BUTTON_TEXT_ID));
        } else if (getArguments().containsKey(KEY_BUTTON_TEXT)) {
            mButtonText = getArguments().getString(KEY_BUTTON_TEXT);
        }
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
        if (getArguments().containsKey(KEY_HAS_HTML) && getArguments().getBoolean(KEY_HAS_HTML)) {
            TextView msg = new TextView(getActivity());
            msg.setText(Html.fromHtml(mMessage));
            msg.setMovementMethod(LinkMovementMethod.getInstance());
            builder.setView(msg);
        } else if (mMessage != null) {
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
