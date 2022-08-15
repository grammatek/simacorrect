/**
 * Spelling/Grammar correction API
 *
 * This API interfaces an Yfirlestur.is compatible service
 *
 * The version of the OpenAPI document: 0.1.2
 * Contact: info@grammatek.com
 *
 * Please note:
 * This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * Do not edit this file manually.
 */

@file:Suppress(
    "ArrayInDataClass",
    "EnumEntryName",
    "RemoveRedundantQualifierName",
    "UnusedImport"
)

package org.grammatek.models

import org.grammatek.models.Result
import org.grammatek.models.Stats

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Contextual

/**
 * 
 *
 * @param valid 
 * @param result 
 * @param stats 
 * @param text 
 */
@Serializable
data class CorrectResponse (

    @SerialName(value = "valid")
    val valid: kotlin.Boolean,

    @SerialName(value = "result")
    val result: kotlin.collections.List<@Contextual kotlin.collections.List<Result>>? = null,

    @SerialName(value = "stats")
    val stats: Stats? = null,

    @SerialName(value = "text")
    val text: kotlin.String? = null

)

