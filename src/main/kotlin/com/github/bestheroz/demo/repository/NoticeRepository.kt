package com.github.bestheroz.demo.repository

import com.github.bestheroz.demo.domain.Notice
import io.github.bestheroz.mybatis.MybatisRepository
import org.apache.ibatis.annotations.Mapper
import org.springframework.stereotype.Repository

@Mapper @Repository
interface NoticeRepository : MybatisRepository<Notice>
