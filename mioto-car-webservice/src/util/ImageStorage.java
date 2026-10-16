/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package util;

import imageType.ImageType;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author tuanlee
 */
public class ImageStorage {
    public static final int MAX_IMAGES = 5;
    public static final long MAX_BYTES = 5 * 1024 * 1024; // 5MB
 
    // ------------------------------------------------------------
    // BƯỚC 1: đường dẫn thư mục
    // ------------------------------------------------------------
    private static String getUploadDir() {
        return Config.getString(ImageStorage.class, "mioto", "uploadDir", "uploads");
    }
    
    public static File getRootDir()
    {
        return new File(getUploadDir(), "images");
    }
 
    public static File getTmpDir() {
        return new File(getUploadDir(), "tmp");
    }
 
    public static File getFinalDir(String type) {
        return new File(getRootDir(), type);
    }
 
    // ------------------------------------------------------------
    // BƯỚC 4: nhìn vài byte đầu để biết đây là ảnh gì
    // Trả về "jpg", "png", hoặc null nếu không phải ảnh
    // ------------------------------------------------------------
    private static String detectExtension(byte[] head) {
        // JPEG bắt đầu bằng FF D8 FF
        if ((head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        // PNG bắt đầu bằng 89 50 4E 47
        if ((head[0] & 0xFF) == 0x89 && head[1] == 0x50 && head[2] == 0x4E && head[3] == 0x47) {
            return "png";
        }
        if (head.length >= 12
                && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P') {
            return "webp";
        }

        return null;
    }
 
    // ------------------------------------------------------------
    // BƯỚC 4 + 5: nhận ảnh từ request, ghi vào tmp, trả về imageId
    // Ảnh sai thì ném IllegalArgumentException
    // ------------------------------------------------------------
    public static String saveToTmp(InputStream in, long userId) throws IOException {
        // đọc 4 byte đầu
        byte[] head = new byte[4];
        try {
            new DataInputStream(in).readFully(head);
        } catch (EOFException e) {
            throw new IllegalArgumentException("File không phải ảnh hợp lệ");
        }
 
        String ext = detectExtension(head);
        if (ext == null) {
            throw new IllegalArgumentException("Chỉ hỗ trợ ảnh JPEG và PNG");
        }
 
        String imageId = "u" + userId + "_" + UUID.randomUUID().toString().replace("-", "") + "." + ext;
 
        // BƯỚC 5: ghi file
        getTmpDir().mkdirs();
        File file = new File(getTmpDir(), imageId);
        boolean success = false;
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(head); // 4 byte đã đọc ở trên
            long total = head.length;
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                total += len;
                if (total > MAX_BYTES) {
                    throw new IllegalArgumentException("Ảnh vượt quá 5MB");
                }
                out.write(buffer, 0, len);
            }
            success = true;
        } finally {
            if (!success) {
                file.delete(); // lỗi giữa chừng thì xóa file dở
            }
        }
        return imageId;
    }
 
    // ------------------------------------------------------------
    // BƯỚC 8: kiểm tra imageId FE gửi lên
    // ------------------------------------------------------------
    public static boolean isValidImageId(long userId, String imageId) {
        if (imageId == null) {
            return false;
        }
        // chặn kiểu ../../abc
        if (imageId.contains("/") || imageId.contains("\\") || imageId.contains("..")) {
            return false;
        }
        return imageId.endsWith(".jpg") || imageId.endsWith(".png");
    }
    
    public static boolean checkIsOwnImage(long userId, String imageId)
    {
        return imageId.startsWith("u" + userId + "_");
    }
 
    public static boolean existsInTmp(String imageId) {
        return new File(getTmpDir(), imageId).exists();
    }
 
    // ------------------------------------------------------------
    // BƯỚC 9: chuyển tmp -> cars. Trả về true nếu chuyển hết thành công
    // ------------------------------------------------------------
    public static boolean moveToFinal(String type, List<String> imageIds) {
        getFinalDir(type).mkdirs();
        List<String> moved = new ArrayList<String>();
        for (String id : imageIds) {
            boolean ok = new File(getTmpDir(), id).renameTo(new File(getFinalDir(type), id));
            if (!ok) {
                moveBackToTmp(type, moved); 
                return false;
            }
            moved.add(id);
        }
        return true;
    }
 
    // ------------------------------------------------------------
    // BƯỚC 11: lưu DB lỗi thì trả ảnh từ cars về tmp
    // ------------------------------------------------------------
    public static void moveBackToTmp(String type, List<String> imageIds) {
        for (String id : imageIds) {
            new File(getFinalDir(type), id).renameTo(new File(getTmpDir(), id));
        }
    }
 
    // ------------------------------------------------------------
    // BƯỚC 12: ghép URL trả cho FE
    // ------------------------------------------------------------
    public static String buildUrl(String type, String imageId) {
        if(imageId == null || imageId.trim().isEmpty())
        {
            return "";
        }
        String baseUrl = Config.getString(ImageStorage.class, "mioto", "imageBaseUrl", "http://127.0.0.1:8081/images");
        return baseUrl + "/" + type + "/" + imageId;
    }
    
    public static void deleteFromFinal(String type, String imageId) {
        if (imageId == null || imageId.contains("/") || imageId.contains("\\") || imageId.contains("..")) {
            return;
        }
        new File(getFinalDir(type), imageId).delete();
    }
}
