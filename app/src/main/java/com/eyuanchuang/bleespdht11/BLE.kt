package com.eyuanchuang.bleespdht11


import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.eyuanchuang.bleespdht11.MyApplication.Companion.context
import com.eyuanchuang.bleespdht11.model.BleListDevice
import java.util.*
import kotlin.concurrent.thread


object BLE{

    var bluetoothGatt:BluetoothGatt? = null //
    var connectCallback: (ok: Boolean, errCode: Int)-> Unit = { _, _ -> }//lambda表达式中不需要处理的参数用下划线。函数类型，
    var reconnectTime = 0//重新连接次数
    var connectionStateChangeCallback: (ok: Boolean)-> Unit  = { _ -> }
    var getServicesCallback: (servicesList: List<String>) -> Unit = { _ -> }
    var characteristicChangedCallback: (hex: String, string: String) -> Unit = { _, _ -> }
    /************
     * 硬件service uuid和 characteristic uuid  该参数由蓝牙硬件商确定
     * *****************/

    // 蓝牙服务UUID，易加蓝牙模块的UUID，不同蓝牙设备不一样 ecServerId = "0000FFF0-0000-1000-8000-00805F9B34FB"
    val ecServerId = "000000FF-0000-1000-8000-00805F9B34FB"

    //   上面易加蓝牙服务下面的特征UUID,这个特征只是写入     ecWriteCharacteristicId ="0000FFF2-0000-1000-8000-00805F9B34FB"
    val ecWriteCharacteristicId ="0000FF01-0000-1000-8000-00805F9B34FB"
    //  上面易加蓝牙服务下面的特征UUID,,这个特征只是读   val ecReadCharacteristicId = "0000FFF1-0000-1000-8000-00805F9B34FB"
    val ecReadCharacteristicId = "0000FF01-0000-1000-8000-00805F9B34FB"
    //var mBlueAdapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter// 该方式只能在Activity中获取，因为要context,   获取蓝牙适配器
    //var mBlueAdapter = BluetoothAdapter.getDefaultAdapter()//获取蓝牙适配器,该方法已经弃用
    var mBlueAdapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter//在非activity中获取蓝牙适配器
    val scanner = mBlueAdapter.bluetoothLeScanner//扫描

    // private val  bleScanCallback = object : ScanCallback(){}
    fun startScan(bleScanCallback:ScanCallback){
        checkPermission()
        if(isOpenBluetooth()){
            scanner.startScan(bleScanCallback)}
    }
    fun stopScan(bleScanCallback:ScanCallback){
        checkPermission()
        if (isOpenBluetooth()){
            scanner.stopScan(bleScanCallback)}
    }

    //蓝牙连接回调
    var bluetoothGattCallback:BluetoothGattCallback = object :BluetoothGattCallback() {
        //连接状态回调
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
//            Log.e("onConnectionStateChange", "status=" + status + "|" + "newState=" + newState)
            if (status != BluetoothGatt.GATT_SUCCESS) {
                connectCallback(false, status)
                connectCallback = { _, _ -> }
                connectionStateChangeCallback(false)
                connectionStateChangeCallback = { _ -> }
                return
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                //stopScan(bleScanCallback:ScanCallback) //连接上后停止扫描  待完善
                connectCallback(true, 0)
                connectCallback = { _, _ -> }
                return
            }
            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                checkPermission()
                bluetoothGatt?.close()//断开连接  待完善
                connectCallback(false, 0)
                connectCallback = { _, _ -> }
                connectionStateChangeCallback(false)
                connectionStateChangeCallback = { _ -> }
                return
            }
        }

        //发现服务 返回发现所有服务的集合 回调
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            //super.onServicesDiscovered(gatt, status)
            bluetoothGatt = gatt
            val bluetoothGattServices = gatt?.services
            val servicesList: MutableList<String> = ArrayList()
            if (bluetoothGattServices == null) getServicesCallback(servicesList)
            else {
                for (item in bluetoothGattServices) {
//                    Log.e("ble-service", "UUID=:" + item.uuid.toString())
                    servicesList.add(item.uuid.toString())
                }
                getServicesCallback(servicesList)
            }

        }

        //远程设备数据改变回调，（蓝牙设备发回中心设备的数据）
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            //super.onCharacteristicChanged(gatt, characteristic)
            val bytes = characteristic?.value
            //     Log.e("value","收到的数据是：$bytes")
            if (bytes != null) {
                Log.e("ble-receive", "读取成功[hex]:" + bytesToHexString(bytes));
                Log.e("ble-receive", "读取成功[string]:" + String(bytes));
                characteristicChangedCallback(bytesToHexString(bytes), String(bytes))
            }
        }
        /*************************************
         *读取蓝牙设备数据回调     2023.3.1添加
         *
         * ***********************************************************/
        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (characteristic != null) {
                    Log.e("read value: " ,"成功读取蓝牙数据"+ characteristic.value)
                    val bytes = characteristic.value
                    //     Log.e("value","收到的数据是：$bytes")
                    if (bytes != null) {
                        Log.e("ble-receive", "读取成功[hex]:" + bytesToHexString(bytes));
                        Log.e("ble-receive", "读取成功[string]:" + String(bytes));
                        characteristicChangedCallback(bytesToHexString(bytes), String(bytes))
                    }
                }
            }
        }
        /**************************************/
        //MTU改变 回调
        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            if (BluetoothGatt.GATT_SUCCESS == status) {
                Log.e("BleService", "onMtuChanged success MTU = " + mtu)
            } else {
                Log.e("BleService", "onMtuChanged fail ");
            }
        }
    }

    //获得特定服务下的所有特征值
    private fun getBLEDeviceCharacteristics(serviceId: String): MutableList<String> {
        val service = bluetoothGatt?.getService(UUID.fromString(serviceId))
        val listGattCharacteristic = service?.characteristics
        val characteristicsList: MutableList<String> = ArrayList()
        if (listGattCharacteristic == null) return characteristicsList
        for (item in listGattCharacteristic) {
//            Log.e("ble-characteristic", "UUID=:" + item.uuid.toString())
            characteristicsList.add(item.uuid.toString())
        }
        return characteristicsList
    }
    /************************************
     * 创建蓝牙连接直接函数
     * **********************************/
    private fun createBLEConnection(address:CharSequence, mBlueDeviceList:MutableList<BleListDevice>, callback:(ok:Boolean, errCode:Int) -> Unit){
        connectCallback = callback
        connectionStateChangeCallback = { _-> }
        var isExist:Boolean = false
        for (item in mBlueDeviceList) {
            if (item.macNumber == address) {
                checkPermission()
                bluetoothGatt = item.bluetoothDevice.connectGatt(
                    MyApplication.context, false,
                    bluetoothGattCallback
                )
                isExist = true
                break
            }
        }
        if (!isExist){
            connectCallback(false,-1)
        }
    }
    /*******************************************************************
     * 建立连接，直接连接调用easyConnect,这种连接方法简便但不可靠，如果要可靠连接可以多次连接的方式
     * 安卓8中采用直接连接
     * *****************************************************************/
    private fun easyOneConnect(address: CharSequence, mBlueDeviceList: MutableList<BleListDevice>, callback:(ok:Boolean) -> Unit ){
        createBLEConnection(address,mBlueDeviceList){ ok:Boolean,errCode:Int ->
            if (ok){
                /******************************************
                 * 重要：创建连接的时候调用查找服务函数，
                 * 设定函数notifyBLECharacteristicValueChange(ecServerId, ecReadCharacteristicId)必须写在查找函数内部
                 * 才能成功接收到蓝牙发回的数据，另外callback是很重要的状态参数
                 * 连接的时候依赖此状态来判断是否连接上
                 * ***************************************/
                /******************************************
                 * 重要：创建连接的时候调用查找服务函数，
                 * 设定函数notifyBLECharacteristicValueChange(ecServerId, ecReadCharacteristicId)必须写在查找函数内部
                 * 才能成功接收到蓝牙发回的数据，另外callback是很重要的状态参数
                 * 连接的时候依赖此状态来判断是否连接上
                 * ***************************************/
                getBLEDeviceServices () {getServicesCallback ->
                    //           for(item in getServicesCallback)
                    //             {
                    //                   Log.e("ble-service", "UUID=$item")
                    //                }
                    // getBLEDeviceCharacteristics(ecServerId)
                    notifyBLECharacteristicValueChange(ecServerId, ecReadCharacteristicId)
                    callback(true)
                    Thread() {
                        Thread.sleep(300)
                        setMtu(500)
                    }.start()
                }
            }else{
                callback(false)
            }

        }
    }
    /**************************************************************
     * 和蓝牙建立连接 最外层函数
     * 调用：次外层函数 easyOneConnect
     * ************************************************************/
    fun easyConnect(address: CharSequence, mBlueDeviceList: MutableList<BleListDevice>, callback:(ok:Boolean) -> Unit ){
        easyOneConnect(address,mBlueDeviceList){
            if (it){ //如果已经连接上，将连接次数置零，并设置callback为ture
                reconnectTime = 0
                callback(true)
            }else{ //否则重新连接，并记录连接次数
                reconnectTime += 1
                //如果连接次数大于4次，清零，并标定callback 为false
                if (reconnectTime >10){
                    reconnectTime = 0
                    callback(false)
                }else{
                    thread (start =true){
                        easyConnect(address,mBlueDeviceList,callback)
                    }
                }
            }
        }
    }

    /*******************************************************************
     *服务查找，建立连接后首先时查找服务
     *参数为找到的所有服务列表的结果回调,本例中在使用时，没有显性的使用该参数
     * *******************************************************************/
    private fun getBLEDeviceServices(callback: (servicesList: List<String>) -> Unit) {
        getServicesCallback = callback
        checkPermission()
        bluetoothGatt?.discoverServices()
    }
    /*******************************************************************
     *断开连接
     * *******************************************************************/
    fun closeBLEConnection(){
        checkPermission()
        bluetoothGatt?.disconnect()
    }
    /**************************************************************************
     *BLE 应用通常会要求在设备上的特定特征发生变化时收到通知。
     * 以下代码段展示如何使用 setCharacteristicNotification() 方法设置特征的通知：
     *为某个特征启用通知后，如果远程设备上的特征发生更改，则会触发 onCharacteristicChanged() 回调
     * ***********************************************************************/
    private  fun notifyBLECharacteristicValueChange(
        serviceid:String,
        characteristicid:String
    ):Boolean{
        val service = bluetoothGatt?.getService(UUID.fromString(serviceid)) ?: return false //获取服务
        val characteristicRead = service.getCharacteristic(UUID.fromString(characteristicid))//获取服务特征值
        //对特征值进行设定
        val res = bluetoothGatt?.setCharacteristicNotification(characteristicRead,true) ?: return false
        if (!res) return false
        for (dp in characteristicRead.descriptors)
        {
            dp.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            checkPermission()
            bluetoothGatt?.writeDescriptor(dp)
        }
        return true
    }
    /***************************************************
     * 获得蓝牙发过来数据，参数为回调onCharacteristicChanged获得的--->监听的方式获得数据
     * *************************************************/
    fun onBLECharacteristicValueChange(callback: (hex: String, string: String) -> Unit) {
        characteristicChangedCallback = callback
    }

    /*****************
     * 获得蓝牙发过来的数据，通过主动读取的方式获得蓝牙数据,对应的回调函数是2023.3.1
     * *********************************/
    private fun readBLECharacteristicValue(
        serviceId: String,
        characteristicId: String,
    ){
        val service = bluetoothGatt?.getService(UUID.fromString(serviceId))
        val characteristicRead = service?.getCharacteristic(UUID.fromString(characteristicId))
        checkPermission()
        bluetoothGatt?.readCharacteristic(characteristicRead)
    }
    /**
     * 接收数据实现2023.3.1
     * ****/
    fun readRecData()
    {
        readBLECharacteristicValue(ecServerId, ecReadCharacteristicId)
    }
   /*fun readData() {
        val service: BluetoothGattService = mGatt.getService(SERVICE_UUID)
        val characteristic = service.getCharacteristic(CHARACTER_UUID)
        mGatt.readCharacteristic(characteristic)
    }*/
    /********************************************************************************
     * 向蓝牙发送数据
     *
     * *****************************************************************************/
    private fun writeBLECharacteristicValue(
        serviceId: String,
        characteristicId: String,
        data: String,
        isHex: Boolean
    ) {
        val byteArray: ByteArray? = if (isHex) toByteArray(data)
        else data.toByteArray()

        val service = bluetoothGatt?.getService(UUID.fromString(serviceId))
        val characteristicWrite = service?.getCharacteristic(UUID.fromString(characteristicId));

        characteristicWrite?.value = byteArray
        //设置回复形式
        characteristicWrite?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        //开始写数据
        checkPermission()
        bluetoothGatt?.writeCharacteristic(characteristicWrite)
    }
    /**********发送数据实现*******************/
    fun easySendData(data: String, isHex: Boolean) {
        writeBLECharacteristicValue(ecServerId, ecWriteCharacteristicId, data, isHex)
    }

    /**********************************************************
     * 连接状态回调
     * *******************************************************************/
    //连接状态回调，
    fun onBLEConnectionStateChange(callback: (ok: Boolean) -> Unit) {
        connectionStateChangeCallback = callback
    }
    //连接状态回调,
    fun offBLEConnectionStateChange() {
        connectionStateChangeCallback = { _ -> }
    }

    /**********************************************************
     * 设顶MTU数据宽度
     *
     * *******************************************************************/
    private fun setMtu(v: Int) {
        //安卓5.0以上版本
        // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        checkPermission()
        bluetoothGatt?.requestMtu(v)
        //  }
    }
    /***************************************************************/

    /***************************************************************
     * byteToHexString
     * to byteArray
     * *************************************************************/
    fun bytesToHexString(bytes: ByteArray?): String {
        if (bytes == null) return ""
        var str = ""
        for (b in bytes) {
            str += String.format("%02X", b)
        }
        return str
    }

    private fun toByteArray(hexString: String): ByteArray? {
        val byteArray = ByteArray(hexString.length / 2)
        var k = 0
        for (i in byteArray.indices) {
            val high =
                (Character.digit(hexString[k], 16) and 0xf).toByte()
            val low =
                (Character.digit(hexString[k + 1], 16) and 0xf).toByte()
            byteArray[i] = (high * 16 + low).toByte()
            k += 2
        }
        return byteArray
    }
    //暂时未用到，
    private fun checkPermission(){
        return
        // permissionsInit()
    }
    private fun isOpenBluetooth(): Boolean {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = manager.adapter ?: return false
        return adapter.isEnabled
    }
}