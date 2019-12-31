package com.baijunty.scanner;

import androidx.annotation.NonNull;

import com.google.zxing.Result;

public interface OnScanResultFound {
    boolean onFounded(@NonNull Result result);
}
