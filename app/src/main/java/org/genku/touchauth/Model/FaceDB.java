package org.genku.touchauth.Model;
import android.os.Environment;
import com.arcsoft.facerecognition.*;
import com.guo.android_extend.*;
import com.guo.android_extend.java.ExtInputStream;
import com.guo.android_extend.java.ExtOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import android.util.*;

import static android.content.ContentValues.TAG;

public class FaceDB {
    //人脸信息和姓名类
    static class FaceRegist {
        String mName;
        List<AFR_FSDKFace> mFaceList;

        public FaceRegist(String name) {
            mName = name;
            mFaceList = new ArrayList<>();
        }
    }
    //人脸识别相关变量
    public static String appid = "6SZSiE18Q2btggDFF9zbhKGsQQX2AYayAHjMys2wJnT6";
    public static String fr_key = "Boj2cNspJCvKtb1wR8kWU9iEbper9Zq5bixDX9z5jEr7";
    public static String fd_key = "Boj2cNspJCvKtb1wR8kWU9hjxDc9WTSzKESangyrHTVv";
    public static String ft_key = "Boj2cNspJCvKtb1wR8kWU9hcnpM1TZAWTFtbxNUHe8py";
    static String mDBPath;
    static List<FaceRegist> mRegister;
    AFR_FSDKEngine mFREngine;
    static AFR_FSDKVersion mFRVersion;
    static boolean mUpgrade;
    //人脸识别相关代码的类
    public FaceDB(String path){
        mDBPath = path;
        mRegister = new ArrayList<>();
        mFRVersion = new AFR_FSDKVersion();
        mUpgrade = false;
        mFREngine = new AFR_FSDKEngine();
        AFR_FSDKError error = mFREngine.AFR_FSDK_InitialEngine(FaceDB.appid, FaceDB.fr_key);
        if (error.getCode() != AFR_FSDKError.MOK) {
            Log.e(TAG, "AFR_FSDK_InitialEngine fail! error code :" + error.getCode());
        } else {
            mFREngine.AFR_FSDK_GetVersion(mFRVersion);
            Log.d(TAG, "AFR_FSDK_GetVersion=" + mFRVersion.toString());
        }
    }
    //定义析构函数释放引擎占用的系统资源
    public void destroy() {
        if (mFREngine != null) {
            mFREngine.AFR_FSDK_UninitialEngine();
        }

    }
    //将待注册的人脸信息添加到人脸库中
    public static void addFace(String name, AFR_FSDKFace face) {
        try {
            //check if already registered.
            boolean add = true;
            for (FaceRegist frface : mRegister) {
                if (frface.mName.equals(name)) {
                    frface.mFaceList.add(face);
                    add = false;
                    break;
                }
            }
            if (add) { // not registered.
                FaceRegist frface = new FaceRegist(name);
                frface.mFaceList.add(face);
                mRegister.add(frface);
            }

            if (!new File(mDBPath + "/face.txt").exists()) {
                if (!saveInfo()) {
                    Log.e(TAG, "save fail!");
                }
            }

            //save name
            FileOutputStream fs = new FileOutputStream(mDBPath + "/face.txt", true);
            ExtOutputStream bos = new ExtOutputStream(fs);
            bos.writeString(name);
            bos.close();
            fs.close();

            //save feature
            fs = new FileOutputStream(mDBPath + "/" + name + ".data", true);
            bos = new ExtOutputStream(fs);
            bos.writeBytes(face.getFeatureData());
            bos.close();
            fs.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean saveInfo() {
        try {
            FileOutputStream fs = new FileOutputStream(mDBPath + "/face.txt");
            ExtOutputStream bos = new ExtOutputStream(fs);
            bos.writeString(mFRVersion.toString() + "," + mFRVersion.getFeatureLevel());
            bos.close();
            fs.close();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static boolean loadInfo() {
        if (!mRegister.isEmpty()) {
            return false;
        }
        try {
            FileInputStream fs = new FileInputStream(mDBPath + "/face.txt");
            ExtInputStream bos = new ExtInputStream(fs);
            //load version
            String version_saved = bos.readString();
            if (version_saved.equals(mFRVersion.toString() + "," + mFRVersion.getFeatureLevel())) {
                mUpgrade = true;
            }
            //load all regist name.
            if (version_saved != null) {
                for (String name = bos.readString(); name != null; name = bos.readString()){
                    if (new File(mDBPath + "/" + name + ".data").exists()) {
                        mRegister.add(new FaceRegist(new String(name)));
                    }
                }
            }
            bos.close();
            fs.close();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    //从文件中读取人脸
    public static boolean loadFaces(){
        if (loadInfo()) {
            try {
                for (FaceRegist face : mRegister) {
                    Log.d(TAG, "load name:" + face.mName + "'s face feature data.");
                    FileInputStream fs = new FileInputStream(mDBPath + "/" + face.mName + ".data");
                    ExtInputStream bos = new ExtInputStream(fs);
                    AFR_FSDKFace afr = null;
                    do {
                        if (afr != null) {
                            if (mUpgrade) {
                                //upgrade data.
                            }
                            face.mFaceList.add(afr);
                        }
                        afr = new AFR_FSDKFace();
                    } while (bos.readBytes(afr.getFeatureData()));
                    bos.close();
                    fs.close();
                }
                return true;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            if (!saveInfo()) {
                Log.e(TAG, "save fail!");
            }
        }
        return false;
    }
    public boolean delete(String name) {
        try {
            //check if already registered.
            boolean find = false;
            for (FaceRegist frface : mRegister) {
                if (frface.mName.equals(name)) {
                    File delfile = new File(mDBPath + "/" + name + ".data");
                    if (delfile.exists()) {
                        delfile.delete();
                    }
                    mRegister.remove(frface);
                    find = true;
                    break;
                }
            }

            if (find) {
                if (saveInfo()) {
                    //update all names
                    FileOutputStream fs = new FileOutputStream(mDBPath + "/face.txt", true);
                    ExtOutputStream bos = new ExtOutputStream(fs);
                    for (FaceRegist frface : mRegister) {
                        bos.writeString(frface.mName);
                    }
                    bos.close();
                    fs.close();
                }
            }
            return find;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean upgrade() {
        return false;
    }
}