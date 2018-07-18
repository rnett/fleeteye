package com.rnett.ligraph.eve.fleeteye

import com.kizitonwose.time.milliseconds
import io.ktor.util.toLocalDateTime
import kotlinx.coroutines.experimental.*
import java.util.*

object FleetEye {

    fun process(kill: ZKill): Int {
        killQueue.push(kill)
        return queuedKills
    }

    private var queueProcess: Job

    private val killQueue = LinkedList<ZKill>()
    private var stop = false
    private var forceStop = false

    fun stopWhenEmpty() {
        stop = true
    }

    fun awaitStopWhenEmpty() {
        stop = true
        runBlocking {
            joinAll(queueProcess)
        }
    }

    fun forceStop() {
        forceStop = true
        runBlocking {
            joinAll(queueProcess)
        }
    }

    val queuedKills get() = killQueue.size

    private val averageTime = MovingAverage()
    val averageAddTime get() = averageTime.average

    private val averageQueued = MovingAverage()
    val averageQueuedKills get() = averageQueued.average

    private fun getProcess(): Job {

        return launch {
            while (!forceStop && (!stop || killQueue.isNotEmpty())) {
                if (killQueue.isEmpty()) {
                    delay(100)
                    continue
                } else {
                    val startTime = Calendar.getInstance().timeInMillis
                    doProcess(killQueue.pop())
                    val endTime = Calendar.getInstance().timeInMillis
                    val diffTime = (endTime - startTime).milliseconds.inSeconds.value
                    averageTime + diffTime
                }
            }
        }
    }

    fun start() {
        forceStop()
        runBlocking { delay(100) }

        stop = false
        forceStop = false

        queueProcess = getProcess()
    }

    init {
        launch {
            while (true) {
                delay(500)
                averageQueued + queuedKills
            }
        }

        queueProcess = getProcess()

    }

    private fun doProcess(kill: ZKill) {

        println("[${Date().toLocalDateTime()}] Processing Kill " + kill.killmailID)

        if (kill.playerAttackers.size < 2) {
            println("[${Date().toLocalDateTime()}] Single player attacker, skipping")
            println("[${Date().toLocalDateTime()}] Done\n")
            return
        }

        println("[${Date().toLocalDateTime()}] Adding Groupings")
        GroupingHelper.updateGroupings(kill)

        println("[${Date().toLocalDateTime()}] Updating Groups")
        Groups.process(kill)
        println("[${Date().toLocalDateTime()}] Done\n")
    }

    val groups get() = Groups.groups

}


class MovingAverage() : Number() {

    private var currentAverage = 0.0
    private var currentCount = 0.0

    val average get() = currentAverage

    operator fun invoke() = average

    constructor(values: Collection<Number>) : this() {
        add(values)
    }

    fun add(value: Number) {
        currentAverage = (currentAverage * currentCount + value.toDouble()) / (++currentCount)
    }

    fun add(vararg values: Number) {
        values.forEach { add(it) }
    }

    fun add(values: Collection<Number>) {
        values.forEach { add(it.toDouble()) }
    }


    operator fun plus(value: Number): MovingAverage {
        add(value); return this
    }

    operator fun plus(value: Collection<out Number>): MovingAverage {
        add(value); return this
    }


    override fun toByte(): Byte = currentAverage.toByte()

    override fun toChar(): Char = currentAverage.toChar()

    override fun toDouble(): Double = currentAverage.toDouble()

    override fun toFloat(): Float = currentAverage.toFloat()

    override fun toInt(): Int = currentAverage.toInt()

    override fun toLong(): Long = currentAverage.toLong()

    override fun toShort(): Short = currentAverage.toShort()

    override fun toString(): String {
        return currentAverage.toString()
    }
}
