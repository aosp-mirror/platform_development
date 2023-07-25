/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.testadvertiseservice

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.TextView

private val TAG = AdvertiseActivity::class.simpleName

class AdvertiseActivity : AppCompatActivity() {

    private val nsdManager by lazy { getSystemService(NsdManager::class.java) }
    private val lblLog by lazy { findViewById<TextView>(R.id.lblMainActivity) }
    private val log = mutableListOf<String>()
    private var listener: RegistrationListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()

        val service = NsdServiceInfo().apply {
            serviceName = "test_service"
            serviceType = "_nmt._tcp"
            port = 10234
        }

        listener = RegistrationListener()
        log("Registering service")
        nsdManager.registerService(service, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    override fun onStop() {
        super.onStop()

        listener?.let { nsdManager.unregisterService(it) }
        listener = null
    }

    private fun log(msg: String) {
        runOnUiThread {
            log.add(msg)
            Log.i(TAG, msg)
            lblLog.text = TextUtils.join("\n", log)
        }
    }

    inner class RegistrationListener : NsdManager.RegistrationListener {
        override fun onRegistrationFailed(serviceType: NsdServiceInfo?, errorCode: Int) {
            log("Registration failed for type $serviceType, error $errorCode")
        }

        override fun onUnregistrationFailed(serviceType: NsdServiceInfo?, errorCode: Int) {
            log("Unregistration failed for type $serviceType, error $errorCode")
        }

        override fun onServiceRegistered(serviceInfo: NsdServiceInfo?) {
            log("Service registered for type $serviceInfo")
        }

        override fun onServiceUnregistered(serviceType: NsdServiceInfo?) {
            log("Service unregistered for type $serviceType")
        }
    }
}