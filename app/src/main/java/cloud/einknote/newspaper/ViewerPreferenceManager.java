package cloud.einknote.newspaper;

import android.content.Context;
import android.content.SharedPreferences;

public class ViewerPreferenceManager {
    private static final String PREF_NAME = "news_viewer_settings";

    // 설정 키값 정의
    private static final String KEY_FONT_SIZE = "key_font_size";
    private static final String KEY_LINE_SPACING = "key_line_spacing";
//    private static final String KEY_SERVER_SETTING = "key_server_setting";
    private static final String KEY_SCROLL_PERCENT = "key_scroll_percent";
    // 6인치 단말기 최적 기본값
    public static final float DEFAULT_FONT_SIZE = 18f;        // 18sp
    public static final float DEFAULT_LINE_SPACING = 1.4f;     // 1.4배

    // 스크롤 사이즈
    public static final int DEFAULT_SCROLL_PERCENT = 50; // 50% 스크롤

    private final SharedPreferences prefs;

  //  public static final String DEFAULT_SERVER_SETTING = "12hnjugee_A0YLXN48Hn6PF4F_n3RuaZOf7f1C6V_vHQ";

    public ViewerPreferenceManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // 폰트 크기 저장 및 불러오기
    public void saveFontSize(float fontSize) {
        prefs.edit().putFloat(KEY_FONT_SIZE, fontSize).apply();
    }

    public float getFontSize() {
        return prefs.getFloat(KEY_FONT_SIZE, DEFAULT_FONT_SIZE);
    }

    // 줄 간격 저장 및 불러오기
    public void saveLineSpacing(float lineSpacing) {
        prefs.edit().putFloat(KEY_LINE_SPACING, lineSpacing).apply();
    }

    public float getLineSpacing() {
        return prefs.getFloat(KEY_LINE_SPACING, DEFAULT_LINE_SPACING);
    }

    public int getScrollPercent() { 
        try {
            return prefs.getInt(KEY_SCROLL_PERCENT, DEFAULT_SCROLL_PERCENT); 
        } catch (ClassCastException e) {
            // If the value was previously stored as a float, safely convert it or return default
            try {
                float floatVal = prefs.getFloat(KEY_SCROLL_PERCENT, (float) DEFAULT_SCROLL_PERCENT);
                int intVal = (int) floatVal;
                saveScrollPercent(intVal);
                return intVal;
            } catch (ClassCastException e2) {
                return DEFAULT_SCROLL_PERCENT;
            }
        }
    }
    public void saveScrollPercent(int scrollPercent) {
        prefs.edit().putInt(KEY_SCROLL_PERCENT, scrollPercent).apply();
    }

//    public void saveServerSetting(String serverSetting) {
//        prefs.edit().putString(KEY_SERVER_SETTING, serverSetting).apply();
//    }
//    public String getServerSetting() {
//        return prefs.getString(KEY_SERVER_SETTING, DEFAULT_SERVER_SETTING);
//    }
}