package hcmute.edu.vn.nguyenthetan.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks",
        foreignKeys = @ForeignKey(entity = Category.class,
                parentColumns = "id",
                childColumns = "categoryId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("categoryId")})
public class Task {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String name;
    private String description;
    private int categoryId;
    private long dueDate; // Thuộc tính mới: Ngày hết hạn (dạng timestamp)

    public Task(String name, String description, int categoryId, long dueDate) {
        this.name = name;
        this.description = description;
        this.categoryId = categoryId;
        this.dueDate = dueDate;
    }

    // Getter và Setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public long getDueDate() { return dueDate; }
    public void setDueDate(long dueDate) { this.dueDate = dueDate; }
}