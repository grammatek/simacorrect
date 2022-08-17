/**
 * Grammar correction API
 *
 * This API interfaces Yfirlestur.is/
 *
 * The version of the OpenAPI document: 0.1.0-oas3
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


import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * 
 *
 * @param code 
 * @param detail 
 * @param end 
 * @param endChar 
 * @param start 
 * @param startChar 
 * @param suggest 
 * @param text 
 */
@Serializable
data class Annotations (

    @SerialName(value = "code")
    val code: kotlin.String? = null,

    @SerialName(value = "detail")
    val detail: kotlin.String? = null,

    @SerialName(value = "end")
    val end: kotlin.Int? = null,

    @SerialName(value = "end_char")
    val endChar: kotlin.Int? = null,

    @SerialName(value = "start")
    val start: kotlin.Int? = null,

    @SerialName(value = "start_char")
    val startChar: kotlin.Int? = null,

    @SerialName(value = "suggest")
    val suggest: kotlin.String? = null,

    @SerialName(value = "text")
    val text: kotlin.String? = null

)
