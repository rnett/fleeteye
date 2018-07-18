package com.rnett.ligraph.eve.fleeteye

import com.google.gson.Gson
import com.kizitonwose.time.hours
import com.rnett.ligraph.eve.fleeteye.db.characterdata
import com.rnett.ligraph.eve.fleeteye.db.grouping
import com.rnett.ligraph.eve.fleeteye.db.groupings
import com.rnett.ligraph.eve.fleeteye.db.kills
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

const val INITIAL_SCORE = 2
const val ALLIANCE_SCORE = 4
const val CORP_SCORE = 2
const val SHIP_SCORE = 3
const val KILL5_SCORE = 4
const val KILL10_SCORE = 6
const val KILL30_SCORE = 10

object GroupingHelper {
    fun updateGroupings(kill: ZKill) {
        transaction {

            groupings.deleteWhere { groupings.lastupdated lessEq (Calendar.getInstance().timeInMillis - 1.hours.inMilliseconds.longValue) }

            if (kill.victim.isCharacter) {
                groupings.deleteWhere { groupings.charid eq kill.victim.characterID!! }
                groupings.deleteWhere { groupings.othercharid eq kill.victim.characterID!! }
                Groups.removeChar(kill.victim.character!!)
            }

            kill.playerAttackers.forEach {
                it.character!!.shipid = it.shipTypeID
            }

            if (kills.findFromPKs(kill.killmailID) == null) {

                com.rnett.ligraph.eve.fleeteye.db.kill.new {
                    killid = kill.killmailID
                    zkill = Gson().toJson(kill)
                }
            }
        }

        kill.playerAttackers.forEach {
            val original = it

            if (it.shipTypeID != 0)
                kill.playerAttackers.forEach {
                    if (it != original && it.shipTypeID != 0)
                        updateGrouping(original.character!!, it.character!!)
                }
        }
    }

    private fun updateGrouping(char: characterdata, otherChar: characterdata) {
        transaction {
            val newGrouping = groupings.findFromPKs(char.charid, otherChar.charid)

            if (newGrouping == null) {
                TransactionManager.current().exec("INSERT INTO groupings (charid, othercharid, firstcreated, lastupdated, score, hits) VALUES (" +
                        char.charid + ", " +
                        otherChar.charid + ", " +
                        Calendar.getInstance().timeInMillis + ", " +
                        Calendar.getInstance().timeInMillis + ", " +
                        scoreGrouping(char, otherChar, newGrouping) + ", " +
                        1 + ")")

            } else {
                newGrouping.score = scoreGrouping(char, otherChar, newGrouping)
                newGrouping.lastupdated = Calendar.getInstance().timeInMillis
                newGrouping.hits = newGrouping.hits + 1
                newGrouping.storeWrittenValues()
            }
        }
        return
    }

    private fun scoreGrouping(char: characterdata, otherChar: characterdata, grouping: grouping? = null): Int {
        return transaction {
            if (grouping == null) {
                var score = INITIAL_SCORE

                if (char.allianceid == otherChar.allianceid) {
                    score += ALLIANCE_SCORE
                    if (char.corpid == otherChar.corpid)
                        score += CORP_SCORE
                }

                if (char.shipid == otherChar.shipid)
                    score += SHIP_SCORE

                score
            } else { //TODO use alliance stuff as multiplier for update?  prehaps a gentle one (1.3 or so)  need to differentiate 3rd partys
                var score = grouping.score

                if (char.allianceid == otherChar.allianceid) {
                    score += ALLIANCE_SCORE
                    if (char.corpid == otherChar.corpid)
                        score += CORP_SCORE
                }

                if (char.shipid == otherChar.shipid)
                    score += SHIP_SCORE

                score += when {
                    grouping.hits > 30 -> KILL30_SCORE
                    grouping.hits > 10 -> KILL10_SCORE
                    grouping.hits > 5 -> KILL5_SCORE
                    else -> 0
                }

                score
            }
        }
    }
}