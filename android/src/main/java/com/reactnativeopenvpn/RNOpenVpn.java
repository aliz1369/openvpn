package com.reactnativeopenvpn;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import de.blinkt.openvpn.api.IOpenVPNAPIService;
import de.blinkt.openvpn.api.IOpenVPNStatusCallback;

import de.blinkt.openvpn.api.APIVpnProfile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class RNOpenVpn extends ReactContextBaseJavaModule {

	private static final int MSG_UPDATE_STATE = 0;
	private static final int MSG_UPDATE_MY_IP = 1;
	private static final int START_PROFILE_BY_UUID = 3;
	private static final int ICS_OPEN_VPN_PERMISSION = 7;


	protected IOpenVPNAPIService mService=null;
	private Handler mHandler;
	private String config;
	private String username;
	private String password;
	private String latestState;
	private String latestMessage;

	void sendEvent(String eventName, @Nullable WritableMap params) {
		reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, params);
	}

	private APIVpnProfile addProfile(String config, @Nullable String username, @Nullable String password) throws RemoteException {
		try {
			mService.getProfiles().clear();
		} catch (Exception ignored) {

		}
		if(username != null) {
			return mService.addNewVPNProfileWithData("NewProfile", true, config, username, password, reactContext.getPackageName());
		} else {
			return mService.addNewVPNProfile("NewProfile", true, config, reactContext.getPackageName());
		}
	}

	private final ReactApplicationContext reactContext;

	public RNOpenVpn(ReactApplicationContext reactContext) {
		super(reactContext);
		this.reactContext = reactContext;
		createHandler();
		bindService(reactContext);
	}


	private final IOpenVPNStatusCallback mCallback = new IOpenVPNStatusCallback.Stub() {
		/**
		 * This is called by the remote service regularly to tell us about
		 * new values.  Note that IPC calls are dispatched through a thread
		 * pool running in each process, so the code executing here will
		 * NOT be running in our main thread like most other things -- so,
		 * to update the UI, we need to use a Handler to hop over there.
		 */

		@Override
		public void newStatus(String uuid, String state, String message, String level) {
			Message msg = Message.obtain(mHandler, MSG_UPDATE_STATE, state + "|" + message);
			msg.sendToTarget();
		}
	};


	/**
	 * Class for interacting with the main interface of the service.
	 */
	private final ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className,
		                               IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service.  We are communicating with our
			// service through an IDL interface, so get a client-side
			// representation of that from the raw service object.

			mService = IOpenVPNAPIService.Stub.asInterface(service);
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			mService = null;

		}
	};


	private void bindService(Context context) {
		Intent icsOpenVpnService = new Intent(IOpenVPNAPIService.class.getName());
		icsOpenVpnService.setPackage(context.getPackageName());
		context.bindService(icsOpenVpnService, mConnection, Context.BIND_AUTO_CREATE);
	}

	private void unbindService() {
		if(mConnection != null) {
			reactContext.unbindService(mConnection);
		}
	}

	private void prepareStartProfile() throws RemoteException {
		Intent requestPermission = mService.prepareVPNService();
		if(requestPermission == null) {
			onActivityResult(RNOpenVpn.START_PROFILE_BY_UUID, Activity.RESULT_OK);
		} else {
			// Have to call an external Activity since services cannot used onActivityResult
			reactContext.startActivityForResult(requestPermission, RNOpenVpn.START_PROFILE_BY_UUID, Bundle.EMPTY);
		}
	}

	private String getMyOwnIP() throws IOException {
		URLConnection client = (new URL("https://api.ipify.org/?format=text")).openConnection();
		BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
		return in.readLine();
	}

	private void startGetMyOwnIp(@Nullable final Promise promise) {
		new Thread() {
			@Override
			public void run() {
				try {
					String myip = getMyOwnIP();
					if(promise != null){
						promise.resolve(myip);
					}
					Message msg = Message.obtain(mHandler,MSG_UPDATE_MY_IP,myip);
					msg.sendToTarget();
				} catch (Exception e) {
					e.printStackTrace();
					if(promise != null){
						promise.reject(e);
					}
				}

			}
		}.start();
	}

	private void createHandler() {
		mHandler = new Handler(Looper.getMainLooper()){
			@Override
			public void handleMessage(Message msg) {
				WritableMap params = Arguments.createMap();
				if(msg.what == MSG_UPDATE_STATE) {
					String[] state = msg.obj.toString().split("\\|");
					latestState = state.length > 0 ? state[0] : "NOPROCESS";
					latestMessage = state.length > 1 ? state[1] : "";
					params.putString("state", latestState);
					params.putString("msg", latestMessage);
					sendEvent("STATE_CHANGED", params);
				} else if (msg.what == MSG_UPDATE_MY_IP) {
					params.putString("ip", msg.obj.toString());
					sendEvent("IP_CHANGED", params);
				}
			}
		};
	}

	public void onActivityResult(int requestCode, int resultCode) {
		if (resultCode == Activity.RESULT_OK) {
			if(requestCode==START_PROFILE_BY_UUID)
				try {
					String mStartUUID = addProfile(config, username, password).mUUID;
					mService.startProfile(mStartUUID);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			if (requestCode == ICS_OPEN_VPN_PERMISSION) {
				try {
					mService.registerStatusCallback(mCallback);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@NonNull
	@Override
	public String getName() {
		return "RNOpenVpn";
	}

	@Override
	public void onCatalystInstanceDestroy() {
		unbindService();
		super.onCatalystInstanceDestroy();
	}

	@ReactMethod
	public void prepare(Promise promise) {
		try {
			Intent i = mService.prepare(reactContext.getPackageName());
			if (i!=null) {
				reactContext.startActivityForResult(i, ICS_OPEN_VPN_PERMISSION, null);
			}
			onActivityResult(ICS_OPEN_VPN_PERMISSION, Activity.RESULT_OK);
			promise.resolve(null);
		} catch (RemoteException e) {
			e.printStackTrace();
			promise.reject(e);
		}
	}

	@ReactMethod
	public void connect(String config, @Nullable String username, @Nullable String password, Promise promise) {
		this.username = username;
		this.password = password;
		this.config = config;
		try {
			prepareStartProfile();
			promise.resolve(null);
		} catch (RemoteException e) {
			e.printStackTrace();
			promise.reject(e);
		}
	}

	@ReactMethod
	public void disconnect(Promise promise){
		try {
			mService.disconnect();
			promise.resolve(null);
		} catch (RemoteException e) {
			e.printStackTrace();
			promise.reject(e);
		}
	}

	@ReactMethod
	public void getMyIP(Promise promise)
	{
		startGetMyOwnIp(promise);
	}

	@ReactMethod
	public void getCurrentState(Promise promise)
	{
		WritableMap params = Arguments.createMap();
		params.putString("state", latestState);
		params.putString("msg", latestMessage);
		promise.resolve(params);
	}
}
