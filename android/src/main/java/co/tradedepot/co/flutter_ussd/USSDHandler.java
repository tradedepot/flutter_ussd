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

public class USSDHandler implements MethodChannel.MethodCallHandler {
    private Handler handler;
    private Activity activity;
    private MethodChannel.Result mResult;


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
                        new Callback() {
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
                       new Callback() {
                            @Override
                            public void invoke(Object... args) {
                                result.error(
                                        /* type */ "PermissionError",
                                        "Failed to dial ussd", null);
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
            mResult = result;
            telephonyManager.sendUssdRequest(ussd, new TelephonyManager.UssdResponseCallback() {
                @Override
                public void onReceiveUssdResponse(TelephonyManager telephonyManager, String request, CharSequence response) {
                    super.onReceiveUssdResponse(telephonyManager, request, response);
                    final MethodChannel.Result _result = mResult;
                    if (_result == null) {
                        return;
                    }
                    mResult = null;
                    Log.d("ussd", "Success with response : " + response);
                    _result.success(response);
                }

                @Override
                public void onReceiveUssdResponseFailed(TelephonyManager telephonyManager, String request, int failureCode) {
                    super.onReceiveUssdResponseFailed(telephonyManager, request, failureCode);
                    final MethodChannel.Result _result = mResult;
                    if (_result == null) {
                        return;
                    }
                    mResult = null;
                    Log.e("ussd","failed with code " + Integer.toString(failureCode));
                    _result.error("dial", "Failed to send ussd request. Error code: ", Integer.toString(failureCode));
                }
            }, handler);
        } else {
            result.error("dial", "Failed to send ussd request. Error code: Unsupported ", -1);
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
