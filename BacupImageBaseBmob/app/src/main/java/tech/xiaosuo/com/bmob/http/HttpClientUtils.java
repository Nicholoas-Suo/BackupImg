package tech.xiaosuo.com.bmob.http;

import android.content.Context;
import android.os.Environment;
import android.renderscript.ScriptGroup;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

import tech.xiaosuo.com.bmob.Utils;
import tech.xiaosuo.com.bmob.bean.ImageInfo;
import tech.xiaosuo.com.bmob.bean.JsonKey;
import tech.xiaosuo.com.bmob.bean.UserInfo;

/**
 * Created by wangshumin on 2017/11/10.
 */

public class HttpClientUtils {

    private static final String TAG = "HttpClientUtils";
    private static final String DOMAIN = "208224z11z.iask.in:";
    static String PORT = "28094";

    public static final String TEST_SERVER_ADRESS = "http://192.168.1.105:8090/BackupImageServer/servlet/DataControllerServlet";//"http://192.168.191.1:8080/BackupImageServer/servlet/DataControllerServlet?phonenumber=112&username=Test1";
    public static final String TEST_SERVER_LOGIN = "http://192.168.1.105:8090/BackupImageServer/servlet/LoginServlet";//"http://192.168.191.1:8080/BackupImageServer/servlet/DataControllerServlet?phonenumber=112&username=Test1";
    public static final String TEST_SERVER_REGISTER = "http://192.168.1.105:8090/BackupImageServer/servlet/RegisterServlet";
    public static final String TEST_SERVER_GET_IMGINFO= "http://192.168.1.105:8090/BackupImageServer/servlet/RequestImageInfoServlet";
    public static final String TEST_SERVER_CHECK_IMGINFO_EXIST= "http://192.168.1.105:8090/BackupImageServer/servlet/CheckImageExistServlet";
    /*
  //103.46.128.47

  public static final String TEST_SERVER_ADRESS = "http://" + DOMAIN + PORT + "/BackupImageServer/servlet/DataControllerServlet";//"http://192.168.191.1:8080/BackupImageServer/servlet/DataControllerServlet?phonenumber=112&username=Test1";
  public static final String TEST_SERVER_LOGIN = "http://" + DOMAIN + PORT + "/BackupImageServer/servlet/LoginServlet";//"http://192.168.191.1:8080/BackupImageServer/servlet/DataControllerServlet?phonenumber=112&username=Test1";
  public static final String TEST_SERVER_REGISTER = "http://" + DOMAIN + PORT + "/BackupImageServer/servlet/RegisterServlet";
  public static final String TEST_SERVER_GET_IMGINFO= "http://" + DOMAIN + PORT + "/BackupImageServer/servlet/RequestImageInfoServlet";
  public static final String TEST_SERVER_CHECK_IMGINFO_EXIST= "http://" + DOMAIN + PORT +" /BackupImageServer/servlet/CheckImageExistServlet";
*/
    public static final String LOGIN_SUCESS = "login_sucess";
    public static final String LOGIN_FAIL = "login_fail";
    public static final String USER_NEED_LOGIN = "user_need_login";
    public static final String NETWORK_EXCEPTION = "network_exception";


    public static final String INVALID_USERINFO = "invalid_userinfo";
    public static final String USERNAME_EXIST = "username_exist";
    public static final String EMAIL_EXIST = "email_exist";
    public static final String TELNUMBER_EXIST = "telnumber_exist";
    public static final String NO_EXIST = "no_exist";
    public static final String REGISTER_SUCCESS = "register_sucess";
    public static final String REGISTER_FAIL = "register_fail";


    /*
    String displayName;
    Long imgSize;
    String mimeType;
    String bucketName;
    String data;*/
    private static  final String KEY_DISPLAY_NAME = "key_display_name";
    private static  final String KEY_IMG_SIZE = "key_img_size";
    private static  final String KEY_MIME_TYPE = "key_mime_type";
    private static  final String KEY_BUCKET_NAME = "key_bucket_name";
    private static  final String KEY_DATA = "key_data";
    private static  final String KEY_MD5 = "key_md5";

    public static final int CONNECT_TIMEOUT = 3000;

    public static int sendImgFileToServer(Context context, ImageInfo imageInfo){

        final int TIME_OUT = 3000;
        final String CHARSET = "utf-8";
        String BOUNDARY = UUID.randomUUID().toString(); //boundary
        String CONTENT_TYPE = "multipart/form-data"; //multipart/form-data for upload the file data.
        if(context == null || imageInfo == null){
            Log.d(TAG," wangsm sendImgFileToServer - > the  imageInfo is null,return ");
            return Utils.PARAMS_IS_NULL;
        }
        Map<String,String> imgMap = new HashMap<String,String>();
        imgMap.put(KEY_DISPLAY_NAME,imageInfo.getDisplayName());
        imgMap.put(KEY_IMG_SIZE,Long.toString(imageInfo.getImgSize()));
        imgMap.put(KEY_MIME_TYPE,imageInfo.getMimeType());
        imgMap.put(KEY_BUCKET_NAME,imageInfo.getBucketName());
        imgMap.put(KEY_DATA,imageInfo.getData());

        try {
            URL url = new URL(TEST_SERVER_ADRESS);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(TIME_OUT); conn.setConnectTimeout(TIME_OUT);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Charset", CHARSET);

            conn.setRequestProperty("connection", "keep-alive");
            conn.setRequestProperty("Content-Type", CONTENT_TYPE + ";boundary=" + BOUNDARY);
            String sessionid = Utils.getCookiePreferences(context);
            Log.d(TAG," wagnsm the sessionid is " + sessionid);
            conn.setRequestProperty("Cookie","JSESSIONID=" + sessionid);
            File sdCard  = Environment.getExternalStorageDirectory();

            //Log.d(TAG," sdCard path " + sdCard.getAbsolutePath());
            // /storage/emulated/0/0/test/test.jpg
            // /storage/emulated/0/Pictures/chihuahua2-1.JPG
            // /storage/emulated/0/0/test.jpg
            String fileName = imageInfo.getDisplayName();
            String data = imageInfo.getData();

            int endIndex = data.lastIndexOf("/");
            String dir = data.substring(0,endIndex);
            boolean isSdcardRoot = false;
            if(sdCard.getAbsolutePath().equals(dir)){
                isSdcardRoot = true;
            }else{
                isSdcardRoot = false;
            }

            Log.d(TAG," wangsm sendImgFileToServer -> the  dir is: " + dir);
            // File file = new File(sdCard + "/tupian/test1.jpg");
            File file = null;
            if(isSdcardRoot){
                file = new File(sdCard + File.separator + fileName);
                Log.d(TAG," wangsm sendImgFileToServer -> is sdcard root: " );
            }else{
                int beginIndex = sdCard.getAbsolutePath().length();
                dir = data.substring(beginIndex,endIndex);
                Log.d(TAG," sendImgFileToServer -> the  dir is: " + dir);
                file = new File(sdCard + dir + File.separator + fileName);
            }

            if(file!=null) {
                String md5Str = Utils.getMD5Str(file);
                Log.d(TAG," wangsm the md5str is " + md5Str);
                imgMap.put(KEY_MD5,md5Str);
                OutputStream outputSteam=conn.getOutputStream();
                DataOutputStream dos = new DataOutputStream(outputSteam);
                StringBuffer sb = new StringBuffer();
                sb.append(Utils.LINE_BEGIN);
                sb.append(BOUNDARY);
                sb.append(Utils.LINE_END);
                /**
                 *
                 * name key
                 * filename.xxx
                 */
                sb.append("Content-Disposition: form-data; name=" +"\"" + dir + "\"" + "; filename=\""+file.getName()+"\""+Utils.LINE_END);
                sb.append("Content-Type: application/octet-stream; charset="+CHARSET+Utils.LINE_END);
                sb.append(Utils.LINE_END);
                dos.write(sb.toString().getBytes());
                InputStream is = new FileInputStream(file);
                byte[] bytes = new byte[1024];
                int len = 0;
                while((len=is.read(bytes))!=-1)
                {
                    dos.write(bytes, 0, len);
                }
                is.close();
                dos.write(Utils.LINE_END.getBytes());

                //add the form data
                for(Map.Entry<String,String> entry : imgMap.entrySet()){
                    StringBuilder formSb = new StringBuilder();
                    formSb.append(Utils.LINE_BEGIN);
                    formSb.append(BOUNDARY);
                    formSb.append(Utils.LINE_END);
                    String key = entry.getKey();
                    String value =entry.getValue();
                    formSb.append("Content-Disposition: form-data; name=" +"\"" + key + "\"" + Utils.LINE_END);
                    formSb.append(Utils.LINE_END);
                    formSb.append(Utils.LINE_END);
                    formSb.append(value);
                    formSb.append(Utils.LINE_END);
                    dos.write(formSb.toString().getBytes());
                    dos.write(Utils.LINE_END.getBytes());
                }
                //end the form data


                byte[] end_data = (Utils.LINE_BEGIN+BOUNDARY+Utils.LINE_BEGIN+Utils.LINE_END).getBytes();
                dos.write(end_data);
                dos.flush();

                int res = conn.getResponseCode();
                Log.d(TAG, " wangsm response code:"+res);
                //here need the server return value,to check whether the img  is sended to the server.
                //now return true for test.
                if(res == 200)
                {
                    InputStream inputStream = conn.getInputStream();
                    String userStatus = inputStreamToString(inputStream);
                    if(USER_NEED_LOGIN.equals(userStatus)){
                       return Utils.USER_INVALID;
                    }
                    return Utils.SUCEESS;
                }
            }
        }catch (ConnectException e){
            e.printStackTrace();
            Log.e(TAG, " wangsm response CONNECT_EXCEPTION:");
            return Utils.CONNECT_EXCEPTION;
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
            Log.e(TAG, " wangsm response MALFORMS_URL_EXCEPTION:");
            return Utils.MALFORMS_URL_EXCEPTION;
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
            Log.e(TAG, " wangsm response FilenotFountExcption:");
            return Utils.FILE_NOT_FOUND;

        }catch (IOException e){
            e.printStackTrace();
            Log.e(TAG, " wangsm response IOException:");
            return Utils.FAIL;

        }
        return Utils.FAIL;
    }
//use url method send params or use HttpClient send param
    public static void sendRestoreImgRequest(){
         String userName = "wangsm";
         String password = "123456";
         String urlPath= TEST_SERVER_ADRESS + "?username="+userName+"&password="+password;
         URL url;
         try{
             url = new URL(urlPath);
             HttpURLConnection conn = (HttpURLConnection)url.openConnection();
             conn.setDoInput(true);
             conn.setConnectTimeout(3000);
             conn.setRequestMethod("GET");
             conn.setRequestProperty("Charset","UTF-8");
             int code = conn.getResponseCode();
             if(code == 200){
                InputStream is = conn.getInputStream();
                byte[] bytes =  inputStreamToByteArray(is);
                 String responseStr = new String(bytes,"utf-8");
                 try{
                     JSONObject jsonObjectOrigin = new JSONObject(responseStr);
                     JSONArray jsonArray = jsonObjectOrigin.getJSONArray("img_list");

                     for(int i=0;i<jsonArray.length();i++){
                         JSONObject jsonObject = jsonArray.getJSONObject(i);
                         String displayNmae = jsonObject.getString("displayName");
                         String bucketName = jsonObject.getString("bucketName");
                         System.out.println(" wangsm json: displayNmae: " + displayNmae + " bucketName: " + bucketName);
                     }
                 }catch (JSONException jse){
                     jse.printStackTrace();
                 }

                 Log.d(TAG," wangsm responseStr " + responseStr);
             }else{
                 Log.d(TAG," wangsm request fail");
             }
         }catch (MalformedURLException e){
             e.printStackTrace();
         }catch (IOException e){
             e.printStackTrace();
         }

    }
    public static void sendRestoreImgRequestHttpClient(){

    }
 /*   intputStream change to bytearray */
    public static  byte[] inputStreamToByteArray(InputStream is){

        if(is == null){
           return null;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = -1;
        try {
               while((len = is.read(buffer)) != -1){
                   baos.write(buffer,0,len);
            }
            baos.flush();
            baos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return  baos.toByteArray();
    }
    /*   intputStream change to String */
    public static  String inputStreamToString(InputStream is){

        if(is == null){
            Log.d(TAG," inputStreamToString->the input stream is null");
            return null;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = -1;
        try {
            while((len = is.read(buffer)) != -1){
                baos.write(buffer,0,len);
            }
            baos.flush();
            baos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return  baos.toString();
    }

   /* register user interface
   * check whether the nickname,email,telnumber is exist or not
   * if not ,insert db,return success.
   * data struct use json to send
   * */
    public static String registerUser(UserInfo userInfo){
        String result = REGISTER_FAIL;
           String jsonStr = null;
           if(userInfo == null){
               Log.d(TAG," wangsm the userInfo is null");
               return result;
           }
         //init the json data

        try {
            JSONObject jsonParams = new JSONObject();
            JSONObject jsonObject = new JSONObject();
            jsonParams.put(JsonKey.KEY_REGISTER_NICK_NAME,userInfo.getUsername());
            jsonParams.put(JsonKey.KEY_REGISTER_EMAIL,userInfo.getEmail());
            jsonParams.put(JsonKey.KEY_REGISTER_TEL_NUMBER,userInfo.getMobilePhoneNumber());
         //   jsonParams.put(JsonKey.KEY_REGISTER_PASSWORD,userInfo.get());
            jsonObject.put(JsonKey.KEY_REGISTER_USERINFO,jsonParams);
/*            jsonParams.put(JsonKey.KEY_REGISTER_NICK_NAME,userInfo.getNickName());
            jsonParams.put(JsonKey.KEY_REGISTER_EMAIL,userInfo.getEmail());
            jsonParams.put(JsonKey.KEY_REGISTER_TEL_NUMBER,userInfo.getTelNumber());
            jsonParams.put(JsonKey.KEY_REGISTER_PASSWORD,userInfo.getPassword());
            jsonObject.put(JsonKey.KEY_REGISTER_USERINFO,jsonParams);*/
            jsonStr = jsonObject.toString();

            Log.d(TAG," wangsm the jsonStr is: " + jsonStr);
        }catch (JSONException e){
            e.printStackTrace();
            Log.d(TAG," wangsm the json exception");
            return result;
        }

           try{
               URL url = new URL(TEST_SERVER_REGISTER);
               HttpURLConnection connection = (HttpURLConnection)url.openConnection();
               connection.setDoOutput(true);
               connection.setDoInput(true);
               connection.setConnectTimeout(CONNECT_TIMEOUT);
               connection.setRequestMethod("POST");
               connection.setRequestProperty("Content-Type","application/json; charset=utf-8");
               OutputStream outputStream = connection.getOutputStream();
               outputStream.write(jsonStr.getBytes());
               int code = connection.getResponseCode();
               if(code == HttpsURLConnection.HTTP_OK){
                    InputStream inputStream = connection.getInputStream();

                    String registerStatus = inputStreamToString(inputStream);
                    Log.d(TAG," wangsm registerStatus: " + registerStatus);
                    return registerStatus;
/*
                    if(INVALID_USERINFO.equals(registerStatus)){
                        return Utils.TAG_INVALID_USERINFO;
                    }else if(USERNAME_EXIST.equals(registerStatus)){
                        return Utils.TAG_USERNAME_EXIST;
                    }else if(EMAIL_EXIST.equals(registerStatus)){
                        return Utils.TAG_EMAIL_EXIST;
                    }else if(TELNUMBER_EXIST.equals(registerStatus)){
                        return Utils.TAG_TELNUMBER_EXIST;
                    }else if(REGISTER_SUCCESS.equals(registerStatus)){
                        return Utils.TAG_REGISTER_SUCCESS;
                    }else if(REGISTER_FAIL.equals(registerStatus)){
                        return Utils.TAG_REGISTER_FAIL;
                    }*/
               }

           }catch (MalformedURLException e){
               e.printStackTrace();
               Log.d(TAG," wangsm the malformed exception");
               return NETWORK_EXCEPTION;
           }catch (IOException e){
               e.printStackTrace();
               Log.d(TAG," wangsm the io IOException");
               return NETWORK_EXCEPTION;
           }



           return result;
    }

//get user image info to check the same mad5 file.
    public static List<ImageInfo> getUserImageInfoFromServer(Context context){
          List<ImageInfo> list = null;
        try {
            URL url = new URL(TEST_SERVER_GET_IMGINFO);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setConnectTimeout(3000);
            conn.setRequestMethod("POST");
            String sessionid = Utils.getCookiePreferences(context);
            Log.d(TAG," wagnsm getUserImageInfoFromServer the sessionid is " + sessionid);
            conn.setRequestProperty("Cookie","JSESSIONID=" + sessionid);
            conn.setDoInput(true);
            InputStream inputStream = conn.getInputStream();
            String jsonStr = inputStreamToString(inputStream);
            Log.d(TAG," wangsm getUserImageInfoFromServer json str is " + jsonStr);
            JSONObject jsonObject = new JSONObject(jsonStr);
            JSONArray array = jsonObject.getJSONArray("img_list");
            list = new ArrayList<ImageInfo>();
            for(int i=0; i<array.length(); i++){
               JSONObject object = array.getJSONObject(i);
                String displayName = object.getString(JsonKey.ImageInfoKey.DISPLAY_NAME);
                long imgSize = object.getLong(JsonKey.ImageInfoKey.IMAGE_SIZE);
                String mimeType = object.getString(JsonKey.ImageInfoKey.MIME_TYPE);
                String bucketName = object.getString(JsonKey.ImageInfoKey.BUCKET_NAME);
                String data = object.getString(JsonKey.ImageInfoKey.DATA);
                String md5 = object.getString(JsonKey.ImageInfoKey.MD5);
                int userId = object.getInt(JsonKey.ImageInfoKey.USER_ID);
                ImageInfo imgInfo = new ImageInfo();
                imgInfo.setDisplayName(displayName);
                imgInfo.setImgSize(imgSize);
                imgInfo.setMimeType(mimeType);
                imgInfo.setBucketName(bucketName);
                imgInfo.setData(data);
              //  imgInfo.setMd5(md5);
               // imgInfo.setUserId(userId);
                list.add(imgInfo);
            }

        }catch (MalformedURLException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }catch (JSONException e){
            e.printStackTrace();
        }

          return list;
    }

    //check the image whether is exist in web server
    public static boolean isImgExistInWebServer(Context context,ImageInfo imageInfo){
           boolean result = false;
           JSONObject jsonObject = new JSONObject();
           HttpURLConnection conn = null;
           try{
               JSONObject jsonParams = new JSONObject();
               jsonParams.put(JsonKey.ImageInfoKey.DISPLAY_NAME,imageInfo.getDisplayName());
               jsonParams.put(JsonKey.ImageInfoKey.IMAGE_SIZE,imageInfo.getImgSize());
               jsonParams.put(JsonKey.ImageInfoKey.BUCKET_NAME,imageInfo.getBucketName());
               jsonParams.put(JsonKey.ImageInfoKey.MIME_TYPE,imageInfo.getMimeType());
               jsonObject.put("image_info",jsonParams);
               String jsonStr = jsonObject.toString();
               Log.d(TAG," wangsm isImgExistInWebServer jsonStr: " + jsonStr);


               URL url = new URL(TEST_SERVER_CHECK_IMGINFO_EXIST);
                conn = (HttpURLConnection)url.openConnection();
               conn.setConnectTimeout(3000);
               conn.setRequestMethod("POST");
               String sessionid = Utils.getCookiePreferences(context);
               Log.d(TAG," wagnsm getUserImageInfoFromServer the sessionid is " + sessionid);
               conn.setRequestProperty("Cookie","JSESSIONID=" + sessionid);
               conn.setDoInput(true);
               conn.setDoOutput(true);
               OutputStream outputStream = conn.getOutputStream();
               outputStream.write(jsonStr.getBytes());
               int code = conn.getResponseCode();
               if(code == HttpsURLConnection.HTTP_OK){
                   InputStream inputStream = conn.getInputStream();

                   String exist = inputStreamToString(inputStream);
                   Log.d(TAG," wangsm  isImgExistInWebServer exist "+ exist + "  " + imageInfo.getDisplayName() + " : " + imageInfo.getBucketName());
                   if(Utils.IMG_EXIST.equals(exist)){
                       return true;
                   }
               }


           }catch (JSONException e){
               e.printStackTrace();
           }catch (MalformedURLException e){
               e.printStackTrace();
           }catch (IOException e){
               e.printStackTrace();
           }finally {

           }

           return false;
    }


}
