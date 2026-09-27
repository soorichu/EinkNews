package cloud.einknote.newspaper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "eink_newspaper.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_ARTICLES = "articles";
    public static final String COL_ID = "id";
    public static final String COL_SECTION = "section";
    public static final String COL_TITLE = "title";
    public static final String COL_CONTENT = "content";
    public static final String COL_UPDATE_DATE = "update_date";

    public static final String TABLE_SERVER_ID = "serverid";
    public static final String COL_SERVER_CODE = "server";

    // 기본으로 세팅할 ID 값 지정
    public static final String DEFAULT_SERVER_ID = "12hnjugee_A0YLXN48Hn6PF4F_n3RuaZOf7f1C6V_vHQ";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 1. 기사 테이블 생성
        String createQuery = "CREATE TABLE " + TABLE_ARTICLES + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_SECTION + " TEXT, " +
                COL_TITLE + " TEXT, " +
                COL_CONTENT + " TEXT, " +
                COL_UPDATE_DATE + " TEXT);";
        db.execSQL(createQuery);

        // 1. 서버 ID 테이블 생성 (id 자동 증가 추가)
        String serveridQuery = "CREATE TABLE " + TABLE_SERVER_ID + " (" + COL_ID +
                " INTEGER PRIMARY KEY AUTOINCREMENT, " + COL_SERVER_CODE +
                " TEXT);";
        db.execSQL(serveridQuery);

        // 2. 초기 데이터 삽입
        ContentValues initialValues = new ContentValues();
        // id는 AUTOINCREMENT이므로 값을 넣지 않으면 알아서 1부터 들어갑니다.
        initialValues.put(COL_SERVER_CODE, DEFAULT_SERVER_ID);
        db.insert(TABLE_SERVER_ID, null, initialValues);
    }

    // 서버 ID 가져오기
    public String getServerID() {
        SQLiteDatabase db = this.getReadableDatabase();
        String serverId = null;

        // ⭐️ id를 기준으로 내림차순(DESC) 정렬한 뒤 맨 위 1개(LIMIT 1)만 가져옵니다.
        String query = "SELECT "+ COL_SERVER_CODE +" FROM " + TABLE_SERVER_ID + " ORDER BY id DESC LIMIT 1";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor != null && cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndex("server");
            if (columnIndex != -1) {
                serverId = cursor.getString(columnIndex);
            }
        }

        if (cursor != null) {
            cursor.close();
        }

        return serverId;
    }

    // 서버 ID 추가
    public void updateServerID(String newServerIDCode) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_SERVER_CODE, newServerIDCode);
        db.insert(TABLE_SERVER_ID, null, values);
    }

    // 초기값으로 리셋
//    public void resetServerIDToDefault() {
//        updateServerID(DEFAULT_SERVER_ID);
//    }
//
//
//    public void deleteServerID() {
//        SQLiteDatabase db = this.getWritableDatabase();
//        db.delete(TABLE_SERVER_ID, null, null); // DROP 대신 레코드만 비움
//    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ARTICLES);
    //    db.execSQL("DROP TABLE IF EXISTS " + TABLE_SERVER_ID);
        onCreate(db);
    }

    // 새 동기화 시 기사 데이터 일괄 삭제
    public void clearAllArticles() {
        SQLiteDatabase db = this.getWritableDatabase();
        // 쌍따옴표 제거하고 상수 TABLE_ARTICLES 사용
        db.delete(TABLE_ARTICLES, null, null);
    }

    // 기사 목록 삽입
    public void insertArticles(List<Article> articles) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (Article item : articles) {
                ContentValues values = new ContentValues();
                values.put(COL_SECTION, item.getSection());
                values.put(COL_TITLE, item.getTitle());
                values.put(COL_CONTENT, item.getContent());
                values.put(COL_UPDATE_DATE, item.getUpdateDate());
                db.insert(TABLE_ARTICLES, null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // 저장된 섹션 목록 가져오기
    public List<String> getSavedSections() {
        List<String> sections = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT DISTINCT " + COL_SECTION + " FROM " + TABLE_ARTICLES, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                sections.add(cursor.getString(0));
            }
            cursor.close();
        }
        return sections;
    }

    // 특정 섹션의 기사 가져오기
    public List<Article> getArticlesBySection(String section) {
        List<Article> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_ARTICLES, null, COL_SECTION + "=?",
                new String[]{section}, null, null, COL_ID + " ASC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE));
                String content = cursor.getString(cursor.getColumnIndexOrThrow(COL_CONTENT));
                String updateDate = cursor.getString(cursor.getColumnIndexOrThrow(COL_UPDATE_DATE));
                list.add(new Article(id, section, title, content, updateDate));
            }
            cursor.close();
        }
        return list;
    }
}