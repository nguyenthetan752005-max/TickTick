package hcmute.edu.vn.nguyenthetan.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories") // Room sẽ tạo bảng tên là categories
public class Category {
    @PrimaryKey(autoGenerate = true) // Tự động tăng ID (1, 2, 3...)
    private int id;

    private String name;

    public Category(String name) {
        this.name = name;
    }

    // Getter và Setter (Bắt buộc phải có để Room hoạt động)
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}