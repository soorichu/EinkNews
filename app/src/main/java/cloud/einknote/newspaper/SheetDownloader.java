package cloud.einknote.newspaper;

import android.os.Build;
import android.text.Html;
import android.widget.EditText;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SheetDownloader {
//    private static final String SPREADSHEET_ID = "12hnjugee_A0YLXN48Hn6PF4F_n3RuaZOf7f1C6V_vHQ";
    private final OkHttpClient client;

    public interface DownloadCallback {
        void onSuccess(List<Article> articles);
        void onError(String errorMessage);
    }

    public SheetDownloader() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    public void downloadAsync(String SPREADSHEET_ID, List<String> sections, DownloadCallback callback) {
        new Thread(() -> {
            List<Article> allArticles = new ArrayList<>();
            String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA).format(new Date());

            for (String sec : sections) {
                String csvUrl = "https://docs.google.com/spreadsheets/d/" + SPREADSHEET_ID + "/gviz/tq?tqx=out:csv&sheet=" + sec;
                Request request = new Request.Builder().url(csvUrl).build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        callback.onError("시트 다운로드 실패 (" + sec + "): HTTP " + response.code());
                        return;
                    }

                    String body = response.body() != null ? response.body().string() : "";
                    List<List<String>> rows = parseCsv(body);

                    // A2 셀 확인 (0-indexed: rows[1][0])
                    if (rows.size() > 1 && !rows.get(1).isEmpty()) {
                        String a2 = rows.get(1).get(0).trim();
                        if ("1".equals(a2)) {
                            callback.onError("서버 시트가 기사 동기화 중입니다. 5분 후에 실행해주세요..");
                            return;
                        }
                    }

                    // A4:A23 (index 3 ~ 22), D4:D23 (index 3)
                    int maxRows = Math.min(23, rows.size());
                    for (int i = 3; i < maxRows; i++) {
                        List<String> row = rows.get(i);
                        String title = row.size() > 0 ? row.get(0).trim() : "";
                        String content = row.size() > 3 ? row.get(3).trim() : "";

                        if (!title.isEmpty()) {
                            String cleanTitle = unescapeHtml(title);
                            String cleanContent = unescapeHtml(content);
                            allArticles.add(new Article(sec, cleanTitle, cleanContent, currentTime));                        }
                    }
                } catch (IOException e) {
                    callback.onError("네트워크 오류: " + e.getMessage());
                    return;
                }
            }
            callback.onSuccess(allArticles);
        }).start();
    }

    /**
     * &lt;, &gt;, &amp;, &quot;, &#39;, &nbsp; 등 모든 HTML 엔티티를 일반 문자로 복원
     */
    private String unescapeHtml(String text) {
        if (text == null || text.isEmpty()) return "";

        // 1. Android 공식 Html 디코더 실행
        String decoded;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            decoded = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY).toString();
        } else {
            decoded = Html.fromHtml(text).toString();
        }

        // 2. 특수 줄바꿈이나 깨진 non-breaking space(\u00A0)를 일반 공백으로 정리
        return decoded.replace('\u00A0', ' ').trim();
    }

    // RFC 4180 개행/따옴표 호환 CSV 파서
    private List<List<String>> parseCsv(String text) {
        List<List<String>> result = new ArrayList<>();
        List<String> currentRow = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean insideQuotes = false;
        int i = 0;

        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '\"') {
                if (insideQuotes && i + 1 < text.length() && text.charAt(i + 1) == '\"') {
                    cell.append('\"');
                    i++;
                } else {
                    insideQuotes = !insideQuotes;
                }
            } else if (c == ',' && !insideQuotes) {
                currentRow.add(cell.toString());
                cell.setLength(0);
            } else if ((c == '\r' || c == '\n') && !insideQuotes) {
                currentRow.add(cell.toString());
                cell.setLength(0);
                if (!currentRow.isEmpty()) {
                    result.add(new ArrayList<>(currentRow));
                }
                currentRow.clear();
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
            } else {
                cell.append(c);
            }
            i++;
        }
        if (cell.length() > 0 || !currentRow.isEmpty()) {
            currentRow.add(cell.toString());
            result.add(currentRow);
        }
        return result;
    }
}