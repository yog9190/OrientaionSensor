/*
Copyright (c) [2014] Yogesh Premanand Pangam

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "OrientationSenor"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package com.customc.orientaionsensor;

import java.util.ArrayList;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

/**
 * This is a class which is used to sense the device orientation. This class
 * makes use of Sensor.TYPE_ACCELEROMETER and Sensor.TYPE_MAGNETIC_FIELD to
 * determine the device orientation.
 * 
 * @author Yogesh Pangam
 * */
public class OrientationSenor {
	ArrayList<SensorListener> listSensorListener = new ArrayList<OrientationSenor.SensorListener>();
	Context ctx;
	private static SensorManager sensorManager;
	private float azimuth, pitch, roll;
	Display display;
	float[] inR = new float[16];
	float[] outR = new float[16];
	float[] I = new float[16];
	float[] gravity = new float[3];
	float[] geomag = new float[3];
	float[] orientVals = new float[3];

	public OrientationSenor(Context ctx) {
		this.ctx = ctx;
		display = ((WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE))
				.getDefaultDisplay();
	}

	/**
	 * <b>interface SensorListener {<br/>
	 * public void onSensorChanged(SensorData objSensorData);<br/>
	 * };</b><br/>
	 * <br/>
	 * This interface needs to be implemented by a listener class. It contains
	 * callback methods to be invoked by the OrientationSensor whenever the
	 * device orientation changes.
	 */
	interface SensorListener {
		/**
		 * <b> public void onSensorChanged(SensorData objSensorData);<br/>
		 * </b>
		 * <br/>
		 * This method will be invoked by the OrientationSensor whenever the device
		 * orientation changes. 
		 * @param objSensorData : It will contain angle data of the
		 * device orientation.
		 */
		public void onSensorChanged(SensorData objSensorData);
	};

	/**
	 * This class represents the data regarding the three angles of device's
	 * current orientation.
	 */
	class SensorData {
		/** angle of rotation around the Z axis */
		final float angleAzimuth;
		/** angle of rotation around the X axis */
		final float anglePitch;
		/** angle of rotation around the Y axis */
		final float angleRoll;

		public SensorData(float angleAzimuth, float anglePitch, float angleRoll) {
			this.angleAzimuth = angleAzimuth;
			this.anglePitch = anglePitch;
			this.angleRoll = angleRoll;
		}
	};

	/**
	 * <b>public void unregister()</b><br/>
	 * <br/>
	 * Adds a listener to listen the orientation changes.
	 */
	public void addSensorListener(SensorListener objSensorListener) {
		listSensorListener.add(objSensorListener);
	}

	private SensorEventListener mySensorEventListener = new SensorEventListener() {
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}

		public float[] filter(float[] newValues, float[] previousValues,
				float alpha) {
			for (int i = 0; i < newValues.length; i++) {
				previousValues[i] = previousValues[i] + alpha
						* (newValues[i] - previousValues[i]);
			}
			return previousValues;
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			boolean ready = false;
			if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
				// System.arraycopy(event.values, 0, gravity, 0, 3);
				gravity = filter(event.values.clone(), gravity, 0.2f);
				if (geomag[0] != 0)
					ready = true;
			}
			if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
				// System.arraycopy(event.values, 0, geomag, 0, 3);
				geomag = filter(event.values.clone(), geomag, 0.5f);
				if (gravity[2] != 0)
					ready = true;
			}

			if (!ready)
				return;
			boolean success = SensorManager.getRotationMatrix(inR, I, gravity,
					geomag);
			if (success) {
				remapCordinateSystem();
				SensorManager.getOrientation(inR, orientVals);
				azimuth = (float) (Math.toDegrees(orientVals[0]));
				pitch = (float) (Math.toDegrees(orientVals[1]));
				roll = (float) (Math.toDegrees(orientVals[2]));
				final SensorData objSensorData = new SensorData(azimuth, pitch,
						roll);
				if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
					for (SensorListener obj : listSensorListener) {
						final SensorListener obj1 = obj;
						if (obj1 != null) {
							obj1.onSensorChanged(objSensorData);
						}
					}
				}
			}
		}
	};

	private void remapCordinateSystem() {
		int rotation = display.getRotation();

		if (rotation == Surface.ROTATION_0)// Default display rotation is
											// portrait
			SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_X,
					SensorManager.AXIS_Y, inR);
		else if (rotation == Surface.ROTATION_180)// Default display rotation is
													// reverse portrait
			SensorManager
					.remapCoordinateSystem(inR, SensorManager.AXIS_MINUS_X,
							SensorManager.AXIS_MINUS_Y, inR);
		else if (rotation == Surface.ROTATION_90)// Default display rotation is
													// landscape
			SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_Y,
					SensorManager.AXIS_MINUS_X, inR);
		else if (rotation == Surface.ROTATION_270)// Default display rotation is
													// reverse landscape
			SensorManager.remapCoordinateSystem(inR,
					SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X, inR);
	}

	/**
	 * <b>public void register()</b><br/>
	 * <br/>
	 * Registers compass to sense the sensors data. It's needed to be called in
	 * activity's onResume() method.<br/>
	 * 
	 * @see {@link #unregister()}
	 * 
	 * */
	public void register() throws NoSensorAvailable {
		PackageManager PM = ctx.getPackageManager();

		boolean acc = PM
				.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER);
		boolean com = PM
				.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS);
		if (!acc || !com) {
			throw new NoSensorAvailable(
					"Either accelerometer or campass sensor is not available");
		}

		sensorManager = (SensorManager) ctx
				.getSystemService(Context.SENSOR_SERVICE);
		sensorManager.registerListener(mySensorEventListener,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_UI);
		sensorManager.registerListener(mySensorEventListener,
				sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
				SensorManager.SENSOR_DELAY_UI);
	}

	/**
	 * <b>public void unregister()</b><br/>
	 * <br/>
	 * Unregisters compass and frees sensors to make them available to other
	 * applications. It's needed to be called in activity's onPause () method.
	 * 
	 * @see {@link #register()}
	 * */
	public void unregister() {
		sensorManager.unregisterListener(mySensorEventListener);
	}

	private class NoSensorAvailable extends Exception {
		public NoSensorAvailable(String problem) {
			super(problem);
		}
	}
}
