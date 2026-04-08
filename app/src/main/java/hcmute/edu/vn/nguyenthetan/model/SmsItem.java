package hcmute.edu.vn.nguyenthetan.model;

/**
 * Model đại diện cho một tin nhắn SMS
 * Lấy từ Content Provider content://sms/inbox
 */
public class SmsItem {
    
    private long id;
    private String phoneNumber;
    private String contactName;  // Có thể null nếu không có trong danh bạ
    private String body;  // Nội dung tin nhắn
    private long timestamp;
    private int type;  // 1 = inbox, 2 = sent
    private boolean isRead;  // true = đã đọc, false = chưa đọc
    
    public SmsItem(long id, String phoneNumber, String contactName, 
                   String body, long timestamp, int type, boolean isRead) {
        this.id = id;
        this.phoneNumber = phoneNumber;
        this.contactName = contactName;
        this.body = body;
        this.timestamp = timestamp;
        this.type = type;
        this.isRead = isRead;
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
    
    public String getBody() {
        return body;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public int getType() {
        return type;
    }
    
    public boolean isRead() {
        return isRead;
    }
    
    /**
     * Lấy tên hiển thị (tên nếu có, không thì số điện thoại)
     */
    public String getDisplayName() {
        return (contactName != null && !contactName.isEmpty()) ? contactName : phoneNumber;
    }
    
    /**
     * Cắt ngắn nội dung tin nhắn nếu quá dài
     */
    public String getBodyPreview(int maxLength) {
        if (body == null) return "";
        if (body.length() <= maxLength) return body;
        return body.substring(0, maxLength) + "...";
    }
    
    /**
     * Kiểm tra có phải tin nhắn đến không
     */
    public boolean isInbox() {
        return type == 1;
    }
    
    /**
     * Kiểm tra có phải tin nhắn đã gửi không
     */
    public boolean isSent() {
        return type == 2;
    }
}
