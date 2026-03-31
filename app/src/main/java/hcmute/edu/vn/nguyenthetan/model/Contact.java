package hcmute.edu.vn.nguyenthetan.model;

/**
 * Model đại diện cho một liên hệ trong danh bạ.
 */
public class Contact {
    private String name;
    private String phone;

    public Contact(String name, String phone) {
        this.name = name != null ? name : "Không rõ";
        this.phone = phone != null ? phone : "";
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
