package com.santhosh.smartassist

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.awareness.fence.FenceState
import com.jakewharton.rxrelay2.BehaviorRelay

/**
 * Created by santhosh-3366 on 23/01/18.
 */
class FenceRxr : BroadcastReceiver() {
    var fenceStateStream : BehaviorRelay<FenceState> = BehaviorRelay.create<FenceState>()

    override fun onReceive(context: Context, intent: Intent) {
        val fenceState = FenceState.extract(intent)
        fenceStateStream.accept(fenceState)
    }
}