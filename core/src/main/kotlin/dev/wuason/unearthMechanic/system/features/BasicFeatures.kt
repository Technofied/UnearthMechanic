package dev.wuason.unearthMechanic.system.features

import dev.wuason.libs.adapter.Adapter
import dev.wuason.libs.adapter.AdapterData
import dev.wuason.unearthMechanic.UnearthMechanic
import dev.wuason.unearthMechanic.config.IGeneric
import dev.wuason.unearthMechanic.config.IStage
import dev.wuason.unearthMechanic.system.ILiveTool
import dev.wuason.unearthMechanic.system.compatibilities.ICompatibility
import kotlin.jvm.optionals.getOrNull
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.inventory.ItemStack

class BasicFeatures: AbstractFeature() {

    override fun onProcess(
        tick: Long,
        p: Player,
        comp: ICompatibility,
        event: Event,
        loc: Location,
        liveTool: ILiveTool,
        iStage: IStage,
        iGeneric: IGeneric
    ) {
    }

    private fun countToolInInventory(player: Player, toolAdapterData: AdapterData): Int {
        return (0..35).sumOf { slot ->
            val item = player.inventory.getItem(slot) ?: return@sumOf 0
            if (item.type.isAir) return@sumOf 0
            val data = Adapter.getAdapterData(Adapter.getAdapterId(item)).getOrNull() ?: return@sumOf 0
            if (data == toolAdapterData) item.amount else 0
        }
    }

    private fun removeToolFromInventory(player: Player, toolAdapterData: AdapterData, amount: Int) {
        var remaining = amount
        for (slot in 0..35) {
            if (remaining <= 0) break
            val item = player.inventory.getItem(slot) ?: continue
            if (item.type.isAir) continue
            val data = Adapter.getAdapterData(Adapter.getAdapterId(item)).getOrNull() ?: continue
            if (data != toolAdapterData) continue
            val toRemove = minOf(remaining, item.amount)
            val newAmount = item.amount - toRemove
            player.inventory.setItem(slot, if (newAmount <= 0) null else item.clone().apply { this.amount = newAmount })
            remaining -= toRemove
        }
    }

    override fun onApply(
        p: Player,
        comp: ICompatibility,
        event: Event,
        loc: Location,
        liveTool: ILiveTool,
        iStage: IStage,
        iGeneric: IGeneric
    ) {
        val reduceInv = iStage.getReduceItemInventory()
        val toolAdapterData = liveTool.getITool().getAdapterData()

        val batches = if (reduceInv > 0 && p.gameMode != GameMode.CREATIVE) {
            maxOf(1, countToolInInventory(p, toolAdapterData) / reduceInv)
        } else 1

        if (iStage.getDrops().isNotEmpty()) repeat(batches) { iStage.dropItems(loc, p) }
        if (iStage.getItems().isNotEmpty()) repeat(batches) { iStage.addItems(p) }

        if (iStage.isRemoveItemMainHand() && p.gameMode != GameMode.CREATIVE) liveTool.setItemMainHand(ItemStack(Material.AIR))

        if (iStage.getReduceItemHand() > 0) liveTool.getItemMainHand()?.let {
            if (p.gameMode != GameMode.CREATIVE) {
                if (!it.type.isAir) it.subtract(iStage.getReduceItemHand())
                UnearthMechanic.getInstance().getStageManager().getAnimator().getAnimation(p)?.let { anim ->
                    anim.updateItemMainHandData()
                }
            }
        }

        if (reduceInv > 0 && p.gameMode != GameMode.CREATIVE) {
            removeToolFromInventory(p, toolAdapterData, batches * reduceInv)
        }

        if (iStage.getSounds().isNotEmpty()) {
            iStage.getSounds().forEach { sound ->
                if (sound.delay > 0) {
                    Bukkit.getScheduler().runTaskLater(UnearthMechanic.getInstance(), Runnable {
                        p.playSound(
                            loc,
                            sound.soundId,
                            SoundCategory.BLOCKS,
                            sound.volume,
                            sound.pitch
                        )
                    }, sound.delay)
                } else {
                    p.playSound(
                        loc,
                        sound.soundId,
                        SoundCategory.BLOCKS,
                        sound.volume,
                        sound.pitch
                    )
                }
            }
        }
    }
}