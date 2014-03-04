package com.example.citypatroltest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.location.Location;
import android.media.ExifInterface;
import android.os.Environment;

public class LocationScanner {

	private List<Location> locationList;
	protected boolean isScan;
	private Thread scanThread;
	private long startTime;
	private MainActivity activity;
	private static final String[] extensions = { "jpg", "jpeg", "JPG", "JPEG" };
	private static final int TIME_OUT = 30000; // 30 seconds
	private static final String ROOT_FOLDER = Environment
			.getExternalStorageDirectory().getPath();

	public LocationScanner(MainActivity activity) {
		this.activity = activity;
		locationList = new ArrayList<Location>();
	}

	private void getExifInfo(File file) {
		ExifInterface exif = null;
		try {
			exif = new ExifInterface(file.getAbsolutePath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (exif != null) {
			float[] lat_lng = new float[2];
			exif.getLatLong(lat_lng);
			if (lat_lng[0] != 0 || lat_lng[1] != 0) {
				Location loc = new Location(file.getAbsolutePath());
				loc.setLatitude(lat_lng[0]);
				loc.setLongitude(lat_lng[1]);
				loc.setTime(file.lastModified());
				locationList.add(loc);
			}
		}
	}

	private void loadAllImages(String rootFolder) {
		if (!weHaveTime()) {
			// time out
			return;
		} else {
			File file = new File(rootFolder);
			File[] files = file.listFiles();
			if (files != null && files.length > 0) {
				for (File f : files) {
					if (!weHaveTime()) {
						// time out
						return;
					} else {
						if (f.isDirectory()) {
							loadAllImages(f.getAbsolutePath());
						} else {
							for (int i = 0; i < extensions.length; i++) {
								if (f.getAbsolutePath().endsWith(extensions[i])) {
									getExifInfo(f);
								}
							}
						}
					}
				}
			}
		}
	}

	private boolean weHaveTime() {
		long now = new Date().getTime();
		return now - startTime < TIME_OUT;
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
					activity.scanFinished(locationList
							.toArray(new Location[locationList.size()]));
					interrupt();
				}
			}
		};
		scanThread.start();
	}
	
}
