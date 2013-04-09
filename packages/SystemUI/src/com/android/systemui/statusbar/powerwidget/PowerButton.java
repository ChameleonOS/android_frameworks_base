package com.android.systemui.statusbar.powerwidget;

import android.app.ActivityManagerNative;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.PorterDuff.Mode;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.Vibrator;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.R;

import java.util.ArrayList;
import java.util.List;

public abstract class PowerButton {
    public static final String TAG = "PowerButton";

    public static final int STATE_ENABLED = 1;
    public static final int STATE_DISABLED = 2;
    public static final int STATE_TURNING_ON = 3;
    public static final int STATE_TURNING_OFF = 4;
    public static final int STATE_INTERMEDIATE = 5;
    public static final int STATE_UNKNOWN = 6;

    public static final String BUTTON_WIFI = "toggleWifi";
    public static final String BUTTON_GPS = "toggleGPS";
    public static final String BUTTON_BLUETOOTH = "toggleBluetooth";
    public static final String BUTTON_BRIGHTNESS = "toggleBrightness";
    public static final String BUTTON_SOUND = "toggleSound";
    public static final String BUTTON_SYNC = "toggleSync";
    public static final String BUTTON_WIFIAP = "toggleWifiAp";
    public static final String BUTTON_SCREENTIMEOUT = "toggleScreenTimeout";
    public static final String BUTTON_MOBILEDATA = "toggleMobileData";
    public static final String BUTTON_LOCKSCREEN = "toggleLockScreen";
    public static final String BUTTON_NETWORKMODE = "toggleNetworkMode";
    public static final String BUTTON_AUTOROTATE = "toggleAutoRotate";
    public static final String BUTTON_AIRPLANE = "toggleAirplane";
    public static final String BUTTON_FLASHLIGHT = "toggleFlashlight";
    public static final String BUTTON_SLEEP = "toggleSleepMode";
    public static final String BUTTON_MEDIA_PLAY_PAUSE = "toggleMediaPlayPause";
    public static final String BUTTON_MEDIA_PREVIOUS = "toggleMediaPrevious";
    public static final String BUTTON_MEDIA_NEXT = "toggleMediaNext";
    public static final String BUTTON_LTE = "toggleLte";
    public static final String BUTTON_WIMAX = "toggleWimax";
    public static final String BUTTON_MEDIA_SCAN = "toggleMediaScan";
    public static final String BUTTON_UNKNOWN = "unknown";
    private static final String SEPARATOR = "OV=I=XseparatorX=I=VO";
    private static final Mode MASK_MODE = Mode.SCREEN;

    public static final String NAME_BUTTON_WIFI = "WiFi";
    public static final String NAME_BUTTON_GPS = "GPS";
    public static final String NAME_BUTTON_BLUETOOTH = "Bluetooth";
    public static final String NAME_BUTTON_BRIGHTNESS = "Brightness";
    public static final String NAME_BUTTON_SOUND = "Sound";
    public static final String NAME_BUTTON_SYNC = "Sync";
    public static final String NAME_BUTTON_WIFIAP = "WiFi Ap";
    public static final String NAME_BUTTON_SCREENTIMEOUT = "Timeout";
    public static final String NAME_BUTTON_MOBILEDATA = "Data";
    public static final String NAME_BUTTON_LOCKSCREEN = "Lock screen";
    public static final String NAME_BUTTON_NETWORKMODE = "Network";
    public static final String NAME_BUTTON_AUTOROTATE = "Rotate";
    public static final String NAME_BUTTON_AIRPLANE = "Airplane";
    public static final String NAME_BUTTON_FLASHLIGHT = "Torch";
    public static final String NAME_BUTTON_SLEEP = "Sleep";
    public static final String NAME_BUTTON_MEDIA_PLAY_PAUSE = "Play";
    public static final String NAME_BUTTON_MEDIA_PREVIOUS = "Previous";
    public static final String NAME_BUTTON_MEDIA_NEXT = "Next";
    public static final String NAME_BUTTON_LTE = "Lte";
    public static final String NAME_BUTTON_WIMAX = "WiMAX";
    public static final String NAME_BUTTON_MEDIA_SCAN = "Scan media";
    public static final String NAME_BUTTON_UNKNOWN = "unknown";

    protected int mIcon;
    protected int mState;
    protected View mView;
    protected String mType = BUTTON_UNKNOWN;

    protected TextView mLabelView = null;
    
    protected boolean mShowLabel = false;

    private ImageView mIconView;

    protected static int sLabelColorOff = 0;
    protected static int sLabelColorOn = 0;

    private View.OnClickListener mExternalClickListener;
    private View.OnLongClickListener mExternalLongClickListener;

    protected boolean mHapticFeedback;
    protected Vibrator mVibrator;
    private long[] mClickPattern;
    private long[] mLongClickPattern;

    // we use this to ensure we update our views on the UI thread
    private Handler mViewUpdateHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (mIconView != null) {
                mIconView.setImageResource(mIcon);
                // check if the drawable is an AnimationDrawable and start
                // the animation if it is.  This allows for animated toggles.
                Drawable d = mIconView.getDrawable();
                if (d != null && d instanceof AnimationDrawable) {
                    ((AnimationDrawable)d).start();
                }
                if (mShowLabel) {
                    switch (mState) {
                        case STATE_ENABLED:
                        case STATE_TURNING_ON:
                            mLabelView.setTextColor(sLabelColorOn);
                            break;
                        case STATE_DISABLED:
                        case STATE_TURNING_OFF:
                        case STATE_INTERMEDIATE:
                        default:
                            mLabelView.setTextColor(sLabelColorOff);
                            break;
                    }
                    mLabelView.setVisibility(View.VISIBLE);
                } else
                    mLabelView.setVisibility(View.GONE);
            }
        }
    };

    protected abstract void updateState(Context context);
    protected abstract void toggleState(Context context);
    protected abstract boolean handleLongClick(Context context);

    protected void update(Context context) {
        updateState(context);
        updateView();
    }

    public String[] parseStoredValue(CharSequence val) {
        if (TextUtils.isEmpty(val)) {
          return null;
        } else {
          return val.toString().split(SEPARATOR);
        }
    }

    protected void onReceive(Context context, Intent intent) {
        // do nothing as a standard, override this if the button needs to respond
        // to broadcast events from the StatusBarService broadcast receiver
    }

    protected void onChangeUri(ContentResolver resolver, Uri uri) {
        // do nothing as a standard, override this if the button needs to respond
        // to a changed setting
    }

    /* package */ void setHapticFeedback(boolean enabled,
            long[] clickPattern, long[] longClickPattern) {
        mHapticFeedback = enabled;
        mClickPattern = clickPattern;
        mLongClickPattern = longClickPattern;
    }

    public void setLabel(String label) {
        mShowLabel = label != null;
        mLabelView.setText(label);
    }

    protected IntentFilter getBroadcastIntentFilter() {
        return new IntentFilter();
    }

    protected List<Uri> getObservedUris() {
        return new ArrayList<Uri>();
    }

    protected void setupButton(View view) {
        mView = view;
        if (mView != null) {
            mView.setTag(mType);
            mView.setOnClickListener(mClickListener);
            mView.setOnLongClickListener(mLongClickListener);

            mIconView = (ImageView) mView.findViewById(R.id.toggle_button_image);
            mLabelView = (TextView) mView.findViewById(R.id.toggle_button_label);
            mVibrator = (Vibrator) mView.getContext().getSystemService(Context.VIBRATOR_SERVICE);
            Resources res = mView.getContext().getResources();

            sLabelColorOff = res.getColor(R.color.toggles_button_label_off);
            sLabelColorOn = res.getColor(R.color.toggles_button_label_on);
        } else {
            mIconView = null;
        }
    }

    protected void updateView() {
        mViewUpdateHandler.sendEmptyMessage(0);
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mHapticFeedback && mClickPattern != null) {
                if (mClickPattern.length == 1) {
                    // One-shot vibration
                    mVibrator.vibrate(mClickPattern[0]);
                } else {
                    // Pattern vibration
                    mVibrator.vibrate(mClickPattern, -1);
                }
            }
            toggleState(v.getContext());
            update(v.getContext());

            if (mExternalClickListener != null) {
                mExternalClickListener.onClick(v);
            }
        }
    };

    private View.OnLongClickListener mLongClickListener = new View.OnLongClickListener() {
        public boolean onLongClick(View v) {
            boolean result = handleLongClick(v.getContext());

            if (result && mHapticFeedback && mLongClickPattern != null) {
                mVibrator.vibrate(mLongClickPattern, -1);
            }

            try {
                ActivityManagerNative.getDefault().dismissKeyguardOnNextActivity();
            } catch (RemoteException e) {
            }

            if (result && mExternalLongClickListener != null) {
                mExternalLongClickListener.onLongClick(v);
            }
            return result;
        }
    };

    void setExternalClickListener(View.OnClickListener listener) {
        mExternalClickListener = listener;
    }

    void setExternalLongClickListener(View.OnLongClickListener listener) {
        mExternalLongClickListener = listener;
    }

    protected SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences("PowerButton-" + mType, Context.MODE_PRIVATE);
    }
}
