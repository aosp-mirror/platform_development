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
package com.example.testdiscoverservice

import android.net.nsd.NsdManager
import android.net.nsd.NsdManager.ServiceInfoCallback
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.RadioGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity

private val TAG = DiscoverActivity::class.simpleName

class DiscoverActivity : AppCompatActivity() {

    private val nsdManager by lazy { getSystemService(NsdManager::class.java) }
    private val lblLog by lazy { findViewById<TextView>(R.id.lblMainActivity) }
    private val radioGroupMode by lazy { findViewById<RadioGroup>(R.id.radioGroupMode) }
    private val log = mutableListOf<String>()
    private var discoveryListener: DiscoveryListener? = null
    private var serviceInfoListener: WatchListener? = null
    private var resolveListener: ResolveListener? = null

    @RequiresApi(34)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        radioGroupMode.setOnCheckedChangeListener { _, _ ->
            stop()
            startNsdManager()
        }
    }

    @RequiresApi(34)
    override fun onStart() {
        super.onStart()
        startNsdManager()
    }

    @RequiresApi(34)
    private fun startNsdManager() {
        when (radioGroupMode.checkedRadioButtonId) {
            R.id.radioDiscover -> startDiscovery()
            R.id.radioWatch -> startWatch()
            R.id.radioResolve -> startResolve()
        }
    }

    private fun startDiscovery() {
        log("Starting discovery")
        discoveryListener = DiscoveryListener()
        nsdManager.discoverServices("_nmt._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    @RequiresApi(34)
    private fun startWatch() {
        log("Starting watch")

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "test_service"
            serviceType = "_nmt._tcp"
        }

        serviceInfoListener = WatchListener().also { listener ->
            nsdManager.registerServiceInfoCallback(serviceInfo, { it.run() }, listener)
        }
    }

    private fun startResolve() {
        log("Starting resolve")

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "test_service"
            serviceType = "_nmt._tcp"
        }

        resolveListener = ResolveListener("Test1").also { listener ->
            nsdManager.resolveService(serviceInfo, { it.run() }, listener)
        }
    }

    @RequiresApi(34)
    override fun onStop() {
        super.onStop()

        stop()
    }

    @RequiresApi(34)
    private fun stop() {
        log("Stopping current operation")
        discoveryListener?.let { nsdManager.stopServiceDiscovery(it) }
        discoveryListener = null
        serviceInfoListener?.let { nsdManager.unregisterServiceInfoCallback(it) }
        serviceInfoListener = null
        resolveListener?.let { nsdManager.stopServiceResolution(it) }
        resolveListener = null
    }

    private fun log(msg: String) {
        runOnUiThread {
            log.add(msg)
            Log.i(TAG, msg)
            lblLog.text = TextUtils.join("\n", log)
        }
    }

    inner class DiscoveryListener : NsdManager.DiscoveryListener {
        override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
            log("Start discovery failed for type $serviceType, error $errorCode")
        }

        override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
            log("Stop discovery failed for type $serviceType, error $errorCode")
        }

        override fun onDiscoveryStarted(serviceType: String?) {
            log("Discovery started for type $serviceType")
        }

        override fun onDiscoveryStopped(serviceType: String?) {
            log("Discovery stopped for type $serviceType")
        }

        override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
            log("Service found for type $serviceInfo")

        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
            log("Service lost for type $serviceInfo")
        }
    }

    inner class ResolveListener(private val name : String) : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
            log("listenerName $name Resolve failed for service $serviceInfo with code $errorCode")
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo?) {
            log("listenerName $name Service resolved: $serviceInfo")
        }
    }

    @RequiresApi(34)
    inner class WatchListener : ServiceInfoCallback {
        override fun onServiceInfoCallbackRegistrationFailed(p0: Int) {}

        override fun onServiceUpdated(serviceInfo: NsdServiceInfo) {
            log("Service updated: $serviceInfo")
        }

        override fun onServiceLost() {
            log("Watched service lost")
        }

        override fun onServiceInfoCallbackUnregistered() {}
    }
}