/*
 * Skytils - Hypixel Skyblock Quality of Life Mod
 * Copyright (C) 2020-2024 Skytils
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package gg.skytils.skytilsmod.listeners

import gg.skytils.skytilsmod.Skytils.Companion.IO
import gg.skytils.skytilsmod.events.impl.HypixelPacketEvent
import gg.skytils.skytilsmod.events.impl.PacketEvent
import io.netty.buffer.Unpooled
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import net.hypixel.modapi.HypixelModAPI
import net.hypixel.modapi.error.ErrorReason
import net.hypixel.modapi.packet.ClientboundHypixelPacket
import net.hypixel.modapi.packet.HypixelPacket
import net.hypixel.modapi.packet.impl.serverbound.ServerboundVersionedPacket
import net.hypixel.modapi.serializer.PacketSerializer
import net.minecraft.client.network.NetHandlerPlayClient
import net.minecraft.network.PacketBuffer
import net.minecraft.network.play.client.C17PacketCustomPayload
import net.minecraft.network.play.server.S3FPacketCustomPayload
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.minutes

object ServerPayloadInterceptor {
    private val receivedPackets = MutableSharedFlow<ClientboundHypixelPacket>()

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onReceivePacket(event: PacketEvent.ReceiveEvent) {
        if (event.packet is S3FPacketCustomPayload) {
            IO.launch {
                val registry = HypixelModAPI.getInstance().registry
                val id = event.packet.channelName
                if (registry.isRegistered(id)) {
                    val packetSerializer = PacketSerializer(event.packet.bufferData.duplicate())
                    if (!packetSerializer.readBoolean()) {
                        val reason = ErrorReason.getById(packetSerializer.readVarInt())
                        HypixelPacketEvent.FailedEvent(id, reason).postAndCatch()
                    } else {
                        val packet = registry.createClientboundPacket(id, packetSerializer)
                        receivedPackets.emit(packet)
                        HypixelPacketEvent.ReceiveEvent(packet).postAndCatch()
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onSendPacket(event: PacketEvent.SendEvent) {
        if (event.packet is C17PacketCustomPayload) {
            val registry = HypixelModAPI.getInstance().registry
            val id = event.packet.channelName
            if (registry.isRegistered(id)) {
                HypixelPacketEvent.SendEvent(id).postAndCatch()
            }
        }
    }

    fun ServerboundVersionedPacket.toCustomPayload(): C17PacketCustomPayload {
        val buffer = PacketBuffer(Unpooled.buffer())
        val serializer = PacketSerializer(buffer)
        this.write(serializer)
        return C17PacketCustomPayload(this.identifier, buffer)
    }

    suspend fun <T : ClientboundHypixelPacket> ServerboundVersionedPacket.getResponse(handler: NetHandlerPlayClient): T = withTimeout(1.minutes) {
        val packet: C17PacketCustomPayload = this@getResponse.toCustomPayload()
        handler.addToSendQueue(packet)
        return@withTimeout receivedPackets.filter { it.identifier == this@getResponse.identifier }.first() as T
    }
}