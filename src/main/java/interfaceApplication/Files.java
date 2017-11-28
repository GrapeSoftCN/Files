package interfaceApplication;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import JGrapeSystem.rMsg;
import Model.CommonModel;
import apps.appsProxy;
import authority.plvDef.plvType;
import browser.PhantomJS;
import check.checkHelper;
import file.fileHelper;
import file.uploadFileInfo;
import httpClient.request;
import httpServer.grapeHttpUnit;
import image.imageHelper;
import interfaceModel.GrapeDBSpecField;
import interfaceModel.GrapeTreeDBModel;
import json.JSONHelper;
import nlogger.nlogger;
import rpc.execRequest;
import security.codec;
import session.session;
import string.StringHelper;
import time.TimeHelper;

public class Files {
    private GrapeTreeDBModel files;
    private GrapeDBSpecField gDbSpecField;
    private CommonModel model;
    private UploadModel uploadModel; // 文件上传转换相关
    private String thumailPath = "\\File\\upload\\icon\\folder.ico";
    private session se;
    private JSONObject userInfo = null;
    private String currentWeb = null;

    public Files() {
        uploadModel = new UploadModel();
        model = new CommonModel();

        files = new GrapeTreeDBModel();
        gDbSpecField = new GrapeDBSpecField();
        gDbSpecField.importDescription(appsProxy.tableConfig("Files"));
        files.descriptionModel(gDbSpecField);
        files.bindApp();

        se = new session();
        userInfo = se.getDatas();
        if (userInfo != null && userInfo.size() != 0) {
            currentWeb = userInfo.getString("currentWeb"); // 当前用户所属网站id
        }
        // files.enableCheck();//开启权限检查
    }

    /**
     * 新增文件夹
     * 
     * @param fileInfo
     * @return
     */
    @SuppressWarnings("unchecked")
    public String AddFolder(String fileInfo) {
        JSONObject object = JSONHelper.string2json(fileInfo);
        object.put("ThumbnailImage", thumailPath);
        object.put("filetype", 0);
        JSONObject temp = add(object);
        if (temp == null || temp.size() <= 0) {
            return rMsg.netMSG(1, "非法参数，新建文件夹失败");
        } else {
            return rMsg.netMSG(0, temp);
        }
    }

    /**
     * 新增操作
     * 
     * @param object
     * @return
     */
    @SuppressWarnings("unchecked")
    private JSONObject add(JSONObject object) {
        String info = null;
        JSONObject temp = null;
        JSONObject rMode = new JSONObject(plvType.chkType, plvType.powerVal).puts(plvType.chkVal, 100);// 设置默认查询权限
        JSONObject uMode = new JSONObject(plvType.chkType, plvType.powerVal).puts(plvType.chkVal, 200);
        JSONObject dMode = new JSONObject(plvType.chkType, plvType.powerVal).puts(plvType.chkVal, 300);
        object.put("rMode", rMode.toJSONString()); // 添加默认查看权限
        object.put("uMode", uMode.toJSONString()); // 添加默认修改权限
        object.put("dMode", dMode.toJSONString()); // 添加默认删除权限
        try {
            info = (String) files.data(object).autoComplete().insertEx();
        } catch (Exception e) {
            nlogger.logout(e);
        }
        if (StringHelper.InvaildString(info)) {
            temp = getFileInfo(info);
        }
        return temp;
    }

    /**
     * 新增网页型文件，即根据url获取缩略图
     * 
     * @param fileInfo
     * @return
     */
    @SuppressWarnings("unchecked")
    public String AddNetFile(String fileInfo) {
        String thumbnail = "";
        JSONObject temp = null;
        String result = rMsg.netMSG(100, "新增失败");
        fileInfo = codec.DecodeHtmlTag(fileInfo);
        fileInfo = codec.decodebase64(fileInfo);
        JSONObject object = JSONObject.toJSON(fileInfo);
        if (object != null && object.size() > 0) {
            // 判断数据库中是否已存在
            if (true) {
                result = rMsg.netMSG(1, "该文件已存在");
            }
            // 获取网页缩略图
            int width = Integer.parseInt(model.getFilePath("width"));
            int height = Integer.parseInt(model.getFilePath("height"));
            String url = getURL(object);
            if (url != null && !url.equals("")) {
                thumbnail = getNetImage(url, width, height);
            }
            object.put("ThumbnailImage", thumbnail);
            object.put("isdelete", 0);
            object.put("filetype", object.get("type")); // 网页类型文件
            object.put("fatherid", "0");
            object.remove("type");
            temp = add(object);
            result = rMsg.netMSG(0, "新增成功", (temp != null && temp.size() > 0) ? temp : new JSONObject());
        }
        return result;
    }

    /**
     * 获取url
     * 
     * @param object
     * @return
     */
    private String getURL(JSONObject object) {
        String url = "";
        if (object != null && object.size() > 0) {
            if (object.containsKey("url")) {
                url = object.getString("url");
            }
        }
        return url;
    }

    /**
     * 获取网页截图，返回相对路径
     * 
     * @param url
     * @param width
     * @param height
     * @return
     */
    private String getNetImage(String url, int width, int height) {
        String image = "";
        String path = "";
        try {
            PhantomJS pjs = new PhantomJS();
            if (url != null && !url.equals("")) {
                url = codec.DecodeHtmlTag(url);
                url = codec.decodebase64(url);
                image = pjs.sreenshot(width, height, url);
                image = "data:image/jpeg;base64," + image;
                path = getOutputFile();
                if (fileHelper.createFile(path)) {
                    path = imageHelper.generateImage(image, path) ? model.getImagepath(path) : "";
                }
            }
        } catch (Exception e) {
            nlogger.logout(e);
            path = "";
        }
        return path;
    }

    /**
     * 获取输出文件路径
     * 
     * @param ext
     *            文件扩展名
     * @return G://Image//File//upload//2017-10-28
     */
    private String getOutputFile() {
        String destFile = model.getFilePath("output");
        if (StringHelper.InvaildString(destFile)) {
            String date = model.getDate();
            String ext = model.getFilePath("ext");
            if (StringHelper.InvaildString(ext)) {
                destFile = destFile + "//" + date;
                File file = new File(destFile);
                if (!file.exists()) {
                    file.mkdirs();
                }
                destFile = destFile + "//" + TimeHelper.nowMillis() + ".jpg";
            }
        }
        return destFile;
    }

    /**
     * 上传文件
     * @param fatherid
     * @return
     */
    @SuppressWarnings("unchecked")
    public String FileUpload(String fatherid){
        JSONObject object;
        String fid = "";
        JSONObject obj = uploadModel.uploadFile(fatherid);
        String result = rMsg.netMSG(100, "文件上传失败");
        if (obj!=null && obj.size() > 0) {
            if (obj.containsKey("errorcode")) {
                return obj.toJSONString();
            }
            obj.put("wbid", currentWeb);
            fid = (String) files.data(uploadModel.AddFileInfo(obj)).insertOnce();
            // 查询文件信息
            object = getFileInfo(fid);
            result = (object != null && object.size() > 0) ? rMsg.netMSG(true, object) : result;
        }
        return result;
    }

    /**
     * 文件修改，即重命名
     * 
     * @param fid
     * @param fileInfo
     * @return
     */
    public String FileUpdate(String fid, String fileInfo) {
        boolean objects = false;
        JSONObject temp = null;
        String result = rMsg.netMSG(100, "修改失败");
        JSONObject object = JSONObject.toJSON(fileInfo);
        if (StringHelper.InvaildString(fid) && object != null && object.size() > 0) {
            objects = files.eq("_id", fid).data(object).updateEx();
            temp = getFileInfo(fid);
            result = objects ? rMsg.netMSG(0, "修改成功", (temp != null && temp.size() > 0) ? temp : new JSONObject()) : result;
        }
        return result;
    }

    /**
     * 将文件移动到某个文件夹内
     * 
     * @param fids
     * @param folderid
     * @return
     */
    public String FileUpdateBatch(String fids, String folderid) {
        int code = 99;
        String result = rMsg.netMSG(100, "文件移动至文件夹失败");
        // 验证文件夹是否存在
        if (getFileInfo(folderid) == null) {
            return rMsg.netMSG(1, "文件夹不存在");
        }
        if (StringHelper.InvaildString(fids)) {
            String FileInfo = "{\"fatherid\":\"" + folderid + "\"" + "}";
            code = updates(fids, JSONObject.toJSON(FileInfo));
            result = code == 0 ? rMsg.netMSG(0, "文件移动至文件夹成功") : result;
        }
        return result;
    }

    /**
     * 删除文件，将文件保存至回收站
     * 
     * @param fid
     * @return
     */
    public String RecyCle(String fid) {
        int code = 99;
        String fileInfo = "{\"isdelete\":1}";
        String result = rMsg.netMSG(100, "修改失败");
        code = RecyBatch(fid, JSONObject.toJSON(fileInfo));
        result = code == 0 ? rMsg.netMSG(0, "删除文件成功") : result;
        return result;
    }

    /**
     * 还原文件，从回收站还原文件
     * 
     * @param fid
     * @return
     */
    public String Restore(String fid) {
        int code = 99;
        String fileInfo = "{\"isdelete\":0}";
        ;
        String result = rMsg.netMSG(100, "修改失败");
        code = RecyBatch(fid, JSONObject.toJSON(fileInfo));
        result = code == 0 ? rMsg.netMSG(0, "删除文件成功") : result;
        return result;
    }

    /**
     * isdelete字段修改操作，即删除文件至回收站，从回收站还原文件
     * 
     * @param fid
     * @param FileInfo
     * @return
     */
    public int RecyBatch(String fid, JSONObject FileInfo) {
        if (!fid.contains(",")) {
            if (isfile(fid) == 0) {
                // 判断该文件夹下是否有文件
                fid = getfid(fid);
            }
        } else {
            fid = Batch(fid.split(","));
        }
        return updates(fid, FileInfo);
    }

    // 多个数据操作
    private String Batch(String[] fids) {
        ArrayList<String> list = new ArrayList<>();
        for (int i = 0, len = fids.length; i < len; i++) {
            if (isfile(fids[i]) == 0) {
                // 判断该文件夹下是否有文件
                list.add(getfid(fids[i]));
            } else {
                list.add(fids[i]);
            }
        }
        return StringHelper.join(list);
    }

    // 判断是否为文件
    private int isfile(String fid) {
        int ckcode = 0;
        String type = getFileInfo(fid).get("filetype").toString();
        if ("0".equals(type)) {
            ckcode = 0; // 文件夹
        } else {
            ckcode = 1; // 文件
        }
        return ckcode;
    }

    // 判断该文件夹下是否有文件，返回所有的id，包含文件夹id
    private String getfid(String fid) {
        ArrayList<String> list = new ArrayList<>();
        String cond = "{\"fatherid\":\"" + fid + "\"" + "}";
        JSONArray array = search(cond);
        if (array.size() != 0) { // 判断文件夹是否包含文件
            for (int i = 0, lens = array.size(); i < lens; i++) {
                JSONObject object = (JSONObject) array.get(i);
                JSONObject object2 = (JSONObject) object.get("_id");
                list.add(object2.get("$oid").toString());
            }
        }
        list.add(fid);
        return StringHelper.join(list);
    }

    /**
     * 批量修改文件
     * 
     * @return
     */
    private int updates(String fids, JSONObject FileInfo) {
        int code = 99;
        if (FileInfo != null && FileInfo.size() > 0) {
            String[] value = fids.split(",");
            files.or();
            for (String fid : value) {
                if (StringHelper.InvaildString(fid)) {
                    files.eq("_id", fid);
                }
            }
        }
        code = files.data(FileInfo).updateAll() != 0 ? 0 : 99;
        return code;
    }

    /**
     * 搜索
     * 
     * @param Info
     * @return
     */
    private JSONArray search(String Info) {
        JSONArray array = null;
        JSONArray condArray = model.buildCond(Info);
        int type = 0;
        // 验证查询条件是否含isdelete,若不含有isdelet，则添加查询条件(isdelete:0)
        if (condArray != null && condArray.size() > 0) {
            type = checkParam(condArray);
            switch (type) {
            case 0:
                array = null;
                break;
            case 1:
                files.eq("isdelete", 0);
            case 2:
                array = files.where(condArray).select();
                break;
            }
        }
        return array;
    }

    /**
     * 验证查询条件是否含isdelete
     * 
     * @param condArray
     * @return 0:参数错误;1:参数中不含有isdelete,2:含有isdelete
     */
    private int checkParam(JSONArray condArray) {
        String field = "";
        JSONObject obj;
        int type = 0; // 默认为0，参数错误
        if (condArray != null && condArray.size() > 0) {
            type = 1; // 参数中不含有isdelete
            for (Object object : condArray) {
                obj = (JSONObject) object;
                field = obj.getString("field");
                if (field.equals("isdelete")) {
                    type = 2;
                    break;
                }
            }
        }
        return type;
    }

    /**
     * 分页显示文件信息
     * 
     * @param idx
     * @param pageSize
     * @param fileInfo
     *            为空或者为null，显示所有的文件信息
     * @return
     */
    public String PageBy(int idx, int pageSize, String fileInfo) {
        long total = 0;
        if (StringHelper.InvaildString(fileInfo)) {
            JSONArray condArray = model.buildCond(fileInfo);
            if (condArray != null && condArray.size() > 0) {
                files.where(condArray);
            } else {
                return rMsg.netPAGE(idx, pageSize, total, new JSONArray());
            }
        }
        total = files.dirty().count();
        JSONArray array = files.asc("filetype").desc("time").page(idx, pageSize);
        return rMsg.netPAGE(idx, pageSize, total, (array != null && array.size() > 0) ? array : new JSONArray());
    }

    /**
     * 文件搜索，分类显示
     * 
     * @param fileInfo
     * @return
     */
    public String FindFile(String fileInfo) {
        JSONArray array = search(fileInfo);
        return rMsg.netMSG(true, (array != null && array.size() > 0) ? array : new JSONArray());
    }

    /**
     * 删除文件
     * 
     * @param FileInfo
     * @return
     */
    public String Delete(String FileInfo) {
        return BatchDelete(FileInfo);
    }

    public String BatchDelete(String FileInfo) {
        int code = 0;
        Object temp;
        boolean flag = false;
        String result = rMsg.netMSG(100, "文件删除失败");
        JSONArray array = JSONArray.toJSONArray(FileInfo);
        List<String> list = new ArrayList<>();
        List<String> lists = new ArrayList<>();
        long FIXSIZE = new Long((long) 4 * 1024 * 1024 * 1024);
        for (int i = 0, len = array.size(); i < len; i++) {
            JSONObject object = (JSONObject) array.get(i);
            if (object.containsKey("isdelete")) {
                flag = true;
                list.add(object.get("_id").toString());
            } else {
                temp = object.get("size");
                if (temp == null) {
                    lists.add(object.get("_id").toString());
                } else {
                    if ((long) temp > FIXSIZE) {
                        flag = true;
                        list.add(object.get("_id").toString());
                    } else {
                        lists.add(object.get("_id").toString());
                    }
                }
            }
        }
        if (flag) {
            code = ckDelete(StringHelper.join(list));
        }
        if (lists.size() != 0) {
            String infos = "{\"isdelete\":1}";
            code = RecyBatch(StringHelper.join(lists), JSONHelper.string2json(infos));
        }
        return code == 0 ? rMsg.netMSG(0, "删除成功") : result;
    }

    public int ckDelete(String fid) {
        if (!fid.contains(",")) {
            if (isfile(fid) == 0) {
                deleteall(fid);
            }
        } else {
            String[] value = fid.split(",");
            ArrayList<String> list = new ArrayList<>();
            for (int i = 0, len = value.length; i < len; i++) {
                if (isfile(value[i]) == 0) {
                    // 判断该文件夹下是否有文件
                    list.add(value[i]);
                }
            }
            if (list.size() != 0) {
                deleteall(StringHelper.join(list));
            }
        }
        return delete(fid);
    }

    private void deleteall(String fid) {
        if (fid.contains(",")) {
            files.or();
            String[] value = fid.split(",");
            for (int i = 0, len = value.length; i < len; i++) {
                files.eq("fatherid", value[i]);
            }
        } else {
            files.eq("fatherid", fid);
        }
        files.deleteAll();
    }

    // 删除文件[包含批量删除]
    private int delete(String fid) {
        if (fid.contains(",")) {
            files.or();
            String[] value = fid.split(",");
            for (int i = 0, len = value.length; i < len; i++) {
                files.eq("_id", new ObjectId(value[i]));
            }
        } else {
            files.eq("_id", new ObjectId(fid));
        }
        return files.deleteAll() != 0 ? 0 : 99;
    }

    /**
     * 查询文件信息
     * 
     * @param fid
     * @return
     */
    private JSONObject getFileInfo(String fid) {
        JSONObject object = null;
        if (StringHelper.InvaildString(fid)) {
            object = files.eq("_id", fid).find();
        }
        return object;
    }

    /**
     * 获取文件内容
     * 
     * @param fid
     * @return
     */
    public String getWord(String fid) {
        JSONObject object = getFileInfo(fid);
        String message = rMsg.netMSG(1, "连接文件服务器失败，无法获取文件内容");
        if (object != null) {
            try {
                // String hoString = "http://" + getFileIp("file", 0);
                String hoString = getConfig("fileHost");
                String filepath = object.get("filepath").toString();
                filepath = filepath.replace("\\", "@t");
                message = request.Get(hoString + "/FileServer/FileConvert?sourceFile=" + filepath + "&type=2");
                message = message.replace("gb2312", "utf-8");
            } catch (Exception e) {
                e.printStackTrace();
                message = rMsg.netMSG(1, "连接文件服务器失败，无法获取文件内容");
            }
        }
        return message;
    }

    /**
     * 根据文件id获取文件信息
     * 
     * @project GrapeFile
     * @package interfaceApplication
     * @file Files.java
     * 
     * @param fid
     * @return {fid1:{},fid2:{}}
     *
     */
    @SuppressWarnings("unchecked")
    public String getFileByID(String fid) {
        String id;
        JSONObject object;
        JSONObject rObject = new JSONObject();
        String[] value = fid.split(",");
        files.or();
        for (String tempid : value) {
            if (StringHelper.InvaildString(tempid)) {
                if (ObjectId.isValid(tempid) || checkHelper.isInt(tempid)) {
                    files.eq("_id", tempid);
                }
            }
        }
        JSONArray array = files.field("_id,fileoldname,size,filetype,filepath,ThumbnailImage").select();
        if (array != null && array.size() != 0) {
            for (Object object2 : array) {
                object = (JSONObject) object2;
                id = object.getMongoID("_id");
                rObject.put(id, setFile(object));
            }
        }
        return (rObject != null && rObject.size() > 0) ? rObject.toJSONString() : new JSONObject().toJSONString();
    }

    /**
     * 添加显示路径
     * 
     * @project GrapeFile
     * @package interfaceApplication
     * @file Files.java
     * 
     * @param object
     * @return
     *
     */
    @SuppressWarnings("unchecked")
    private JSONObject setFile(JSONObject object) {
        String value = "";
        String[] key = { "filepath", "ThumbnailImage" };
        if (object != null && object.size() > 0) {
            // String FileDir = getAppIp("file").split("/")[1];
            String FileDir = getConfig("fileHost");
            for (String string : key) {
                if (object.containsKey(string)) {
                    value = FileDir + object.getString(string);
                    object.put(string, value);
                }
            }
        }
        return object;
    }

    private String getConfig(String key) {
        String value = "";
        try {
            JSONObject object = appsProxy.configValue().getJson("other");
            if (object != null && object.size() > 0) {
                value = object.getString(key);
            }
        } catch (Exception e) {
            nlogger.logout(e);
            value = "";
        }
        return value;
    }
}
