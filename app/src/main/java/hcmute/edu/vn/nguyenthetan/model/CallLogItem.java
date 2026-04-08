package hcmute.edu.vn.nguyenthetan.model;

/**
 * Model đại diện cho một cuộc gọi trong lịch sử (Call Log)
 * Lấy từ Content Provider content://call_log/calls
 */
public class CallLogItem {
    
    // Loại cuộc gọi
    public static final int TYPE_INCOMING = 1;
    public static final int TYPE_OUTGOING = 2;
    public static final int TYPE_MISSED = 3;
    public static final int TYPE_REJECTED = 5;
    
    private long id;
    private String phoneNumber;
    private String contactName;  // Có thể null nếu không có trong danh bạ
    private long timestamp;
    private int type;  // INCOMING, OUTGOING, MISSED, REJECTED
    private long duration;  // Thời lượng (giây)
    
    public CallLogItem(long id, String phoneNumber, String contactName, 
                       long timestamp, int type, long duration) {
        this.id = id;
        this.phoneNumber = phoneNumber;
        this.contactName = contactName;
        this.timestamp = timestamp;
        this.type = type;
        this.duration = duration;
    }
    
    public long getId() {
        return id;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public String getContactName() {
        return contactName;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public int getType() {
        return type;
    }
    
    public long getDuration() {
        return duration;
    }
    
    /**
     * Kiểm tra có phải cuộc gọi nhỡ không
     */
    public boolean isMissed() {
        return type == TYPE_MISSED;
    }
    
    /**
     * Kiểm tra có phải cuộc gọi đến không
     */
    public boolean isIncoming() {
        return type == TYPE_INCOMING;
    }
    
    /**
     * Kiểm tra có phải cuộc gọi đi không
     */
    public boolean isOutgoing() {
        return type == TYPE_OUTGOING;
    }
    
    /**
     * Lấy tên hiển thị (tên nếu có, không thì số điện thoại)
     */
    public String getDisplayName() {
        return (contactName != null && !contactName.isEmpty()) ? contactName : phoneNumber;
    }
    
    /**
     * Format thời lượng cuộc gọi thành chuỗi dễ đọc
     * Ví dụ: "2 phút 30 giây" hoặc "45 giây"
     */
    public String getFormattedDuration() {
        if (duration == 0) return "";
        
        if (duration < 60) {
            return duration + " giây";
        } else {
            long minutes = duration / 60;
            long seconds = duration % 60;
            if (seconds == 0) {
                return minutes + " phút";
            } else {
                return minutes + " phút " + seconds + " giây";
            }
        }
    }
    
    /**
     * Lấy loại cuộc gọi dạng chuỗi
     */
    public String getTypeString() {
        switch (type) {
            case TYPE_INCOMING:
                return "Cuộc gọi đến";
            case TYPE_OUTGOING:
                return "Cuộc gọi đi";
            case TYPE_MISSED:
                return "Cuộc gọi nhỡ";
            case TYPE_REJECTED:
                return "Từ chối";
            default:
                return "Không xác định";
        }
    }
    
    /**
     * Lấy resource icon cho loại cuộc gọi
     */
    public int getTypeIcon() {
        switch (type) {
            case TYPE_INCOMING:
                return android.R.drawable.ic_menu_call;
            case TYPE_OUTGOING:
                return android.R.drawable.ic_menu_call;
            case TYPE_MISSED:
                return android.R.drawable.ic_menu_close_clear_cancel;
            case TYPE_REJECTED:
                return android.R.drawable.ic_menu_close_clear_cancel;
            default:
                return android.R.drawable.ic_menu_call;
        }
    }
}
