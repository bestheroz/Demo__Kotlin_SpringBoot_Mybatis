package com.github.bestheroz.standard.config

import com.github.bestheroz.standard.common.enums.AuthorityEnum
import com.github.bestheroz.standard.common.mybatis.handler.GenericListTypeHandler
import org.mybatis.spring.SqlSessionFactoryBean
import org.mybatis.spring.annotation.MapperScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import javax.sql.DataSource

@Configuration
@MapperScan("com.github.bestheroz")
class MyBatisConfig {
    @Bean
    fun sqlSessionFactory(dataSource: DataSource): SqlSessionFactoryBean {
        val sqlSessionFactoryBean = SqlSessionFactoryBean()
        sqlSessionFactoryBean.setDataSource(dataSource)

        // 매퍼 XML 파일 위치 설정
        sqlSessionFactoryBean.setMapperLocations(
            *PathMatchingResourcePatternResolver().getResources("classpath*:mappers/*.xml"),
        )

        // MyBatis 설정 추가: underscore -> camelCase 매핑 활성화
        val configuration =
            org.apache.ibatis.session
                .Configuration()
        configuration.isMapUnderscoreToCamelCase = true
        configuration.typeHandlerRegistry.register(
            List::class.java,
            GenericListTypeHandler(AuthorityEnum::class.java),
        )

        sqlSessionFactoryBean.setConfiguration(configuration)

        return sqlSessionFactoryBean
    }
}
