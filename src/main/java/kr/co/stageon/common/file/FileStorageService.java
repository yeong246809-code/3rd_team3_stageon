package kr.co.stageon.common.file;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/** 관리자 업로드 파일을 로컬 폴더(uploads/posters)에 저장합니다. */
@Service
public class FileStorageService {

    private static final String UPLOAD_DIR = "uploads/posters";

    /** 파일이 없으면 null, 있으면 저장 후 접근 가능한 URL("/uploads/posters/xxx.jpg")을 반환합니다. */
    public String save(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        try {
            Path dir = Paths.get(UPLOAD_DIR);
            Files.createDirectories(dir);

            String original = file.getOriginalFilename();
            String ext = "";
            if (original != null && original.contains(".")) {
                ext = original.substring(original.lastIndexOf("."));
            }
            String filename = UUID.randomUUID() + ext;

            Path target = dir.resolve(filename);
            file.transferTo(target);

            return "/uploads/posters/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("포스터 이미지 저장에 실패했습니다.", e);
        }
    }
}