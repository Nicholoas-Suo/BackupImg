package tech.xiaosuo.com.bmob;

import android.content.Context;
import tech.xiaosuo.com.bmob.ICallBack;
/**
 * Created by wangshumin on 2017/11/13.
 */

interface IBackupService {
   void registerCallBack(ICallBack callBack);
   void unregisterCallBack();
   void startBackupImageToServer();
   void startRestoreImageFromServer();
}
