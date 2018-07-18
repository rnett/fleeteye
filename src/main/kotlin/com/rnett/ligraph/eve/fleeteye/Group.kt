package com.rnett.ligraph.eve.fleeteye

import com.rnett.eve.ligraph.sde.invtype
import com.rnett.ligraph.eve.fleeteye.db.characterdata
import com.rnett.ligraph.eve.fleeteye.db.kill
import com.rnett.ligraph.eve.fleeteye.db.zKill
import org.jetbrains.exposed.sql.transactions.transaction


const val GROUP_THRESHOLD = 4


fun isScoutShip(ship: invtype): Boolean {
    return false
}

//TODO flesh out updates.  should be one method that gets called when a new kill comes in
object Groups {

    val groups = mutableSetOf<Group>()

    val strongestBonds = mutableMapOf<characterdata, Double>()

    fun removeChar(char: characterdata) {
        strongestBonds.remove(char)
        groups.forEach {
            it.members.removeIf { it.pilot == char }
        }
    }

    fun makeGroupSet(chars: List<characterdata>): Set<GroupMember> {
        val set = mutableSetOf<characterdata>()

        chars.forEach { addToSetR(it, set) }

        val strSet = set.map {
            GroupMember(it, bondStrength(it, set))
        }.filter { it.strength >= strongestBonds[it.pilot] ?: 0.0 }

        strSet.forEach {
            val pilot = it.pilot
            strongestBonds[it.pilot] = it.strength
            groups.forEach {
                it.members.removeIf { it.pilot == pilot }
            }
        }

        return strSet.toSet()
    }

    fun bondStrength(char: characterdata, group: Set<characterdata>): Double =
            transaction { char.characterdata_groupings_char.filter { group.contains(it.otherchar) }.sumBy { it.score } }.toDouble() / (group.size * 0.9)
    //TODO some kind of thingy to make big groups above a certain threshhold.  No reason to make that many subgroups.
    //TODO want to look at sub group of possible members?  e.g. members w/o strength


    private fun addToSetR(char: characterdata, set: MutableSet<characterdata>) {
        if (set.contains(char))
            return

        set.add(char)

        transaction {
            char.characterdata_groupings_char.filter { it.score >= GROUP_THRESHOLD }.map { it.otherchar }
        }.forEach { addToSetR(it, set) }

    }

}

data class GroupMember(val pilot: characterdata, val strength: Double)

class Group(val latestKill: ZKill) {

    val members = Groups.makeGroupSet(latestKill.attackers.filter { it.isCharacter }.map { it.character!! }).toMutableSet()

    val pilots by lazy { members.map { it.pilot } }

    val location = latestKill.solarSystem

    val scouts by lazy { transaction { pilots.filter { isScoutShip(it.ship) } } }

    val ships by lazy { transaction { pilots.map { it.ship }.groupingBy { it }.eachCount().toList().sortedByDescending { it.second } } }
    val shipsPercent = lazy { ships.map { Pair(it.first, it.second / pilots.size) } }

    val alliances by lazy { transaction { pilots.map { it.alliance }.groupingBy { it }.eachCount().toList().sortedByDescending { it.second } } }
    val alliancesPercent = lazy { alliances.map { Pair(it.first, it.second / pilots.size) } }

    //TODO look at times (firstcreated in grouping?)
    val kills by lazy {
        transaction { kill.all().map { it.zKill } }.filter {
            val kill = it
            pilots.any { kill.playerAttackers.map { it.character!! }.contains(it) }
        }
    }

    init {
        Groups.groups.add(this)

        Groups.groups.removeIf { it.members.size < 2 }
    }

}