package com.dinogo.sysmsg.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 每個 msg_function prefix 的獨立計數器。 */
@Entity
@Table(name = "msg_function_sequence", schema = "sysmsg")
public class MsgFunctionSequenceEntity {

    @Id
    @Column(name = "prefix", length = 2, nullable = false, updatable = false)
    private String prefix;

    @Column(name = "current_value", nullable = false)
    private Integer currentValue;

    protected MsgFunctionSequenceEntity() {
    }

    public String getPrefix() {
        return prefix;
    }

    public Integer getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(Integer currentValue) {
        this.currentValue = currentValue;
    }
}
