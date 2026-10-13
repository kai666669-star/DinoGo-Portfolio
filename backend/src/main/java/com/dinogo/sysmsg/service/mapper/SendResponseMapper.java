package com.dinogo.sysmsg.service.mapper;

import org.springframework.stereotype.Component;

import com.dinogo.sysmsg.dto.response.SendResponse;
import com.dinogo.sysmsg.dto.response.SendTemplateResponse;
import com.dinogo.sysmsg.entity.SendEntity;
import com.dinogo.sysmsg.entity.SendSellerEntity;

/** 集中處理 Send Entity 對外 DTO 映射，不承擔驗證或資料存取。 */
@Component
public class SendResponseMapper {
    public SendResponse toResponse(SendEntity source) {
        SendResponse target = new SendResponse();
        target.setSendId(source.getSendId());
        target.setMsgFunction(source.getMsgFunction());
        target.setMsgfromSellerId(source.getMsgfromSellerId());
        target.setMsgLabel(source.getMsgLabel());
        target.setSendTitle(source.getSendTitle());
        target.setSendContent(source.getSendContent());
        if (source instanceof SendSellerEntity seller) {
            target.setOrderNo(seller.getOrderNo());
        }
        target.setSendStatus(source.getSendStatus());
        target.setSendUpdAt(source.getSendUpdAt());
        return target;
    }

    public SendTemplateResponse toTemplateResponse(SendEntity source) {
        SendTemplateResponse target = new SendTemplateResponse();
        target.setSendId(source.getSendId());
        target.setMsgFunction(source.getMsgFunction());
        target.setMsgfromSellerId(source.getMsgfromSellerId());
        target.setMsgLabel(source.getMsgLabel());
        target.setSendTitle(source.getSendTitle());
        target.setSendContent(source.getSendContent());
        if (source instanceof SendSellerEntity seller) {
            target.setSendRemark(seller.getSendRemark());
            target.setImgOne(seller.getImgOne());
            target.setImgOnePublicId(seller.getImgOnePublicId());
            target.setImgTwo(seller.getImgTwo());
            target.setImgTwoPublicId(seller.getImgTwoPublicId());
            target.setImgThree(seller.getImgThree());
            target.setImgThreePublicId(seller.getImgThreePublicId());
        }
        target.setSendStatus(source.getSendStatus());
        target.setSendUpdAt(source.getSendUpdAt());
        return target;
    }
}
