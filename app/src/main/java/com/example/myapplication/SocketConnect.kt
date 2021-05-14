package com.example.myapplication

import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.minutes
import kotlin.time.seconds
import kotlin.time.TimeSource.Monotonic as TimeSource

@OptIn(ExperimentalTime::class)
val MAX_ATTEMPT_TIME = 2.minutes
const val DEFAULT_ATTEMPT_INTERVAL: Long = 2  // milliseconds

open class SocketConnect(private val host: String, private val port: Int) {
    private val TAG = this::class.java.name
    private val address = "tcp://$host:$port"
    private var _socket: Socket? = null
    private val invalidSocket
        get() = _socket == null || _socket!!.isClosed || !_socket!!.isConnected
    private val socket: Socket
        get() = runBlocking {
            attempt("connect socket") {
                if (invalidSocket)
                    _socket = Socket(host, port)
                Log.d(TAG, "---------------------------------------- Connected to state server on $address.")
                if (!_socket!!.isConnected)
                    throw IOException("Unexpected error, socket should be connected")
            }
            _socket!!
        }

    private var _input: InputStream? = null
    private val input: InputStream
        get() {
            if (_input == null || invalidSocket)
                _input = socket.getInputStream()

            return _input!!
        }

    private var _output: OutputStream? = null
    private val output: OutputStream
        get() {
            if (_output == null || invalidSocket)
                _output = socket.getOutputStream()

            return _output!!
        }

    suspend fun write(msg: ByteArray) {
        attempt("write `$msg` to $address") {
            output.write(msg)
        }
    }


    suspend fun flush() {
             output.flush()
     }

    suspend fun read(numBytes: Int): ByteArray {
        val bArray = ByteArray(numBytes)
        val value = attempt("read from $address") {
            input.read(bArray, 0, numBytes)
        }
        return bArray
    }

//    private suspend fun call(cmd: String, default: String = "\n"): String {
//        write(cmd)
//        return read(default)
//    }

//    override suspend fun fetch(default: Long) = try {
//        val value = call("fetch", "$default\n").trim().toLong()
//        Log.d(TAG, "Fetching remotely << $value")
//        value
//    } catch (e: Exception) {
//        Log.e(TAG, "Error when fetching state")
//        e.printStackTrace()
//        default
//    }
//
//    override suspend fun store(value: Long) {
//        Log.d(TAG, "Storing remotely -- $value")
//        write("store $value")
//    }

    private suspend fun disconnect() {
        try {
            _socket?.close()
            _input?.close()
            _output?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Problems to close socket:")
            e.printStackTrace()
        }
        _socket = null
        _input = null
        _output = null
        Log.e(TAG, "Socket disconnected.")
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun <T> attempt(
            desc: String,
            max_duration: Duration = MAX_ATTEMPT_TIME,
            interval: Long = DEFAULT_ATTEMPT_INTERVAL,
            block: suspend () -> T
    ): T? {
        lateinit var ex: Exception
        val start = TimeSource.markNow()
        var elapsed = 0.seconds

        while (elapsed <= max_duration) try {
            return block()
        } catch (e: Exception) {
            ex = e
            disconnect()
            elapsed = start.elapsedNow()
            Log.e(TAG, "Operation `$desc` is taking a while (elapsed time: $elapsed)")
            delay(interval)
        }

        Log.e(TAG, "Operation failed: $desc")
        ex.printStackTrace()
        return null
    }
}