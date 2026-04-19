# 🏏 IPL Tournament Management System

## 📌 Project Description
This project is a **Cricket Match Management System** inspired by the IPL tournament.
It is built using **Java (Swing)** and **MySQL (DBMS)** to manage teams, players, matches, live scoring, and points table automatically.

## 🎯 Features
* ➕ Add Teams
* 👤 Add Players to Teams
* 📅 Schedule Matches
* 🏏 Live Match Scoring System
* 🔄 Automatic Match Result Update
* 🏆 Points Table Generation
* 📊 View Match Schedule & Stats

## 🛠️ Technologies Used
* 💻 Java (Swing GUI)
* 🛢️ MySQL Database
* 🔌 JDBC (Java Database Connectivity)

## 🗂️ Project Structure
```
IPL-DBMS/
│
├── DBConnection.java
├── Homepage.java
├── Livematchpage.java
├── Statspage.java
├── ipl_db.sql
└── README.md

```
## ⚙️ Database Setup
1. Open MySQL
2. Create database:
```sql
CREATE DATABASE cricket_db;
```

3. Import SQL file:
```sql
USE cricket_db;
SOURCE ipl_db.sql;
```

## ▶️ How to Run the Project
1. Open project in **NetBeans / Eclipse**
2. Update database credentials in `DBConnection.java`:

```java
"root",
"your_password"
```

3. Run:
Homepage.java
## 🚀 Future Enhancements
* 🔐 Admin Login System
* 📈 Net Run Rate Calculation
* 🌐 Web Version (Spring Boot / React)
* 📱 Mobile App Integration

## ⚠️ Important Notes
* Do NOT upload real database password
* Make sure MySQL server is running
* Database must be created before running project

## ⭐ If You Like This Project
Give it a ⭐ on GitHub and share it!
