package kr.ac.inha.nsl.mindforecaster;

import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class CustomNotificationDialog extends DialogFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        root = (ViewGroup) inflater.inflate(R.layout.dialog_custom_notification, container, true);
        init();
        return root;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (!returnResult) {
            super.onDismiss(dialog);
            return;
        }

        int res = Integer.parseInt(timeValueText.getText().toString());

        switch (timeScaleSpinner.getSelectedItemPosition()) {
            case 0:
                // minute case
                break;
            case 1:
                // hour case
                res *= 60;
                break;
            case 2:
                // day case
                res *= 1440;
                break;
            default:
                break;
        }

        switch (timeDirectionSpinner.getSelectedItemPosition()) {
            case 0:
                // before event case
                res = -Math.abs(res);
                break;
            case 1:
                // after event case
                res = Math.abs(res);
                break;
            default:
                break;
        }

        if (getActivity() instanceof EventActivity) {
            ((EventActivity) getActivity()).setCustomNotifParams(res);
        } else if (getActivity() instanceof InterventionsActivity) {
            ((InterventionsActivity) getActivity()).setCustomNotifParams(res);
        }

        super.onDismiss(dialog);
    }

    // region Variables
    private ViewGroup root;
    private EditText timeValueText;
    private Spinner timeScaleSpinner;
    private Spinner timeDirectionSpinner;
    private boolean returnResult = false;
    //endregion

    private void init() {
        timeValueText = root.findViewById(R.id.number);
        timeScaleSpinner = root.findViewById(R.id.spinner_timescale);
        timeDirectionSpinner = root.findViewById(R.id.spinner_timedirection);

        if (getArguments().getBoolean("isEventNotification"))
            timeDirectionSpinner.setEnabled(false);

        root.findViewById(R.id.btn_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (timeValueText.length() == 0) {
                    Toast.makeText(getActivity(), "Value field cannot be empty, please try again!", Toast.LENGTH_SHORT).show();
                } else {
                    returnResult = true;
                    dismiss();
                }
            }
        });

        root.findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                returnResult = false;
                dismiss();
            }
        });
    }
}
