package com.baijunty.scanner

import com.google.zxing.Result

interface OnScanResultFound {
    fun onFounded(result: Result?): Boolean
}