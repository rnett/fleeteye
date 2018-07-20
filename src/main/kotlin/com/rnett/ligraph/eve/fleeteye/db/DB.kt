package com.rnett.ligraph.eve.fleeteye.db

import com.rnett.eve.ligraph.sde.invtype
import com.rnett.eve.ligraph.sde.invtypes
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.transactions.transaction


object kills : IntIdTable(columnName = "killid") {

    // Database columns


    val killid = integer("killid")
    val zkill = text("zkill")


    // Foreign keys


    // Helper methods

    fun findFromPKs(killid: Int): kill? {
        return kill.findById(killid)
    }

}

class kill(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<kill>(kills)

    // Database columns

    var killid by kills.killid
    var zkill by kills.zkill


    // Foreign keys


    // Helper Methods

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is kill)
            return false

        other
        return other.killid == killid
    }

    override fun hashCode(): Int {
        return transaction { killid } 

    }


}


object groupings : IntIdTable(columnName = "charid\" << 8 | \"othercharid") {

    // Database columns


    val charid = integer("charid")
    val othercharid = integer("othercharid")
    val firstcreated = long("firstcreated")
    val lastupdated = long("lastupdated")
    val score = integer("score")
    val hits = integer("hits")


    // Foreign keys

    // Many to One
    val char = reference("charid", characterdatas)
    val otherchar = reference("othercharid", characterdatas)

    // Helper methods

    fun idFromPKs(charid: Int, othercharid: Int): Int {
        return charid shl 8 or othercharid
    }

    fun findFromPKs(charid: Int, othercharid: Int): grouping? {
        return grouping.findById(idFromPKs(charid, othercharid))
    }

}

class grouping(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<grouping>(groupings)

    // Database columns

    var charid by groupings.charid
    var othercharid by groupings.othercharid
    var firstcreated by groupings.firstcreated
    var lastupdated by groupings.lastupdated
    var score by groupings.score
    var hits by groupings.hits


    // Foreign keys

    // Many to One
    val char by characterdata referencedOn groupings.char
    val otherchar by characterdata referencedOn groupings.otherchar


    // Helper Methods

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is grouping)
            return false

        other
        return other.charid == charid && other.othercharid == othercharid
    }

    override fun hashCode(): Int {
        return transaction { groupings.idFromPKs(charid, othercharid) } 

    }


}


object alliances : IntIdTable(columnName = "allianceid") {

    // Database columns


    val allianceid = integer("allianceid")
    var alliancename = varchar("alliancename", 100)


    // Foreign keys

    // One to Many (not present in object)


    // Helper methods

    fun findFromPKs(allianceid: Int): alliance? {
        return alliance.findById(allianceid)
    }

}

class alliance(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<alliance>(alliances)

    // Database columns

    var allianceid by alliances.allianceid
    var alliancename by alliances.alliancename


    // Foreign keys

    // One to Many
    val alliance_corporatia_alliance by corporation referrersOn corporations.alliance
    val alliance_characterdata_alliance by characterdata optionalReferrersOn characterdatas.alliance


    // Helper Methods

    override fun toString(): String {
        return this.alliancename
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is alliance)
            return false

        other
        return other.allianceid == allianceid
    }

    override fun hashCode(): Int {
        return transaction { allianceid } 

    }


}


object corporations : IntIdTable(columnName = "corpid") {

    // Database columns


    val corpid = integer("corpid")
    var corpname = varchar("corpname", 100)
    val allianceid = integer("allianceid").nullable()


    // Foreign keys

    // Many to One
    val alliance = reference("allianceid", alliances)

    // One to Many (not present in object)


    // Helper methods

    fun findFromPKs(corpid: Int): corporation? {
        return corporation.findById(corpid)
    }

}

class corporation(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<corporation>(corporations)

    // Database columns

    var corpid by corporations.corpid
    var corpname by corporations.corpname
    var allianceid by corporations.allianceid


    // Foreign keys

    // Many to One
    val alliance by com.rnett.ligraph.eve.fleeteye.db.alliance referencedOn corporations.alliance

    // One to Many
    val corporation_characterdata_corp by characterdata optionalReferrersOn characterdatas.corp


    // Helper Methods

    override fun toString(): String {
        return this.corpname
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is corporation)
            return false

        other
        return other.corpid == corpid
    }

    override fun hashCode(): Int {
        return transaction { corpid } 

    }


}


object characterdatas : IntIdTable(columnName = "charid") {

    // Database columns


    val charid = integer("charid")
    val shipid = integer("shipid")
    var charname = varchar("charname", 100)
    val allianceid = integer("allianceid").nullable()
    val corpid = integer("corpid").nullable()


    // Foreign keys

    // Many to One
    val alliance = optReference("allianceid", alliances)
    val corp = optReference("corpid", corporations)
    val ship = reference("shipid", invtypes)

    // One to Many (not present in object)


    // Helper methods

    fun findFromPKs(charid: Int): characterdata? {
        return characterdata.findById(charid)
    }

}

class characterdata(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<characterdata>(characterdatas)

    // Database columns

    var charid by characterdatas.charid
    var shipid by characterdatas.shipid
    var charname by characterdatas.charname
    var allianceid by characterdatas.allianceid
    var corpid by characterdatas.corpid


    // Foreign keys

    // Many to One
    val alliance by com.rnett.ligraph.eve.fleeteye.db.alliance optionalReferencedOn characterdatas.alliance
    val corp by corporation optionalReferencedOn characterdatas.corp
    val ship by invtype referencedOn characterdatas.ship

    // One to Many
    val characterdata_groupings_char by grouping referrersOn groupings.char
    val characterdata_groupings_otherchar by grouping referrersOn groupings.otherchar


    // Helper Methods

    override fun toString(): String {
        return this.charname
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is characterdata)
            return false

        other
        return other.charid == charid
    }

    override fun hashCode(): Int {
        return transaction { charid } 

    }


}

