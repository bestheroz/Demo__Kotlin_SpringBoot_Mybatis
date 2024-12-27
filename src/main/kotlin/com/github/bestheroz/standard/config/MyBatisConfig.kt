package com.github.bestheroz.standard.config

import com.github.bestheroz.standard.common.enums.AuthorityEnum
import com.github.bestheroz.standard.common.mybatis.handler.GenericListTypeHandler
import org.mybatis.spring.annotation.MapperScan
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@MapperScan("com.github.bestheroz")
class MyBatisConfig {
    @Bean
    fun mybatisConfigurationCustomizer(): ConfigurationCustomizer =
        ConfigurationCustomizer { configuration ->
            configuration.typeHandlerRegistry.register(
                List::class.java,
                GenericListTypeHandler(AuthorityEnum::class.java),
            )
        }
}
