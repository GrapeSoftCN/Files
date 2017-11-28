package interfaceApplication;

import java.io.File;
import java.io.FileOutputStream;

import org.json.simple.JSONObject;

import JGrapeSystem.rMsg;
import Model.CommonModel;
import authority.plvDef.plvType;
import file.fileHelper;
import file.uploadFileInfo;
import httpServer.grapeHttpUnit;
import io.netty.buffer.ByteBuf;
import net.coobird.thumbnailator.Thumbnails;
import nlogger.nlogger;
import rpc.execRequest;
import session.session;
import string.StringHelper;
import time.TimeHelper;

public class UploadModel {
    private CommonModel model;
    private session se;
    private JSONObject userInfo = null;
    private String currentWeb = null;

    public UploadModel() {
        model = new CommonModel();

        se = new session();
        userInfo = se.getDatas();
        if (userInfo != null && userInfo.size() != 0) {
            currentWeb = userInfo.getString("currentWeb"); // 当前用户所属网站id
        }
    }

    /**
     * 上传文件
     *
     * @param fatherid
     * @return 上传成功，返回文件信息
     */
    @SuppressWarnings("unchecked")
    public JSONObject uploadFile(String fatherid) {
        long size = 0;
        String oldname = "", extname = "", type = "", tip = null;
        uploadFileInfo out = null;
        JSONObject Info = new JSONObject();
        JSONObject rMode = new JSONObject(plvType.chkType, plvType.powerVal).puts(plvType.chkVal, 100);// 设置默认查询权限
        JSONObject uMode = new JSONObject(plvType.chkType, plvType.powerVal).puts(plvType.chkVal, 200);
        JSONObject dMode = new JSONObject(plvType.chkType, plvType.powerVal).puts(plvType.chkVal, 300);
        Info.put("rMode", rMode.toJSONString()); // 添加默认查看权限
        Info.put("uMode", uMode.toJSONString()); // 添加默认修改权限
        Info.put("dMode", dMode.toJSONString()); // 添加默认删除权限
        JSONObject post = (JSONObject) execRequest.getChannelValue(grapeHttpUnit.formdata);
        if (post != null && post.size() > 0) {
            if (!post.containsKey("file")) {
                out = (uploadFileInfo) post.get("media");
                if (out == null) {
                    return JSONObject.toJSON(rMsg.netMSG(1, "获取上传文件失败"));
                }
                oldname = out.getClientName();
                type = out.getFileType();
                tip = out.getContent().toString();
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
            tip = upload(out, oldname, Info); // 上传操作
        }
        return (StringHelper.InvaildString(tip) && tip.equals("true")) ? Info : null;
    }

    /**
     * 上传文件操作
     * 
     * @param out
     * @param oldname
     *            文件名称
     * @param Info
     *            文件信息
     * @return
     */
    @SuppressWarnings("unchecked")
    public String upload(uploadFileInfo out, String oldname, JSONObject Info) {
        boolean flag = true;
        Object rString = "true";
        String extname = getExt(oldname); // 获取文件扩展名
        String newname = TimeHelper.nowMillis() + model.getUnqueue() + "." + extname; // 文件新名称
        String Date = TimeHelper.stampToDate(TimeHelper.nowMillis()).split(" ")[0];
        String dirpath = model.getFilePath("filepath") + Date; // 上传文件存储地址
        if (!new File(dirpath).exists()) {
            new File(dirpath).mkdir();
        }
        String filepath = dirpath + "//" + newname;
        File file = new File(filepath);
        try {
            while (flag) {
                if (file.exists()) {
                    newname = TimeHelper.nowMillis() + model.getUnqueue() + "." + extname;
                    filepath = dirpath + newname;
                } else {
                    flag = false;
                }
            }
            if (out.isBuff()) {
                FileOutputStream fin = null;
                try {
                    fin = new FileOutputStream(file);
                    ByteBuf buff = (ByteBuf) out.getLocalBytes();
                    buff.readBytes(fin, buff.readableBytes());
                } catch (Exception e) {
                    e.printStackTrace();
                    rString = "false";
                } finally {
                    fin.close();
                }
            } else {
                File src = out.getLocalFile();
                rString = src.renameTo(file);
            }
            Info.put("filenewname", newname);
            Info.put("filepath", filepath);
        } catch (Exception e) {
            nlogger.logout(e);
        }
        return rString.toString();
    }

    /**
     * 获取文件扩展名
     * 
     * @project GrapeFile
     * @package interfaceApplication
     * @file Files.java
     * 
     * @param name
     * @return
     *
     */
    public String getExt(String name) {
        String extname = "";
        if (name.contains(".")) {
            extname = name.substring(name.lastIndexOf(".") + 1);
        }
        return extname;
    }

    @SuppressWarnings("unchecked")
    protected JSONObject AddFileInfo(JSONObject info) {
        int filetype = 0;
        String type = "";
        if (info != null && info.size() > 0) {
            info.put("wbid", currentWeb);
            if (info.containsKey("type")) {
                type = info.getString("type");
                info.put("filetype", getType(type));
            }
            // 获取缩略图
            info = getThumbnail(filetype, info);
            info = getFileUrl(info);
        }
        return info;
    }

    /**
     * 图片类型文件，显示图片缩略图，视频类型的文件显示视频缩略图
     * 
     * @project GrapeFile
     * @package interfaceApplication
     * @file Files.java
     * 
     * @param filetype
     * @return
     *
     */
    @SuppressWarnings("unchecked")
    private JSONObject getThumbnail(int filetype, JSONObject object) {
        String newname = "", out = "", filepath = "";
        String dirpath = model.getFilePath("filepath");
        String Date = TimeHelper.stampToDate(TimeHelper.nowMillis()).split(" ")[0];
        String ThumbailsPath = dirpath + Date + "\\Thumbails"; // 缩略图地址
        if (!new File(ThumbailsPath).exists()) {
            new File(ThumbailsPath).mkdir();
        }
        if (object != null && object.size() > 0) {
            if (object.containsKey("newname")) {
                newname = object.getString("newname");
                newname = (StringHelper.InvaildString(newname)) ? TimeHelper.nowMillis() + model.getUnqueue() : newname.substring(0, newname.lastIndexOf("."));
            }
            if (object.containsKey("filepath")) {
                filepath = object.getString("filepath");
            }
        }
        String outpath = ThumbailsPath + newname.substring(0, newname.lastIndexOf("."));
        switch (filetype) {
        case 1: // 图片
            out = ImageThumbnail(filepath, outpath);
            break;
        case 2: // 视频
            out = VideoThumbnail(filepath, outpath);
            break;

        default:
            break;
        }
        object.put("ThumbnailImage", out);
        return object;
    }

    /**
     * 获取图片缩略图
     * 
     * @project GrapeFile
     * @package model
     * @file FileModel.java
     * 
     * @param outpath
     * @return
     *
     */
    private String ImageThumbnail(String inputPath, String outpath) {
        String out = "";
        JSONObject thumbnail = JSONObject.toJSON(model.getFilePath("ThumbnailImage"));
        if (thumbnail != null && thumbnail.size() > 0) {
            try {
                if (fileHelper.createFileEx(outpath)) {
                    Thumbnails.of(inputPath).scale(Double.parseDouble(thumbnail.getString("ImageScale"))).outputFormat(thumbnail.getString("ImgType")).outputQuality(Float.parseFloat(thumbnail.getString("ImgQuality"))).toFile(outpath);
                }
                out = outpath;
            } catch (Exception e) {
                nlogger.logout(e);
                out = "";
            }
        }
        return out;
    }

    /**
     * 获取视频截图
     * 
     * @project GrapeFile
     * @package model
     * @file FileModel.java
     * 
     * @param outpath
     * @return
     *
     */
    private String VideoThumbnail(String inputPath, String outpath) {
        // List<String> commend = new ArrayList<String>();
        // commend.add(getPath("ffmpegUrl"));
        // commend.add("-i");
        // commend.add(inputPath);
        // commend.add("-y");
        // commend.add("-ss");
        // commend.add(getPath("Video")); // 截取多少秒之后的图片
        // commend.add("-s");
        // commend.add(getPath("VideoSize"));
        // commend.add(outpath);
        // try {
        // ProcessBuilder builder = new ProcessBuilder();
        // builder.command(commend);
        // builder.start();
        // } catch (Exception e) {
        // nlogger.logout(e);
        // outpath = "";
        // }
        return outpath;
    }

    /**
     * 获取上传文件类型
     * 
     * @project GrapeFile
     * @package interfaceApplication
     * @file Files.java
     * 
     * @param type
     * @return 1：图片；2：视频；3：文档:txt,word,excel,ppt。。。。。；4：音频；5：其他
     *
     */
    private int getType(String type) {
        int filetype = 5;
        if (!type.equals("")) {
            type = type.toLowerCase();
            if (type.contains("image")) {
                filetype = 1;
            }
            if (type.contains("video")) {
                filetype = 2;
            }
            if (type.contains("application")) {
                filetype = 3;
            }
        }
        return filetype;
    }

    /**
     * 获取文件地址，缩略图地址相对路径,获取文件类型
     * 
     * @param object
     * @return
     */
    @SuppressWarnings("unchecked")
    private JSONObject getFileUrl(JSONObject object) {
        String temp = "";
        if (object != null && object.size() > 0) {
            if (object.containsKey("filepath")) {
                temp = object.getString("filepath");
                object.put("filepath", model.getImagepath(temp));
            }
            if (object.containsKey("ThumbnailImage")) {
                temp = object.getString("ThumbnailImage");
                object.put("ThumbnailImage", model.getImagepath(temp));
            }
        }
        return object;
    }
}
