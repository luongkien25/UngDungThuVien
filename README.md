# \# 📚 Hệ thống Quản lý Thư viện

# 

# Chương trình quản lý thư viện viết bằng \*\*Java + Swing\*\* với cơ sở dữ liệu \*\*MySQL\*\*, hỗ trợ quản lý sách, người dùng, mượn – trả, tìm kiếm nhanh và tích hợp \*\*Google Books API\*\* để nhập dữ liệu sách cùng ảnh bìa.

# 

# ---

# 

# \## 🚀 Chức năng chính

# 

# \### 📖 Quản lý sách

# \- Thêm, sửa, xóa sách

# \- Cập nhật số lượng

# \- Hiển thị chi tiết sách kèm ảnh bìa

# \- Thống kê số lượt mượn cho từng sách

# \- Tìm kiếm theo \*\*tiêu đề, tác giả, ISBN, thể loại\*\*

# \- Hỗ trợ tải thông tin sách từ \*\*Google Books API\*\*

# 

# \### 👤 Quản lý người dùng

# \- Đăng ký / Đăng nhập (User)

# \- Quản trị viên (Admin) với phân quyền riêng

# \- Hash mật khẩu trong database

# \- Đổi mật khẩu cho cả Admin và User

# \- Thống kê số lượt mượn sách của từng user

# \- Giới hạn số lượt mượn cùng lúc (tối đa \*\*3 cuốn\*\*)

# 

# \### 🔄 Mượn – trả sách

# \- Ghi nhận và hiển thị lịch sử mượn – trả của hệ thống, người dung mượn quá hạn sẽ được đẩy lên đầu danh sách

# \- Hiển thị danh sách sách đã mượn

# \- Quản lý số lượng sách đang mượn

# \- Không cho phép mượn thêm khi đã đủ 3 cuốn

# 

# \### 💡 Tính năng nâng cao

# \- Tính năng tìm kiếm sách nâng cao cho phép có sai xót trong nhập xâu kí tự tìm kiếm, danh sách hiển thị theo độ liên quan giảm dần, cho phép lọc xếp danh sách hiển thị theo thể loại, sắp xếp danh sách hiển thị theo các chỉ định.

# \- Gợi ý sách dựa trên cho người dung dựa trên thể loại sách yêu thích của họ(ưu tiên hơn) và sách phổ biến trong hệ trong khoảng thời gian.

# \- Hiển thị sách vừa đọc gần nhất

# \- Cache ảnh để tăng tốc hiển thị danh sách

# \- Giao diện riêng:

#   - \*\*AdminGUI\*\*: Quản lý sách, người dùng, thống kê

#   - \*\*UserGUI\*\*: Mượn – trả, gợi ý sách, đổi mật khẩu, xem lịch sử

# 

# ---

# 

# \## 🛠 Yêu cầu hệ thống

# \- \*\*Java JDK\*\* 17 trở lên

# \- \*\*MySQL\*\* 8.x

# \- \*\*Maven\*\* 3.9.x

# 

# ---

# 

# \## ⚙️ Cài đặt \& chạy chương trình

# 1\. Clone project: https://github.com/luongkien25/UngDungThuVien.git

# Cấu hình cơ sở dữ liệu trong DatabaseManager.java

# Import cơ sở dữ liệu từ file Dump20250818.sql

# 

# 🔑 Tài khoản mặc định

# Admin

# 

# user: admin

# 

# pass: 1234

# 

# User 

Có thể tự đăng ký mới
## 🗂 Cấu trúc thư viện
===

# 

!\[Cấu trúc thư viện](CauTrucThuVien.png)
## 🎬Video Demo
===

https://drive.google.com/drive/folders/1sjD\_W2HCHnKucx7E3ioPmj5BIkiVxZOh?usp=drive\_link

===



