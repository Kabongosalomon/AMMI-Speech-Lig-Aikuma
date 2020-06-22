/*
	Copyright (C) 2013, The Aikuma Project
	AUTHORS: Oliver Adams and Florian Hanke
*/
package org.getalp.ligaikuma.lig_aikuma.ui.sensors;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.BuildConfig;

/**
 * A simple location detector using GPS and NETWORK
 *
 * @author Sangyeop Lee	<sangl1@student.unimelb.edu.au>
 */
public class LocationDetector implements LocationListener
{
	/**
	 * Constants used in the class
	 * MIN_TIME_INTERVAL, MIN_DISTANCE_INTERVAL
	 * for location updates
	 */
	private final long MIN_TIME_INTERVAL = 60000;
	private final long MIN_DISTANCE_INTERVAL = 0;	//Only time to not melt the battery when moving
	private final String TAG ="LocationDetector";

	/**
	 * locationMagaer which provides location data
	 */
	protected LocationManager locationManager;
	/**
	 * Current best provider
	 */
	protected String provider;
	/**
	 * True if GPS is enabled
	 */
	protected boolean isGPS;
	/**
	 * True if Network is enabled
	 */
	protected boolean isNetwork;
	/**
	 * True if new location data is available
	 */
	protected boolean isLocation;
	/**
	 * Location data (latitude, longitude)
	 */
	protected Location bestLocation;
	/**
	 * Current context
	 */
	protected Context context;

	/**
	 * Constructor for the class
	 * @param context context of the locationManager
	 */
	public LocationDetector(Context context) {
		this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		this.isGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		this.isNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		this.isLocation = false;
		this.context = context;

		Criteria c = new Criteria();
		c.setPowerRequirement(Criteria.POWER_LOW);
		c.setAccuracy(Criteria.ACCURACY_COARSE);
		c.setAltitudeRequired(false);
		c.setBearingRequired(false);
		c.setSpeedRequired(false);
		c.setCostAllowed(false);
		this.provider = locationManager.getBestProvider(c, true);
		for(String s : locationManager.getProviders(true))if(BuildConfig.DEBUG)Log.d(TAG, "Potential provider: "+s);
		if(BuildConfig.DEBUG)Log.d(TAG, "Provider: "+this.provider);
	}

	/**
	 * Start the locationListener
	 *
	 * @return boolean value indicating the availability of provider
	 */
	public boolean start() {
		if(provider == null &&
				ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
				ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
			return false;
		locationManager.requestLocationUpdates(provider, MIN_TIME_INTERVAL, MIN_DISTANCE_INTERVAL, this);
		return true;

	}

	/**
	 * Stop the locatioinListener
	 */
	public void stop() {
		locationManager.removeUpdates(this);
	}

	/**
	 *
	 * @return the latitude
	 */
	public Double getLatitude() {
		if(isLocation) return bestLocation.getLatitude();
		if(provider != null)
		{
			if(ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
					ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
				return null;
			bestLocation = locationManager.getLastKnownLocation(provider);
			if(bestLocation != null)	return bestLocation.getLatitude();
		}
		return null;
	}
	/**
	 * 
	 * @return the longitude
	 */
	public Double getLongitude() {
		if(isLocation) return bestLocation.getLongitude();
		if(provider != null)
		{
			if(ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
					ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
				return null;
			bestLocation = locationManager.getLastKnownLocation(provider);
			if(bestLocation != null) return bestLocation.getLongitude();
		}
		return null;
	}
	
	
	@Override
	public void onLocationChanged(Location location)
	{
		if(BuildConfig.DEBUG)Log.d(TAG, "onLocationChanged called");
		if(null == bestLocation || location.getAccuracy() < bestLocation.getAccuracy())
		{
			if(BuildConfig.DEBUG)Log.d(TAG, "New best location find");
			bestLocation = location;
			if(!isLocation)	isLocation = true;
		}
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {}

	@Override
	public void onProviderEnabled(String provider) {}

	@Override
	public void onProviderDisabled(String provider) {}
	
	
}
