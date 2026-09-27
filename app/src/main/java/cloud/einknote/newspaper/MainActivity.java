package cloud.einknote.newspaper;

import static cloud.einknote.newspaper.FileUtil.getStorageDirPath;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static Context context;
    private CheckBox cbHome, cbPolitics, cbEconomy, cbSociety, cbWorld, cbClimate, cbCulture, cbTech, cbPeople, cbUnderstanding;
    private Button btnDownload, btnViewNews;
    private TextView tvStatus;
    private DatabaseHelper dbHelper;
    private SheetDownloader downloader;

    private EditText serverID;
    private TextView serverSettingText;

    private ViewerPreferenceManager prefManager;
    private FileUtil fileUtil;

 //   private Button btnGetServerID;
    private Button btnSetServerID;
    private Button btnGetServerID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        downloader = new SheetDownloader();

        cbHome = findViewById(R.id.cbHome);
        cbPolitics = findViewById(R.id.cbPolitics);
        cbEconomy = findViewById(R.id.cbEconomy);
        cbSociety = findViewById(R.id.cbSociety);
        cbWorld = findViewById(R.id.cbWorld);
        cbClimate = findViewById(R.id.cbClimate);
        cbCulture = findViewById(R.id.cbCulture);
        cbTech = findViewById(R.id.cbTech);
        cbPeople = findViewById(R.id.cbPeople);
        cbUnderstanding = findViewById(R.id.cbUnderstanding);
        btnDownload = findViewById(R.id.btnDownload);
        btnViewNews = findViewById(R.id.btnViewNews);
        tvStatus = findViewById(R.id.tvStatus);
        serverID = findViewById(R.id.serverID);
        serverSettingText = findViewById(R.id.tvServerSetting);
        btnSetServerID = findViewById(R.id.btnSetServerID);
        btnGetServerID = findViewById(R.id.btnGetServerID);


        tvStatus.setText(" PC에서 zrr.kr/wNxTE8 주소로 들어간 후 구글 시트를 복제한 주소값을 id.txt에 담아 " + fileUtil.getStorageDirPath(this) + "에 넣어주세요.");

        // 1. 평소(실행 시): DB에 저장된 serverid 값을 읽어와서 EditText에 표시
        loadServerIdToEditText();

        // 2. 버튼 클릭 시: EditText에 적힌 값을 DB의 serverid 테이블에 저장
        btnSetServerID.setOnClickListener(v -> {
            String input = serverID.getText().toString().trim();

            if (input.isEmpty()) {
                Toast.makeText(MainActivity.this, "ID 또는 URL을 입력해 주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            // URL 전체가 입력된 경우 ID 부분만 안전하게 분리
            String parsedId = extractSheetId(input);

            // DB에 저장
            dbHelper.updateServerID(parsedId);

            // EditText에도 파싱된 ID로 다시 반영 및 커서 이동
            serverID.setText(parsedId);
            serverID.setSelection(parsedId.length());

            Toast.makeText(MainActivity.this, "서버 ID가 저장되었습니다.", Toast.LENGTH_SHORT).show();
        });


        btnGetServerID.setOnClickListener(v -> {
            setServerID();
        });

        btnDownload.setOnClickListener(v -> {
            if(serverID.getText().toString().isEmpty()){
                Toast.makeText(MainActivity.this, "서버 ID를 세팅해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            List<String> selected = getSelectedSections();
            if (selected.isEmpty()) {
                Toast.makeText(MainActivity.this, "하나 이상의 섹션을 선택해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            startDownload(selected);
        });

        btnViewNews.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NewspaperActivity.class);
            startActivity(intent);
        });

    }

    private void setServerID(){
        // context(this)를 전달해 id.txt 내용 가져오기
        String idText = FileUtil.readIdFile(this);

        if (idText != null && !idText.isEmpty()) {
            serverID.setText(idText);
            dbHelper.updateServerID(idText);
            Toast.makeText(this, "ID: " + idText, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "id.txt 파일이 없거나 내용이 비어 있습니다.", Toast.LENGTH_SHORT).show();
        }


    }

    private List<String> getSelectedSections() {
        List<String> list = new ArrayList<>();
        if (cbHome.isChecked()) list.add("home");
        if (cbPolitics.isChecked()) list.add("politics");
        if (cbEconomy.isChecked()) list.add("economy");
        if (cbSociety.isChecked()) list.add("society");
        if (cbWorld.isChecked()) list.add("world");
        if (cbClimate.isChecked()) list.add("climate");
        if (cbCulture.isChecked()) list.add("culture");
        if (cbTech.isChecked()) list.add("tech");
        if (cbPeople.isChecked()) list.add("people");
        if (cbUnderstanding.isChecked()) list.add("understanding");
        return list;
    }

    private void startDownload(List<String> sections) {
        tvStatus.setText("기사를 내려받는 중입니다...");
        String serverid = this.serverID.getText().toString();
        btnDownload.setEnabled(false);

        downloader.downloadAsync(serverid, sections, new cloud.einknote.newspaper.SheetDownloader.DownloadCallback() {
            @Override
            public void onSuccess(List<Article> articles) {
                // DB 갱신 (이전 데이터 삭제 후 삽입)
                dbHelper.clearAllArticles();
                dbHelper.insertArticles(articles);

                runOnUiThread(() -> {
                    btnDownload.setEnabled(true);
                    tvStatus.setText("동기화 완료: " + articles.size() + "개 기사가 저장되었습니다.");
                    Toast.makeText(MainActivity.this, "내려받기가 완료되었습니다.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    btnDownload.setEnabled(true);
                    tvStatus.setText(errorMessage);
                    Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * DB에서 ID를 가져와 EditText에 세팅하는 메서드
     */
    private void loadServerIdToEditText() {
        String savedId = dbHelper.getServerID();
        if (savedId != null && !savedId.isEmpty()) {
            serverID.setText(savedId);
        }
    }

    /**
     * URL 전체가 들어왔을 때 '/d/{ID}' 부분만 추출하는 헬퍼 메서드
     */
    private String extractSheetId(String text) {
        if (text.contains("/")) {
            String[] parts = text.split("/");
            for (int i = 0; i < parts.length; i++) {
                if ("d".equals(parts[i]) && i + 1 < parts.length) {
                    return parts[i + 1];
                }
            }
            if (parts.length > 5) {
                return parts[5];
            }
        }
        return text;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
  //      loadServerIdToEditText();
    }


}