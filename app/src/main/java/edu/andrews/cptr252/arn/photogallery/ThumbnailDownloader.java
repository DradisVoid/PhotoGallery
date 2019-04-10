package edu.andrews.cptr252.arn.photogallery;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Thread responsible for downloading thumbnails from Flickr
 */
public class ThumbnailDownloader<Token> extends HandlerThread {
    private static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;
    Handler mHandler;
    Map<Token, String> requestMap = Collections.synchronizedMap(new HashMap<Token, String>());

    /**
     * Handler from main thread that will process
     * responses from the downloader thread
     */
    Handler mResponseHandler;

    /** Listener for thumbnail-downloaded event */
    public interface Listener<Token> {
        void onThumbnailDownloaded(Token token, Bitmap thumbnail);
    }

    /** Listener that handles thumbnail download events */
    Listener<Token> mListener;

    /** Register listener defining action taken when the thumbnail is downloaded */
    public void setListener(Listener<Token> listener) {
        mListener = listener;
    }

    /** Create new ThumbnailDownloader thread
     * with a reference to a handler for responses
     */
    public ThumbnailDownloader(Handler responseHandler) {
        super(TAG);
        mResponseHandler = responseHandler;
    }

    public ThumbnailDownloader() {
        super(TAG);
    }

    /**
     * Download thumbnail for given image.
     * Triggers onThumbnailDownloaded event
     */
    private void handleRequest(final Token token) {
        try {
            final String url = requestMap.get(token);
            if (url == null) {
                return;
            }
            // Download the actual image data
            byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
            // convert data to a bitmap
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0,
                    bitmapBytes.length);
            Log.i(TAG, "Bitmap created");
            // send response back to the handler in the main thread.
            // The response is a runnable block of code that will be
            // executed in the main thread for the handler.
            mResponseHandler.post(new Runnable() {
                public void run() {
                    if (requestMap.get(token) != url) {
                        return;
                    }
                    // remove download request from hash table
                    requestMap.remove(token);

                    // triggers onThumbnailDownloaded event
                    mListener.onThumbnailDownloaded(token, bitmap);
                }
            });
        } catch (IOException ioe) {
            Log.e(TAG, "Error downloading image", ioe);
        }
    }
    /** Setup the handler for thumbnail-download request messages */
    @SuppressLint("HandlerLeak")
    @Override
    protected void onLooperPrepared() {
        mHandler = new Handler() {
            /** Execute whenever a download request is processed */
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    @SuppressWarnings("unchecked")
                    Token token = (Token) msg.obj;
                    Log.i(TAG, "Got a request for url: " + requestMap.get(token));
                    handleRequest(token);
                }
            }
        };
    }

    /**
     * Add a download message to thread's message queue
     *
     * @param token Object to identify with the download
     * @param url   URL for the image download
     */
    public void queueThumbnail(Token token, String url) {
        Log.i(TAG, "Got a URL: " + url);
        // save the url for the ImageView in our hash table
        // so we can look it up later when we want
        requestMap.put(token, url);
        // get a message from the message pool and add it to the message queue
        mHandler.obtainMessage(MESSAGE_DOWNLOAD, token).sendToTarget();
    }

    /** Delete all download-request messages from current message queue */
    public void clearQueue() {
        Log.i(TAG, "Clear thread's message queue");
        mHandler.removeMessages(MESSAGE_DOWNLOAD);
    }
}
