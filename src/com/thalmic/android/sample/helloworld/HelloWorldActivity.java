/*
 * Copyright (C) 2014 Thalmic Labs Inc.
 * Confidential and not for redistribution. See LICENSE.txt.
 */

package com.thalmic.android.sample.helloworld;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import orbotix.macro.MacroObject;
import orbotix.robot.base.CollisionDetectedAsyncData;
import orbotix.robot.base.ConfigureLocatorCommand;
import orbotix.robot.base.Robot;
import orbotix.robot.base.RobotProvider;
import orbotix.robot.sensor.DeviceSensorsData;
import orbotix.robot.sensor.LocatorData;
import orbotix.sphero.CollisionListener;
import orbotix.sphero.ConnectionListener;
import orbotix.sphero.DiscoveryListener;
import orbotix.sphero.LocatorListener;
import orbotix.sphero.PersistentOptionFlags;
import orbotix.sphero.SensorControl;
import orbotix.sphero.SensorFlag;
import orbotix.sphero.SensorListener;
import orbotix.sphero.Sphero;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
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
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Myo.VibrationType;
import com.thalmic.myo.Pose;
import com.thalmic.myo.Quaternion;
import com.thalmic.myo.Vector3;
import com.thalmic.myo.scanner.ScanActivity;
import com.thalmic.myo.trainer.TrainActivity;

@SuppressLint("NewApi")
public class HelloWorldActivity extends Activity {

	// This code will be returned in onActivityResult() when the enable
	// Bluetooth activity exits.
	private static final int REQUEST_ENABLE_BT = 1;

	private TextView X;
	private TextView Y;
	private TextView Z;

	private List<MyMyo> myos = new ArrayList<MyMyo>(4);

	private float rotationX;
	private float rotationY;
	private float rotationZ;
	private float ref_rotationZ;
	private float ref_rotationX;
	private float ref_rotationY;

	private static int maxRadius = 150;
	private static int radius[] = { 30, 60, 150 };
	private static int step = 0;

	private static float sphero_x = 0;
	private static float sphero_y = 0;

	private float rotx = 0;
	private float rotz = 0;
	private float heading = 0;

	private boolean playingGame = false;

	private int seekBarValue = 50;

	public static final String TAG = "OBX-HelloWorld";

	/** The Sphero Robot */
	private Sphero mRobot;

	private boolean connecting = false;
	private boolean blinking = false;
	private boolean ride = false;

	private Myo connectedMyo;

	private GestureOverlayView gestureOverlayView;

	// Classes that inherit from AbstractDeviceListener can be used to receive
	// events from Myo devices.
	// If you do not override an event, the default behavior is to do nothing.
	private DeviceListener mListener = new AbstractDeviceListener() {

		// onConnect() is called whenever a Myo has been connected.
		@Override
		public void onConnect(Myo myo, long timestamp) {
			// Set the text color of the text view to cyan when a Myo connects.
			// mTextView.setTextColor(Color.CYAN);
			connectedMyo = myo;
			myos.add(new MyMyo(myo));
			myo.vibrate(VibrationType.LONG);
		}

		// onDisconnect() is called whenever a Myo has been disconnected.
		@Override
		public void onDisconnect(Myo myo, long timestamp) {
			// Set the text color of the text view to red when a Myo
			// disconnects.
			// mTextView.setTextColor(Color.RED);
			myos.remove(myo);
			connectedMyo = null;
		}

		// onOrientationData() is called whenever a Myo provides its current
		// orientation,
		// represented as a quaternion.
		@Override
		public void onOrientationData(Myo myo, long timestamp,
				Quaternion rotation) {
			// Calculate Euler angles (roll, pitch, and yaw) from the
			// quaternion.

			MyMyo selected = myos.get(myos.indexOf(new MyMyo(myo)));

			rotationZ = (float) Math.toDegrees(Quaternion.roll(rotation));
			rotationX = (float) Math.toDegrees(Quaternion.pitch(rotation));
			rotationY = (float) Math.toDegrees(Quaternion.yaw(rotation));

			selected.setRotationX(rotationX);
			selected.setRotationY(rotationY);
			selected.setRotationZ(rotationZ);

			if (playingGame) {
				selected.addSum();
			}

			if (myos.size() > 1)
				return;

			float heading1 = 0;
			float speed1 = 0;
			if (ride) {
				// if (ref_rotationZ < rotationZ) {
				// float zero = ref_rotationZ;
				// float max = left_rotationZ - zero;
				// heading1 = rotationZ - zero;
				// heading1 /= max;
				// heading += 1;
				// heading *= -heading1;
				// } else {
				// float zero = ref_rotationZ;
				// float max = right_rotationZ - zero;
				//
				//
				// }
				speed1 = (ref_rotationX - rotationX) / 100;
				if (speed1 != Float.NaN) {
					ref_rotationX = rotationX;
					rotx -= speed1;
				}
				// speed += rotx;
				// heading = heading % 360;

				heading1 = (ref_rotationZ - rotationZ) / 50;
				ref_rotationZ = rotationZ;
				rotz += heading1;
				heading += rotz;
				heading = heading % 360;
				if (heading < 0)
					heading = 360 + heading;
				if (rotx < 0)
					rotx = 0;
				if (rotx > 1)
					rotx = 1;
				if (mRobot != null && ride)
					mRobot.drive(heading, rotx);
				else if (mRobot != null)
					mRobot.drive(0.0f, 0.0f);
			}

			// X.setText(String.format("x: %.3f / %.3f", rotationX, rotx));
			// Y.setText(String.format("y: %.3f", rotationY));
			// Z.setText(String.format("z: %.3f / %.3f / %.3f", rotationZ, rotz,
			// heading));
			// Next, we apply a rotation to the text view using the roll, pitch,
			// and yaw.
			// mTextView.setRotation(-rotationZ);
			// mTextView.setRotationX(-rotationX);
			// mTextView.setRotationY(rotationY);
		}

		// onPose() is called whenever a Myo provides a new pose.
		@Override
		public void onPose(Myo myo, long timestamp, Pose pose) {
			// Handle the cases of the Pose.Type enumeration, and change the
			// text of the text view

			// based on the pose we receive.
			switch (pose.getType()) {
			case NONE:
				// mTextView.setText(getString(R.string.hello_world));
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
				if (ride)
					stop();
				else
					start();
				break;
			case TWIST_IN:
				// mTextView.setText(getString(R.string.myosdk__pose_twistin));

				break;
			}
		}

		@Override
		public void onAccelerometerData(Myo myo, long timestamp, Vector3 accel) {
			// TODO Auto-generated method stub
			super.onAccelerometerData(myo, timestamp, accel);
		}

		@Override
		public void onGyroscopeData(Myo myo, long timestamp, Vector3 gyro) {
			// TODO Auto-generated method stub
			super.onGyroscopeData(myo, timestamp, gyro);
		}

		@Override
		public void onPair(Myo myo, long timestamp) {
			// TODO Auto-generated method stub
			super.onPair(myo, timestamp);
		}

		@Override
		public void onRssi(Myo myo, long timestamp, int rssi) {
			// TODO Auto-generated method stub
			super.onRssi(myo, timestamp, rssi);
		}

	};

	private void start() {
		gestureOverlayView.setBackgroundColor(Color.GREEN);
		ride = true;
		rotx = 0;
		rotz = 0;
		heading = 0;
		ref_rotationZ = rotationZ;
		ref_rotationX = rotationX;
		ref_rotationY = rotationY;
	}

	private void stop() {
		ride = false;
		gestureOverlayView.setBackgroundColor(Color.RED);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_hello_world);

		// mTextView = (TextView) findViewById(R.id.text);
		X = (TextView) findViewById(R.id.x);
		Y = (TextView) findViewById(R.id.y);
		Z = (TextView) findViewById(R.id.z);

		Button start = (Button) findViewById(R.id.start);
		start.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				start();
			}
		});
		Button stop = (Button) findViewById(R.id.stop);
		stop.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				stop();
			}
		});

		final Button moveStop = (Button) findViewById(R.id.move_stop);
		moveStop.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mRobot != null) {
					mRobot.drive(0f, 0f);
				}

			}
		});

		final Button testGame = (Button) findViewById(R.id.test_game);
		testGame.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				new Thread(new Runnable() {

					@Override
					public void run() {
						ConfigureLocatorCommand.sendCommand(mRobot, 0, 0, 0, 0);
						try {
							// norm_x = sphero_x;
							// norm_y = sphero_y;
							mRobot.setColor(0, 255, 0);
							Thread.sleep(1000);
							mRobot.setColor(0, 0, 0);
							Thread.sleep(1000);
							mRobot.setColor(0, 255, 0);
							Thread.sleep(1000);
							mRobot.setColor(0, 0, 0);
							Thread.sleep(1000);
							mRobot.setColor(0, 255, 0);
							Thread.sleep(1000);
							mRobot.setColor(255, 0, 0);
							step++;
							testHeadToXY(0, radius[0]);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}).start();

			}
		});

		final Button game = (Button) findViewById(R.id.game);
		game.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// if (myos.size() < 2) {
				// Toast.makeText(HelloWorldActivity.this,
				// "Pripojte alespon 2 zarizeni myo",
				// Toast.LENGTH_LONG).show();
				// return;
				// }
				if (playingGame) {
					game.setText("Let's play a game");
					// scheduler.shutdown();
					// gameCheck.cancel(true);
					playingGame = false;
				} else {
					ConfigureLocatorCommand.sendCommand(mRobot, 0, 0, 0, 0);
					mRobot.drive(0f, 0f);
					playingGame = true;
					rotx = 0;
					rotz = 0;
					heading = 0;
					for (MyMyo myo : myos) {
						myo.posToRef();
					}
					game.setText("Stop game");
					new Thread(new Runnable() {

						@Override
						public void run() {
							try {

								int i = 0;
								for (MyMyo myo : myos) {
									myo.getMyo().vibrate(VibrationType.LONG);
									switch (i) {
									case 0: {
										myo.setPos_x(0);
										myo.setPos_y(maxRadius);
										myo.setColor(255, 0, 0);
										mRobot.setColor(255, 0, 0);
										break;
									}
									case 1: {
										myo.setPos_x(0);
										myo.setPos_y(-maxRadius);
										myo.setColor(0, 0, 255);
										mRobot.setColor(0, 0, 255);
										break;
									}
									case 2: {
										myo.setPos_x(maxRadius);
										myo.setPos_y(0);
										break;
									}
									case 3: {
										myo.setPos_x(-maxRadius);
										myo.setPos_y(0);
										break;
									}
									}
									Thread.sleep(2000);
									i++;
								}
								mRobot.setColor(0, 255, 0);
								Thread.sleep(1000);
								mRobot.setColor(0, 0, 0);
								Thread.sleep(1000);
								mRobot.setColor(0, 255, 0);
								Thread.sleep(1000);
								mRobot.setColor(0, 0, 0);
								Thread.sleep(1000);
								mRobot.setColor(0, 255, 0);
								Thread.sleep(1000);
								// mRobot.setColor(255, 0, 0);
								while (Math.abs(sphero_y) < 100) {
									for (MyMyo myo : myos) {
										myo.posToRef();
										myo.clearSum();
									}
									Thread.sleep(500);
									Collections.sort(myos);
									MyMyo myo = myos.get(0);
									myo.getMyo().vibrate(VibrationType.MEDIUM);
									int[] color = myo.getColor();
									mRobot.setColor(color[0], color[1],
											color[2]);
									if (myo.getPos_y() < 0) {
										mRobot.drive(180, 0.1f);
									} else if (myo.getPos_y() > 0) {
										mRobot.drive(0, 0.1f);
									}

								}
								mRobot.drive(0f, 0f);
								mRobot.setColor(255, 255, 0);

								runOnUiThread(new Runnable() {

									@Override
									public void run() {
										game.setText("Let's play a game");
										playingGame = false;

									}
								});
								// Toast.makeText(
								// getApplicationContext(),
								// myos.get(0).getMyo().getMacAddress()
								// + " LOST", Toast.LENGTH_LONG)
								// .show();
								// step++;
								// headToXY((myo.getPos_x() / radius.length)
								// * step,
								// (myo.getPos_y() / radius.length) * step);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}).start();

				}
			}
		});

		SeekBar bar = (SeekBar) findViewById(R.id.seekBar1);
		bar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				seekBarValue = seekBar.getProgress();
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				// TODO Auto-generated method stub

			}
		});

		// First, we initialize the Hub singleton.
		Hub hub = Hub.getInstance();
		if (!hub.init(HelloWorldActivity.this)) {
			// We can't do anything with the Myo device if the Hub
			// can't be
			// initialized, so exit.
			Toast.makeText(getApplicationContext(), "Couldn't initialize Hub",
					Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		// Next, register for DeviceListener callbacks.
		hub.addListener(mListener);
		gestureOverlayView = (GestureOverlayView) findViewById(R.id.gestureOverlayView1);
		gestureOverlayView.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_MOVE: {

					float x = event.getX() - v.getHeight() / 2;
					x /= v.getHeight() / 2;
					// x *= -1;
					float y = event.getY() - v.getWidth() / 2;
					y /= v.getWidth() / 2;
					y *= -1;
					float heading = (float) (Math.atan2(x, y) / Math.PI * 180);
					if (heading < 0)
						heading = 360 + heading;
					// System.err.println(x + ":" + y + "=" + heading);
					if (mRobot != null) {
						mRobot.setColor(
								(int) (256 - (x / (event.getX() - v.getHeight() / 2)) * 256),
								(int) (256 - (y / (event.getY() - v.getWidth() / 2)) * 256),
								(int) (128 + (heading % 256) / 2));
						mRobot.drive(heading, (float) seekBarValue
								/ (float) 100.0);
					}
					break;
				}
				case MotionEvent.ACTION_UP:
					float x = 0;
					float y = 0;
					float heading = 0;
					System.err.println(x + ":" + y + "=" + heading);
					if (mRobot != null && mRobot.isConnected()) {
						mRobot.drive(heading, .0f);
					}
					break;
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
		if (mRobot != null) {
			mRobot.disconnect();
		}

		Hub.getInstance().removeListener(mListener);

		if (isFinishing()) {
			// The Activity is finishing, so shutdown the Hub. This will
			// disconnect from the Myo.
			Hub.getInstance().shutdown();
		}
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
		} else if (R.id.action_findSphero == id) {
			findSphero();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void findSphero() {

		RobotProvider.getDefaultProvider().addConnectionListener(
				new ConnectionListener() {

					@Override
					public void onDisconnected(Robot arg0) {
						// TODO Auto-generated method stub
						mRobot = null;
					}

					@Override
					public void onConnectionFailed(Robot arg0) {
						// TODO Auto-generated method stub

					}

					@Override
					public void onConnected(Robot arg0) {
						mRobot = (Sphero) arg0;

						// RobotProvider.getDefaultProvider().control(mRobot);
						// mRobot.enableStabilization(false);
						mRobot.setColor(0, 255, 0);
						connected();

						RobotProvider.getDefaultProvider().endDiscovery();

					}
				});

		RobotProvider.getDefaultProvider().addDiscoveryListener(
				new DiscoveryListener() {
					@Override
					public void onBluetoothDisabled() {
						Log.d(TAG, "Bluetooth Disabled");
						Toast.makeText(HelloWorldActivity.this,
								"Bluetooth Disabled", Toast.LENGTH_LONG).show();
					}

					@Override
					public void discoveryComplete(List<Sphero> spheros) {
						Log.d(TAG, "Found " + spheros.size() + " robots");
					}

					@Override
					public void onFound(List<Sphero> sphero) {
						Log.d(TAG, "Found: " + sphero);
						RobotProvider.getDefaultProvider().connect(
								sphero.iterator().next());
						RobotProvider.getDefaultProvider().endDiscovery();
					}
				});

		RobotProvider.getDefaultProvider().startDiscovery(
				getApplicationContext());

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
		// Toast.makeText(HelloWorldActivity.this,
		// mRobot.getName() + " Connected", Toast.LENGTH_LONG).show();

		LocatorListener mLocatorListener = new LocatorListener() {
			@Override
			public void onLocatorChanged(LocatorData locatorData) {
				// Do stuff with the locator data
				sphero_x = locatorData.getPositionX();
				sphero_y = locatorData.getPositionY();
			}
		};

		final SensorControl control = mRobot.getSensorControl();
		control.addLocatorListener(mLocatorListener);

		control.setRate(20);

		mRobot.enableStabilization(true);

		mRobot.getCollisionControl().addCollisionListener(
				new CollisionListener() {
					public void collisionDetected(
							CollisionDetectedAsyncData collisionData) {
						Log.i(TAG, collisionData.toString());
						if (connectedMyo != null)
							connectedMyo.vibrate(VibrationType.MEDIUM);
					}
				});
		mRobot.getCollisionControl().startDetection(125, 60, 125, 60, 100);// ball
																			// to
																			// wall
																			// Xt=200,
																			// Xsp=0,
																			// Yt=125,
																			// Ysp=0,
																			// deadTime=100
																			// mRobot.getConfiguration().setPersistentFlag(
		// PersistentOptionFlags.PreventSleepInCharger, false);
		// mRobot.getConfiguration().setPersistentFlag(
		// PersistentOptionFlags.EnableVectorDrive, true);

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

	private void testHeadToXY(final float x, final float y) {
		if (x == 0 && y == 0) {
			mRobot.drive(0f, 0f);
			return;
		}
		new Thread(new Runnable() {

			@Override
			public void run() {

				System.out.println("test");
				while (Math.abs(x - sphero_x) > 25
						|| Math.abs(y - sphero_y) > 25) {
					float heading = (float) Math
							.toDegrees((float) ((Math.PI / 2) - Math.atan2(y
									- sphero_y, x - sphero_x))) % 360;
					if (heading < 0)
						heading = (360 - heading) % 360;
					final float printh = heading;
					runOnUiThread(new Runnable() {

						@Override
						public void run() {

							X.setText(String
									.format("Heading: %.2f, x:%.1f, y=%.1f; toX:%.1f, toY:%.1f",
											printh, sphero_x, sphero_y, x, y));

						}
					});
					// mRobot.executeMacro(new MacroObject()))
					mRobot.drive(heading, (float) seekBarValue / 100.0f);
				}
				mRobot.drive(0f, 0f);

				if (step < radius.length) {
					testHeadToXY(0, radius[step++]);
				} else {
					testHeadToXY(0, 0);
				}
				if (x == 0 && y == 0) {
					mRobot.drive(0f, 0f);
				}
			}
		}).start();
	}

	private void headToXY(final float x, final float y) {
		new Thread(new Runnable() {

			@Override
			public void run() {

				System.out.println("test");
				while (Math.abs(x - sphero_x) > 25
						|| Math.abs(y - sphero_y) > 25) {
					float heading = (float) Math
							.toDegrees((float) ((Math.PI / 2) - Math.atan2(y
									- sphero_y, x - sphero_x))) % 360;
					if (heading < 0)
						heading = (360 - heading) % 360;
					final float printh = heading;
					runOnUiThread(new Runnable() {

						@Override
						public void run() {

							X.setText(String
									.format("Heading: %.2f, x:%.1f, y=%.1f; toX:%.1f, toY:%.1f",
											printh, sphero_x, sphero_y, x, y));

						}
					});
					mRobot.drive(heading, (float) seekBarValue / 100.0f);
				}
				mRobot.drive(0f, 0f);

				if (step < radius.length) {
					testHeadToXY(0, radius[step++]);
					for (MyMyo myo : myos) {
						myo.posToRef();
						myo.clearSum();
					}
					Collections.sort(myos);
					MyMyo myo = myos.get(0);
					headToXY((myo.getPos_x() % 1) * radius[step],
							(myo.getPos_y() % 1) * radius[step]);
				} else {
					testHeadToXY(0, 0);
				}
			}

		}).start();
	}
}
