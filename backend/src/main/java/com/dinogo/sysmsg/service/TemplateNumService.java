package com.dinogo.sysmsg.service;

public interface TemplateNumService {

    /**
     * 根據 prefix 產生下一個 msg_function。
     *
     * 例如：
     *
     * generateMsgFunction("OA")
     *
     * → OA-001
     * → OA-002
     * → OA-003
     *
     * 必須使用：
     *
     * @Transactional
     * +
     * msg_function_sequence 單列悲觀鎖
     */
    String generateMsgFunction(String prefix);
}
