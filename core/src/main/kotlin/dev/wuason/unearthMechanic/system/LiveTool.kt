package dev.wuason.unearthMechanic.system

import dev.wuason.libs.adapter.Adapter
import dev.wuason.libs.adapter.AdapterData
import dev.wuason.unearthMechanic.config.ITool
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import kotlin.jvm.optionals.getOrNull

class LiveTool(private var itemMainHand: ItemStack, private val iTool: ITool, private val player: Player, private val stageManager: StageManager, private val toolSlot: Int = -1): ILiveTool {

    private val initialSlotType: AdapterData? = if (toolSlot >= 0) {
        player.inventory.getItem(toolSlot)?.let { Adapter.getAdapterData(Adapter.getAdapterId(it)).getOrNull() }
    } else null

    override fun getItemMainHand(): ItemStack {
        if (toolSlot >= 0) {
            return player.inventory.getItem(toolSlot) ?: itemMainHand
        }
        return if (!stageManager.getAnimator().isAnimating(player)) {
            player.inventory.itemInMainHand
        } else {
            stageManager.getAnimator().getAnimation(player)!!.getItemMainHand()
        }
    }

    override fun getITool(): ITool {
        return iTool
    }

    override fun setItemMainHand(item: ItemStack) {
        itemMainHand = item
        if (toolSlot >= 0) {
            player.inventory.setItem(toolSlot, item)
        } else if (!stageManager.getAnimator().isAnimating(player)) {
            player.inventory.setItemInMainHand(item)
        } else {
            stageManager.getAnimator().getAnimation(player)?.setItemMainHand(item)
        }
    }

    override fun isValid(): Boolean {
        if (toolSlot >= 0) {
            val current = player.inventory.getItem(toolSlot)
            if (current == null || current.type.isAir) return initialSlotType == null
            val currentType = Adapter.getAdapterData(Adapter.getAdapterId(current)).getOrNull()
            return currentType == initialSlotType
        }
        if (stageManager.getAnimator().isAnimating(player)) {
            return stageManager.getAnimator().getAnimation(player)!!.isValid()
        }
        return player.inventory.itemInMainHand == itemMainHand
    }

    override fun isOriginalItem(): Boolean {
        return Adapter.getAdapterData(Adapter.getAdapterId(getItemMainHand())).get() == iTool.getAdapterData()
    }
}