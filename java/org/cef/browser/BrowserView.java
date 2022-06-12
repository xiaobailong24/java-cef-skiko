package org.cef.browser;

import org.cef.CefClient;
import org.cef.callback.CefDragData;
import org.cef.handler.CefRenderHandler;
import org.cef.handler.CefScreenInfo;
import org.jetbrains.skia.Bitmap;
import org.jetbrains.skiko.HardwareLayer;

import javax.swing.*;
import java.awt.*;
import java.awt.dnd.DropTarget;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BrowserView extends CefBrowser_N implements CefRenderHandler {
    private BrowserViewBitmap bitmapHandler;
    private HardwareLayer canvas;
    private long windowHandle = 0;
    private Rectangle browserRect = new Rectangle(0, 0, 1, 1); // Work around CEF issue #1437.
    private Point screenPoint = new Point(0, 0);
    private Lock locker = new ReentrantLock();

    public BrowserView(HardwareLayer canvas, CefClient client, String url, CefRequestContext context) {
        this(canvas, client, url, context, null, null);
    }

    private BrowserView(HardwareLayer canvas, CefClient client, String url, CefRequestContext context,
                        BrowserView parent, Point inspectAt) {
        super(client, url, context, parent, inspectAt);
        this.canvas = canvas;
        bitmapHandler = new BrowserViewBitmap();
        new DropTarget(canvas, new BrowserDropTargetListener(this));
    }

    @Override
    public void createImmediately() {
        createBrowserIfRequired(false);
    }

    @Override
    public Component getUIComponent() {
        return canvas;
    }

    @Override
    public CefRenderHandler getRenderHandler() {
        return this;
    }

    @Override
    protected CefBrowser_N createDevToolsBrowser(CefClient client, String url, CefRequestContext context,
                                                 CefBrowser_N parent, Point inspectAt) {
        return new BrowserView(canvas, client, url, context, (BrowserView) this, inspectAt);
    }

    @Override
    public Rectangle getViewRect(CefBrowser browser) {
        return browserRect;
    }

    @Override
    public Point getScreenPoint(CefBrowser browser, Point viewPoint) {
        Point sp = new Point(screenPoint);
        sp.translate(viewPoint.x, viewPoint.y);
        return sp;
    }

    @Override
    public void onPopupShow(CefBrowser browser, boolean show) {
        if (!show) {
            bitmapHandler.clearPopupRects();
            invalidate();
        }
    }

    @Override
    public void onPopupSize(CefBrowser browser, Rectangle size) {
        bitmapHandler.onPopupSize(size);
    }

    @Override
    public void onPaint(CefBrowser browser, boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width,
                        int height) {
        onBitmapChanged(popup, buffer, width, height);
    }

    public void onBitmapChanged(boolean popup, ByteBuffer buffer, int width, int height) {
        bitmapHandler.setBitmapData(popup, buffer, width, height);
    }

    @Override
    public boolean onCursorChange(CefBrowser browser, final int cursorType) {
        SwingUtilities.invokeLater(() -> {
            canvas.setCursor(new Cursor(cursorType));
        });
        return true;
    }

    @Override
    public boolean startDragging(CefBrowser browser, CefDragData dragData, int mask, int x, int y) {
        return false;
    }

    @Override
    public void updateDragCursor(CefBrowser browser, int operation) {
    }

    public void onMouseEvent(MouseEvent event) {
        event.translatePoint(-browserRect.x, -browserRect.y);
        sendMouseEvent(event);
    }

    public void onMouseScrollEvent(MouseWheelEvent event) {
        event.translatePoint(browserRect.x, browserRect.y);
        sendMouseWheelEvent(event);
    }

    public void onKeyEvent(KeyEvent event) {
        sendKeyEvent(event);
    }

    public void onResized(int x, int y, int width, int height) {
        browserRect.setBounds(x, y, width, height);
        screenPoint = canvas.getLocationOnScreen();
        wasResized(width, height);
    }

    public void onFocusGained() {
        if (windowHandle != 0) {
            MenuSelectionManager.defaultManager().clearSelectedPath();
            setFocus(true);
        }
    }

    public void onFocusLost() {
        if (windowHandle != 0) {
            setFocus(false);
        }
    }

    public void dispose() {
        bitmapHandler.clean();
    }

    public void onStart() {
        SwingUtilities.invokeLater(() -> {
            createBrowserIfRequired(true);
        });
    }

    public Bitmap getBitmap() {
        return bitmapHandler.getBitmap();
    }

    private void createBrowserIfRequired(boolean hasParent) {
        long windowHandle = 0;
        if (hasParent) {
            windowHandle = getWindowHandle();
        }
        if (getNativeRef("CefBrowser") == 0) {
            if (getParentBrowser() != null) {
                createDevTools(getParentBrowser(), getClient(), windowHandle, true, false, null, getInspectAt());
            } else {
                createBrowser(getClient(), windowHandle, getUrl(), true, false, null, getRequestContext());
            }
        } else {
            setFocus(true);
        }
    }

    private synchronized long getWindowHandle() {
        if (windowHandle == 0) {
            windowHandle = canvas.getWindowHandle();
        }
        return windowHandle;
    }

    @Override
    public CompletableFuture<BufferedImage> createScreenshot(boolean nativeResolution) {
        throw new UnsupportedOperationException("BrowserView:createScreenshot - Not implemented, yet.\n");
    }

    @Override
    public boolean getScreenInfo(CefBrowser browser, CefScreenInfo screenInfo) {
        // TODO Auto-generated method stub
        return false;
    }
}