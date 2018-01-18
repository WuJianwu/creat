package com.intsig.imageprocessdemo;


import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
/**
 * Get a file path from a Uri
 *
 */
public class DocumentUtil {
	private static final String TAG = "DocumentUtil";
    public static  DocumentUtil getInstance(){
    	return new DocumentUtil();
    }
   
   
    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     */
    public  String getPath(final Context context, final Uri uri) {
        String path = null;
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        Log.d(TAG, "getPath, uri="+uri);
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
        	Log.d(TAG, "isKitKat, isDocumentUri");
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                	path =  Environment.getExternalStorageDirectory() + "/" + split[1];
                }else{
                	Log.d(TAG, "type="+type);
                }
                Log.d(TAG, "isKitKat, isExternalStorageDocument, type="+type);
            } else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                Log.d(TAG, "isKitKat, isDownloadsDocument,contentUri="+contentUri);
                path =  getDataColumn(context, contentUri, null, null);
            } else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                } else {
                	Log.d(TAG, "type="+type);
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] { split[1]};

                path = getDataColumn(context, contentUri, selection, selectionArgs);
                Log.d(TAG, "isKitKat, isMediaDocument type="+type);
            }
        }else if ("content".equalsIgnoreCase(uri.getScheme())) {
        	path = getDataColumn(context, uri, null, null);
        	Log.d(TAG, "getPath, getDataColumn"); 
        }else if ("file".equalsIgnoreCase(uri.getScheme())) {
        	path = uri.getPath();
        	Log.d(TAG, "getPath, uri.getPath()"); 
        }else {
        	Log.d(TAG, "uri="+uri);
        }

        return path;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    private  String getDataColumn(Context context, Uri uri, String selection,
            String[] selectionArgs) {
        String columnstr = null;
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, 
            		                     selection, selectionArgs, null);
            if(cursor != null){
            	if(cursor.moveToFirst()){
            		columnstr =  cursor.getString(0);
            	}
            	cursor.close();
            }  
        } catch (Exception e) {
        	Log.d(TAG, "Exception", e);
		}
        return columnstr;
    }
    private final String External_Uri_Authority = "com.android.externalstorage.documents";
    private final String Download_Uri_Authority = "com.android.providers.downloads.documents";
    private final String Media_Uri_Authority = "com.android.providers.media.documents";
    

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private boolean isExternalStorageDocument(Uri uri) {
        return External_Uri_Authority.equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private boolean isDownloadsDocument(Uri uri) {
        return Download_Uri_Authority.equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private boolean isMediaDocument(Uri uri) {
        return Media_Uri_Authority.equals(uri.getAuthority());
    }
}
