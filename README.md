# 🚀🧠 BrainRocket – Cartsy Store Management System

**A sleek, professional Java Swing desktop application for managing a single large store with products, sales, customers, sellers, and analytics.**

---

## 📝 Project Overview

Cartsy (aka **BrainRocket**) is a desktop application crafted for store owners to manage all aspects of a single large store.  
It handles product management, sales tracking, orders, seller management, ads, and customer communication.  
Interactive analytics dashboards provide insights into sales trends, inventory levels, and seller performance.  

**Tech Stack:** Java Swing + MySQL + JFreeChart

---

## ✨ Features

### 👑 Admin / Store Owner
- Manage all sellers and customers  
- Approve or reject seller registrations  
- Monitor all sales and orders  
- Approve or boost seller ads  
- View analytics on sales, products, and ads  

### 🛍️ Sellers
- Self-register and wait for admin approval  
- Manage product catalog (add, update, remove)  
- Track orders for their products  
- Post ads and monitor performance  
- View analytics dashboards for sales and inventory  

### 🧑‍💻 Customers
- Browse products  
- Place orders with multi-address support  
- Track order history and delivery status  
- Communicate with sellers via chat  
- Receive notifications for orders, approvals, and ads  

### 🔗 Common Features
- Cart and checkout system  
- Payment tracking (cash, card, wallet, online)  
- Real-time chat between customers and sellers  
- Analytics and visual charts (bar, pie, line, time-series)  

---

## 📂 File / Folder Structure
Cartsy/
├─ src/
│  ├─ admin/
│  │  ├─ AdminDashboard.java
│  │  ├─ AdminLogin.java
│  │  ├─ ManageInventoryDialog.java
│  │  ├─ ViewSalesDialog.java
│  │  ├─ RunAdsDialog.java
│  │  ├─ BillDialog.java
│  │  └─ CheckOutDialog.java
│  │
│  ├─ seller/
│  │  ├─ SellerDashboard.java
│  │  ├─ SellerLogin.java
│  │  ├─ SellerRegister.java
│  │  ├─ AddProductDialog.java
│  │  ├─ UpdateProductDialog.java
│  │  ├─ ViewMyProductsDialog.java
│  │  ├─ SellerChatDialog.java
│  │  └─ SelectSellerChatDialog.java
│  │
│  ├─ customer/
│  │  ├─ CustomerDashboard.java
│  │  ├─ CustomerLogin.java
│  │  ├─ CustomerRegister.java
│  │  ├─ CustomerCartDialog.java
│  │  ├─ CustomerOrdersDialog.java
│  │  ├─ CustomerCatalogDialog.java
│  │  ├─ EditCustomerProfileDialog.java
│  │  └─ CustomerChatDialog.java
│  │
│  ├─ shared/
│  │  ├─ DBConnection.java
│  │  ├─ UtilsHelper.java
│  │  ├─ ProductSearchDialog.java
│  │  ├─ AddAddressDialog.java
│  │  ├─ ManageAddressesDialog.java
│  │  └─ ImprovedChatDialog.java
│  │
│  ├─ Main.java
│  └─ MainMenu.java
│
├─ lib/
│  ├─ mysql-connector-java.jar
│  └─ jfreechart.jar
│
├─ assets/
│  ├─ images/
│  │  ├─ logo.png
│  │  ├─ banner.jpg
│  │  └─ icons/
│  │     ├─ add.png
│  │     ├─ edit.png
│  │     └─ delete.png
│  └─ fonts/
│     └─ Roboto-Regular.ttf
│
├─ README.md
├─ .gitignore
└─ database/
   └─ cartsydb.sql
 
✅ Reorganized for readability and to prevent GitHub from collapsing it into a paragraph.

---

## 🗄️ Database Structure Overview

The database is designed for multi-role operations and real-world store management:

- **Users Table** – Stores Admin, Seller, Customer profiles and login info  
- **Sellers Table** – Tracks seller-specific details (approval status, tier, seller code)  
- **Products & Inventory** – Stores product details and stock changes  
- **Orders & Payments** – Manages customer orders and payments  
- **Cart** – Temporary storage before checkout  
- **Sales** – Tracks completed sales for analytics  
- **Ads & Notifications** – Seller ads with admin approval & system notifications  
- **Chat** – Communication between sellers and customers  

> Ensures **data integrity, referential relationships, and analytics-ready design**  

---

## 🧑‍💻 Author

**M. Adeel Khan**  
📧 Email: madeelkhan072@gmail.com  
💻 GitHub: [https://github.com/MuhammadAdeel0072](https://github.com/MuhammadAdeel0072)

