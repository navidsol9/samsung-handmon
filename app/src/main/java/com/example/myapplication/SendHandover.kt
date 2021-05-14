package com.example.myapplication

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope



import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.Socket
import java.net.UnknownHostException

class SendHandover(host: String, port: Int, private val activity: AppCompatActivity) : SocketConnect(host, port) {
    fun exec(current_time: String?, prevCellPci: Int, currentCellPci: Int) {
        val socket: Socket
        try {
            val jHO_msg = JSONObject()
            jHO_msg!!.put("timestamp", current_time)
            jHO_msg!!.put("origin-pci", prevCellPci)
            jHO_msg!!.put("destination-pci", currentCellPci)
            jHO_msg!!.put("imei", "35-346710-231891-6")
//            socket = Socket(AppConstants.ORCHESTRATOR_IP, AppConstants.ORCHESTRATOR_PORT)
//            val outputs = socket.getOutputStream()
            val msgByte = jHO_msg.toString().toByteArray()
            val jHO_msg_length = msgByte.size
            //          BigInteger jHO_msg_length_big = BigInteger.valueOf(jHO_msg_length);
            val headerByte = byteArrayOf(
                    0x80.toByte(),
                    0x01.toByte(),
                   (jHO_msg_length ushr 8).toByte(),
                    jHO_msg_length.toByte()
            )

            activity.lifecycleScope.launch(IO) {
                write(headerByte)
                write(msgByte)
                flush()
                val status  = read(2);
                Log.d("Status ", status.toString());
                val length  = (read(1)[0].toInt() shl 8) + (read(1)[0].toInt());
                Log.d("Len ", length.toString());
                if (length > 0){
                    Log.d("Payload ",  read(length).toString());
                }
            }
        } catch (e: UnknownHostException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }
}