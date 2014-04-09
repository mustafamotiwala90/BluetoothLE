package com.android.bluetoothle;

import java.util.ArrayList;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;



/**
 * Activity for scanning and displaying available BLE devices.
 */
public class DeviceScanActivity extends ListActivity {
	ArrayList<String> listItems=new ArrayList<String>();
	ArrayAdapter<String> adapter;
	ArrayList<String> listUniqueItemsDevice = new ArrayList<String>();


	static final String TAG=DeviceScanActivity.class.getSimpleName();
	BluetoothAdapter mBluetoothAdapter;
	boolean mScanning;
	Handler mHandler = new Handler();
	int scanCount;
	static final int REQUEST_ENABLE_BT=123456;

	// Stops scanning after 10 seconds.
	static final long SCAN_PERIOD = 10000;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		adapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,listItems);
		setListAdapter(adapter);

		registerReceiver(ActionFoundReceiver,
				new IntentFilter(BluetoothDevice.ACTION_FOUND));

		checkBLE();
		init();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private void init(){
		final BluetoothManager bluetoothManager =
				(BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();
	}
	private void startScan(boolean success){
		if(mBluetoothAdapter == null){
			init();
		}
		if(success){
			mScanning=true;
			scanLeDevice(mScanning);
			return;
		}
		if(enableBLE()){
			mScanning=true;
			scanLeDevice(mScanning);
		}else{
			Log.d(TAG," startScan Waiting for on onActivityResult success:"+success);
		}
	}
	private void scanLeDevice(final boolean enable) {
		if (enable) {
			// Stops scanning after a pre-defined scan period.
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					mScanning = false;
					Log.d(TAG,"run stopLeScan");
					mBluetoothAdapter.stopLeScan(mLeScanCallback);
				}
			}, SCAN_PERIOD);
			Log.d(TAG," scanLeDevice startLeScan:"+enable);
			mBluetoothAdapter.startLeScan(mLeScanCallback);
		} else {
			Log.d(TAG," scanLeDevice stopLeScan:"+enable);
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
		}
	}
	// Device scan callback.
	BluetoothAdapter.LeScanCallback mLeScanCallback =
			new BluetoothAdapter.LeScanCallback() {
		@Override
		public void onLeScan(final BluetoothDevice device, final int rssi,
				final byte[] scanRecord) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					scanCount++;

					String msg=
							"\nDevice:" +device+
							"\nName:"+device.getName()+
							"\nRssi:" + rssi;
					//							"\nScanRecord:" + Arrays.toString(scanRecord);
					Log.d(TAG,msg);

					String tempCheck = device.toString();
					if(!listUniqueItemsDevice.contains(tempCheck))
					{
						listUniqueItemsDevice.add(device.toString());
						addItems(msg);
					}
				}
			});
		}

	};
	void addItems(String msg) {
		synchronized(listItems){
			listItems.add(msg);
			adapter.notifyDataSetChanged();
		}
	}

	public void startScan(View v) {
		startScan(false);
	}
	public void stopScan(View v) {
		mScanning=false;
		scanLeDevice(mScanning);
	}

	public void clear(View v) {
		synchronized(listItems){
			listItems.clear();
			adapter.notifyDataSetChanged();
		}
	}

	public void discoverBluetooth(View v){
		mBluetoothAdapter.startDiscovery();
	}

	private final BroadcastReceiver ActionFoundReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				adapter.add(device.getName() + "\n"
						+ device.getAddress());
				adapter.notifyDataSetChanged();
			}
		}
	};

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(ActionFoundReceiver);
	}

	private  void checkBLE(){
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
			finish();
		}
	}
	private boolean enableBLE(){
		boolean ret=true;
		if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
			Log.d(TAG," enableBLE either mBluetoothAdapter == null or disabled:"+mBluetoothAdapter);
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent,REQUEST_ENABLE_BT);
			ret=false;
		}
		return ret;
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG," onActivityResult requestCode="+requestCode+
				", resultCode="+resultCode+", Intent:"+data);
	}
}
