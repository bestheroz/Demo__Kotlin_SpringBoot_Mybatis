package com.github.bestheroz.demo.domain

import com.github.bestheroz.standard.common.domain.IdCreatedUpdated
import com.github.bestheroz.standard.common.security.Operator
import jakarta.persistence.Table
import java.time.Instant

@Table(name = "notices")
data class Notice(
    var title: String = "",
    var content: String = "",
    var useFlag: Boolean = false,
    var removedFlag: Boolean = false,
    private var removedAt: Instant? = null,
) : IdCreatedUpdated() {
    companion object {
        fun of(
            title: String,
            content: String,
            useFlag: Boolean,
            operator: Operator,
        ) = Notice(title = title, content = content, useFlag = useFlag).apply {
            val now = Instant.now()
            this.setCreatedBy(operator, now)
            this.setUpdatedBy(operator, now)
        }
    }

    fun update(
        title: String,
        content: String,
        useFlag: Boolean,
        operator: Operator,
    ) {
        this.title = title
        this.content = content
        this.useFlag = useFlag
        val now = Instant.now()
        this.setUpdatedBy(operator, now)
    }

    fun remove(operator: Operator) {
        this.removedFlag = true
        val now = Instant.now()
        this.removedAt = now
        this.setUpdatedBy(operator, now)
    }
}
