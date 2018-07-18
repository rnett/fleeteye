package com.rnett.ligraph.eve.fleeteye

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.rnett.core.PostProcessable
import com.rnett.core.PostProcessingEnabler
import com.rnett.eve.ligraph.sde.*
import com.rnett.ligraph.eve.fleeteye.db.*
import org.jetbrains.exposed.sql.transactions.transaction

@JsonAdapter(PostProcessingEnabler::class)
class ZKill(
        val attackers: List<Attacker> = listOf(),
        val victim: Victim = Victim(),
        val zkb: Zkb = Zkb(),
        @SerializedName("killmail_id") val killmailID: Int = 0,
        @SerializedName("killmail_time") val killmailTime: String = "",
        @SerializedName("solar_system_id") val solarSystemID: Int = 0
) : PostProcessable {

    override fun gsonPostProcess() {
        _solarSystem = lazy { transaction { mapsolarsystems.findFromPKs(solarSystemID)!! } }
        _playerAttackers = attackers.filter { it.isCharacter }
    }

    @Transient
    private var _solarSystem: Lazy<mapsolarsystem?> = lazy { null }
    @Transient
    private var _playerAttackers: List<Attacker> = listOf<Attacker>()

    val solarSystem get() = _solarSystem.value!!
    val playerAttackers get() = _playerAttackers

    @JsonAdapter(PostProcessingEnabler::class)
    class Attacker(
            @SerializedName("damage_done") val damageDone: Int = 0,
            @SerializedName("final_blow") val finalBlow: Boolean = false,
            @SerializedName("security_status") val securityStatus: Double = 0.0,
            @SerializedName("ship_type_id") val shipTypeID: Int = 0,

            @SerializedName("faction_id") val factionID: Int? = null,
            @SerializedName("alliance_id") val allianceID: Int? = null,
            @SerializedName("corporation_id") val corporationID: Int? = null,
            @SerializedName("character_id") val characterID: Int? = null,
            @SerializedName("weapon_type_id") val weaponTypeID: Int? = null) : PostProcessable {

        override fun gsonPostProcess() {
            _alliance = lazy { if (allianceID == null) null else alliances.getOrMake(allianceID) }
            _corporation = lazy { if (corporationID == null) null else corporations.getOrMake(corporationID) }
            _character = lazy { if (characterID == null) null else characterdatas.getOrMake(characterID, shipTypeID) }
            _ship = lazy { invtypes.fromID(shipTypeID) }
        }

        val isCharacter: Boolean get() = characterID != null

        @Transient
        private var _alliance: Lazy<alliance?> = lazy { null }
        @Transient
        private var _corporation: Lazy<corporation?> = lazy { null }
        @Transient
        private var _character: Lazy<characterdata?> = lazy { null }
        @Transient
        private var _ship: Lazy<invtype?> = lazy { null }

        val alliance get() = _alliance.value
        val corporation get() = _corporation.value
        val character get() = _character.value
        val ship get() = _ship.value
    }

    @JsonAdapter(PostProcessingEnabler::class)
    data class Victim(
            @SerializedName("damage_taken") val damageTaken: Int = 0,
            @SerializedName("ship_type_id") val shipTypeID: Int = 0,

            @SerializedName("faction_id") val factionID: Int? = null,
            @SerializedName("alliance_id") val allianceID: Int? = null,
            @SerializedName("corporation_id") val corporationID: Int? = null,
            @SerializedName("character_id") val characterID: Int? = null) : PostProcessable {

        override fun gsonPostProcess() {
            _alliance = lazy { if (allianceID == null) null else alliances.getOrMake(allianceID) }
            _corporation = lazy { if (corporationID == null) null else corporations.getOrMake(corporationID) }
            _character = lazy { if (characterID == null) null else characterdatas.getOrMake(characterID, shipTypeID) }
            _ship = lazy { invtypes.fromID(shipTypeID) }
        }

        val isCharacter: Boolean get() = characterID != null

        @Transient
        private var _alliance: Lazy<alliance?> = lazy { null }
        @Transient
        private var _corporation: Lazy<corporation?> = lazy { null }
        @Transient
        private var _character: Lazy<characterdata?> = lazy { null }
        @Transient
        private var _ship: Lazy<invtype?> = lazy { null }

        val alliance get() = _alliance.value
        val corporation get() = _corporation.value
        val character get() = _character.value
        val ship get() = _ship.value

        //TODO do I need items?
    }

    data class Zkb(
            val locationID: Int = 0,
            val hash: String = "",
            val fittedValue: Double = 0.0,
            val totalValue: Double = 0.0,
            val npc: Boolean = false,
            val solo: Boolean = false,
            val awox: Boolean = false,

            val esi: String = "",
            val url: String = "")
}


