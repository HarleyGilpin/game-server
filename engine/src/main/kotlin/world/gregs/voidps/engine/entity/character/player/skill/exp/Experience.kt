package world.gregs.voidps.engine.entity.character.player.skill.exp

import world.gregs.voidps.engine.data.Settings
import world.gregs.voidps.engine.entity.character.player.Player
import world.gregs.voidps.engine.entity.character.player.skill.Skill
import world.gregs.voidps.engine.entity.character.player.skill.level.Interpolation.interpolate
import world.gregs.voidps.engine.entity.character.player.skill.level.Level
import world.gregs.voidps.engine.event.EventDispatcher

class Experience(
    val experience: DoubleArray = defaultExperience.clone(),
    val blocked: MutableSet<Skill> = mutableSetOf(),
    private val maximum: Double = MAXIMUM_EXPERIENCE
) {

    lateinit var events: EventDispatcher

    fun get(skill: Skill): Double {
        return experience[skill.ordinal]
    }

    fun set(skill: Skill, experience: Double) {
        if (experience in 0.0..maximum && !blocked.contains(skill)) {
            val previous = get(skill)
            this.experience[skill.ordinal] = experience
            update(skill, previous)
        }
    }

    fun update(skill: Skill, previous: Double = get(skill)) {
        val experience = get(skill)
        events.emit(GrantExp(skill, previous, experience))
    }

    fun add(skill: Skill, baseExperience: Double) {
        if (baseExperience <= 0.0) return

        val rateMin = Settings["world.experienceRateMin", 1.0]
        val rateMax = Settings["world.experienceRateMax", 1.0]

        val playerLevel = level(skill, get(skill)).toDouble()
        val maxLevel = Level.MAX_LEVEL.toDouble()

        val modifier = if (rateMin != rateMax) {
            interpolate(rateMin, rateMax, playerLevel / maxLevel)
        } else {
            rateMin
        }

        val adjustedExperience = baseExperience * modifier

        if (blocked.contains(skill)) {
            events.emit(BlockedExperience(skill, adjustedExperience))
        } else {
            val current = get(skill)
            set(skill, current + adjustedExperience)
        }
    }

    fun addBlock(skill: Skill) {
        blocked.add(skill)
    }

    fun blocked(skill: Skill) = blocked.contains(skill)

    fun removeBlock(skill: Skill) {
        blocked.remove(skill)
    }

    companion object {
        const val MAXIMUM_EXPERIENCE = 200000000.0
        val defaultExperience = DoubleArray(Skill.count) {
            if (it == Skill.Constitution.ordinal) 1154.0 else 0.0
        }

        fun level(skill: Skill, experience: Double): Int {
            var total = 0
            for (level in 1..if (skill == Skill.Dungeoneering) 120 else 99) {
                total += Level.experienceAt(level)
                if (experience < total / 4) {
                    return if (skill == Skill.Constitution) level * 10 else level
                }
            }
            return if (skill == Skill.Constitution) 990 else 99
        }
    }
}

fun Player.exp(skill: Skill, experience: Double) = this.experience.add(skill, experience)