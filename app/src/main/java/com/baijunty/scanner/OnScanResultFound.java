package com.baijunty.scanner;

import androidx.annotation.NonNull;

public interface OnScanResultFound {
    boolean onFounded(@NonNull ScanResult result);
}
