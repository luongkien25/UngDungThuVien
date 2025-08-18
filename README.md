# \# 📚 Hệ thống Quản lý Thư viện

# 

# Chương trình quản lý thư viện viết bằng \*\*Java + Swing\*\* với cơ sở dữ liệu \*\*MySQL\*\*, hỗ trợ quản lý sách, người dùng, mượn – trả, tìm kiếm nhanh và tích hợp \*\*Google Books API\*\* để nhập dữ liệu sách cùng ảnh bìa.

# 

# ---

# 

# \## 🚀 Chức năng chính

# 

# \- \*\*Quản lý sách\*\*

# &nbsp; - Thêm, sửa, xóa sách

# &nbsp; - Cập nhật số lượng, thống kê số lượt mượn

# &nbsp; - Tìm kiếm theo tiêu đề, tác giả, ISBN, thể loại

# &nbsp; - Hỗ trợ tải thông tin sách từ Google Books API

# 

# \- \*\*Quản lý người dùng\*\*

# &nbsp; - Đăng nhập / Đăng ký (User)

# &nbsp; - Quản trị viên (Admin) với phân quyền

# &nbsp; - Đổi mật khẩu (có mã hóa password)

# &nbsp; - Thống kê số lượt mượn của từng user

# 

# \- \*\*Mượn – trả sách\*\*

# &nbsp; - Mỗi người dùng được mượn tối đa 3 cuốn cùng lúc

# &nbsp; - Ghi nhận lịch sử mượn – trả

# &nbsp; - Hiển thị sách đã mượn và gợi ý sách theo sở thích

# ---

# 

# \## 🛠 Yêu cầu hệ thống

# 

# \- \*\*Java JDK\*\* 17 trở lên  

# \- \*\*MySQL\*\* 8.x  

# \- \*\*Maven\*\* 3.9.x  

# 

# ---

# 

# \## ⚙️ Cài đặt \& chạy chương trình

# 

# 1\. Clone project:

# &nbsp;  ```bash

# &nbsp;  git clone https://github.com/your-username/library-system.git

&nbsp;  cd library-system
2. Cấu hình cơ sở dữ liệu trong DatabaseManager.java:
===

# &nbsp;  private static final String URL =       "jdbc:mysql://localhost:3306/library\_db";

# &nbsp;  private static final String USER = "root";

# &nbsp;  private static final String PASS = "1234";

3\. Lấy csdl trong file Dump20250818.sql
## 🔑 Tài khoản mặc định
===

# 

# Admin:

# 

# user: admin

# 

# pass: luongkien25

# 

# User: có thể đăng ký mới ngay trên giao diện.



