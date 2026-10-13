package com.dinogo.sysmsg.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.sysmsg.entity.MsgFunctionSequenceEntity;
import com.dinogo.sysmsg.repository.MsgFunctionSequenceRepository;
import com.dinogo.sysmsg.service.TemplateNumService;

@Service
public class TemplateNumServiceImpl implements TemplateNumService {

    private final MsgFunctionSequenceRepository sequenceRepository;

    public TemplateNumServiceImpl(
            MsgFunctionSequenceRepository sequenceRepository
    ) {
        this.sequenceRepository = sequenceRepository;
    }

    /**
     * ============================================================
     * Spring Boot 控制：
     *
     * 1. Transaction
     * 2. 鎖定 msg_function_sequence 對應 prefix 的單一資料列
     * 3. 下一個流水號
     *
     * SQL Server 控制：
     *
     * 1. CHECK msg_function 格式
     * 2. UNIQUE SAVE msg_function
     *
     * 不使用 Trigger。
     * ============================================================
     */
    @Override
    @Transactional
    public String generateMsgFunction(String prefix) {

        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("msg_function prefix 不可空白");
        }
        String normalizedPrefix = prefix.trim().toUpperCase();

        validatePrefix(normalizedPrefix);

        /*
         * Repository 對該 prefix 的 sequence 資料列使用悲觀寫入鎖。
         *
         * 注意：
         * SQL Server 的 JPA 悲觀鎖實際 SQL 會依 Hibernate
         * Dialect 轉換成 SQL Server 對應的鎖定語法。
         */
        MsgFunctionSequenceEntity sequence = sequenceRepository
                .findById(normalizedPrefix)
                .orElseThrow(() -> new IllegalStateException(
                        "缺少 msg_function_sequence 初始資料：" + normalizedPrefix
                ));

        int nextNumber = sequence.getCurrentValue() + 1;

        if (nextNumber > 999) {
            throw new IllegalStateException(
                    "msg_function 已達到 999：" + normalizedPrefix
            );
        }

        sequence.setCurrentValue(nextNumber);
        sequenceRepository.save(sequence);

        return String.format(
                "%s-%03d",
                normalizedPrefix,
                nextNumber
        );
    }

    private void validatePrefix(String prefix) {

        switch (prefix) {

            case "OA":
            case "OC":
            case "OS":
            case "AC":
            case "AS":
            case "SC":
                return;

            default:
                throw new IllegalArgumentException(
                        "不合法的 msg_function prefix：" + prefix
                );
        }
    }
}
