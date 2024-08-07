package com.exptech.eew.client

import net.fabricmc.api.ClientModInitializer

class EewClient : ClientModInitializer {

    override fun onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (!hasDisplayedMessage && client.player != null) {
                client.player?.sendMessage(Text.literal("hello"), false)
                hasDisplayedMessage = true
            }
        }
    }
}
