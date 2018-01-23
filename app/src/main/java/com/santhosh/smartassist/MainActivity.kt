package com.santhosh.smartassist

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Logger.addLogAdapter(AndroidLogAdapter())
        var mainFragment : Fragment? = supportFragmentManager.findFragmentByTag("MAIN_FRAGMENT")
        if (mainFragment == null) {
            mainFragment = MainFragment()
            supportFragmentManager.beginTransaction().add(R.id.parent_node, mainFragment, "MAIN_FRAGMENT").commit()
            supportFragmentManager.executePendingTransactions()
        }
    }
}
