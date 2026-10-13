# DinoGo B2B2C 電商平台

DinoGo 是資展國際 Java 軟體工程師養成班的五人團隊專題，
提供會員購物、商品管理、訂單、付款、優惠券、商家中心及管理後台等功能。

> 本倉庫用於個人作品展示。我主要負責 Seller Center 賣家中心模組；
> 其他功能由團隊成員共同完成。

## 專案資訊

- 開發期間：2026/07～2026/09
- 團隊人數：5 人
- 專案類型：B2B2C 電商平台
- 我的角色：Seller Center 前後端功能開發

## 技術棧

### Backend

- Java 21
- Spring Boot
- Spring MVC
- Spring Data JPA / Hibernate
- Spring Security / JWT
- SQL Server
- Maven

### Frontend

- Vue 3
- Pinia
- Axios
- Chart.js
- Vite

### Development

- Git / GitHub
- Docker
- Postman

## 我的負責範圍

- 賣家商品查詢、新增、修改及上下架
- Seller 與 Product 的商品歸屬判斷
- 優惠券新增、查詢、修改及刪除
- 商家品牌資料與 Logo 上傳
- 商家營運 Dashboard 與銷售分析
- 商家錢包及提款功能
- 訂單列表、訂單詳情與出貨功能串接
- 前後端 API 整合與問題排查

## 代表性技術問題

### 避免賣家操作其他賣家的商品

早期功能曾使用前端傳入的 sellerId 判斷資料範圍，
可能造成不同賣家操作相同資料。

後續調整為：

1. 從 JWT 取得登入者身分。
2. 由登入會員取得對應 Seller。
3. 透過 Seller 與 Product 關聯確認商品歸屬。
4. 非商品擁有者不能查詢或修改該商品。

## 系統畫面

系統畫面與操作流程將持續補充至 `docs/screenshots`。

## 系統架構

架構圖將補充至 `docs/architecture`。

## 團隊聲明

本作品為五人團隊共同開發。本倉庫僅供個人求職作品展示，
並區分個人負責功能與團隊共同完成內容。