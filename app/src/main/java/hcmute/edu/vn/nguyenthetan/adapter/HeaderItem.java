/**
 * HeaderItem: Wrapper class cho tiêu đề section trong Inbox.
 * Implement InboxItem để hiển thị header trong RecyclerView.
 */
package hcmute.edu.vn.nguyenthetan.adapter;

public class HeaderItem implements InboxItem {
    private String title;

    /**
     * Constructor tạo HeaderItem với tiêu đề chỉ định.
     * @param title Tiêu đề hiển thị trên header
     */
    public HeaderItem(String title) {
        this.title = title;
    }

    /**
     * Trả về tiêu đề của header.
     * @return Chuỗi tiêu đề
     */
    public String getTitle() {
        return title;
    }

    /**
     * Trả về loại view TYPE_HEADER.
     * @return InboxItem.TYPE_HEADER
     */
    @Override
    public int getViewType() {
        return TYPE_HEADER;
    }
}
