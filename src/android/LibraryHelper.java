// LibraryHelper-cordova
// http://github.com/coryjthompson/LibraryHelper-cordova
package com.coryjthompson.libraryhelper;


import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.UUID;
import java.util.Date;
import java.util.Locale;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.os.Environment;
import android.os.Build;
import android.provider.DocumentsContract;
import android.content.ContentValues;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.media.MediaScannerConnection.OnScanCompletedListener;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ApplicationInfo;



/**
 * Original Code pulled and altered from
 * https://github.com/philipp-at-greenqloud/pluginRefreshMedia
 *
 * @author Philipp Veit (for GreenQloud.com)
 */
public class LibraryHelper extends CordovaPlugin {

  /**
   * Executes the request and returns PluginResult.
   *
   * @param action
   *            The action to execute.
   * @param args
   *            JSONArry of arguments for the plugin.
   * @param callbackId
   *            The callback id used when calling back into JavaScript.
   * @return A  object with a status and message.
   */
  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {

    try {

      if (action.equals("saveImageToLibrary")) {
        String filePath = checkFilePath(args.getString(0));
        if (filePath.equals("")) {
          callbackContext.error("Error: filePath is empty");
          return true; //even though results failed, the action was valid.
        }

        boolean results = addToPhotoLibrary(filePath);
        if(results) {
          callbackContext.success();
        } else {
          callbackContext.error("Could not add to photo library");
        }

        return true;
      }

      if(action.equals("saveVideoToLibrary")){

        Log.i("LibraryHelper", "Entramos a save video to library");

        String filePath = checkFilePath(args.getString(0));
        String videoName = args.getString(1);

        if (filePath.equals("")) {
          Log.i("LibraryHelper", "Error del checkFilePath");
          callbackContext.error("Error: filePath is empty");
          return true; //even though results failed, the action was valid.
        }


        Boolean results = addVideoToLibrary(filePath, videoName);

        if(results) {
          callbackContext.success();
        } else {
          callbackContext.error("Could not add to Video to library");
        }

        return true;

      }

//addVideoToLibrary()

      if(action.equals("getVideoInfo")) {
        String filePath = checkFilePath(args.getString(0));
        if (filePath.equals("")) {
          callbackContext.error("Error: filePath is empty");
          return true; //even though results failed, the action was valid.
        }

        JSONObject results = new JSONObject();
        results.put("duration", getVideoDurationInSeconds(filePath));
        results.put("thumbnail", getThumbnailPath(filePath));
        callbackContext.success(results);
        return true;
      }

      return false; //if we got this far, the action wasn't found.

    } catch (JSONException e) {
      callbackContext.error("JsonException: " + e.getMessage());
    } catch (Exception e) {
      callbackContext.error("Error: " + e.getMessage());
    }

    return true;
  }

  private String checkFilePath(String filePath) {
    String returnValue = "";
    try {
      returnValue = filePath.replaceAll("^file://", "").replaceAll("^file:", "");
    } catch (Exception e) {
      Log.e("LibraryHelper", "Error with the filePath: " + e.getMessage());
      return "";
    }

    return returnValue;
  }

  private boolean addToPhotoLibrary(String filePath) {
    File file = new File(filePath);

    Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
    scanIntent.setData(Uri.fromFile(file));

    // For more information about cordova.getContext() look here:
    // http://simonmacdonald.blogspot.com/2012/07/phonegap-android-plugins-sometimes-we.html?showComment=1342400224273#c8740511105206086350
    Context context = this.cordova.getActivity().getApplicationContext();
    context.sendBroadcast(scanIntent);

    return false;
  }

  private boolean addVideoToLibrary(String filePath, String videoName) {

    Log.i("LibraryHelper", "Entramos en addToLibrary");

    if(videoName != null && !videoName.trim().isEmpty()) {

      File sourceFile = new File(filePath);
      Context context = this.cordova.getActivity().getApplicationContext();


      // ··············································································

      // Create a path where we will place our picture in the user's
      // public pictures directory.  Note that you should be careful about
      // what you place here, since the user often manages these files.  For
      // pictures and other media owned by the application, consider
      // Context.getExternalMediaDir().

      File path = Environment.getExternalStoragePublicDirectory(
        Environment.DIRECTORY_MOVIES);
      File file = new File(path, videoName + ".mp4");

      Log.i("LibraryHelper", "Hemos creado el file de destino en: " + Uri.fromFile(file));


      // Make sure the Pictures directory exists.
      if(!path.exists()) {
        if(!path.mkdirs()){
          Log.i("LibraryHelper", "Error al crear los directorios");
          return false;
        };
      }

      // Very simple code to copy a file from the application's
      // resource into the external file.  Note that this code does
      // no error checking, and assumes the picture is small (does not
      // try to copy it in chunks).  Note that if external storage is
      // not currently mounted this will silently fail.

      BufferedInputStream bis = null;
      BufferedOutputStream bos = null;

      try {
        bis = new BufferedInputStream(new FileInputStream(sourceFile));
        bos = new BufferedOutputStream(new FileOutputStream(file, false));
        byte[] buf = new byte[1024];
        bis.read(buf);
        do {
          bos.write(buf);
        } while (bis.read(buf) != -1);
      } catch (IOException e) {

        // Unable to create file, likely because external storage is
        // not currently mounted.
        Log.w("LibraryHelper", "Error writing " + file, e);

      } finally {
        try {
          if (bis != null) bis.close();
          if (bos != null) bos.close();
        } catch (IOException e) {

          Log.w("LibraryHelper", "Pete al intentar CERRAR los archivos");

        }
      }

      Log.i("LibraryHelper", "Hemos terminado de generar el OutputStream");

      // Tell the media scanner about the new file so that it is
      // immediately available to the user.

      MediaScannerConnection.scanFile(
        context,
        new String[]{file.toString()},
        null,
        new MediaScannerConnection.OnScanCompletedListener() {
          public void onScanCompleted(String path, Uri uri) {
            Log.i("LibraryHelper", "Scanned " + path + ":");
            Log.i("LibraryHelper", "-> uri=" + uri);
          }
        });

      Log.i("LibraryHelper", "Hemos acabado con el MediaScannerConnection");

      // ··············································································

      return true;

    }else{

      Log.i("LibraryHelper", "El nombre del video no puede estar vacio");
      return false;

    }

  }


  private long getVideoDurationInSeconds(String filePath) {
    Context context = this.cordova.getActivity().getApplicationContext();
    File file = new File(filePath);

    try {
      MediaMetadataRetriever retriever = new MediaMetadataRetriever();
      retriever.setDataSource(context, Uri.fromFile(file));
      String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
      if(time == null)
        return Long.parseLong("0");

      return Long.parseLong(time)/1000;
    } catch (Exception e) {
      return Long.parseLong("0");
    }
  }

  private String getThumbnailPath(String filePath) {
    Context context = this.cordova.getActivity().getApplicationContext();
    FileOutputStream out = null;
    try {
      String randomFilePrefix = UUID.randomUUID().toString();
      File outputDir = context.getCacheDir(); // context being the Activity pointer
      File outputFile = File.createTempFile(randomFilePrefix, ".png", outputDir);
      out = new FileOutputStream(outputFile);
      Bitmap thumb;
      if(isImage(filePath)) {
        thumb = Bitmap.createScaledBitmap(BitmapFactory.decodeFile(filePath), 640, 360, false);
      } else {
        thumb = ThumbnailUtils.createVideoThumbnail(filePath, MediaStore.Images.Thumbnails.MINI_KIND);
      }
      thumb.compress(Bitmap.CompressFormat.PNG, 100, out);// PNG is a loseless format, compress factor 100 is ignored.
      return outputFile.getAbsolutePath();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    } finally {
      if(out != null) {
        try {
          out.close();
        } catch (Exception e) {
        }
      }
    }
  }

  private static boolean isImage(String filePath) {
    //cbf doing this correctly
    return filePath.endsWith(".png") || filePath.endsWith(".jpg") || filePath.endsWith(".jpeg") || filePath.endsWith(".gif");
  }

}
