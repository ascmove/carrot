package com.zhimatiao.carrot;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

class Alerter {
    static void newWelcomeAlerts(final Context context, final newWelcomeAlertsInterface interfaces) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.newversionfunc_title);
        builder.setMessage(R.string.newversionfunc_tip);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.iknow, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                interfaces.iknow();
            }
        });
        builder.setNegativeButton(R.string.lockapp, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                interfaces.jumpActivity();
            }
        });
        builder.show();
    }

    static void appKillAlerts(final Context context, final appKillAlertsInterface interfaces) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.appkill_title);
        builder.setMessage(R.string.appkill_tip);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.ignore, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                interfaces.ignore();
            }
        });
        builder.setNegativeButton(R.string.lockapp, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                interfaces.jumpActivity();
            }
        });
        builder.show();
    }

    static void showRecordPermissionAlerts(final Context context, final showRecordPermissionAlertsInterface interfaces) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.permission_title);
        builder.setMessage(R.string.permission_tip);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                interfaces.requestPermission();
            }
        });
        builder.setNegativeButton(R.string.exit, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                System.exit(0);
            }
        });
        builder.show();
    }

    static void showWriteSDPermissionAlerts(final Context context, final showWriteSDPermissionAlertsInterface interfaces) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.permission_title);
        builder.setMessage(R.string.permission_tip);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                interfaces.requestPermission();
            }
        });
        builder.setNegativeButton(R.string.later, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                return;
            }
        });
        builder.show();
    }

    static void hasNewModelAlert(Context context, final hasNewModelAlertInterface interfaces) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.updatemodel_title);
        builder.setMessage(R.string.updatemodel_tip);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                interfaces.newModelDownload();
            }
        });
        builder.setNegativeButton(R.string.later, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                interfaces.newModelIgnore();
            }
        });
        builder.show();
    }

    static void safetyAlert(Context context, final safetyAlertInterface interfaces) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.safetyalert_title);
        builder.setMessage(R.string.safetyalert_tip);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.iknow, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                interfaces.iknow();
            }
        });
        builder.setNegativeButton(R.string.exit, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                System.exit(0);
            }
        });
        builder.show();
    }

    static void logAlert(Context context, final logAlertInterface interfaces) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.attention);
        builder.setMessage(R.string.logalert_tip);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.iknow, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                interfaces.iknow();
            }
        });
        builder.setNegativeButton(R.string.exit, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                interfaces.refuse();
            }
        });
        builder.show();
    }

    static void noiseAutoAlert(Context context, final noiseAutoInterface interfaces) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.attention);
        builder.setMessage(R.string.noise_auto_tip);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.iknow, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                interfaces.iknow();
            }
        });
        builder.show();
    }

    interface showRecordPermissionAlertsInterface {
        void requestPermission();
    }

    interface newWelcomeAlertsInterface {
        void jumpActivity();

        void iknow();
    }

    interface appKillAlertsInterface {
        void ignore();

        void jumpActivity();
    }

    interface showWriteSDPermissionAlertsInterface {
        void requestPermission();
    }

    interface hasNewModelAlertInterface {
        void newModelDownload();

        void newModelIgnore();
    }

    interface safetyAlertInterface {
        void iknow();
    }

    interface logAlertInterface {
        void iknow();

        void refuse();
    }

    interface noiseAutoInterface {
        void iknow();
    }
}
