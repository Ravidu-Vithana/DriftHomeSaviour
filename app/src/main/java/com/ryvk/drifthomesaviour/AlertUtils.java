package com.ryvk.drifthomesaviour;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class AlertUtils {
    public static void showAlert(Context context, String title, String message) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
    public static void showConfirmDialog(Context context, String title, String message,
                                         DialogInterface.OnClickListener yesListener) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Yes", yesListener)
                .setNegativeButton("No", null)
                .show();
    }
}
