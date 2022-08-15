package com.baijunty.scanner;

import com.google.zxing.Result;

public final class ScanResult {
    private final String text;
    private final String format;
    private final long timestamp;

    public ScanResult(String text, String format, long timestamp) {
        this.text = text;
        this.format = format;
        this.timestamp = timestamp;
    }

    public ScanResult(Result result){
        this.text=result.getText();
        this.format=result.getBarcodeFormat().name();
        this.timestamp= result.getTimestamp();
    }

    public String getText() {
        return text;
    }

    public String getFormat() {
        return format;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
