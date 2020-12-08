package co.tradedepot.co.flutter_ussd;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;

public class USSDHandler implements MethodChannel.MethodCallHandler {
    private Handler handler;
    private Activity activity;


    USSDHandler() {
        handler = new Handler(Looper.getMainLooper());
    }

    @Nullable
    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, final @NonNull MethodChannel.Result result) {
        if (call.method.equals("dial")) {
            final String ussd = call.argument("ussd");
            if (ContextCompat.checkSelfPermission(getActivity(),
                    Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                final ArrayList<String> requestPermissions = new ArrayList<>();
                requestPermissions.add(Manifest.permission.CALL_PHONE);
                requestPermissions(
                        requestPermissions,
                        /* successCallback */ new Callback() {
                            @Override
                            public void invoke(Object... args) {
                                List<String> grantedPermissions = (List<String>) args[0];
                                // If we fail to create either, destroy the other one and fail.
                                if (!grantedPermissions.contains(Manifest.permission.CALL_PHONE)) {
                                    result.error(
                                            /* type */ "PermissionError",
                                            "Failed to dial ussd", null);
                                    return;
                                }
                                sendUSSD(ussd, result);
                            }
                        },
                        /* errorCallback */ new Callback() {
                            @Override
                            public void invoke(Object... args) {
                                result.error(
                                        /* type */ "PermissionError",
                                        "Failed to save file", null);
                            }
                        }
                );
            } else{
                sendUSSD(ussd, result);
            }
        } else if (call.method.equals("isSupported")) {
            result.success(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O);
        } else {
            result.notImplemented();
        }
    }

    private void sendUSSD(final String ussd, final @NonNull MethodChannel.Result result) {
        final TelephonyManager telephonyManager = (TelephonyManager)getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            telephonyManager.sendUssdRequest(ussd, new TelephonyManager.UssdResponseCallback() {
                @Override
                public void onReceiveUssdResponse(TelephonyManager telephonyManager, String request, CharSequence response) {
                    super.onReceiveUssdResponse(telephonyManager, request, response);
                    Log.d("ussd", "Success with response : " + response);
                    result.success(response);
                }

                @Override
                public void onReceiveUssdResponseFailed(TelephonyManager telephonyManager, String request, int failureCode) {
                    super.onReceiveUssdResponseFailed(telephonyManager, request, failureCode);
                    Log.e("ussd","failed with code " + Integer.toString(failureCode));
                    result.error("dial", "peerConnection is null", Integer.toString(failureCode));
                }
            }, handler);
        }
    }

    public void requestPermissions(
            final ArrayList<String> permissions, final Callback successCallback,  final Callback errorCallback) {

        PermissionUtils.Callback callback = new PermissionUtils.Callback() {
            @Override
            public void invoke(String[] permissions_, int[] grantResults) {
                List<String> grantedPermissions = new ArrayList<>();
                List<String> deniedPermissions = new ArrayList<>();

                for (int i = 0; i < permissions_.length; ++i) {
                    String permission = permissions_[i];
                    int grantResult = grantResults[i];

                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        grantedPermissions.add(permission);
                    } else {
                        deniedPermissions.add(permission);
                    }
                }

                // Success means that all requested permissions were granted.
                for (String p : permissions) {
                    if (!grantedPermissions.contains(p)) {
                        errorCallback.invoke(deniedPermissions);
                        return;
                    }
                }
                successCallback.invoke(grantedPermissions);
            }
        };

        PermissionUtils.requestPermissions(
                getActivity(),
                permissions.toArray(new String[permissions.size()]),
                callback);
    }
}
