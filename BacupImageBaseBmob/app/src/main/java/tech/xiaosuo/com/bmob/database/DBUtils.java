package tech.xiaosuo.com.bmob.database;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import cn.bmob.v3.datatype.BmobFile;
import tech.xiaosuo.com.bmob.ICallBack;
import tech.xiaosuo.com.bmob.R;
import tech.xiaosuo.com.bmob.Utils;
import tech.xiaosuo.com.bmob.bean.ImageInfo;
import tech.xiaosuo.com.bmob.bean.UserInfo;
import tech.xiaosuo.com.bmob.http.HttpClientUtils;

/**
 * Created by wangshumin on 2017/11/13.
 */

public class DBUtils {
    private static final String TAG = "DBUtils";
    static Uri IMG_CONTENT_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    static String[] IMG_COLUMNS = new String[]{MediaStore.Images.ImageColumns._ID, MediaStore.Images.ImageColumns.DISPLAY_NAME, MediaStore.Images.ImageColumns.SIZE
            , MediaStore.Images.ImageColumns.MIME_TYPE, MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME, MediaStore.Images.ImageColumns.DATA};

    public static List<ImageInfo> scanImageFromPhone(Context context,ICallBack mCallback,boolean isupload){
        if(context == null){
            Log.d(TAG," scanImageFromPhone,the context is null");
           return  null;
        }
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = null;
        try{
             cursor = MediaStore.Images.Media.query(contentResolver,IMG_CONTENT_URI,IMG_COLUMNS);
        }catch (SecurityException se){
            try {
                if(mCallback != null){
                    mCallback.confirmPermissionsDialog(Utils.MSG_PLS_CHECK_PERMISSION);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
//            Toast.makeText(context, R.string.read_external_storage_exception,Toast.LENGTH_SHORT).show();
            Log.d(TAG," scanImageFromPhone,has no read/write extarnal storage permission");
            se.printStackTrace();
            return null;
        }
        if(cursor == null && isupload){
            Log.d(TAG," the img is null");
            try {
                mCallback.imagesIsEmpty();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            return null;
        }
        List<ImageInfo> imgData = new ArrayList<ImageInfo>();
        imgData.clear();
        Log.d(TAG," scanImageFromPhone,begin: ");
        while(cursor.moveToNext()){
            ImageInfo imgInfo = new ImageInfo();
            Log.d(TAG," scanImageFromPhone,the mediastore image media has data.");
/*            int idIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns._ID);
            long id = cursor.getLong(idIndex);
            imgInfo.setImageId(id);*/
            int displayNameIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DISPLAY_NAME);
            String displayName = cursor.getString(displayNameIndex);
            imgInfo.setDisplayName(displayName);
            int sizeIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.SIZE);
            Long size = cursor.getLong(sizeIndex);
            imgInfo.setImgSize(size);
            int mimeTypeIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.MIME_TYPE);
            String mimeType = cursor.getString(mimeTypeIndex);
            imgInfo.setMimeType(mimeType);
            int bucketNameIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME);
            String bucketName = cursor.getString(bucketNameIndex);
            imgInfo.setBucketName(bucketName);
            int dataIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            String data = cursor.getString(dataIndex);
            imgInfo.setData(data);

            File  picFile = Utils.getPhoneImageFile(imgInfo);
            if(!picFile.exists()){
                Log.d(TAG," picFile file not exist.contine..." + picFile.getAbsolutePath());
                continue;
            }else{
                String fileMd5 = Utils.getFileMD5(picFile);
                Log.d(TAG," picFile fileMd5: " + fileMd5);
                imgInfo.setMd5(fileMd5);
            }
            BmobFile file = new BmobFile(picFile);//the prcFile must exist.
            file.obtain(displayName,"","");
            imgInfo.setFile(file);
            UserInfo bmobUser = UserInfo.getCurrentUser(UserInfo.class);
            imgInfo.setUserInfo(bmobUser);
            imgData.add(imgInfo);
            File sdCard = Environment.getExternalStorageDirectory();
            int beginIndex = sdCard.getAbsolutePath().length();
            int endIndex = data.lastIndexOf("/");
            String dir = data.substring(beginIndex,endIndex);
            Log.d(TAG," the img name: " + displayName + " , size: " + size + "  ,mimeType: " + mimeType + " ,bucketName: " + bucketName + " data: " + data + " dir: " + dir);
        }
        Log.d(TAG," scanImageFromPhone,end");
        return imgData;
    }
/**
 *  after restore image from web server,
 *  we will update the device's image db info.
 */

   public static Uri insertRestoreImageInfoToDb(Context context, ImageInfo imageInfo){
       ContentResolver resolver = context.getContentResolver();
       ContentValues values = new ContentValues();
       values.put(MediaStore.Images.ImageColumns.DISPLAY_NAME,imageInfo.getDisplayName());
       values.put(MediaStore.Images.ImageColumns.SIZE,imageInfo.getImgSize());
       values.put(MediaStore.Images.ImageColumns.MIME_TYPE,imageInfo.getMimeType());
       values.put(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,imageInfo.getBucketName());
       values.put(MediaStore.Images.ImageColumns.DATA,imageInfo.getData());
       Uri uri = resolver.insert(IMG_CONTENT_URI,values);
       Log.d(TAG," the insert uri: " + uri.toString());
       return uri;
   }
}
