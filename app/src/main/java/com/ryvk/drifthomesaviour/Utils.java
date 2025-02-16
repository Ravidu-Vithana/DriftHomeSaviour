package com.ryvk.drifthomesaviour;

import android.content.Context;
import android.widget.Toast;

public class Utils {
    public static void reportTrip(Context context){
        Toast.makeText(context, "Reporting incident...", Toast.LENGTH_SHORT).show();
    }
}
