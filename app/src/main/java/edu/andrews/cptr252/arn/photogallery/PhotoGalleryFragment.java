package edu.andrews.cptr252.arn.photogallery;

import java.util.ArrayList;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

public class PhotoGalleryFragment extends Fragment {
    private static final String TAG = "PhotoGalleryFragment";
    /** Thread responsible for downloading thumbnails */
    ThumbnailDownloader<ImageView> mThumbnailThread;

    GridView mGridView;
    ArrayList<GalleryItem> mItems;

    /** Create an image view containing the photo for each photo in the gallery */
    private class GalleryItemAdapter extends ArrayAdapter<GalleryItem> {
        public GalleryItemAdapter(ArrayList<GalleryItem> items) {
            super(getActivity(), 0, items);
        }

        /** Setup the view for a gallery item at a given position in an array list */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                // There is now an existing view to recycle
                // create a new one by inflating the gallery item layout
                convertView = getActivity().getLayoutInflater()
                        .inflate(R.layout.gallery_item, parent, false);
            }

            // Specify the image for the image view
            ImageView imageView = convertView.findViewById(R.id.gallery_item_imageView);
            imageView.setImageResource(android.R.drawable.ic_menu_gallery);
            GalleryItem item = getItem(position);
            // make thumbnail download request
            mThumbnailThread.queueThumbnail(imageView, item.getUrl());

            return convertView;
        }
    }

    /** Asynchronous task responsible for downloading items from flickr */
    private class FetchItemsTask extends AsyncTask<Void, Void, ArrayList<GalleryItem>> {
        @Override
        protected ArrayList<GalleryItem> doInBackground(Void... params) {
            return new FlickrFetchr().fetchItems();
        }

        @Override
        protected void onPostExecute(ArrayList<GalleryItem> items) {
            mItems = items;
            setupAdapter();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setRetainInstance(true);

        // start AsyncTask running
        new FetchItemsTask().execute();

        // Create a handler for responses from the downloader thread
        // The handler attaches to the main thread's looper
        mThumbnailThread = new ThumbnailDownloader<>(new Handler());
        // Each time a thumbnail finishes downloading, display it
        mThumbnailThread.setListener(new ThumbnailDownloader.Listener<ImageView>() {
            @Override
            public void onThumbnailDownloaded(ImageView imageView, Bitmap thumbnail) {
                if (isVisible()) {
                    imageView.setImageBitmap(thumbnail);
                }
            }
        });
        // start thread running
        mThumbnailThread.start();
        // get the looper associated with this thread.
        // the looper manages the thread's message queue
        mThumbnailThread.getLooper();
        Log.i(TAG, "Background thread started");
    }

    /** Fragment destroyed. Stop download thread. */
    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailThread.quit();
        Log.i(TAG, "Background thread destroyed");
    }

    /** Fragment view destroyed. Clear download queue */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailThread.clearQueue();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        
        mGridView = v.findViewById(R.id.gridView);
        setupAdapter();
        
        return v;
    }
    
    void setupAdapter() {
        if (getActivity() == null || mGridView == null) return;
        
        if (mItems != null) {
            // There are gallery items, use our own Adapter to generate the views.
            mGridView.setAdapter(new GalleryItemAdapter(mItems));
        } else {
            mGridView.setAdapter(null);
        }
    }


}
