package com.goofish.emm.util

import android.os.SystemClock

class ClickUtils {


    /**
     * 5s内连续点击10次
     */
    companion object {
        private val COUNT = 10
        private var mHits = LongArray(COUNT)
        private val DURATION = 5000
        fun shouldForward(): Boolean {
            //每次点击时，数组向前移动一位
            System.arraycopy(mHits, 1, mHits, 0, mHits.size - 1)
            //为数组最后一位赋值
            mHits[mHits.size - 1] = SystemClock.uptimeMillis()
            if (mHits[0] >= (SystemClock.uptimeMillis() - DURATION)) {
                mHits = LongArray(COUNT) //重新初始化数组
                return true
            }
            return false
        }

        fun reset() {
            mHits = LongArray(COUNT)
        }
    }
}