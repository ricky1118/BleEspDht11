package com.eyuanchuang.bleespdht11.ui

import android.Manifest
import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.eyuanchuang.bleespdht11.BLE
import com.eyuanchuang.bleespdht11.MyApplication.Companion.context
import com.eyuanchuang.bleespdht11.adapter.BleRecAdapter
import com.eyuanchuang.bleespdht11.databinding.ActivityMainBinding
import com.eyuanchuang.bleespdht11.model.BleListDevice
import com.permissionx.guolindev.PermissionX

class MainActivity : AppCompatActivity() {
    //蓝牙适配器
    private lateinit var mBlueAdapter:BluetoothAdapter
    //recycleview适配器
    lateinit var adapter:BleRecAdapter
    //蓝牙设备列表
    var mDeviceList:MutableList<BleListDevice> = ArrayList()
    var mBlueDeviceList:MutableList<BleListDevice> = ArrayList()
    //  var mDeviceList01:MutableList<bleListDeviceTest> = ArrayList()
    //蓝牙地址列表
    var addressList: MutableList<String> = ArrayList()
    //扫描Activity的binding
    lateinit var binding: ActivityMainBinding
    //扫描者
    private lateinit var scanner: BluetoothLeScanner
    //是否正在扫描
    var isScanning = false

    //注册 打开蓝牙的回调
    private val activityResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if (it.resultCode == Activity.RESULT_OK) showMsg(if (mBlueAdapter.isEnabled) "蓝牙已打开" else "蓝牙未打开")
    }
    //注册 打开GPS定位权限     安卓10 使用蓝牙需要打开该权限
    private  val gpsRequest =registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if (it.resultCode == Activity.RESULT_OK) showMsg("成功获得位置授权")
    }

    /******************************************************************************/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        mBlueAdapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter//获取蓝牙适配器
        //   scanner = mBlueAdapter.bluetoothLeScanner
        // permissionsInit()
        //   bluetoothInit()
        initScanView()//初始化扫描页

    }

    override fun onResume() {
        super.onResume()
        //默认停止扫描动画  不成功
        // binding.radarView.stopScan()
    }

    override fun onPause() {
        super.onPause()
        if (isOpenBluetooth()){
            stopScan(bleScanCallback)}
    }
    override fun onStop() {
        super.onStop()
        if (isOpenBluetooth()){
            stopScan(bleScanCallback)}
    }

    /******************************************************
     * 初始化扫描
     ******************************************************/
    private fun initScanView()
    {
        binding.scan.setOnClickListener{
            checkPermission()
            if (isOpenBluetooth()){
                if (!isScanning) startScan(bleScanCallback) else stopScan(bleScanCallback)}
        }
        //隐藏扫描动画
        binding.radarView.visibility = View.INVISIBLE

        //initDeviceList01()测试列表使用
        //设备列表recycleView
        val layoutManager = LinearLayoutManager(this)
        binding.bleRecycleView.layoutManager = layoutManager
        adapter = BleRecAdapter(mBlueDeviceList)
        //  val adapter = BleRecAdapter(mDeviceList01) 测试列表使用
        binding.bleRecycleView.adapter = adapter
    }
    /***********************************************************************************************
     * 扫描回调
     **********************************************************************************************/
    private val  bleScanCallback = object : ScanCallback(){
        override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult?) {
            super.onScanResult(callbackType, result)

            if (result != null) {
                //checkPermission()
                val bluetoothDevice: BluetoothDevice = result.device
                checkPermission()
                val name =
                    result.device.name ?: "Unknown"
                val rssi = result.rssi
                Log.e("bleDiscovery", "$name|$rssi|${bluetoothDevice.address}")
                addBlueListDevice(BleListDevice(bluetoothDevice, name,bluetoothDevice.address, result.rssi))
                //mBlueDeviceList.add(BleListDevice( name,bluetoothDevice.address, result.rssi ))
            }

        }
    }
    /***********************************************************************************************
     * 添加到蓝牙列表:
     * 功能：1、新扫描到的设备添加到设备列表
     *      2、扫描到重复设备时不应重复添加到设备列表，但同时已在列表中设备rssi必须及时更新
     * ********************************************************************************************/
    @SuppressLint("NotifyDataSetChanged")
    fun addBlueListDevice(bleListDevice: BleListDevice){
        val address = bleListDevice.macNumber
        val name = bleListDevice.name
        val rssi = bleListDevice.rssi
        if (addressList.contains(address))//
        {
            for (item in mBlueDeviceList)
            {  if (item.macNumber == address)
            //    item.macNumber = address
            //    item.name = name
                item.rssi = rssi
            }
            Log.e("bleDiscovery3", name)
            Log.e("bleDiscovery2", address)
            //刷新列表适配器
            //val mListAdapter = BleRecAdapter(mBlueDeviceList)
            adapter.notifyDataSetChanged()  }
        else {
            mBlueDeviceList.add(bleListDevice) //添加设备到列表
            addressList.add(address)
            adapter.notifyDataSetChanged()
            Log.e("bleDiscovery4", address)
            Log.e("bleDiscovery5", name)
        }

    }
    /***********************************************************************************************
     * 蓝牙连接回调
     * ********************************************************************************************/




    /***********************************************************************************************
     * 蓝牙权限相关
     * ********************************************************************************************/
    private fun permissionsInit() {

        val requestList = ArrayList<String>()
        //android  6 到 android 11的权限
        if ( Build.VERSION.SDK_INT <31 ){
            requestList.add(ACCESS_FINE_LOCATION)
            requestList.add(ACCESS_COARSE_LOCATION)
            //    requestList.add(ACCESS_COARSE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestList.add(Manifest.permission.BLUETOOTH_SCAN)
            requestList.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            requestList.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (requestList.isNotEmpty()) {
            PermissionX.init(this)
                .permissions(requestList)
                .explainReasonBeforeRequest()
                .onExplainRequestReason { scope, deniedList ->
                    val message = "BLE需要您同意以下权限才能正常使用"
                    scope.showRequestReasonDialog(deniedList, message, "Allow", "Deny")
                }
                .request { allGranted, grantedList, deniedList ->
                    if (allGranted) {
                        if(!isOpenBluetooth()) {       openBluetooth()}
                        openGpsPermission()
                        //Toast.makeText(this, "所有申请的权限都已通过", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "您拒绝了如下权限：$deniedList", Toast.LENGTH_SHORT).show()
                    }
                }
        }

    }

    /***********************************************************************************************
     * 打开蓝牙openBluetooth
     * ********************************************************************************************/
    private fun openBluetooth() = mBlueAdapter.let {
        if (!it.isEnabled)
        {
            AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage("当前扫描蓝牙需要先打开蓝牙开关")
                .setNegativeButton("取消") { _, _ -> finish() }
                .setPositiveButton("设置") { _, _ ->
                    val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    activityResult.launch(intent)
                }
                .setCancelable(false)
                .show()
            /*val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
           intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
           activityResult.launch(intent)*/
        }
    }

    /***********************************************************************************************
     * 判断蓝牙适配器是否打开
     * ********************************************************************************************/
    private fun isOpenBluetooth(): Boolean {
        val manager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = manager.adapter ?: return false
        return adapter.isEnabled
    }

    /***********************************
     * GPS或者WIFI 是否打开
     * ********************************/
    private fun  isLocServiceEnable(context: Context): Boolean {
        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gps: Boolean = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val network: Boolean =
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        return gps || network
    }
    /***********************************
     * 打开定位权限
     * ********************************/

    /*************************************************
     * 安卓12的新权限，打开新的权限
     * **********************************************/

    /******************************************************
     * 判断系统是否是安卓10版本,必须获得GPS权限才能蓝牙扫描
     * *****************************************************/
    private fun isAndroid10() = Build.VERSION.SDK_INT ==  Build.VERSION_CODES.Q
    //安卓版本10，是否有GPS权限，没有就打开
    private fun openGpsPermission (){
        if (isAndroid10())
        { if(!isLocServiceEnable(context))
        {
            AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage("当前扫描蓝牙需要打开定位功能")
                .setNegativeButton("取消") { _, _ -> finish() }
                .setPositiveButton("设置") { _, _ ->
                    val settingsIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    gpsRequest.launch(settingsIntent)
                }
                .setCancelable(false)
                .show()

        }
        }
    }
    /***********************************
     * 判断是否是安卓12版本
     * ********************************/
    private fun isAndroid12() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    /********************************
     * 是否有某项权限
     *******************************/
    private fun hasPermission(permission: String) =
        checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    /********************************
     * 消息提示
     * ******************************/
    private fun showMsg(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }


    /********************************
     * 开始扫描蓝牙
     * ******************************/
    private fun startScan(bleScanCallback:ScanCallback) {
        if (!isScanning) {
            // checkPermission()
            //   mBlueAdapter.startDiscovery()
            //   mBlueDeviceList.clear()
            if (isOpenBluetooth())
            {BLE.startScan(bleScanCallback)}
            // scanner.startScan( bleScanCallback)
            isScanning = true
            //显示扫描动画
            binding.radarView.visibility = View.VISIBLE
            //开始扫描
            binding.radarView.startScan()
            Log.e("SCAN","$isScanning")
            binding.scanStartBtn.text = "停止扫描"
        }
    }
    /********************************
     * 停止蓝牙扫描
     * ******************************/
    private fun stopScan(bleScanCallback:ScanCallback) {
        if (isScanning) {
            // checkPermission()
            //    mBlueAdapter.cancelDiscovery()
            if (isOpenBluetooth()){
                BLE.stopScan(bleScanCallback)}
            // scanner.stopScan( bleScanCallback)
            isScanning = false
            //隐藏扫描动画
            // binding.radarView.visibility = View.INVISIBLE
            //停止扫描动画
            binding.radarView.stopScan()
            Log.e("SCAN","$isScanning")
            binding.scanStartBtn.text = "扫描蓝牙"
        }
    }
    //对话框
    private fun showAlert(title: String, content: String, callback: () -> Unit) {
        runOnUiThread {
            AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(content)
                .setPositiveButton("确定",
                    DialogInterface.OnClickListener { _, _ -> callback() })
                .setCancelable(false)
                .create().show()
        }
    }
    private fun checkPermission(){

        permissionsInit()
    }

    /*private fun bluetoothInit(){
        if(mBlueAdapter == null) mBlueAdapter =  (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
        if (!getBluetoothAdapterState())
        openBluetooth()
    }
    private fun getBluetoothAdapterState():Boolean{
        return mBlueAdapter.isEnabled ?: false
    }*/


}