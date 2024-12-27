package com.github.bestheroz.demo.entity.service

import com.github.bestheroz.demo.entity.Admin
import com.github.bestheroz.demo.entity.User
import com.github.bestheroz.demo.repository.AdminRepository
import com.github.bestheroz.demo.repository.UserRepository
import com.github.bestheroz.standard.common.entity.IdCreated
import com.github.bestheroz.standard.common.entity.IdCreatedUpdated
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
                .getTargetItemsByMap(
                    targetColumns = setOf("id", "loginId", "name"),
                    whereConditions = mapOf("id:in" to adminIds),
                ).associateBy { it.id!! }
        }

    private fun fetchUserMap(userIds: Set<Long>): Map<Long, User> =
        if (userIds.isEmpty()) {
            emptyMap()
        } else {
            userRepository
                .getTargetItemsByMap(
                    targetColumns = setOf("id", "loginId", "name"),
                    whereConditions = mapOf("id:in" to userIds),
                ).associateBy { it.id!! }
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
                    adminMap[operator.createdObjectId]?.let { admin -> operator.createdByAdmin = admin }
                UserTypeEnum.USER ->
                    userMap[operator.createdObjectId]?.let { user -> operator.createdByUser = user }
            }

            if (includeUpdated && operator is IdCreatedUpdated) {
                when (operator.updatedObjectType) {
                    UserTypeEnum.ADMIN ->
                        adminMap[operator.updatedObjectId]?.let { admin -> operator.updatedByAdmin = admin }
                    UserTypeEnum.USER ->
                        userMap[operator.updatedObjectId]?.let { user -> operator.updatedByUser = user }
                }
            }
        }
    }
}
