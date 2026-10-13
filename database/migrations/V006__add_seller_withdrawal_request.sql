IF OBJECT_ID(N'seller.withdrawal_request', N'U') IS NULL
BEGIN
    CREATE TABLE seller.withdrawal_request (
        withdrawal_id int IDENTITY(1,1) NOT NULL,
        seller_id int NOT NULL,
        amount decimal(12,2) NOT NULL,
        status nvarchar(20) NOT NULL
            CONSTRAINT df_seller_withdrawal_request_status DEFAULT N'PROCESSING',
        requested_at datetime2 NOT NULL
            CONSTRAINT df_seller_withdrawal_request_requested_at DEFAULT SYSDATETIME(),
        CONSTRAINT pk_seller_withdrawal_request PRIMARY KEY (withdrawal_id),
        CONSTRAINT fk_seller_withdrawal_request_seller
            FOREIGN KEY (seller_id) REFERENCES seller.Seller(seller_id),
        CONSTRAINT ck_seller_withdrawal_request_amount
            CHECK (amount > 0),
        CONSTRAINT ck_seller_withdrawal_request_status
            CHECK (status IN (N'PROCESSING', N'PAID', N'REJECTED'))
    );

    CREATE INDEX ix_seller_withdrawal_request_seller_status
        ON seller.withdrawal_request (seller_id, status);
END;
