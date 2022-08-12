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


import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Contextual

/**
 * 
 *
 * @param i 
 * @param k 
 * @param o 
 * @param x 
 */
@Serializable
data class Tokens (

    @SerialName(value = "i")
    val i: kotlin.Int? = null,

    @SerialName(value = "k")
    val k: kotlin.Int? = null,

    @SerialName(value = "o")
    val o: kotlin.String? = null,

    @SerialName(value = "x")
    val x: kotlin.String? = null

)

