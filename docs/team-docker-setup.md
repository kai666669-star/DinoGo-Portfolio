# DinoGo 團隊 Docker 與共用 Demo DB 指南

## 架構

所有組員共用 Server PC `192.168.31.70` 上的 DinoGo Final Demo DB：`0901`。SQL Server 位於 Docker 的 `database` service，資料保存在 Server PC 的 `dinogo_db_data` volume。這不是每位組員各自建立的資料庫。

正式網站為 [https://dinogo-shop.site](https://dinogo-shop.site)。它與共用 Demo DB 使用同一份資料；請將資料操作視為團隊共同行為。

## 組員前置需求

1. 安裝 Docker Desktop。
2. 取得最新 `develop`：

   ```powershell
   git pull origin develop
   ```

3. 由 `.env.example` 建立自己的 root `.env`，並向團隊管理者取得必要的應用程式帳號與 secret。`.env` 不可提交。
4. Team Docker client 的 `.env` 至少設定：

   ```dotenv
   SHARED_DB_HOST=192.168.31.70
   SHARED_DB_PORT=9434
   SHARED_DB_NAME=0901
   ```

   `DB_USERNAME`、`DB_PASSWORD` 使用團隊提供的應用程式帳號；不可使用 `sa`。

## 組員 Docker 啟動方式

組員**不可**使用 `compose.yml` 啟動 database service。請只使用 app-only Team Compose：

```powershell
docker compose -f compose.team-client.yml up -d --build
```

這個 Compose 僅啟動 backend、frontend、reverse-proxy；不會建立 SQL Server container 或 `dinogo_db_data`。

開啟 [http://localhost:8088](http://localhost:8088)。停止本機 app stack 時，請使用：

```powershell
docker compose -f compose.team-client.yml down
```

不得在任何共用 DB 作業中加入 `-v`。

## SSMS 連線

| 項目 | 值 |
| --- | --- |
| Server | `192.168.31.70,9434` |
| Database | `0901` |
| 帳號 | 向團隊管理者取得的個人／應用程式帳號 |

請不要使用 `sa`。只有在團隊明確授權下才可用 SSMS 修改資料；所有 DDL 與資料大量異動都會影響正式 Demo。

## 共用資料與 Demo 帳號

- `0901` 是所有組員共用的 Final Demo DB。
- Demo 帳號、商品、訂單、評論與測試資料都是共用資料。
- 測試前先確認不會覆蓋或刪除其他組員正在展示的資料。
- Demo 前請確認登入、商品、購物車、訂單、賣場、評論與管理功能的資料狀態；必要的測試資料調整請在團隊群組先協調。

## 絕對禁止

- `docker compose down -v`
- `docker volume prune`
- 刪除或重建 Server PC 的 `dinogo_db_data`
- 重新 import BACPAC
- `DROP`、`TRUNCATE`、schema 修改、未經授權的 migration 或 reset
- 使用 `sa` 作為應用程式或一般 SSMS 帳號

也不得連線或使用已淘汰的 `192.168.31.151:1433`。

## Legacy `/uploads` 與 Cloudinary

部分舊資料仍指向 `/uploads/...`。組員的 `compose.team-client.yml` 使用本機 uploads volume，因此可能無法顯示部分舊圖片；內容與呈現請以 [正式網站](https://dinogo-shop.site) 為準。Cloudinary 圖片不受這項限制。

## Server PC 專用設定

`compose.tunnel.yml` 只供 Server PC 的正式 Cloudflare Tunnel runtime 使用。Server PC 若需開放團隊 SSMS LAN 存取，root `.env` 設定：

```dotenv
MSSQL_BIND_IP=192.168.31.70
MSSQL_HOST_PORT=9434
```

並使用：

```powershell
docker compose -p dinogo -f compose.yml -f compose.tunnel.yml -f compose.team.yml up -d
```

此設定只將 SQL Server 綁定到 Server PC 的 LAN IP `192.168.31.70:9434`；不得改成 `0.0.0.0`、host port `1433` 或公開網際網路。
