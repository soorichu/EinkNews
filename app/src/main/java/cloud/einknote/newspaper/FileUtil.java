package cloud.einknote.newspaper;

import android.content.Context;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class FileUtil {

    private static final String FILE_NAME = "id.txt";
    private static final String DEFAULT_URL = "https://docs.google.com/spreadsheets/d/12hnjugee_A0YLXN48Hn6PF4F_n3RuaZOf7f1C6V_vHQ";


    // 저장 폴더(디렉터리) 경로만 반환
    // 예: /storage/emulated/0/Android/data/cloud.einknote.newspaper/files
    public static String getStorageDirPath(Context context) {
        File targetDir = context.getExternalFilesDir(null);
        if (targetDir == null) {
            targetDir = context.getFilesDir();
        }
        return targetDir.getAbsolutePath();
    }

    /**
     * 외부 저장소(앱 전용 외부 디렉터리)의 id.txt 파일을 읽어 문자열(ID)로 반환합니다.
     * 파일이 없으면 기본 DEFAULT_URL로 파일을 생성하고 ID를 추출해 반환합니다.
     */
    public static String readIdFile(Context context) {
        File targetDir = context.getExternalFilesDir(null);
        if (targetDir == null) {
            // 외부 저장소가 마운트되지 않은 경우 내부 저장소로 폴백(fallback)
            targetDir = context.getFilesDir();
        }

        File file = new File(targetDir, FILE_NAME);

        // 파일 존재 여부 확인 후 없으면 생성
        if (!file.exists()) {
            boolean created = createDefaultIdFile(file, DEFAULT_URL);
            if (!created) {
                return null;
            }
            return extractSheetId(DEFAULT_URL);
        }

        // 파일 읽기
        StringBuilder stringBuilder = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)) {

            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }

            String id_text = stringBuilder.toString().trim();
            return extractSheetId(id_text);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * URL 전체가 들어온 경우 /d/ 뒷부분의 시트 ID만 안전하게 추출
     */
    private static String extractSheetId(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        if (text.contains("/")) {
            String[] parts = text.split("/");
            // https://docs.google.com/spreadsheets/d/{ID}/edit... 형태 대응
            for (int i = 0; i < parts.length; i++) {
                if ("d".equals(parts[i]) && i + 1 < parts.length) {
                    return parts[i + 1];
                }
            }
            // fallback: 기존 index 5 사용
            if (parts.length > 5) {
                return parts[5];
            }
        }
        return text;
    }

    private static boolean createDefaultIdFile(File file, String content) {
        try {
            // 상위 디렉터리가 없으면 생성
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(content.getBytes(StandardCharsets.UTF_8));
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}