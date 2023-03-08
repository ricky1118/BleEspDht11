package com.eyuanchuang.bleespdht11.adapter
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.eyuanchuang.bleespdht11.BLE
import com.eyuanchuang.bleespdht11.MyApplication.Companion.context
import com.eyuanchuang.bleespdht11.databinding.ItemBinding
import com.eyuanchuang.bleespdht11.model.BleListDevice
import com.eyuanchuang.bleespdht11.ui.DeviceActivity

//import com.eyuanchuang.bletool.model.bleListDeviceTest

/*************************************************************************************************************************************
 * RecycleView 标准原生写法，
 * create by ricky yu 14/05/2022   update 28/5/2022
 * 引入ViewBinding,
 * 参考《android权威编程指南》第4版，第九章RecycleView部分，（《第一行代码》3版 P192页，这里点击事件与实际测试不符不能引入position,否者程序奔溃）
 * item 为数据模型 可变集合列表，BleListDevice为数据模型，mane 和 macAddress为数据模型的具体内容
 * 点击事件：两种引入方式,一种在onCreateViewHolder里面加入点击事件，不需要获得position变量，点击某个获得的就是当前的值
 * 另一种方式在 内部类viewHolder中加入点击事件，采用接口的方式，首先viewHolder实现点击事件接口，然后初始化点击事件，再重载点击事件，
 * 这种方式需要获得position的值才能获得点击是具体是列表中某个
 *************************************************************************************************************************************/
class  BleRecAdapter(private var item: MutableList<BleListDevice>) :
    RecyclerView.Adapter<BleRecAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        //val view =LayoutInflater.from(parent.context).inflate(R.layout.item,parent,false)
        /*   加入点击事件*/
        val viewHolder = ViewHolder(binding)
        /* binding.deviceName.setOnClickListener{
             Toast.makeText(context,"you clicked device name:${binding.deviceName.text}  Mac: ${binding.deviceRssi.text} RSSI: ${binding.deviceAddress.text} ",Toast.LENGTH_SHORT).show()
          }*/
        viewHolder.itemView.setOnClickListener {
            /*for(item0 in item)
            {
              if (binding.deviceAddress.text == item0.macNumber)
            }*/

            val name = binding.deviceName.text
            val rssi =binding.deviceRssi.text
            val address = binding.deviceAddress.text
            BLE.easyConnect(address,item,){
                if (it){
                    val intent: Intent = Intent(context,DeviceActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) //在非ACTIVITY中实现跳转时需要添加一个任务
                    context.startActivity(intent)
                }

            }

            Toast.makeText(context,"you clicked device name:$name  Mac: $address RSSI: $rssi ",Toast.LENGTH_SHORT).show()
        }

        return ViewHolder(binding)
    }

    override fun getItemCount() = item.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        /* val name = item[position].name ?: ""
           val macAddress = item[position].macNumber ?: ""
           holder.binding.deviceName.text = name
           holder.binding.macAddress.text = macAddress*/
        holder.bind(item[position].name ?: "", item[position].macNumber ?: "",item[position].rssi)
    }

    /*class ViewHolder(val view: ItemBluetoothDeviceBinding) : RecyclerView.ViewHolder(view.root) {*/
    inner class ViewHolder(private val binding:ItemBinding): RecyclerView.ViewHolder(binding.root)/*,View.OnClickListener*/ {
        /*val name :TextView = binding.deviceName  // 有问题？
        val macAddress:TextView= binding.macAddress // 有问题？*/
        fun bind(name: String, macNumber: String, rssi: Int) {
            binding.deviceName.text = name
            binding.deviceAddress.text = macNumber
            binding.deviceRssi.text = rssi.toString()
        }
        /******************************************************
         *viewholder实现 Onclicklistener接口,点击事件在这里具体实现
         *******************************************************/
        /*init {
                   itemView.setOnClickListener(this)
        }
        override fun onClick(v: View?) {
                            val  position  = bindingAdapterPosition
                            val name = item[position].name
                            val address = item[position].macNumber
                            val rssi = item[position].rssi
                    Toast.makeText(context,"you clicked device name:${name}  Mac: $address RSSI: $rssi  ",Toast.LENGTH_SHORT).show()
       }*/
    }


}

