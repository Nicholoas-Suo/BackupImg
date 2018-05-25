/**
 * Created by wangshumin on 2017/11/13.
 */
package tech.xiaosuo.com.bmob;

interface ICallBack {
    void updateNotifaction(int percent);
    void backupImageFail(int percent,int result);
    void imagesIsEmpty();
    void networkIsNotConnect();
    boolean confirmContinueDialog();
    void confirmPermissionsDialog(int type);
    void notNeedBackupOrRestore(int isBackup);
    void showSuccess(int isBackup);
}
