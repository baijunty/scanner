package com.baijunty.scanner;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;

public final class ScanResult {
    private final String text;
    private final BarcodeFormat format;
    private final long timestamp;

    public ScanResult(String text, BarcodeFormat format, long timestamp) {
        this.text = text;
        this.format = format;
        this.timestamp = timestamp;
    }

    public ScanResult(Result result){
        this.text=result.getText();
        this.format=result.getBarcodeFormat();
        this.timestamp= result.getTimestamp();
    }

    public String getText() {
        return text;
    }

    public BarcodeFormat getFormat() {
        return format;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
