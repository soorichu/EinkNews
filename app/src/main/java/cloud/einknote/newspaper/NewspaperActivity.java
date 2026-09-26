package cloud.einknote.newspaper;

import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class NewspaperActivity extends AppCompatActivity {

    private Button btnHome, btnPrev, btnNext;
    private Button btnScrollUp, btnScrollDown;
    private Button btnFontPlus, btnFontMinus;
    private LinearLayout tabLayoutContainer, layoutIndex, layoutArticle;
    private TextView tvArticleTitle, tvArticleBody, tvPageIndicator;
    private ScrollView articleScrollView;

    private DatabaseHelper dbHelper;
    private String currentSection = "";
    private List<Article> currentArticles = new ArrayList<>();
    private int currentPage = 0; // 0 = INDEX, 1..N = 각 기사 페이지

    // 기본 본문 폰트 크기 (sp)
    private float currentBodyFontSize = 17.0f;
    private static final float MIN_FONT_SIZE = 10.0f;
    private static final float MAX_FONT_SIZE = 30.0f;

    // 기사 본문 줄 간격
    private Button btnLineSpacingMinus;
    private Button btnLineSpacingPlus;

    // 초기 기본 줄간격 배수 (기존 XML의 1.35에 맞춤)
    private float currentLineSpacingMultiplier = 1.35f;
    private final float MIN_LINE_SPACING = 1.0f;
    private final float MAX_LINE_SPACING = 2.2f;
    private final float SPACING_STEP = 0.15f;

    // 기사 인덱스 카드
    private RecyclerView rvIndex;
    private HeadlineAdapter headlineAdapter;

    private float currentLineSpacing;
    private ViewerPreferenceManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_newspaper);

        prefManager = new ViewerPreferenceManager(this); // 필수 확인!
        dbHelper = new DatabaseHelper(this);

        btnHome = findViewById(R.id.btnHome);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        btnScrollUp = findViewById(R.id.btnScrollUp);
        btnScrollDown = findViewById(R.id.btnScrollDown);
        btnFontPlus = findViewById(R.id.btnFontPlus);
        btnFontMinus = findViewById(R.id.btnFontMinus);
        btnLineSpacingMinus = findViewById(R.id.btnLineSpacingMinus);
        btnLineSpacingPlus = findViewById(R.id.btnLineSpacingPlus);

        tabLayoutContainer = findViewById(R.id.tabLayoutContainer);
        layoutIndex = findViewById(R.id.layoutIndex);
        layoutArticle = findViewById(R.id.layoutArticle);
        tvArticleTitle = findViewById(R.id.tvArticleTitle);
        tvArticleBody = findViewById(R.id.tvArticleBody);
        tvPageIndicator = findViewById(R.id.tvPageIndicator);
        articleScrollView = findViewById(R.id.articleScrollView);


        btnHome.setOnClickListener(v -> finish());

        rvIndex = findViewById(R.id.rvIndex);
        setupIndexRecyclerView();

        // 이전 페이지
        btnPrev.setOnClickListener(v -> {
            if (currentPage > 0) {
                currentPage--;
                renderPage();
            }
        });

        // 다음 페이지
        btnNext.setOnClickListener(v -> {
            if (currentPage < currentArticles.size()) {
                currentPage++;
                renderPage();
            }
        });

        // 반 페이지 위로 스크롤 (△)
        btnScrollUp.setOnClickListener(v -> {
            int halfHeight = articleScrollView.getHeight() / 2;
            articleScrollView.smoothScrollBy(0, -halfHeight);
        });

        // 반 페이지 아래로 스크롤 (▽)
        btnScrollDown.setOnClickListener(v -> {
            int halfHeight = articleScrollView.getHeight() / 2;
            articleScrollView.smoothScrollBy(0, halfHeight);
        });

        // 폰트 크기 증가 (+)
        btnFontPlus.setOnClickListener(v -> {
            if (currentBodyFontSize < MAX_FONT_SIZE) {
                currentBodyFontSize += 1.0f;
                updateFontSizes();
                prefManager.saveFontSize(currentBodyFontSize); // 영구 저장
            }
        });

        // 폰트 크기 감소 (-)
        btnFontMinus.setOnClickListener(v -> {
            if (currentBodyFontSize > MIN_FONT_SIZE) {
                currentBodyFontSize -= 1.0f;
                updateFontSizes();
                prefManager.saveFontSize(currentBodyFontSize); // 영구 저장
            }
        });

        // 줄간격 좁히기 (-)
        btnLineSpacingMinus.setOnClickListener(v -> {
            if (currentLineSpacingMultiplier - SPACING_STEP >= MIN_LINE_SPACING) {
                currentLineSpacingMultiplier -= SPACING_STEP;
                tvArticleBody.setLineSpacing(0f, currentLineSpacingMultiplier);
                prefManager.saveLineSpacing(currentLineSpacing); // 영구 저장
            }
        });

        // 줄간격 넓히기 (+)
        btnLineSpacingPlus.setOnClickListener(v -> {
            if (currentLineSpacingMultiplier + SPACING_STEP <= MAX_LINE_SPACING) {
                currentLineSpacingMultiplier += SPACING_STEP;
                tvArticleBody.setLineSpacing(0f, currentLineSpacingMultiplier);
                prefManager.saveLineSpacing(currentLineSpacing); // 영구 저장
            }
        });
        // 1. 섹션 보기
        loadSavedSections();
    }

    private void loadSavedSections() {
        List<String> savedSections = dbHelper.getSavedSections();
        if (savedSections.isEmpty()) {
            Toast.makeText(this, "저장된 기사가 없습니다. 메인에서 내려받기를 해주세요.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        buildTabs(savedSections);
        selectSection(savedSections.get(0));
    }

    private void applyLineSpacing() {
        // 첫 번째 인자는 추가 픽셀(add), 두 번째 인자는 줄간격 배수(mult)
        tvArticleBody.setLineSpacing(0f, currentLineSpacingMultiplier);
    }

    private void buildTabs(List<String> sections) {
        tabLayoutContainer.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        int whiteColor = getResources().getColor(R.color.white);
        int blackColor = getResources().getColor(R.color.black);

        for (String sec : sections) {
            Button tabBtn = new Button(this);
            tabBtn.setText(getSectionDisplayName(sec));
            tabBtn.setTextColor(blackColor);
            tabBtn.setBackgroundResource(R.drawable.border_button);
            tabBtn.setBackgroundTintList(ColorStateList.valueOf(whiteColor));
            tabBtn.setTextSize(13);
            tabBtn.setPadding(24, 0, 24, 0);

            tabBtn.setOnClickListener(v -> selectSection(sec));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    (int) (36 * density)
            );
            params.setMargins(0, 0, (int) (8 * density), 0);
            tabLayoutContainer.addView(tabBtn, params);
        }
    }

    private void selectSection(String sec) {
        currentSection = sec;
        currentPage = 0;
        currentArticles = dbHelper.getArticlesBySection(sec);
        renderPage();
    }

    private void renderPage() {
        if (currentPage == 0) {
            // INDEX 화면
            rvIndex.setVisibility(View.VISIBLE);  // 인덱스 카드 보이기
            layoutIndex.setVisibility(View.VISIBLE);
            layoutArticle.setVisibility(View.GONE);
            btnScrollUp.setVisibility(View.GONE);
            btnScrollDown.setVisibility(View.GONE);
            tvPageIndicator.setText(getSectionDisplayName(currentSection) + " INDEX");
        //    renderIndexList();
            setupIndexRecyclerView();  // 카드로 대체
        } else {
            // 기사 상세 화면
            rvIndex.setVisibility(View.GONE);  // 인덱스 카드 숨기기
            layoutIndex.setVisibility(View.GONE);
            layoutArticle.setVisibility(View.VISIBLE);
            btnScrollUp.setVisibility(View.VISIBLE);
            btnScrollDown.setVisibility(View.VISIBLE);
            tvPageIndicator.setText(currentPage + " / " + currentArticles.size());

            Article item = currentArticles.get(currentPage - 1);

            // 기사 제목 10자 이내 적용
            String shortTitle = getTenCharTitle(item.getTitle());
            tvArticleTitle.setText("["+ getSectionDisplayName(currentSection) +"] "+shortTitle);

            tvArticleBody.setText(item.getContent().isEmpty() ? "본문 내용이 없습니다." : item.getContent());

            // 폰트 크기 설정 (기사제목 = 본문 + 2sp)
            updateFontSizes();

            // 기사 변경 시 맨 위로 스크롤 초기화
            articleScrollView.scrollTo(0, 0);
        }
        // 이전에 저장된 설정값 불러오기 및 본문 적용
        loadSavedSettings();
    }

    /**
     * E-Ink 잔상 방지를 위해 배경 반전 없이
     * 모든 탭의 흰 바탕을 유지하고 선택된 탭의 폰트만 BOLD로 강조합니다.
     */
    // 폰트 크기 업데이트: 일반 기사 제목은 본문보다 항상 2포인트 큼
    private void updateFontSizes() {
        tvArticleBody.setTextSize(currentBodyFontSize);
        tvArticleTitle.setTextSize(currentBodyFontSize + 3.0f);
    }

    // 기사 제목 15자 이내 자르기 유틸 함수
    private String getTenCharTitle(String title) {
        if (title == null) return "";
        String trimmed = title.trim();
        String text[] = trimmed.split(" ");
        return text[0] + " " + text[1]+ " " + text[2] + " " + text[3] + " " + text[4];
    }

    private String getSectionDisplayName(String sec) {
        switch (sec) {
            case "home": return "브리핑";
            case "politics": return "정치";
            case "economy": return "경제";
            case "society": return "사회";
            case "world": return "세계";
            case "climate": return "환경";
            case "culture": return "문화";
            case "people": return "인물";
            case "tech": return "기술";
            case "understanding": return "칼럼";
            default: return sec;
        }
    }

    private void setupIndexRecyclerView() {
        // 카드 최소 너비를 약 280dp로 기준 잡아 열 개수 자동 계산 (가로 좁으면 1열, 넓으면 2~3열)
        int screenWidthDp = getResources().getConfiguration().screenWidthDp;
        int spanCount = Math.max(1, screenWidthDp / 280);
        if (spanCount > 3) spanCount = 3; // 최대 3열 제한

        GridLayoutManager layoutManager = new GridLayoutManager(this, spanCount);
        rvIndex.setLayoutManager(layoutManager);

        headlineAdapter = new HeadlineAdapter(currentArticles, (article, pageNum) -> {
            currentPage = pageNum;
            renderPage();
        });
        rvIndex.setAdapter(headlineAdapter);
    }

    private void renderIndexList() {
        if (headlineAdapter != null) {
            headlineAdapter.notifyDataSetChanged();
        }
    }

    /**
     * 저장된 글자 크기와 줄 간격을 불러와 적용
     */
    private void loadSavedSettings() {
        currentBodyFontSize = prefManager.getFontSize();
        currentLineSpacing = prefManager.getLineSpacing();

        applySettingsToView();
    }

    /**
     * 현재 메모리의 설정값을 TextView 본문에 즉각 반영
     */
    private void applySettingsToView() {
        // 폰트 크기 적용 (단위: SP)
        tvArticleBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, currentBodyFontSize);
        // 줄 간격 배수 적용
        tvArticleBody.setLineSpacing(0f, currentLineSpacing);
    }

}