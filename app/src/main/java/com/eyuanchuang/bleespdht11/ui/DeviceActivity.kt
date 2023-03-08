package com.eyuanchuang.bleespdht11.ui



import android.annotation.SuppressLint
import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.eyuanchuang.bleespdht11.BLE
import com.eyuanchuang.bleespdht11.R
import com.eyuanchuang.bleespdht11.databinding.ActivityDeviceBinding
import java.util.regex.Pattern

@SuppressLint("StaticFieldLeak")
lateinit var binding: ActivityDeviceBinding
class DeviceActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.iv.setOnClickListener{
            BLE.closeBLEConnection()
            BLE.offBLEConnectionStateChange()
            finish()
        }
        //发送数据
        binding.btSend.setOnClickListener {
            //edittext是否有数据
            if (binding.etSend.text.toString().isEmpty()){
                return@setOnClickListener
            }
            //发送的hex是否被选中，如果选中先判断数据格式是否正确
            if (binding.cbHexSend.isChecked == true){
                //格式是否在16进制范围
                if (!Pattern.compile("^[0-9a-fA-F]+$").matcher(binding.etSend.text.toString()).matches())
                {
                    showAlert("提示","格式错误，只能是0-9、a-f,A-F"){}
                    return@setOnClickListener
                }
                //长度是否是双数
                if (binding.etSend.text.toString().length %2 ==1){
                    showAlert("提示","长度只能是双数"){}
                }
                BLE.easySendData(binding.etSend.text.toString(),true)

            }else{
                //注意replace的作用，
                BLE.easySendData(binding.etSend.text.toString().replace("\n","\r\n"),false)
            }
        }
        //注册一个回调，断开时调用
        BLE.onBLEConnectionStateChange {
            showToast("设备断开")
        }
        //注册一个回调，收到数据时触发
        BLE.onBLECharacteristicValueChange{ hex, string ->
            runOnUiThread {
                val nowStr = binding.tvReceiveData.text.toString()
                Log.e("rece","rece data is $nowStr" )
                if (binding.cbHexRev.isChecked){
                    binding.tvReceiveData.text = nowStr + hex +"\r\n"
                }else{
                    binding.tvReceiveData.text = nowStr + string + "\r\n"
                }
                if (binding.cbScroll.isChecked){
                    binding.svReceive.post { binding.svReceive.fullScroll(ScrollView.FOCUS_DOWN) }}
            }
        }
        /******
         * 手动读取蓝牙数据
         * *******/
        binding.rdReceiveData.setOnClickListener {
            BLE.readRecData()
        }

        //接收数据框清零
        binding.rcClear.setOnClickListener {
            binding.tvReceiveData.text =""
        }
        //发送数据框清零
        binding.sendClear.setOnClickListener {
            binding.etSend.setText("")
        }



    }

    override fun onDestroy() {
        super.onDestroy()
        BLE.closeBLEConnection()
    }

    //提示框
    fun showToast(text: String) {
        runOnUiThread {
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
        }
    }
    //带互动提示框
    fun showAlert(title: String, content: String, callback: () -> Unit) {
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

}