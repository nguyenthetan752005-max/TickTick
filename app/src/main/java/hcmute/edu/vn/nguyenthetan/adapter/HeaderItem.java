package hcmute.edu.vn.nguyenthetan.adapter;

public class HeaderItem implements InboxItem {
    private String title;

    public HeaderItem(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public int getViewType() {
        return TYPE_HEADER;
    }
}
