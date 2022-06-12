package org.cef.browser;

import org.jetbrains.skia.Bitmap;
import org.jetbrains.skia.ColorAlphaType;
import org.jetbrains.skia.ImageInfo;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class BrowserViewBitmap {

    private Bitmap bitmap = null;
    private byte[] pixels = null;
    private int width = 0;
    private int height = 0;
    private Rectangle popupRect = new Rectangle(0, 0, 0, 0);
    private Rectangle popupOriginRect = new Rectangle(0, 0, 0, 0);
    private boolean popup = false;

    private Lock lock = new ReentrantLock();

    void clean() {
        width = height = 0;
    }

    private byte[] getBytes(ByteBuffer buffer, int width, int height) {
        byte[] pixels = new byte[buffer.capacity()];
        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[index] = buffer.get((x + y * width) * 4 + 0);
                pixels[index] = buffer.get((x + y * width) * 4 + 1);
                pixels[index] = buffer.get((x + y * width) * 4 + 2);
                pixels[index] = buffer.get((x + y * width) * 4 + 3);
            }
        }
        return pixels;
    }

    Bitmap getBitmap() {
        lock.lock();
        try {
            if (bitmap == null) {
                init();
            }
            return bitmap;
        } finally {
            lock.unlock();
        }
    }

    void init() {
        bitmap = new Bitmap();
        bitmap.allocPixels(ImageInfo.Companion.makeS32((int) width, (int) height, ColorAlphaType.PREMUL));
    }

    void setBitmapData(boolean popup, ByteBuffer buffer, int width, int height) {
        lock.lock();
        try {
            this.popup = popup;
            if (this.width != width || this.height != height) {
                this.height = height;
                this.width = width;
                init();
            }
            pixels = getBytes(buffer, width, height);
            bitmap.installPixels(bitmap.getImageInfo(), pixels, width * 4);
        } finally {
            lock.unlock();
        }
    }

    void onPopupSize(Rectangle rect) {
        if (rect.width <= 0 || rect.height <= 0)
            return;
        popupOriginRect = rect;
        popupRect = getPopupRectInWebView(popupOriginRect);
    }

    Rectangle getPopupRect() {
        return (Rectangle) popupRect.clone();
    }

    Rectangle getPopupRectInWebView(Rectangle originalRect) {
        Rectangle rc = originalRect;
        if (rc.x < 0)
            rc.x = 0;
        if (rc.y < 0)
            rc.y = 0;
        if (rc.x + rc.width > width)
            rc.x = width - rc.width;
        if (rc.y + rc.height > height)
            rc.y = height - rc.height;
        if (rc.x < 0)
            rc.x = 0;
        if (rc.y < 0)
            rc.y = 0;
        return rc;
    }

    void clearPopupRects() {
        popupRect.setBounds(0, 0, 0, 0);
        popupOriginRect.setBounds(0, 0, 0, 0);
    }
}