package com.example.citypatroltest;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.location.Location;
import android.media.ExifInterface;
import android.os.Environment;

public class LocationScanner {

	public interface ScanLocation {
		void onScanFinished(Location[] location);
	}

	private ScanLocation scanLocation;
	private List<Location> locationList;
	private ArrayList<String> allImages = new ArrayList<String>();
	protected boolean isScan;
	private Thread scanThread;
	private long startTime;
	private static final String[] extensions = { "jpg", "jpeg", "JPG", "JPEG" };
	private static final int TIME_OUT = 30000; // 30 seconds
	private static final String ROOT_FOLDER = Environment
			.getExternalStorageDirectory().getPath();
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(
			"yyyy:MM:dd hh:mm:ss", Locale.US);

	public LocationScanner(Activity activity) {
		scanLocation = (ScanLocation) activity;
		locationList = new ArrayList<Location>();
	}

	private void getExifInfo(String filepath) {
		ExifInterface exif = null;
		try {
			exif = new ExifInterface(filepath);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (exif != null) {
			String date = exif.getAttribute(ExifInterface.TAG_DATETIME);
			String gpsLatitude = exif
					.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
			String gpsLatitudeRef = exif
					.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
			String gpsLongitude = exif
					.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
			String gpsLongitudeRef = exif
					.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);

			if ((gpsLatitude != null) && (gpsLatitudeRef != null)
					&& (gpsLongitude != null) && (gpsLongitudeRef != null)) {
				Float latitude, longitude;
				if (gpsLatitudeRef.equals("N")) {
					latitude = convertToDegree(gpsLatitude);
				} else {
					latitude = 0 - convertToDegree(gpsLatitude);
				}
				if (gpsLongitudeRef.equals("E")) {
					longitude = convertToDegree(gpsLongitude);
				} else {
					longitude = 0 - convertToDegree(gpsLongitude);
				}
				Location loc = new Location("locationprovider");
				loc.setLatitude(latitude);
				loc.setLongitude(longitude);
				if (date != null) {
					Date d;
					try {
						d = DATE_FORMAT.parse(date);
						loc.setTime(d.getTime());
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				locationList.add(loc);
			}
		}
	}

	private Float convertToDegree(String gpsToConvert) {
		Float result = null;
		String[] DMS = gpsToConvert.split(",", 3);

		String[] stringD = DMS[0].split("/", 2);
		Float D0 = Float.valueOf(stringD[0]);
		Float D1 = Float.valueOf(stringD[1]);
		Float FloatD = D0 / D1;

		String[] stringM = DMS[1].split("/", 2);
		Float M0 = Float.valueOf(stringM[0]);
		Float M1 = Float.valueOf(stringM[1]);
		Float FloatM = M0 / M1;

		String[] stringS = DMS[2].split("/", 2);
		Float S0 = Float.valueOf(stringS[0]);
		Float S1 = Float.valueOf(stringS[1]);
		Float FloatS = S0 / S1;

		result = Float.valueOf(FloatD + (FloatM / 60) + (FloatS / 3600));

		return result;
	};

	private void loadAllImages(String rootFolder) {
		long now = new Date().getTime();
		if (now - startTime > TIME_OUT) {
			// Its more than 30 secs
			return;
		} else {
			File file = new File(rootFolder);
			if (file.isDirectory()) {
				File[] files = file.listFiles();
				if (files != null && files.length > 0) {
					for (File f : files) {
						if (f.isDirectory()) {
							loadAllImages(f.getAbsolutePath());
						} else {
							for (int i = 0; i < extensions.length; i++) {
								if (f.getAbsolutePath().endsWith(extensions[i])) {
									allImages.add(f.getAbsolutePath());
									getExifInfo(f.getAbsolutePath());
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Searching the images in all directories on sd card.
	 */
	public void scan() {
		scanThread = new Thread() {
			public void run() {
				startTime = new Date().getTime();
				while (!isInterrupted()) {
					loadAllImages(ROOT_FOLDER);
					scanLocation.onScanFinished(locationList
							.toArray(new Location[locationList.size()]));
					interrupt();
				}
			}
		};
		scanThread.start();
	}
}
