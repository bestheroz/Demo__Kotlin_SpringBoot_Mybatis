package com.github.bestheroz.demo.domain.service

import com.github.bestheroz.demo.domain.Admin
import com.github.bestheroz.demo.domain.User
import com.github.bestheroz.demo.repository.AdminRepository
import com.github.bestheroz.demo.repository.UserRepository
import com.github.bestheroz.standard.common.domain.IdCreated
import com.github.bestheroz.standard.common.domain.IdCreatedUpdated
import com.github.bestheroz.standard.common.enums.UserTypeEnum
import org.springframework.stereotype.Component

@Component
class OperatorHelper(
    private val adminRepository: AdminRepository,
    private val userRepository: UserRepository,
) {
    fun <T : IdCreatedUpdated> fulfilOperator(operators: List<T>): List<T> {
        if (operators.isEmpty()) return operators

        val adminIds = mutableSetOf<Long>()
        val userIds = mutableSetOf<Long>()

        collectIds(operators, adminIds, userIds, includeUpdated = true)

        val adminMap = fetchAdminMap(adminIds)
        val userMap = fetchUserMap(userIds)

        setOperatorData(operators, adminMap, userMap, includeUpdated = true)

        return operators
    }

    fun <T : IdCreatedUpdated> fulfilOperator(operator: T?): T? {
        if (operator == null) return null
        return fulfilOperator(listOf(operator)).first()
    }

    fun <T : IdCreated> fulfilCreatedOperator(operators: List<T>): List<T> {
        if (operators.isEmpty()) return operators

        val adminIds = mutableSetOf<Long>()
        val userIds = mutableSetOf<Long>()

        collectIds(operators, adminIds, userIds, includeUpdated = false)

        val adminMap = fetchAdminMap(adminIds)
        val userMap = fetchUserMap(userIds)

        setOperatorData(operators, adminMap, userMap, includeUpdated = false)

        return operators
    }

    fun <T : IdCreated> fulfilCreatedOperator(operator: T?): T? {
        if (operator == null) return null
        return fulfilCreatedOperator(listOf(operator)).first()
    }

    private fun collectIds(
        operators: List<IdCreated>,
        adminIds: MutableSet<Long>,
        userIds: MutableSet<Long>,
        includeUpdated: Boolean,
    ) {
        operators.forEach { operator ->
            when (operator.createdObjectType) {
                UserTypeEnum.ADMIN -> adminIds.add(operator.createdObjectId)
                UserTypeEnum.USER -> userIds.add(operator.createdObjectId)
            }

            if (includeUpdated && operator is IdCreatedUpdated) {
                when (operator.updatedObjectType) {
                    UserTypeEnum.ADMIN -> adminIds.add(operator.updatedObjectId)
                    UserTypeEnum.USER -> userIds.add(operator.updatedObjectId)
                }
            }
        }
    }

    private fun fetchAdminMap(adminIds: Set<Long>): Map<Long, Admin> =
        if (adminIds.isEmpty()) {
            emptyMap()
        } else {
            adminRepository
                .getTargetItemsByMap(setOf("id", "loginId", "name"), mapOf("id:in" to adminIds))
                .associateBy { it.id!! }
        }

    private fun fetchUserMap(userIds: Set<Long>): Map<Long, User> =
        if (userIds.isEmpty()) {
            emptyMap()
        } else {
            userRepository
                .getTargetItemsByMap(setOf("id", "loginId", "name"), mapOf("id:in" to userIds))
                .associateBy { it.id!! }
        }

    private fun setOperatorData(
        operators: List<IdCreated>,
        adminMap: Map<Long, Admin>,
        userMap: Map<Long, User>,
        includeUpdated: Boolean,
    ) {
        operators.forEach { operator ->
            when (operator.createdObjectType) {
                UserTypeEnum.ADMIN ->
                    adminMap[operator.createdObjectId]?.apply { operator.createdByAdmin = this }
                UserTypeEnum.USER ->
                    userMap[operator.createdObjectId]?.apply { operator.createdByUser = this }
            }

            if (includeUpdated && operator is IdCreatedUpdated) {
                when (operator.updatedObjectType) {
                    UserTypeEnum.ADMIN ->
                        adminMap[operator.updatedObjectId]?.apply { operator.updatedByAdmin = this }
                    UserTypeEnum.USER ->
                        userMap[operator.updatedObjectId]?.apply { operator.updatedByUser = this }
                }
            }
        }
    }
}
