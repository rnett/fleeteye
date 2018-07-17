package com.rnett.ligraph.eve.fleeteye.db

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.rnett.ligraph.eve.fleeteye.ZKill
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.get
import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.exposed.sql.transactions.transaction

//TODO need a better/faster way of getting corp and alliance names

fun alliances.getOrMake(allianceID: Int): alliance = transaction {
    val current = findFromPKs(allianceID)
    if (current != null)
        current
    else {
        println("Query Alliance $allianceID")
        val client = HttpClient(Apache)

        val url = "https://esi.evetech.net/latest/alliances/$allianceID/?datasource=tranquility"
        val name = JsonParser().parse(
                runBlocking {
                    client.get<String>(url) {
                        headers["accept"] = "accept: application/json"
                    }
                }
        ).asJsonObject["name"].asString

        alliance.new {
            allianceid = allianceID
            alliancename = name
        }
    }
}


fun corporations.getOrMake(corpID: Int): corporation = transaction {
    val current = corporations.findFromPKs(corpID)
    if (current != null)
        current
    else {
        println("Query Corp $corpID")
        val client = HttpClient(Apache)

        val url = "https://esi.evetech.net/latest/corporations/$corpID/?datasource=tranquility"
        val json = JsonParser().parse(
                runBlocking {
                    client.get<String>(url) {
                        headers["accept"] = "accept: application/json"
                    }
                }
        ).asJsonObject

        val name = json["name"].asString

        val allianceID: Int? = try {
            json.asJsonObject["alliance_id"].asInt
        } catch (e: Exception) {
            null
        }

        if (allianceID != null)
            alliances.getOrMake(allianceID)

        corporation.new {
            corpid = corpID
            corpname = name
            allianceid = allianceID
        }
    }
}

fun characterdatas.getOrMake(charID: Int, shipID: Int = 0): characterdata = transaction {
    val current = characterdatas.findFromPKs(charID)
    if (current != null)
        current
    else {
        println("Query Character $charID")
        val client = HttpClient(Apache)

        val url = "https://esi.evetech.net/latest/characters/$charID/?datasource=tranquility"
        val json = JsonParser().parse(
                runBlocking {
                    client.get<String>(url) {
                        headers["accept"] = "accept: application/json"
                    }
                }
        ).asJsonObject

        val name = json["name"].asString

        val corpID: Int? = try {
            json.asJsonObject["corporation_id"].asInt
        } catch (e: Exception) {
            null
        }
        val allianceID: Int? = try {
            json.asJsonObject["alliance_id"].asInt
        } catch (e: Exception) {
            null
        }

        if (allianceID != null)
            alliances.getOrMake(allianceID)

        if (corpID != null)
            corporations.getOrMake(corpID)

        characterdata.new {
            charid = charID
            charname = name
            corpid = corpID
            allianceid = allianceID
            shipid = shipID
        }
    }
}

val kill.zKill
    get() = Gson().fromJson<ZKill>(this.zkill)