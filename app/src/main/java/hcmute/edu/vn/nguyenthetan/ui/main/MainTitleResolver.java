package hcmute.edu.vn.nguyenthetan.ui.main;

public final class MainTitleResolver {
    private MainTitleResolver() {}

    public static String resolveTitle(int mode) {
        switch (mode) {
            case 0:
                return "Hộp thư đến";
            case 1:
                return "Hôm nay";
            case 2:
                return "7 ngày kế tiếp";
            case 4:
                return "Đã hoàn thành";
            case 5:
                return "Tất cả nhiệm vụ";
            case 6:
                return "Nhiệm vụ nháp";
            default:
                return "Nhiệm vụ";
        }
    }
}

