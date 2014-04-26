/*
 * Copyright (C) 2014 Thalmic Labs Inc.
 * Confidential and not for redistribution. See LICENSE.txt.
 */

package com.thalmic.android.sample.helloworld;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import orbotix.robot.base.CollisionDetectedAsyncData;
import orbotix.robot.base.Robot;
import orbotix.robot.base.RobotProvider;
import orbotix.robot.sensor.DeviceSensorsData;
import orbotix.sphero.CollisionListener;
import orbotix.sphero.ConnectionListener;
import orbotix.sphero.DiscoveryListener;
import orbotix.sphero.PersistentOptionFlags;
import orbotix.sphero.SensorControl;
import orbotix.sphero.SensorFlag;
import orbotix.sphero.SensorListener;
import orbotix.sphero.Sphero;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.gesture.GestureOverlayView;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.TextView;
import android.widget.Toast;

import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.Quaternion;
import com.thalmic.myo.scanner.ScanActivity;
import com.thalmic.myo.trainer.TrainActivity;

@SuppressLint("NewApi")
public class HelloWorldActivity extends Activity {

	// This code will be returned in onActivityResult() when the enable
	// Bluetooth activity exits.
	private static final int REQUEST_ENABLE_BT = 1;

	private TextView mTextView;

	public static final String TAG = "OBX-HelloWorld";

	/** The Sphero Robot */
	private Sphero mRobot;

	private boolean connecting = false;
	private boolean blinking = false;

	private BluetoothAdapter mBluetoothAdapter;

	private GestureOverlayView gestureOverlayView;

	// Classes that inherit from AbstractDeviceListener can be used to receive
	// events from Myo devices.
	// If you do not override an event, the default behavior is to do nothing.
	private DeviceListener mListener = new AbstractDeviceListener() {

		// onConnect() is called whenever a Myo has been connected.
		@Override
		public void onConnect(Myo myo, long timestamp) {
			// Set the text color of the text view to cyan when a Myo connects.
			mTextView.setTextColor(Color.CYAN);
		}

		// onDisconnect() is called whenever a Myo has been disconnected.
		@Override
		public void onDisconnect(Myo myo, long timestamp) {
			// Set the text color of the text view to red when a Myo
			// disconnects.
			mTextView.setTextColor(Color.RED);
		}

		// onOrientationData() is called whenever a Myo provides its current
		// orientation,
		// represented as a quaternion.
		@Override
		public void onOrientationData(Myo myo, long timestamp,
				Quaternion rotation) {
			// Calculate Euler angles (roll, pitch, and yaw) from the
			// quaternion.
			float rotationZ = (float) Math.toDegrees(Quaternion.roll(rotation));
			float rotationX = (float) Math
					.toDegrees(Quaternion.pitch(rotation));
			float rotationY = (float) Math.toDegrees(Quaternion.yaw(rotation));

			// Next, we apply a rotation to the text view using the roll, pitch,
			// and yaw.
			mTextView.setRotation(-rotationZ);
			mTextView.setRotationX(-rotationX);
			mTextView.setRotationY(rotationY);
		}

		// onPose() is called whenever a Myo provides a new pose.
		@Override
		public void onPose(Myo myo, long timestamp, Pose pose) {
			// Handle the cases of the Pose.Type enumeration, and change the
			// text of the text view
			// based on the pose we receive.
			switch (pose.getType()) {
			case NONE:
				mTextView.setText(getString(R.string.hello_world));
				break;
			case FIST:
				// mTextView.setText(getString(R.string.myosdk__pose_fist));
				break;
			case WAVE_IN:
				// mTextView.setText(getString(R.string.myosdk__pose_wavein));
				break;
			case WAVE_OUT:
				// mTextView.setText(getString(R.string.myosdk__pose_waveout));
				break;
			case FINGERS_SPREAD:
				// mTextView.setText(getString(R.string.myosdk__pose_fingersspread));
				break;
			case TWIST_IN:
				// mTextView.setText(getString(R.string.myosdk__pose_twistin));
				break;
			}
		}
	};

	// Create a BroadcastReceiver for ACTION_FOUND
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			// When discovery finds a device
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				// Get the BluetoothDevice object from the Intent
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if (device.getAddress().equals("68:86:E7:03:BE:1D")) {
					Log.d("MyoRacer", "Sphero found. Now connect to Myo");
					mRobot = new Sphero(device);

					RobotProvider.getDefaultProvider().connect(mRobot);
					RobotProvider.getDefaultProvider().control(mRobot);
					mRobot.setConnected(true);

					mRobot.setColor(0, 0, 255);
					// connected();

					mBluetoothAdapter.cancelDiscovery();

					Hub hub = Hub.getInstance();
					if (!hub.init(HelloWorldActivity.this)) {
						// We can't do anything with the Myo device if the Hub
						// can't be
						// initialized, so exit.
						Toast.makeText(getApplicationContext(),
								"Couldn't initialize Hub", Toast.LENGTH_SHORT)
								.show();
						finish();
						return;
					}

					// Next, register for DeviceListener callbacks.
					hub.addListener(mListener);
					unregisterReceiver(mReceiver);
				}
				// Add the name and address to an array adapter to show in a
				// ListView
				System.out.println(device.getName() + "\n"
						+ device.getAddress());
			} else if (action
					.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
				mBluetoothAdapter.startDiscovery();
			}

		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_hello_world);

		mTextView = (TextView) findViewById(R.id.text);

		// First, we initialize the Hub singleton.

		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(mReceiver, filter);

		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		this.registerReceiver(mReceiver, filter);

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter.isDiscovering()) {
			mBluetoothAdapter.cancelDiscovery();
		}
		// Request discover from BluetoothAdapter
		mBluetoothAdapter.startDiscovery();

		gestureOverlayView = (GestureOverlayView) findViewById(R.id.gestureOverlayView1);
		gestureOverlayView.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_MOVE: {
					if (mRobot != null && mRobot.isConnected()) {
						float x = event.getX() - v.getHeight() / 2;
						x /= v.getHeight() / 2;
						float y = event.getY() - v.getWidth() / 2;
						y /= v.getWidth() / 2;
						float heading = (float) (Math.atan2(x, y) / Math.PI * 180);
						System.err.println(x + ":" + y+"="+heading);
						mRobot.drive(heading, 0.5f);
					}
					break;
				}
				default:
					break;
				}
				return false;
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();

		// If Bluetooth is not enabled, request to turn it on.
		if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
			Intent enableBtIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
		// ==================================== Sphero
		// startSphero();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// We don't want any callbacks when the Activity is gone, so unregister
		// the listener.
		Hub.getInstance().removeListener(mListener);

		if (isFinishing()) {
			// The Activity is finishing, so shutdown the Hub. This will
			// disconnect from the Myo.
			Hub.getInstance().shutdown();
		}

		unregisterReceiver(mReceiver);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// User chose not to enable Bluetooth, so exit.
		if (requestCode == REQUEST_ENABLE_BT
				&& resultCode == Activity.RESULT_CANCELED) {
			finish();
			return;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (R.id.action_train == id) {
			onTrainActionSelected();
			return true;
		} else if (R.id.action_scan == id) {
			onScanActionSelected();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void onTrainActionSelected() {
		// Check if there are any connected Myos. Don't do anything if there
		// aren't any.
		ArrayList<Myo> connectedDevices = Hub.getInstance()
				.getConnectedDevices();
		if (connectedDevices.isEmpty()) {
			return;
		}

		// Get the Myo to train. In this case, we will train the first Myo in
		// the Hub's
		// connected devices list.
		Myo myo = connectedDevices.get(0);

		// Launch the TrainActivity for the specified Myo.
		Intent intent = new Intent(this, TrainActivity.class);
		intent.putExtra(TrainActivity.EXTRA_ADDRESS, myo.getMacAddress());
		startActivity(intent);
	}

	private void onScanActionSelected() {
		// Launch the ScanActivity to scan for Myos to connect to.
		Intent intent = new Intent(this, ScanActivity.class);
		startActivity(intent);
	}

	// ----------------------------_Sphero

	private void stopBlink() {
		blinking = false;
	}

	/**
	 * Causes the robot to blink once every second.
	 * 
	 * @param lit
	 */
	private void blink(final boolean lit) {
		if (mRobot == null) {
			blinking = false;
			return;
		}

		// If not lit, send command to show blue light, or else, send command to
		// show no light
		if (lit) {
			mRobot.setColor(0, 0, 0);

		} else {
			mRobot.setColor(0, 255, 0);
		}

		if (blinking) {
			// Send delayed message on a handler to run blink again
			final Handler handler = new Handler();
			handler.postDelayed(new Runnable() {
				public void run() {
					blink(!lit);
				}
			}, 2000);
		}
	}

	private void connected() {
		Log.d(TAG, "Connected On Thread: " + Thread.currentThread().getName());
		Log.d(TAG, "Connected: " + mRobot);
		Toast.makeText(HelloWorldActivity.this,
				mRobot.getName() + " Connected", Toast.LENGTH_LONG).show();

		final SensorControl control = mRobot.getSensorControl();
		control.addSensorListener(new SensorListener() {
			@Override
			public void sensorUpdated(DeviceSensorsData sensorDataArray) {
				Log.d(TAG, sensorDataArray.toString());
			}
		}, SensorFlag.ACCELEROMETER_NORMALIZED, SensorFlag.GYRO_NORMALIZED);

		control.setRate(1);
		mRobot.enableStabilization(false);
		mRobot.drive(90, 0);
		mRobot.setBackLEDBrightness(.5f);

		mRobot.getCollisionControl().startDetection(255, 255, 255, 255, 255);
		mRobot.getCollisionControl().addCollisionListener(
				new CollisionListener() {
					public void collisionDetected(
							CollisionDetectedAsyncData collisionData) {
						Log.d(TAG, collisionData.toString());
					}
				});

		HelloWorldActivity.this.blink(false); // Blink the robot's LED

		// boolean preventSleepInCharger = mRobot.getConfiguration()
		// .isPersistentFlagEnabled(
		// PersistentOptionFlags.PreventSleepInCharger);
		// Log.d(TAG, "Prevent Sleep in charger = " + preventSleepInCharger);
		// Log.d(TAG,
		// "VectorDrive = "
		// + mRobot.getConfiguration().isPersistentFlagEnabled(
		// PersistentOptionFlags.EnableVectorDrive));
		//
		// mRobot.getConfiguration().setPersistentFlag(
		// PersistentOptionFlags.PreventSleepInCharger, false);
		// mRobot.getConfiguration().setPersistentFlag(
		// PersistentOptionFlags.EnableVectorDrive, true);
		//
		// Log.d(TAG,
		// "VectorDrive = "
		// + mRobot.getConfiguration().isPersistentFlagEnabled(
		// PersistentOptionFlags.EnableVectorDrive));
		// Log.v(TAG, mRobot.getConfiguration().toString());

	}

	private void startSphero() {
		RobotProvider.getDefaultProvider().addConnectionListener(
				new ConnectionListener() {
					@Override
					public void onConnected(Robot robot) {
						mRobot = (Sphero) robot;
						connected();
					}

					@Override
					public void onConnectionFailed(Robot sphero) {
						Log.d(TAG, "Connection Failed: " + sphero);
						Toast.makeText(HelloWorldActivity.this,
								"Sphero Connection Failed", Toast.LENGTH_SHORT)
								.show();
					}

					@Override
					public void onDisconnected(Robot robot) {
						Log.d(TAG, "Disconnected: " + robot);
						Toast.makeText(HelloWorldActivity.this,
								"Sphero Disconnected", Toast.LENGTH_SHORT)
								.show();
						stopBlink();
						mRobot = null;
					}
				});

	}
}
