package hcmute.edu.vn.nguyenthetan.ui.taskdialog;

public final class ReminderTimeCalculator {
    private ReminderTimeCalculator() {}

    /**
     * Tính thời gian nhắc nhở dựa trên deadline và khoảng cách.
     * unitIndex: 0=phút, 1=giờ, 2=ngày
     */
    public static long calculateReminderTime(long dueDate, int value, int unitIndex) {
        long offset;
        switch (unitIndex) {
            case 0:
                offset = value * 60 * 1000L;
                break; // Phút
            case 1:
                offset = value * 60 * 60 * 1000L;
                break; // Giờ
            case 2:
                offset = value * 24 * 60 * 60 * 1000L;
                break; // Ngày
            default:
                offset = 0;
        }
        return dueDate - offset;
    }
}

