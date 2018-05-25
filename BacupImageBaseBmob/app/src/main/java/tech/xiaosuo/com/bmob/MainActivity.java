package tech.xiaosuo.com.bmob;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import tech.xiaosuo.com.bmob.bean.ImageInfo;
import tech.xiaosuo.com.bmob.bean.UserInfo;
import tech.xiaosuo.com.bmob.service.BackupService;
import cn.bmob.v3.Bmob;


 
public class MainActivity extends AppCompatActivity implements View.OnClickListener{
 private static final String TAG = "MainActivity";
    private static final String IMG_BAKCUP_DIR = "tupian";
    Button scanImgButton = null;
    ImageView backupImg = null;
    Button restoreImgButton = null;
    ContentResolver contentResolver = null;
    Uri IMG_CONTENT_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    String[] IMG_COLUMNS = new String[]{Images.ImageColumns._ID,Images.ImageColumns.DISPLAY_NAME,Images.ImageColumns.SIZE
                                           ,Images.ImageColumns.MIME_TYPE,Images.ImageColumns.BUCKET_DISPLAY_NAME,Images.ImageColumns.DATA};
    List<ImageInfo> imgList = null;//new ArrayList<ImageInfo>();
  //  BackupService mService= null;
    private IBackupService mService;
    private Context mainContext;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder builder;
    private static final int NOTIFICATION_TAG = 0;
    ContentLoadingProgressBar progressBar;
    LinearLayout progressbarLL;
    TextView progressPercentText;
    BackUpApplication mApp;
    ObjectAnimator backUpAnimator;
    ImageView waveView;
	Button backupButton;
    Button recoverButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Bmob.initialize(this, "your Bmob key");
        UserInfo bmobUser = UserInfo.getCurrentUser(UserInfo.class);
        if(bmobUser != null){
            // 允许用户使用应用
        }else{
            Intent intent = new Intent(this,LoginActivity.class);
            startActivity(intent);
            finish();
        }
        mainContext = getApplicationContext();
        notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        //scanImgButton = (Button)findViewById(R.id.scan_img);
        //scanImgButton.setOnClickListener(this);
        backupImg = (ImageView)findViewById(R.id.backup_img);
        backupImg.setOnClickListener(this);
        waveView = (ImageView)findViewById(R.id.wave_img);
        contentResolver = getContentResolver();
        backupButton = (Button)findViewById(R.id.backup_button);
        backupButton.setOnClickListener(this);
        recoverButton = (Button)findViewById(R.id.recover_button);
        recoverButton.setOnClickListener(this);
        progressbarLL = (LinearLayout)findViewById(R.id.progressbar_ll);
        progressBar = (ContentLoadingProgressBar)findViewById(R.id.main_progressbar);
        progressPercentText = (TextView)findViewById(R.id.progress_percent_text);

        Intent serviceIntent = new Intent(this,BackupService.class);
        bindService(serviceIntent,mServiceConn, Service.BIND_AUTO_CREATE);
    }
    // listener the rservice wether death
       private IBinder.DeathRecipient deathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            mService.asBinder().unlinkToDeath(deathRecipient,0);
            mService = null;
            Intent serviceIntent = new Intent(mainContext,BackupService.class);
            bindService(serviceIntent,mServiceConn, Service.BIND_AUTO_CREATE);
        }
    };
    //end
    // the callback fumction
    private ICallBack.Stub mCallBack = new ICallBack.Stub(){

        @Override
        public void backupImageFail(int percent,int result) throws RemoteException {
                Log.d(TAG," wangsm callback backupImageFail percent: " + percent);
            Message msg = new Message();
            msg.what = Utils.MSG_BACKUP_IMG_FAIL;
            msg.arg1 = result;
            mainHandler.sendMessage(msg);
        }

        @Override
        public void imagesIsEmpty() throws RemoteException {
            Log.d(TAG," wangsm callback imagesIsEmpty");
            Message msg = new Message();
            msg.what = Utils.MSG_IMG_LIST_IS_EMPERTY;
            mainHandler.sendMessage(msg);
        }

        @Override
        public void updateNotifaction(int percent) throws RemoteException {
            Log.d(TAG," wangsm callback updateNotifaction percent: " + percent);
            Message msg = new Message();
            msg.what = Utils.MSG_UPDATE_NOTIFICATION;
            msg.arg1 = percent;
            mainHandler.sendMessage(msg);
        }

        @Override
        public void networkIsNotConnect() throws RemoteException {
            Log.d(TAG," wangsm callback networkIsNotConnect");
            Message msg = new Message();
            msg.what = Utils.MSG_NETWORK_IS_NOT_CONNECT;
            mainHandler.sendMessage(msg);

        }

        @Override
        public boolean confirmContinueDialog() throws RemoteException {
            Log.d(TAG," wangsm callback confirmContinueDialog");

            return false;
        }

        @Override
        public void confirmPermissionsDialog(int type) throws RemoteException {
            Log.d(TAG," wangsm callback confirmPermissionsDialog type: " + type);
            Message msg = new Message();
            msg.what = Utils.MSG_PLS_CHECK_PERMISSION;
            mainHandler.sendMessage(msg);
        }

        @Override
        public void notNeedBackupOrRestore(int isBackup) throws RemoteException{
            Message msg = new Message();
            msg.what = Utils.MSG_NO_NEED_BACKUP_OR_RESTORE;
            msg.arg1 = isBackup;
            mainHandler.sendMessage(msg);
        }
        @Override
        public void showSuccess(int isBackup) throws RemoteException{
            Message msg = new Message();
            msg.what = Utils.MSG_SHOW_SUCCESS;
            msg.arg1 = isBackup;
            mainHandler.sendMessage(msg);
        }
    };

//service connection.
    private ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = IBackupService.Stub.asInterface(service);
            if(mService != null){
                try{
                    mService.registerCallBack(mCallBack);
                    mService.asBinder().linkToDeath(deathRecipient,0);
                }catch (Exception e){
                    e.printStackTrace();
                }

            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if(mService != null){
                mService.unregisterCallBack();
            }

            unbindService(mServiceConn);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        int button_id = v.getId();

        switch (button_id){
/*            case R.id.scan_img:
                ScanImageInfoTask imgInfoTask = new ScanImageInfoTask();
                imgInfoTask.execute();
               // scanImg();
                break;*/
            case R.id.backup_img:
            case R.id.recover_button:
            case R.id.backup_button:
                disableViewStatus();
                backUpAnimator = ObjectAnimator.ofFloat(backupImg,"rotation",0,360);
                backUpAnimator.setDuration(1000);
                backUpAnimator.setInterpolator(new LinearInterpolator());
                backUpAnimator.setRepeatCount(ValueAnimator.INFINITE);
                backUpAnimator.start();
                AnimationSet animationSet = new AnimationSet(true);
                ScaleAnimation scaleAnimation = new ScaleAnimation(1f, 2.3f, 1f, 2.3f,
                        ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                        ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
                scaleAnimation.setDuration(1800);
                scaleAnimation.setRepeatCount(Animation.INFINITE);
                AlphaAnimation alphaAnimation = new AlphaAnimation(1,0);
                alphaAnimation.setDuration(1800);
                alphaAnimation.setRepeatCount(Animation.INFINITE);
                animationSet.addAnimation(scaleAnimation);
                animationSet.addAnimation(alphaAnimation);
                waveView.startAnimation(animationSet);

                if(mService != null){
                    //network is not connect.
                    if(!Utils.isNetworkConnected(mainContext)){
                            Log.d(TAG," wangsm the network is not connect");
                            Message msg = new Message();
                            msg.what = Utils.MSG_NETWORK_IS_NOT_CONNECT;
                            mainHandler.sendMessage(msg);
                            return;
                    }

                    try{
                        //check the network which is wifi or data.
                        if(Utils.isMobileDataConnected(mainContext)){
                            confirmContinueDialog();
                            return;
                        }

                        if(Utils.isWifiConnected(mainContext)){

                            if(button_id == R.id.backup_img || button_id == R.id.backup_button){
                                Log.d(TAG," wangsm send img to server");
                                mService.startBackupImageToServer();
                            }else if(button_id == R.id.recover_button){
                                Log.d(TAG," wangsm send restore request to server");
                                mService.startRestoreImageFromServer();
                            }

                            return;
                        }

                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
                break;

        }

    }
    //storage/emulated/0/tupian/test1.jpg
    //tupian
    //test1.jpg
    private void copyImg(){
        int size = imgList.size();
        if(size < 1){
            Toast.makeText(getApplicationContext(),R.string.data_empty,Toast.LENGTH_SHORT).show();
            return;
        }
        File sdCard  = Environment.getExternalStorageDirectory();
    //    Log.d(TAG," the  path is: " + path);
       // File file = new File(sdCard + "/tupian/test1.jpg");
        File dir = new File(sdCard + File.separator + IMG_BAKCUP_DIR + File.separator);
        if(!dir.exists()){
            Log.d(TAG," the dir mkdirs");
            dir.mkdirs();
        }
        for(int i=0; i < size; i++){
            ImageInfo imgInfo = imgList.get(i);
            String displayName = imgInfo.getDisplayName();
            Log.d(TAG," the file.seperator is:  " + File.separator);
            File bkFile = new File(dir + File.separator + displayName);
            if(bkFile.exists()){
                if(bkFile.length() == imgInfo.getImgSize()){
                    //need not tu download the img,if the img is exist.
                    Log.d(TAG," the file exist,continue , displayName " + displayName);
                    continue;
                }else{
                    bkFile.delete();
                }
            }

            if(!bkFile.exists()){
                Log.d(TAG," the  create file testbk.jpg");
                try {
                    bkFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            //FileInputStream fis = null;
            InputStream inputStream = null;;
            FileOutputStream fos = null;
            try{
                //ImageInfo info = imgList.get(i);
                Uri uri = null;// ContentUris.withAppendedId(IMG_CONTENT_URI,imgInfo.getImageId());
                inputStream = contentResolver.openInputStream(uri);//fis = new FileInputStream(file);
                fos = new FileOutputStream(bkFile);
                byte[] byteArray = new byte[512];
                int len = -1;
                while ((len = inputStream.read(byteArray)) != -1){//fis.read(byteArray
                    Log.d(TAG," the  read the bkjpg len " + len);
                    fos.write(byteArray,0,len);
                }
                fos.flush();


            }catch (Exception e){
                e.printStackTrace();
            }finally {
                try{
                    fos.close();
                    inputStream.close();
                }catch (Exception e){

                }

            }
            //update IMG_CONTENT_URI;//the MediaStore,Images.Media.
            ContentValues values = new ContentValues();
            values.put(Images.ImageColumns.DISPLAY_NAME,displayName);
            values.put(Images.ImageColumns.SIZE,bkFile.length());
            values.put(Images.ImageColumns.MIME_TYPE,"image/jpg");
            values.put(Images.ImageColumns.BUCKET_DISPLAY_NAME,IMG_BAKCUP_DIR);
            values.put(Images.ImageColumns.DATA,bkFile.getAbsolutePath());
            contentResolver.insert(IMG_CONTENT_URI,values);
        }

    }
    private List<ImageInfo> scanImg(){
        Cursor cursor = Images.Media.query(contentResolver,IMG_CONTENT_URI,IMG_COLUMNS);
        if(cursor == null){
            Log.d(TAG," the img is null");
            return null;
        }
        List<ImageInfo> imgData = new ArrayList<ImageInfo>();
        imgData.clear();
        while(cursor.moveToNext()){
            ImageInfo imgInfo = new ImageInfo();
/*            int idIndex = cursor.getColumnIndex(Images.ImageColumns._ID);
            long id = cursor.getLong(idIndex);
            imgInfo.setImageId(id);*/
            int displayNameIndex = cursor.getColumnIndex(Images.ImageColumns.DISPLAY_NAME);
            String displayName = cursor.getString(displayNameIndex);
            imgInfo.setDisplayName(displayName);
            int sizeIndex = cursor.getColumnIndex(Images.ImageColumns.SIZE);
            Long size = cursor.getLong(sizeIndex);
            imgInfo.setImgSize(size);
            int mimeTypeIndex = cursor.getColumnIndex(Images.ImageColumns.MIME_TYPE);
            String mimeType = cursor.getString(mimeTypeIndex);
            imgInfo.setMimeType(mimeType);
            int bucketNameIndex = cursor.getColumnIndex(Images.ImageColumns.BUCKET_DISPLAY_NAME);
            String bucketName = cursor.getString(bucketNameIndex);
            imgInfo.setBucketName(bucketName);
            int dataIndex = cursor.getColumnIndex(Images.ImageColumns.DATA);
            String data = cursor.getString(dataIndex);
            imgInfo.setData(data);
            imgData.add(imgInfo);
            File sdCard = Environment.getExternalStorageDirectory();
            int beginIndex = sdCard.getAbsolutePath().length();
            int endIndex = data.lastIndexOf("/");
            String dir = data.substring(beginIndex,endIndex);
            Log.d(TAG," the img name: " + displayName + " , size: " + size + "  ,mimeType: " + mimeType + " ,bucketName: " + bucketName + " data: " + data + " dir: " + dir);
        }
        return imgData;
    }

     public class ScanImageInfoTask extends AsyncTask<String,Integer,List<ImageInfo>>{

         @Override
         protected List<ImageInfo> doInBackground(String... params) {
             List<ImageInfo> imgInfos = scanImg();
             return imgInfos;
         }

         @Override
         protected void onPostExecute(List<ImageInfo> imageInfos) {
             super.onPostExecute(imageInfos);
             if(imageInfos != null){
                 imgList = imageInfos;
             }
         }
     }


     //upload the img to the net server.
     private void upLoadImgToServer(List<ImageInfo> imgList){
         if(imgList == null || imgList.size() < 1){
             Log.d(TAG," upLoadImgToServer the imgListist is null");
             Toast.makeText(getApplicationContext(),R.string.data_empty,Toast.LENGTH_SHORT).show();
             return;
         }
         for(int i = 0;i < imgList.size(); i++){
            ImageInfo imgInfo = imgList.get(i);
             Uri uri = null;//ContentUris.withAppendedId(IMG_CONTENT_URI,imgInfo.getImageId());
             InputStream inputStream = null;
             try{
                 inputStream = getContentResolver().openInputStream(uri);
             }catch (Exception e){
                 e.printStackTrace();
             }
            //connect network ,then upload the imginfo and inputStream;
         }
     }

    private Handler mainHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg == null){
              Log.d(TAG," mainHandler the msg is null");
            }
            int what = msg.what;
            int result = msg.arg1;
            Log.d(TAG," mainHandler result: " + result);
            switch (what){
                case Utils.MSG_BACKUP_IMG_FAIL:
                    showErrorDialog(what,result);
                    notificationManager.cancel(NOTIFICATION_TAG);
                    break;
                case Utils.MSG_NO_NEED_BACKUP_OR_RESTORE:
                    showErrorDialog(what,result);
                    break;
                case Utils.MSG_IMG_LIST_IS_EMPERTY:
                case Utils.MSG_NETWORK_IS_NOT_CONNECT:
                case Utils.MSG_PLS_CHECK_PERMISSION:
                    showErrorDialog(what,-1);
                    break;
                case Utils.MSG_UPDATE_NOTIFICATION:
                    int percent = msg.arg1;
                    if(builder == null){
                        builder = new NotificationCompat.Builder(mainContext);
                    }
                    builder.setSmallIcon(R.mipmap.ic_launcher_round);
                    builder.setContentTitle(getString(R.string.app_name));
                    String percentStr = " :" + percent +"%";
                    builder.setContentText(getString(R.string.uploading) + percentStr);

                    builder.setProgress(100,percent,false);
                    Notification notification = builder.build();
                    notification.flags |= Notification.FLAG_ONGOING_EVENT;
                    Log.d(TAG," wangsm main activity show notification percent: " + percent);
                    if(percent == 100){
                        notificationManager.cancel(NOTIFICATION_TAG);
                        progressbarLL.setVisibility(View.GONE);
                        //showBackUpSuccess();
                    }else{
                        notificationManager.notify(NOTIFICATION_TAG,notification);
                        progressbarLL.setVisibility(View.VISIBLE);
                        progressBar.setProgress(percent);
                        progressPercentText.setText(percentStr);
                    }

                    break;
                case Utils.MSG_SHOW_SUCCESS:
                    int isBackup = msg.arg1;
                    showSuccessDialog(isBackup);
                    break;
                default:
                    super.handleMessage(msg);
            }

        }
    };
    //show wuccess dialog,backup or recovery
    private void showSuccessDialog(int isBackup){
            if(isBackup == 1){
                showBackUpSuccess();
            }else{
                showRecoverSuccess();
            }
    }
     //show back up sucess dialog
    private  void showBackUpSuccess(){
        resetViewStatus();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_title).setMessage(R.string.back_up_success).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).show();
    }
    //show recover sucess dialog
    private  void showRecoverSuccess(){
        resetViewStatus();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_title).setMessage(R.string.recover_success).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).show();
    }
    //show the erro information.
    private void showErrorDialog(int what, int result){
/*        if(backUpAnimator != null){
            backUpAnimator.cancel();
        }
        backupButton.setEnabled(true);
        recoverButton.setEnabled(true);*/
        resetViewStatus();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        int message = R.string.dialog_title;
        switch(what){
            case Utils.MSG_BACKUP_IMG_FAIL:
                 if(result == Utils.CONNECT_EXCEPTION){
                     message = R.string.network_conn_exception;
                 }else if(result == Utils.FILE_NOT_FOUND){
                     message = R.string.file_not_found;
                 }else if(result == Utils.USER_INVALID){
                     message = R.string.user_invalid;
                 }
                 else{
                     message = R.string.back_up_fail;
                 }

                break;
            case Utils.MSG_IMG_LIST_IS_EMPERTY:
                message = R.string.img_is_empty;
                break;
            case Utils.MSG_NETWORK_IS_NOT_CONNECT:
                message = R.string.network_not_connect;
                break;
            case Utils.MSG_PLS_CHECK_PERMISSION:
                message = R.string.read_external_storage_exception;
                break;
            case Utils.MSG_NO_NEED_BACKUP_OR_RESTORE:
                if(result == Utils.IS_BACKPU){
                    message = R.string.no_need_backup_error;
                }else if(result == Utils.IS_RESTORE){
                    message = R.string.no_need_recover;
                }
                break;
            default:
                break;
        }
        final int temResult = result;
        builder.setTitle(R.string.dialog_title).setMessage(message).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        }).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(temResult == Utils.USER_INVALID){
                    dialog.dismiss();
                    Intent loginIntent = new Intent(Utils.ACTION_LOGIN);
                    startActivity(loginIntent);
                    finish();
                }
            }
        });
        Log.d(TAG," wangsm show error dialog ");
        AlertDialog dialog = builder.show();
    }

//data connect confirm continue or not?
    private void confirmContinueDialog(){

         AlertDialog.Builder builder = new AlertDialog.Builder(this);
         builder.setTitle(R.string.back_img_confirm).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    mService.startBackupImageToServer();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        //alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
         builder.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_option_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case R.id.settings:
                //startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.login_out:
                //mApp.setmLoginSstatus(false);
                //connect server remove info
                UserInfo.logOut();
                startActivity(new Intent(this,LoginActivity.class));
                finish();
            default:
                break;
        }
        return true;
    }

    private void resetViewStatus(){
        if(backUpAnimator != null){
            backUpAnimator.cancel();
        }
        backupButton.setEnabled(true);
        recoverButton.setEnabled(true);
        backupImg.setEnabled(true);
    }
    private void disableViewStatus(){
        backupButton.setEnabled(false);
        recoverButton.setEnabled(false);
        backupImg.setEnabled(false);
    }
}
