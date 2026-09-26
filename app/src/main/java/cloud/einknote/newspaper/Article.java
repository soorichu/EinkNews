package cloud.einknote.newspaper;

public class Article {
    private long id;
    private String section;
    private String title;
    private String content;
    private String updateDate;

    public Article(long id, String section, String title, String content, String updateDate) {
        this.id = id;
        this.section = section;
        this.title = title;
        this.content = content;
        this.updateDate = updateDate;
    }

    public Article(String section, String title, String content, String updateDate) {
        this(-1, section, title, content, updateDate);
    }

    public long getId() { return id; }
    public String getSection() { return section; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getUpdateDate() { return updateDate; }
}