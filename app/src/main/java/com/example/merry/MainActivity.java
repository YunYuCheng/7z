package com.example.merry;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.storage.StorageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;


import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final static long bigsize=1024*1024;
    private final static long midsize=1024*100;
    private final static long smallsize=1024*10;



    private ProgressBar progress;
    private Button start;
    private Button back;
    private TextView tv;

    private EditText editText;
    private AlertDialog dialog;

    private String fileName="";
    private static String rootPath="";

    private ListView list;
    private ArrayAdapter<String> adapter;

    private static String[] from;
    private static List<String> fromlist=new ArrayList<>();

    private static final long PROGRESS_MAX =1;
    private static final long UPDATE = 2;

    private String fromPath="";
    private String toPath="";
    private String psw=null;

    private long contentLen;
    private static int filesize;

    private Button button;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences pref = getSharedPreferences("data", MODE_PRIVATE);

        progress=(ProgressBar)findViewById(R.id.process);
        start=(Button)findViewById(R.id.start);
        back=(Button)findViewById(R.id.back);
        tv = (TextView) findViewById(R.id.text);
        list=(ListView)findViewById(R.id.list);

        View view = getLayoutInflater().inflate(R.layout.dialog_view, null);
        editText = (EditText) view.findViewById(R.id.dialog_edit);

        start.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                // TODO 这里需要改一下，一开始应该加载的是自身内存卡或者外置内存
                fromlist.add("自身内存");
                fromlist.add("外置内存");
                from=new String[fromlist.size()];
                for(int i=0;i<fromlist.size();i++){
                    from[i]=fromlist.get(i);
                }
                ThreadOutput(1);           //显示对应的列表

            }

        });

        back.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(fromPath.equals(rootPath)||fromPath.equals("")||fromPath.equals(" ")||fromPath==null)
                    return ;
                fromPath=fromPath.substring(0,fromPath.lastIndexOf(File.separator));
                getFilePath(new File(fromPath));
                ThreadOutput(1);
            }
        });

        dialog= new AlertDialog.Builder(this)
                .setTitle("请输入密码")     //设置对话框的标题
                .setView(view)
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        editText.setText("");
                        dialog.dismiss();
                    }
                })
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        psw=editText.getText().toString();
                        Log.i("TAG", psw);
                        new DownLoad().execute(fromPath,toPath,psw);                          //但是这个需要密码的时候，再次执行就不会显示进度
                        editText.setText("");
                    }
                }).create();

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                fileName = fromlist.get(position);
                if(fileName.equals("自身内存")){
                     isExternal=true;
                     getRootPath();
                     return;
                }else if(fileName.equals("外置内存")){
                    isExternal=false;
                    getRootPath();
                    return;
                }

                if(fileName.endsWith(".7z")){                                       //要是还在原来的目录点击，那么就得截取
                    String path="";
                    //内置
                    // content://com.android.externalstorage.documents/tree/primary%3Alibs
                    //content://com.android.externalstorage.documents/tree/primary:libs
                    //外置
                    // content://com.android.externalstorage.documents/tree/542C-8214%3A%E6%96%B0%E5%BB%BA%E6%96%87%E4%BB%B6%E5%A4%B9
                    //content://com.android.externalstorage.documents/tree/542C-8214:新建文件夹
                    try {
                        path=URLDecoder.decode(mask, "UTF-8").toString();
                        if(path.contains("primary")){
                            isExternal_mk=true;
                            getrtPath(MainActivity.this);

                        }else{
                            isExternal_mk=false;
                            ArrayList<StorageBean> externel = getStorageData(MainActivity.this);
                            File file;
                            for(int i=0;i<externel.size();i++){
                                if(externel.get(i).getPath().contains("emulated")){         //这个一开始并没有对应的值
                                    continue;
                                }
                                rootPath=externel.get(i).getPath();
                                break;
                            }
                        }
                        toPath=rootPath;
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    Log.i("TAG24",toPath);
                    Log.i("TAG24",path);
                    if(path.lastIndexOf(":")!=mask.length()-1)
                        toPath+=(File.separator+path.substring(path.lastIndexOf(":")+1));               //这时toPath已经赋值为rootPath了

                    fromPath+=(File.separator+fileName);
                    Log.i("TAG24",toPath);                            //TODO
                    new DownLoad().execute(fromPath,toPath,null).getStatus();      //一开始先用一个没有密码的解压

                }else if(!fileName.equals("自身内存")&&!fileName.equals("外置内存")){
                    fromPath+=(File.separator+fileName);
                    File file=new File(fromPath);
                    getFilePath(file);
                    ThreadOutput(1);
                }
                Toast.makeText(MainActivity.this, fileName,Toast.LENGTH_SHORT).show();
            }
        });

        button=(Button)findViewById(R.id.button);                                                      //这个之后会按照一开始的方式来实现
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent  = new Intent("android.intent.action.OPEN_DOCUMENT_TREE");           //这种方法只能选择文件夹，无法选择文件
                MainActivity.this.startActivityForResult(intent,42);
            }
        });
    }

    private boolean isExternal;
    public void getRootPath(){
        if(isExternal) {
            getrtPath(MainActivity.this);
            getFilePath(new File(rootPath));
        }
        else{
            ArrayList<StorageBean> externel = getStorageData(MainActivity.this);
            Log.i("TAG4",String.valueOf(externel.size()));     //突然想起来这个错误的原因，应该选择第二个的
            File file;
            for(int i=0;i<externel.size();i++){
                if(externel.get(i).getPath().contains("emulated")){         //这个一开始并没有对应的值
                    continue;
                }
                rootPath=externel.get(i).getPath();
                file=new File(externel.get(i).getPath());
                getFilePath(file);
                break;
            }
        }
       //TODO  切记在选择了外置内存卡的时候，rootPath与mask可不一样，rootPath只是为了获取路径；对于内置内存卡，rootPath则一样
        fromPath = rootPath;
        toPath = rootPath;
        Log.i("TAG23",rootPath);
        ThreadOutput(1);
    }

    /**
     * 获取内置内存根路径
     * @param context
     * @return
     */
    public static String getrtPath(Context context){            //选择了自身内存
        String path="";
        // 是否有SD卡
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)|| !Environment.isExternalStorageRemovable()) {  //可是明明是sd卡为什么显示的是内存
            rootPath=Environment.getExternalStorageDirectory().toString();
            for(File file : Environment.getExternalStorageDirectory().listFiles()){
                if(file.isDirectory()&&!file.isHidden()){
                    path+=file.getName()+"\n";
                }
            }
            return path;
        } else {
            return android.os.Environment.getExternalStorageDirectory().toString();        // 无
        }
    }

    public static ArrayList<StorageBean> getStorageData(Context pContext) {         //选择外置内存，需要通过反射来实现

        final StorageManager storageManager = (StorageManager) pContext.getSystemService(Context.STORAGE_SERVICE);

        try {
            //得到StorageManager中的getVolumeList()方法的对象
            final Method getVolumeList = storageManager.getClass().getMethod("getVolumeList");
            //---------------------------------------------------------------------

            //得到StorageVolume类的对象
            final Class<?> storageValumeClazz = Class.forName("android.os.storage.StorageVolume");
            //---------------------------------------------------------------------
            //获得StorageVolume中的一些方法
            final Method getPath = storageValumeClazz.getMethod("getPath");
            Method isRemovable = storageValumeClazz.getMethod("isRemovable");

            Method mGetState = null;
            //getState 方法是在4.4_r1之后的版本加的，之前版本（含4.4_r1）没有
            // （http://grepcode.com/file/repository.grepcode.com/java/ext/com.google.android/android/4.4_r1/android/os/Environment.java/）
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                try {
                    mGetState = storageValumeClazz.getMethod("getState");
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }    //调用getVolumeList方法，参数为：“谁”中调用这个方法
            final Object invokeVolumeList = getVolumeList.invoke(storageManager);
            //---------------------------------------------------------------------
            final int length = Array.getLength(invokeVolumeList);
            ArrayList<StorageBean> list = new ArrayList<>();
            for (int i = 0; i < length; i++) {
                final Object storageValume = Array.get(invokeVolumeList, i);//得到StorageVolume对象
                final String path = (String) getPath.invoke(storageValume);
                final boolean removable = (Boolean) isRemovable.invoke(storageValume);
                String state = null;
                if (mGetState != null) {
                    state = (String) mGetState.invoke(storageValume);
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        state = Environment.getStorageState(new File(path));
                    } else {
                        if (removable) {
                            state = Environment.getStorageState(new File(path));
                        } else {
                            //不能移除的存储介质，一直是mounted
                            state = Environment.MEDIA_MOUNTED;
                        }
                        final File externalStorageDirectory = Environment.getExternalStorageDirectory();          //TODO 这个在之后可以删除
                        Log.i("TAG", "externalStorageDirectory==" + externalStorageDirectory);
                    }
                }
                StorageBean storageBean = new StorageBean();
                storageBean.setMounted(state);
                storageBean.setPath(path);
                storageBean.setRemovable(removable);
                list.add(storageBean);
            }
            return list;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 获取每一个文件夹的文件列表
     * @param tempfile
     */
    public static void getFilePath(File tempfile){
        if(tempfile.isFile()){
            Log.i("TAG", "选择了文件");
            return ;
        }
        Log.i("TAG3", tempfile.getName());
        fromlist.clear();
        for(File temp:tempfile.listFiles()){         //原来listFiles是绝对路径一起打印,但是一旦getName就是只有文件和目录名字，而list只是打印当前的目录的名字
            if(!temp.isHidden()){
                fromlist.add(temp.getName().toString());
            }
        }
        from=new String[fromlist.size()];
        for(int i=0;i<fromlist.size();i++){
            from[i]=fromlist.get(i);
        }
    }

    /**
     * 执行线程任务
     */
    private int flags=0;
    public void ThreadOutput(int flag){
        flags=flag;
        new Thread(new Runnable(){
            @Override
            public void run() {
                Message msg=new Message();
                msg.what=flags;
                handler.sendMessage(msg);
            }
        }).start();
    }
    private Handler handler=new Handler(){
        public void handleMessage(Message msg){
            switch(msg.what){
                case 1:
                    adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, from);
                    list.setAdapter(adapter);
                    break;
                case 2:
                    dialog.show();
                    break;
                case 3:
                    Toast.makeText(MainActivity.this,"请确认您选择的是一个文件夹或者一个7z文件",Toast.LENGTH_LONG).show();
                    break;
                default:
                    break;
            }
        }
    };
    private boolean isExternal_mk=true;
    private String mask="";             //TODO   其实在选择文件路径之后，就还需要将前面那个码取出来，但是解压的时候还是mask起作用
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (resultCode == RESULT_OK) {
            Uri treeUri = resultData.getData();
            mask=treeUri.toString();
            Log.i("TAG",mask);
        }
    }

    class DownLoad extends AsyncTask<String,Long,String> {
        private SevenZFile sevenZFile=null;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progress.setProgress(0);
        }
        @Override
        protected String doInBackground(String... params) {

            String orgPath=params[0];
            String tarpath=params[1];
            String password=params[2];
            Log.i("TAG", orgPath+"_"+tarpath+"_"+password);

            byte[] passendcoder=null;
            long sum=0;

            try {
                if(password==null){
                    sevenZFile= new SevenZFile(new File(orgPath));       //TODO 其实应该先读取一下，要不然还是会走到下面去，直到read
                }else{
                    Charset charset=Charset.forName("UnicodeLittleUnmarked");
                    ByteBuffer buf=charset.encode(password);
                    passendcoder=buf.array();
                    sevenZFile= new SevenZFile(new File(orgPath),passendcoder);
                }
                //sevenZFile.read(new byte[1024]);                   //这样就不至于一直走下去
                contentLen=new File(orgPath).length();
                if(contentLen>=new File(toPath).getFreeSpace()){
                    return "空间不足";
                }
                publishProgress(PROGRESS_MAX,contentLen);

                if(contentLen>=bigsize){
                    filesize=4096;
                }else if(contentLen>=midsize){
                    filesize=2048;
                }else{
                    filesize=1024;
                }
                Log.i("TAG", " "+filesize);

                SevenZArchiveEntry entry = sevenZFile.getNextEntry();

                if (isExternal_mk) {
                    while (entry != null) {
                        if (entry.isDirectory() && !new File(tarpath + File.separator + entry.getName()).exists()) {
                            new File(tarpath + File.separator + entry.getName()).mkdirs();
                            entry = sevenZFile.getNextEntry();
                            continue;
                        }
                        File file = new File(tarpath + File.separator + entry.getName());
                        if (!file.exists()) {
                            if (!file.getParentFile().exists()) {
                                file.getParentFile().mkdirs();
                            }
                            try {
                                file.createNewFile();

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        FileOutputStream out = new FileOutputStream(file.getAbsolutePath());
                        byte[] bytes = new byte[filesize];
                        int len;
                        while ((len = sevenZFile.read(bytes)) != -1) {
                            sum += filesize;
                            out.write(bytes, 0, len);
                            publishProgress(UPDATE, sum);
                        }
                        entry = sevenZFile.getNextEntry();
                    }
                }else{
                    //TODO 先就这样，但时候再改
                    //mask="content://com.android.externalstorage.documents/tree/542C-8214%3A%E6%96%B0%E5%BB%BA%E6%96%87%E4%BB%B6%E5%A4%B9%2FJavascript%2F%E8%A7%86%E9%A2%91";
//                    if(mask.equals("")||mask.equals(" "))
//                        mask="content://com.android.externalstorage.documents/tree/542C-8214%3A";        //这里还缺一些东西，不过应该这个码应该不一样
//                    String temp=URLDecoder.decode(mask, "UTF-8");
//                    if(temp.lastIndexOf(":")!=mask.length()-1)
//                        tarpath+=File.separator+temp.substring(temp.lastIndexOf(":")+1);                //TODO  这里要和mask同步，这里也可能会出错，会缺少文件分隔符
//                    else{
//                        tarpath= tarpath.substring(0,tarpath.length()-1);                 //这个有文件分隔符
//                    }
                    Log.i("TAG22",tarpath);

                    Uri treeUri=Uri.parse((String) mask);
                    Log.i("TAG26",treeUri.toString());
                    DocumentFile  pickedDir  = DocumentFile.fromTreeUri(MainActivity.this, treeUri);        //这个就是创建以这个文件为根的树，接下来就是在这个树中创建文件
                    DocumentFile rootDir=pickedDir;
                    while (entry != null) {
                        File file = new File(tarpath + File.separator + entry.getName());
                        //之所以这个没有效果，是因为这个扫描文件的原因，因为这个会先扫描文件，之后才是文件夹
                        if (entry.isDirectory() ||file.isDirectory()) {         //为什么这个有没有效果，造了文件夹就应该向文件夹里面去写，所以才会出现在的情况,
                            Log.i("TAG2",entry.getName());                            //很显然对于存在的情况自己没考虑
                            if(!file.exists()){
                                String[] dirnames= entry.getName().split(File.separator);   //在android里面不允许斜杠的出现
                                String dir="";
                                for(int i=0;i<dirnames.length;i++){                       //每次都进入这个文件所属的文件夹
                                    dir+=dirnames[i];
                                    if (!new File(tarpath + File.separator + dir).exists()) {                    //直到这个文件的父目录出现，那也就到了倒数第二个了
                                        pickedDir.createDirectory(dirnames[i]);
                                        Log.i("TAG2","创建了"+dirnames[i]);
                                    }
                                    DocumentFile subDir = pickedDir.findFile(dirnames[i]);
                                    pickedDir=subDir;
                                    dir+=File.separator;
                                }
                            }
                            entry = sevenZFile.getNextEntry();
                            pickedDir= rootDir;
                            continue;
                        }
                        Log.i("TAG1",entry.getName());
                        int index=entry.getName().lastIndexOf(File.separator);
                        String fname=null;
                        if(index>=0){
                            fname=entry.getName().substring(index);
                        }else{
                            fname=entry.getName();
                        }

                        DocumentFile newFile=null;
                        if (!file.exists()) {                                            //之前果然是这里出错了
                            String[] dirnames= entry.getName().split(File.separator);   //在android里面不允许斜杠的出现
                            String dir="";
                            for(int i=0;i<dirnames.length-1;i++){                       //每次都进入这个文件所属的文件夹
                                dir+=dirnames[i];
                                if (!new File(tarpath + File.separator + dir).exists()) {                    //直到这个文件的父目录出现，那也就到了倒数第二个了
                                    pickedDir.createDirectory(dirnames[i]);
                                    Log.i("TAG1","创建了"+dirnames[i]);
                                }
                                DocumentFile subDir = pickedDir.findFile(dirnames[i]);
                                pickedDir=subDir;
                                dir+=File.separator;
                            }

                            try {
                                if(fname.endsWith("jpg")||fname.endsWith("jpeg")||fname.endsWith("JPG")||fname.endsWith("JPEG")||fname.endsWith("PNG")||fname.endsWith("png")){          //TODO  之后继续补充
                                    newFile=pickedDir.createFile("image/jpeg",fname);
                                }else if(fname.endsWith("txt")){
                                    newFile=pickedDir.createFile("text/plain",fname);
                                }else if(fname.endsWith("mp4")){
                                    newFile=pickedDir.createFile("video/mp4",fname);
                                }else if(fname.endsWith("flv")){
                                    newFile=pickedDir.createFile("video/x-flv",fname);
                                }else if(fname.endsWith("avi")){
                                    newFile=pickedDir.createFile("video/x-msvideo",fname);
                                }else if(fname.endsWith("mkv")){
                                    newFile=pickedDir.createFile("video/x-matroska",fname);
                                }else if(fname.endsWith("mp3")){
                                    newFile=pickedDir.createFile("audio/mpeg",fname);
                                }else if(fname.endsWith("wav")){
                                    newFile=pickedDir.createFile("audio/x-wav",fname);
                                }else if(fname.endsWith("wma")){
                                    newFile=pickedDir.createFile("audio/x-ms-wma",fname);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        OutputStream out = getContentResolver().openOutputStream(newFile.getUri());
                        byte[] bytes = new byte[filesize];
                        int len;
                        while ((len = sevenZFile.read(bytes)) != -1) {
                            sum += filesize;
                            out.write(bytes, 0, len);
                            publishProgress(UPDATE, sum);
                        }
                        entry = sevenZFile.getNextEntry();
                        pickedDir= rootDir;
                    }
                }
                sevenZFile.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }catch (IOException e) {
                e.printStackTrace();
                return "解压失败";
            }catch (Exception e1){
                e1.printStackTrace();
                return "解压失败";
            }
            return "解压成功";
        }
        @Override
        protected void onProgressUpdate(Long... values) {
            super.onProgressUpdate(values);
            if(values[0]==PROGRESS_MAX){
                progress.setMax(100);
            }else if(values[0]==UPDATE){
                progress.setProgress((int)(values[1]*100/contentLen));
               // Log.i("TAG",String.valueOf(progress.getProgress()));
                int i=progress.getProgress();
                tv.setText("解压进度："+i+"%");
            }
        }
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if(s.equals("解压成功")){
                progress.setProgress(100);
                tv.setText(s);
                //progress.setVisibility(View.GONE);
            }else if(s.equals("解压失败")){
                dialog.show();
                return ;
            }else{
                tv.setText(s);
            }
        }
    }
    public void onDestroy(){
        ThreadOutput(5);
        super.onDestroy();
    }
}
