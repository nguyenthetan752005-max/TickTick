package hcmute.edu.vn.nguyenthetan.adapter;

public interface InboxItem {
    int TYPE_HEADER = 0;
    int TYPE_TASK = 1;
    int TYPE_NOTIFICATION = 2;

    int getViewType();
}
