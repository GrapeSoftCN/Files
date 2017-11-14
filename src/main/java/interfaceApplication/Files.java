package interfaceApplication;

import java.io.File;
import java.util.ArrayList;

import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import JGrapeSystem.rMsg;
import Model.CommonModel;
import apps.appsProxy;
import browser.PhantomJS;
import file.fileHelper;
import file.uploadFileInfo;
import httpServer.grapeHttpUnit;
import image.imageHelper;
import interfaceModel.GrapeDBSpecField;
import interfaceModel.GrapeTreeDBModel;
import json.JSONHelper;
import nlogger.nlogger;
import rpc.execRequest;
import security.codec;
import string.StringHelper;
import time.TimeHelper;

public class Files {
    private GrapeTreeDBModel files;
    private GrapeDBSpecField gDbSpecField;
    private CommonModel model;
    private UploadModel uploadModel; // 文件上传转换相关
    private String thumailPath = "\\File\\upload\\icon\\folder.ico";

    public Files() {
        uploadModel = new UploadModel();
        model = new CommonModel();

        files = new GrapeTreeDBModel();
        gDbSpecField = new GrapeDBSpecField();
        gDbSpecField.importDescription(appsProxy.tableConfig("Files"));
        files.descriptionModel(gDbSpecField);
        files.bindApp();
    }

    /**
     * 上传文件
     * 
     * @param fatherid
     * @return
     */
    @SuppressWarnings("unchecked")
    public String uploadFile(String fatherid) {
        long size = 0;
        String oldname = "", extname = "", type = "", fid;
        uploadFileInfo out = null;
        JSONObject Info = new JSONObject(), object = null; // 文件信息
        String result = rMsg.netMSG(100, "文件上传失败");
        JSONObject post = (JSONObject) execRequest.getChannelValue(grapeHttpUnit.formdata);
        if (post != null) {
            if (!post.containsKey("file")) {
                out = (uploadFileInfo) post.get("media");
                oldname = out.getClientName();
                type = out.getFileType();
                String tip = out.getContent().toString();
                File tempfile = new File(tip);
                if (tempfile.exists()) {
                    size = tempfile.length();
                }
            } else {
                out = (uploadFileInfo) post.get("file");
                oldname = post.getString("name");
                type = post.getString("type");
                size = Long.parseLong(post.getString("size"));
            }
            Info.put("fileoldname", oldname);
            Info.put("filetype", type);
            Info.put("size", size);
            Info.put("fileextname", extname);
            String tip = uploadModel.upload(out, oldname, Info); // 上传操作
            if (tip.equals("true")) {
                fid = (String) files.data(uploadModel.AddFileInfo(Info)).insertOnce();
                // 查询文件信息
                object = getFileInfo(fid);
                result = (object != null && object.size() > 0) ? rMsg.netMSG(true, object) : result;
            }
        }
        return result;
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
        JSONObject temp = add(object);
        return rMsg.netMSG(0, "新增成功", (temp != null && temp.size() > 0) ? temp : new JSONObject());
    }

    /**
     * 新增操作
     * 
     * @param object
     * @return
     */
    private JSONObject add(JSONObject object) {
        JSONObject temp = null;
        String info = (String) files.data(object).insertOnce();
        temp = getFileInfo(info);
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
     * 文件修改，即重命名
     * 
     * @param fid
     * @param fileInfo
     * @return
     */
    public String FileUpdate(String fid, String fileInfo) {
        int code = 99;
        JSONObject temp = null;
        String result = rMsg.netMSG(100, "修改失败");
        JSONObject object = JSONObject.toJSON(fileInfo);
        if (!StringHelper.InvaildString(fid) && ObjectId.isValid(fid) && object != null && object.size() > 0) {
            code = files.eq("_id", fid).data(object).update() != null ? 0 : 99;
            temp = getFileInfo(fid);
            result = code == 0 ? rMsg.netMSG(0, "新增成功", (temp != null && temp.size() > 0) ? temp : new JSONObject())
                    : result;
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
        if (!StringHelper.InvaildString(fids)) {
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
            for (String fid : value) {
                if (ObjectId.isValid(fid)) {
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
        if (!StringHelper.InvaildString(fileInfo)) {
            JSONArray condArray = model.buildCond(fileInfo);
            if (condArray != null && condArray.size() > 0) {
                files.where(condArray);
            } else {
                return rMsg.netPAGE(idx, pageSize, total, new JSONArray());
            }
        }
        JSONArray array = files.dirty().page(idx, pageSize);
        total = files.count();
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
        return null;
    }

    /**
     * 查询文件信息
     * 
     * @param fid
     * @return
     */
    private JSONObject getFileInfo(String fid) {
        JSONObject object = null;
        if (!StringHelper.InvaildString(fid)) {
            object = files.eq("_id", fid).find();
        }
        return object;
    }

}
