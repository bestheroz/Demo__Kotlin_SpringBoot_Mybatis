package com.github.bestheroz.standard.config

import com.github.bestheroz.standard.common.enums.AuthorityEnum
import com.github.bestheroz.standard.common.mybatis.handler.GenericListTypeHandler
import org.apache.ibatis.session.SqlSessionFactory
import org.mybatis.spring.SqlSessionFactoryBean
import org.mybatis.spring.annotation.MapperScan
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import javax.sql.DataSource

@Configuration
@MapperScan("com.github.bestheroz")
class MyBatisConfig {
    @Bean
    fun sqlSessionFactory(dataSource: DataSource): SqlSessionFactory {
        val sessionFactory = SqlSessionFactoryBean()
        sessionFactory.setDataSource(dataSource)

        // Try to load mapper XML files if they exist
        try {
            sessionFactory.setMapperLocations(
                *PathMatchingResourcePatternResolver().getResources("classpath*:mybatis/mapper/**/*.xml"),
            )
        } catch (e: Exception) {
            // Mapper XMLs are optional, can use annotations instead
        }

        val configuration =
            org.apache.ibatis.session
                .Configuration()
        configuration.isMapUnderscoreToCamelCase = true
        configuration.typeHandlerRegistry.register(
            List::class.java,
            GenericListTypeHandler(AuthorityEnum::class.java),
        )

        sessionFactory.setConfiguration(configuration)
        return checkNotNull(sessionFactory.`object`)
    }

    @Bean
    fun mybatisConfigurationCustomizer(): ConfigurationCustomizer =
        ConfigurationCustomizer { configuration ->
            configuration.isMapUnderscoreToCamelCase = true

            configuration.typeHandlerRegistry.register(
                List::class.java,
                GenericListTypeHandler(AuthorityEnum::class.java),
            )
        }
}
