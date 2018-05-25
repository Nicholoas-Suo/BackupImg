package tech.xiaosuo.com.bmob.service;

import android.app.AlertDialog;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.bmob.v3.BmobObject;
import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.datatype.BmobFile;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.CountListener;
import cn.bmob.v3.listener.DownloadFileListener;
import cn.bmob.v3.listener.FindListener;
import cn.bmob.v3.listener.SaveListener;
import cn.bmob.v3.listener.UploadBatchListener;
import cn.bmob.v3.listener.UploadFileListener;
import tech.xiaosuo.com.bmob.IBackupService;
import tech.xiaosuo.com.bmob.ICallBack;
import tech.xiaosuo.com.bmob.R;
import tech.xiaosuo.com.bmob.Utils;
import tech.xiaosuo.com.bmob.bean.ImageInfo;
import tech.xiaosuo.com.bmob.bean.UserInfo;
import tech.xiaosuo.com.bmob.database.DBUtils;
import tech.xiaosuo.com.bmob.http.HttpClientUtils;

//import tech.xiaosuo.com.bmob.IBackupService;

/**
 * Created by wangshumin on 2017/11/13.
 */

public class BackupService extends Service {

    private static final String TAG = "BackupService";
    private static  final String PERMISSION = "permission.bind.backup.service";
    ICallBack mCallback;
    Context mContext;
    List<ImageInfo> phoneImgList = null;
    private static final int SHOW_CONFIRM_CONTINUE_UPLOAD = 0;
    boolean backupResult = false;
    Handler mServiceHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
             if(what == SHOW_CONFIRM_CONTINUE_UPLOAD){
                 //confirmContinueDialog(null);
             }
            //super.handleMessage(msg);
        }
    };
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        int permission = checkCallingOrSelfPermission(PERMISSION);
        if(permission == PackageManager.PERMISSION_DENIED){
              Log.d(TAG," has no permission");
              return null;
        }
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;//super.onStartCommand(intent, flags, startId);
    }

    IBackupService.Stub mBinder = new IBackupService.Stub(){

       @Override
       public void registerCallBack(ICallBack callBack) throws RemoteException {
           mCallback = callBack;
       }

       @Override
       public void unregisterCallBack() throws RemoteException {
           mCallback = null;
       }

       @Override
       public void startBackupImageToServer() throws RemoteException {
             new Thread(runnable).start();
       }

        @Override
        public void startRestoreImageFromServer() throws RemoteException {
             new Thread(restoreRunnable).start();
        }

       /*
       * restore image from server Runnable
       */
        Runnable restoreRunnable = new Runnable() {
           @Override
           public void run() {
               restoreImageFromServer();
           }
       };

        // scan img and send to server thread.
       Runnable runnable = new Runnable() {
           @Override
           public void run() {
/*               if(phoneImgList != null){
                   phoneImgList.clear();
                   phoneImgList = null;
               }
               phoneImgList = DBUtils.scanImageFromPhone(mContext,mCallback);*/
               initPhoneImagList(true);
               //the image lis is empty.
               if(phoneImgList == null){
                   Log.d(TAG," wangsm the imglist is null");
                   return;
               }

               sendImgToServer(mContext,phoneImgList);
           }
       };

   };

 /*   private void confirmContinueDialog(final List<ImageInfo> imgList){
;
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(R.string.back_img_confirm).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sendImgToServer(imgList);
            }
        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        alertDialog.show();
    }*/
//
    private void sendImgToServer(Context context ,List<ImageInfo> imgList){
        if(context == null || imgList == null || imgList.size() == 0){
            try {
                mCallback.imagesIsEmpty();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            if(context == null){
                Log.d(TAG," wangsm sendImgToServer  context is null: ");
            }
            if(imgList == null){
                Log.d(TAG," wangsm sendImgToServer  imgList is null: ");
            }else{
                Log.d(TAG," wangsm sendImgToServer  imgList size:" + imgList.size());
            }
            Log.d(TAG," wangsm sendImgToServer  return img is empty: ");
            return;
        }
        uploadFilesToBmobServer(context,imgList);
    }
    //upload the one file
    private void uploadOneFile(final Context context,String picPath){
        if(picPath == null){
            Log.d(TAG," the  picPath is null");
            return;
        }
        // String picPath = "sdcard/temp.jpg";
        final BmobFile bmobFile = new BmobFile(new File(picPath));
        bmobFile.uploadblock(new UploadFileListener() {

            @Override
            public void done(BmobException e) {
                if(e==null){
                    //bmobFile.getFileUrl()--返回的上传文件的完整地址
                    Log.d(TAG," the picfile url:" + bmobFile.getFileUrl());
                    Toast.makeText(context,R.string.back_up_success,Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(context,R.string.back_up_fail,Toast.LENGTH_SHORT).show();
                    Log.d(TAG," the pic file upload fail :" + e.getMessage());
                }

            }

            @Override
            public void onProgress(Integer value) {
                // 返回的上传进度（百分比）
                Log.d(TAG," the picfile upload pecent value :" + value);
            }
        });
    }

    //upload the one file params File

    private void uploadOneFile(File file){
        uploadOneFile(null,file);
    }

    /**
     * only upload file,amd insert user info
     *
     */
    private void uploadOneFile(final ImageInfo imageInfo){
        if(imageInfo == null){
            Log.d(TAG," uploadOneFile  imageInfo is null");
            return;
        }
        File picFile = Utils.getPhoneImageFile(imageInfo);
        final BmobFile bmobFile = new BmobFile(picFile);
        bmobFile.uploadblock(new UploadFileListener() {

            @Override
            public void done(BmobException e) {
                if(e==null){
                    //bmobFile.getFileUrl()--返回的上传文件的完整地址
                    Log.d(TAG," uploadOneFile the picfile upload success :");
                    Log.d(TAG," uploadOneFile the picfile url:" + bmobFile.getFileUrl());
                    // Toast.makeText(context,R.string.back_up_success,Toast.LENGTH_SHORT).show();
                        BmobFile file = new BmobFile(bmobFile.getFilename(),bmobFile.getGroup(),bmobFile.getFileUrl());
                        imageInfo.setFile(file);
                        insertBombObject(imageInfo);
                    try {
                        mCallback.showSuccess(Utils.IS_BACKPU);
                    } catch (RemoteException re) {
                        re.printStackTrace();
                    }
                }else{
                    // Toast.makeText(context,R.string.back_up_fail,Toast.LENGTH_SHORT).show();
                    Log.d(TAG," uploadOneFile the pic file upload fail :" + e.getMessage());
                }

            }

            @Override
            public void onProgress(Integer value) {
                // 返回的上传进度（百分比）
                Log.d(TAG," uploadOneFile the picfile upload pecent value :" + value);
                try {
/*                    if(value == 100){
                        BmobFile file = new BmobFile(bmobFile.getFilename(),bmobFile.getGroup(),bmobFile.getFileUrl());
                        imageInfo.setFile(file);
                        insertBombObject(imageInfo);
                    }*/
                    mCallback.updateNotifaction(value);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    /**
     * only upload file,do not insert user info
     *
     */
    private void uploadOneFile(final Context context,File file){
        if(file == null){
            Log.d(TAG," the  file is null");
            return;
        }
        // String picPath = "sdcard/temp.jpg";
        final BmobFile bmobFile = new BmobFile(file);
        bmobFile.uploadblock(new UploadFileListener() {

            @Override
            public void done(BmobException e) {
                if(e==null){
                    //bmobFile.getFileUrl()--返回的上传文件的完整地址
                    Log.d(TAG," the picfile upload success :");
                    try {
                        mCallback.showSuccess(Utils.IS_BACKPU);
                    } catch (RemoteException re) {
                        re.printStackTrace();
                    }
                    Log.d(TAG," the picfile url:" + bmobFile.getFileUrl());
                   // Toast.makeText(context,R.string.back_up_success,Toast.LENGTH_SHORT).show();
                }else{
                   // Toast.makeText(context,R.string.back_up_fail,Toast.LENGTH_SHORT).show();
                    Log.d(TAG," the pic file upload fail :" + e.getMessage());
                }

            }

            @Override
            public void onProgress(Integer value) {
                // 返回的上传进度（百分比）
                Log.d(TAG," the picfile upload pecent value :" + value);
                try {
                    mCallback.updateNotifaction(value);
                    if(value == 100){
                        //insertBombObject(imageInfo);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    private void uploadMultiFiles(final Context context,final List<ImageInfo> imgList){

        if(imgList == null){
           Log.d(TAG," upload muti files fail,imglist is null");
            return;
        }
        Log.d(TAG," upload muti files begin");

        ArrayList<String> arrayList = new ArrayList<String>();
        int total = imgList.size();
        for(int i=0; i < total; i++){
            ImageInfo tempImg = imgList.get(i);
            String path = tempImg.getData();
            arrayList.add(path);
        }
        final String[] pathArray = new String[total];
        arrayList.toArray(pathArray);;
        BmobFile.uploadBatch(pathArray, new UploadBatchListener() {
            int currentIndex;
            @Override
            public void onSuccess(List<BmobFile> files,List<String> urls) {
                //1、files-上传完成后的BmobFile集合，是为了方便大家对其上传后的数据进行操作，例如你可以将该文件保存到表中
                //2、urls-上传文件的完整url地址
               // Log.d(TAG," uploadMultiFiles success files.size " + files + " urls size: " + urls.size());
                int index = currentIndex-1;

                ImageInfo imageInfo = imgList.get(index);
                BmobFile file = files.get(index);
                Log.d(TAG," uploadMultiFiles success files.size " + file.getFileUrl() + " url : " + urls.get(index));
             //   file.setUrl(urls.get(index));
                imageInfo.setFile(file);
                insertBombObject(imageInfo);
                if(urls.size()==pathArray.length){//如果数量相等，则代表文件全部上传完成
                    //do something
                   // Log.d(TAG," uploadMultiFiles success ");
                    try {
                        mCallback.showSuccess(Utils.IS_BACKPU);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onError(int statuscode, String errormsg) {
                Log.d(TAG," uploadMultiFiles fail,statuscode:" + statuscode + " errormsg:" + errormsg );
                Toast.makeText(context,statuscode + errormsg,Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProgress(int curIndex, int curPercent, int total,int totalPercent) {
                //1、curIndex--表示当前第几个文件正在上传
                //2、curPercent--表示当前上传文件的进度值（百分比）
                //3、total--表示总的上传文件数
                //4、totalPercent--表示总的上传进度（百分比）

                Log.d(TAG," uploadMultiFiles onProgress curPercent: " + curPercent + "  curIndex: " + curIndex + " totalPercent:" + totalPercent + " total " + total);
                try {
                    if(curPercent == 100){
                        currentIndex = curIndex;
   /*                     ImageInfo imageInfo = imgList.get(curIndex-1);
                        insertBombObject(imageInfo);*/
                        mCallback.updateNotifaction(totalPercent);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void uploadFilesToBmobServer(Context context,List<ImageInfo> imgList) {
        int total = 0;
        if(imgList == null){
            Log.d(TAG, " wangsm uploadFilesToBmobServer listis null return:");
            return;
        }
        if(imgList.size() > 0){
            queryExistImageInfo(imgList.get(0));
        }
    }

    private void insertBombObject(final ImageInfo obj){

        obj.save(new SaveListener<String>() {
            @Override
            public void done(String objectId,BmobException e) {
                if(e==null){
                    Log.d(TAG, " insertBombObject success");
                  //  toast("添加数据成功，返回objectId为："+objectId);
                }else{
                    Log.d(TAG, " insertBombObject fail  " + e.getMessage());
                    Toast.makeText(mContext,"insert obj fail," + e.toString(),Toast.LENGTH_SHORT).show();
                  //  toast("创建数据失败：" + e.getMessage());
                }
            }
        });
    }

    //query whether the image info exist.
   private void queryExistImageInfo(ImageInfo imageInfo){
       BmobQuery<ImageInfo> query = new BmobQuery<ImageInfo>();

       UserInfo userInfo = imageInfo.getUserInfo();
       query.addWhereEqualTo("userInfo", userInfo);

       //返回50条数据，如果不加上这条语句，默认返回10条数据
       //query.setLimit(50);
       //执行查询方法
       query.findObjects(new FindListener<ImageInfo>() {
           @Override
           public void done(List<ImageInfo> list, BmobException e) {
               if(e==null){
                   Log.d(TAG, " queryExistImageInfo success the server list number: " + list.size());
                   for(int i=0;i<list.size();i++){
                        ImageInfo serverImageInfo = list.get(i);
                        String sData = serverImageInfo.getData();
                        Long sSize = serverImageInfo.getImgSize();
                        String sMd5 = serverImageInfo.getMd5();
                        for(int j=0;j<phoneImgList.size();j++){
                            ImageInfo deviceImg = phoneImgList.get(j);
                            Log.d(TAG, " queryExistImageInfo sData is : " + sData + " deviceImg.getData(): " + deviceImg.getData());
                            Log.d(TAG, " queryExistImageInfo sSize is : " + sSize + " deviceImg.getImgSize(): " + deviceImg.getImgSize());
                            Log.d(TAG, " queryExistImageInfo sMd5 is : " + sMd5 + " deviceImg.getMd5(): " + deviceImg.getMd5());

                            if(deviceImg.getData().equals(sData) && deviceImg.getImgSize().equals(sSize) && deviceImg.getMd5().equals(sMd5)){
                                Log.d(TAG, " queryExistImageInfo duplicate img sData is : " + sData);
                                phoneImgList.remove(j);
                                break;
                            }
                        }
                   }

                   int total = phoneImgList.size();
                   Log.d(TAG, " begin upload img to server, total:" + total);
                   if (total == 1) {
                       uploadOneFile(phoneImgList.get(0));
                   } else if(total > 1){
                       uploadMultiFiles(mContext,phoneImgList);
                   }else{
                       Log.d(TAG, "  there is not new image,do not backup :" );
                       //Toast.makeText(mContext," there is not new image,no need update",Toast.LENGTH_SHORT).show();
                       try {
                               mCallback.notNeedBackupOrRestore(Utils.IS_BACKPU);
                       } catch (RemoteException re) {
                           re.printStackTrace();
                       }
                   }
               }else{
                   Log.d(TAG, " queryExistImageInfo query fail,: " + e.toString());
                   if(e.getErrorCode() == 101){
                       int total = phoneImgList.size();
                       Log.d(TAG, " begin upload img to server, total:" + total);
                       if (total == 1) {
                           uploadOneFile(phoneImgList.get(0));
                       } else if(total > 1){
                           uploadMultiFiles(mContext,phoneImgList);
                       }else{
                           Log.d(TAG, "  there is not new image,do not backup :" );
                           //Toast.makeText(mContext," there is not new image,no need update",Toast.LENGTH_SHORT).show();
                           try {
                               mCallback.notNeedBackupOrRestore(Utils.IS_BACKPU);
                           } catch (RemoteException re) {
                               re.printStackTrace();
                           }
                       }
                   }else{
                       Toast.makeText(mContext,e.toString(),Toast.LENGTH_SHORT).show();
                   }
               }
           }
       });
   }


    /*
    * download the pic file froem Bmob webserver
    * */
    private boolean downloadFile(final ImageInfo imageInfo,final int index,final int total){
        //允许设置下载文件的存储路径，默认下载文件的目录为：context.getApplicationContext().getCacheDir()+"/bmob/"
       // File saveFile = new File(Environment.getExternalStorageDirectory(), file.getFilename());
        File saveFile = Utils.getPhoneImageFile(imageInfo);
        BmobFile bmobFile = imageInfo.getFile();
        Log.d(TAG, " downloadFile begin download img from server, getDisplayName:" + imageInfo.getDisplayName());
       if(bmobFile == null){
            Log.d(TAG, " the bmobFile is null... ");
          //  bmobFile = new BmobFile(saveFile.getName(),"","");
        }

        bmobFile.download(saveFile, new DownloadFileListener() {

            @Override
            public void onStart() {
                Log.d(TAG, " begin downloading... ");
                try {
                      mCallback.updateNotifaction(0);
                } catch (RemoteException e) {
                     e.printStackTrace();
                }
            }

            @Override
            public void done(String savePath,BmobException e) {
                if(e==null){
                    Log.d(TAG, " download succes..savePath. " + savePath + " index: " + index +" total: " + total);
                    int percent = 0;
                    int mod = 1;
                    try {
                            percent = (index)*100/total;
                            if(total > 100){
                                mod = 20;
                            }else if(total > 10){
                                mod = 10;
                            }else{
                                mod = 1;
                            }
                            if((percent%mod) == 0){
                                mCallback.updateNotifaction(percent);
                            }
                            DBUtils.insertRestoreImageInfoToDb(mContext,imageInfo);
                            if(index == total){
                                mCallback.showSuccess(Utils.IS_RESTORE);
                            }

                    } catch (RemoteException re) {
                        re.printStackTrace();
                    }
                }else{
                    Log.d(TAG, " download fail. " + e.getErrorCode()+","+e.getMessage());
                    Toast.makeText(mContext," download fail. " + e.getErrorCode()+","+e.getMessage(),Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onProgress(Integer value, long newworkSpeed) {
                Log.d(TAG, " download progress.value: " + value +",newworkSpeed: " + newworkSpeed);

            }

        });
        return backupResult;
    }


/*
*     query the imageinfo from server ,wether the server's image is same with the device's
*     then download the different image.
* */
    private void restoreImageFromServer(){
        initPhoneImagList(false);
        BmobQuery<ImageInfo> query = new BmobQuery<ImageInfo>();

        UserInfo userInfo = UserInfo.getCurrentUser(UserInfo.class);
        Log.d(TAG," userinfo id is: " + userInfo.getObjectId());

        query.addWhereEqualTo("userInfo", userInfo);

        //返回50条数据，如果不加上这条语句，默认返回10条数据
        query.setLimit(50);
        //执行查询方法
        query.findObjects(new FindListener<ImageInfo>() {
            @Override
            public void done(List<ImageInfo> list, BmobException e) {
                if(e==null){
                    Log.d(TAG, " restoreImageFromServer query data success the server list number: " + list.size());
                    for(int i=0;i<phoneImgList.size();i++){
                        ImageInfo deviceImg = phoneImgList.get(i);
                        String deviceData = deviceImg.getData();
                        Long deviceSize = deviceImg.getImgSize();
                        String deviceMd5 = deviceImg.getMd5();
                        for(int j=0;j<list.size();j++){
                            ImageInfo serverImageInfo = list.get(j);
                            String sData = serverImageInfo.getData();
                            Long sSize = serverImageInfo.getImgSize();
                            String sMd5 = serverImageInfo.getMd5();
                            Log.d(TAG, " restoreImageFromServer the same info deviceData is : " + deviceData + " ,serverData: " + sData);
                            Log.d(TAG, " restoreImageFromServer the same info deviceSize is : " + deviceSize + " ,sSize: " + sSize);
                            Log.d(TAG, " restoreImageFromServer the same info deviceMd5 is : " + deviceMd5 + " ,sMd5: " + sMd5);
                            if(sData.equals(deviceData) && sSize.equals(deviceSize) && sMd5.equals(deviceMd5)){
                                Log.d(TAG, " restoreImageFromServer duplicate img sData is : " + deviceData);
                                list.remove(j);
                                break;
                            }
                        }
                    }

                    int total = list.size();
                    Log.d(TAG, " restoreImageFromServer begin download img to server, total:" + total);
                    if(total > 0){
                        int successCount = 0;
                        for(int i=0;i<total;i++){
                            ImageInfo serverImg = list.get(i);
                           downloadFile(serverImg,i+1,total);//i is begin 0,so +1.
                        }
                    }else{
                        Log.d(TAG, "  there is not new image,do not backup :" );
                        try {
                            mCallback.notNeedBackupOrRestore(Utils.IS_RESTORE);
                        } catch (RemoteException re) {
                            re.printStackTrace();
                        }
                    }
                }else{
                    Log.d(TAG, " restoreImageFromServer query fail,: " + e.toString());
                    Toast.makeText(mContext,e.toString(),Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
  /*
  *   the method need in a thread run.
  * */
    private void initPhoneImagList(boolean isupload){
        if(phoneImgList != null){
            phoneImgList.clear();
            phoneImgList = null;
        }
        phoneImgList = DBUtils.scanImageFromPhone(mContext,mCallback,isupload);
    }
}
