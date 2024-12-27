package com.github.bestheroz.standard.common.health

import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Select
import org.springframework.stereotype.Repository

@Mapper
@Repository
interface HealthRepository {
    @Select("select now()")
    fun selectNow()
}
