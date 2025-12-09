# 🏬 Cartsy – Store Management System

**A comprehensive Java Swing desktop application for managing a single large store, including products, sales, customers, sellers, and analytics.**

---

## Project Overview
Cartsy is a professional desktop application designed to manage all aspects of a single large store. It allows store owners to manage products, track sales, handle orders, manage sellers, monitor ads, and communicate with customers. Analytics dashboards provide insights into sales trends, inventory levels, and seller performance, all powered by **Java Swing**, **MySQL**, and **JFreeChart**.

---

## Features
### Store Owner / Admin:
- Manage all sellers and customers
- Approve or reject seller registrations
- Monitor all sales and orders
- Approve or boost seller ads
- View analytics on sales, products, and ads

### Sellers:
- Self-register and wait for admin approval
- Manage product catalog (add, update, remove)
- Track orders for their products
- Post ads and monitor performance
- View analytics dashboards for sales and inventory

### Customers:
- Browse products
- Place orders using multiple addresses
- Track order history and delivery status
- Communicate with sellers via chat
- Receive notifications for orders, approvals, and ads

### Common Features:
- Multi-address support for customers
- Cart and checkout system
- Payment tracking (cash, card, wallet, online)
- Real-time chat between customers and sellers
- Analytics and visual charts (bar, pie, line, time-series)

---

## File / Folder Structure
 src/
├─ admin/
│ └─ AdminDashboard.java
│ └─ AdminLogin.java
│ └─ ManageInventoryDialog.java
│ └─ ViewSalesDialog.java
│ └─ RunAdsDialog.java
│ └─ BillDialog.java
│ └─ CheckOutDialog.java
├─ seller/
│ └─ SellerDashboard.java
│ └─ SellerLogin.java
│ └─ SellerRegister.java
│ └─ AddProductDialog.java
│ └─ UpdateProductDialog.java
│ └─ ViewMyProductsDialog.java
│ └─ SellerChatDialog.java
│ └─ SelectSellerChatDialog.java
├─ customer/
│ └─ CustomerDashboard.java
│ └─ CustomerLogin.java
│ └─ CustomerRegister.java
│ └─ CustomerCartDialog.java
│ └─ CustomerOrdersDialog.java
│ └─ CustomerCatalogDialog.java
│ └─ EditCustomerProfileDialog.java
│ └─ CustomerChatDialog.java
├─ shared/
│ └─ DBConnection.java
│ └─ UtilsHelper.java
│ └─ ProductSearchDialog.java
│ └─ AddAddressDialog.java
│ └─ ManageAddressesDialog.java
│ └─ ImprovedChatDialog.java
├─ Main.java
└─ MainMenu.java

## Database Structure Overview
  Cartsy’s database is designed to support multi-role operations and real-world store management. Key points:  
- **Users Table**: Stores all users (Admin, Seller, Customer) with profile and login info  
- **Sellers Table**: Tracks seller-specific info like approval status, tier, and seller     code  
- **Products & Inventory**: Store product details and stock changes  
- **Orders & Payments**: Manage customer orders, items, and payments  
- **Cart**: Temporary storage before checkout  
- **Sales**: Track completed sales for analytics  
- **Ads & Notifications**: Seller ads with admin approval and system notifications  
- **Chat**: Communication between sellers and customers  

  This structure ensures **data integrity, referential relationships, and analytics-ready design**

 ---

## Author
 **M.Adeel Khan**  
 Email: madeelkhan072@gmail.com 
 GitHub: https://github.com/MuhammadAdeel0072
