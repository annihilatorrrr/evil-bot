package com.github.djaler.evilbot.handlers

import com.github.djaler.evilbot.components.TelegramClient
import com.github.djaler.evilbot.service.ChatService
import com.github.djaler.evilbot.service.UserService
import com.github.djaler.evilbot.utils.getForm
import com.github.djaler.evilbot.utils.getFormByGender
import com.github.insanusmokrassar.TelegramBotAPI.types.ExtendedBot
import com.github.insanusmokrassar.TelegramBotAPI.types.chat.abstracts.PublicChat
import com.github.insanusmokrassar.TelegramBotAPI.types.message.CommonMessageImpl
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UpdateStatisticHandler(
    private val chatService: ChatService,
    private val userService: UserService
) : CommonMessageHandler() {
    override val order = 0

    @Transactional
    override suspend fun handleMessage(message: CommonMessageImpl<*>): Boolean {
        val chat = message.chat as? PublicChat ?: return false

        val (chatEntity, _) = chatService.getOrCreateChatFrom(chat)
        val (userEntity, _) = userService.getOrCreateUserFrom(message.user)

        userService.registerMessageInStatistic(userEntity, chatEntity)

        return false
    }
}

@Component
class DisplayStatisticHandler(
    botInfo: ExtendedBot,
    private val chatService: ChatService,
    private val userService: UserService,
    private val telegramClient: TelegramClient
) : CommandHandler(
    botInfo,
    command = arrayOf("statistic"),
    commandDescription = "сколько сообщений ты написал"
) {
    override suspend fun handleCommand(message: CommonMessageImpl<*>, args: String?) {
        val chat = message.chat as? PublicChat ?: return

        val (chatEntity, _) = chatService.getOrCreateChatFrom(chat)
        val (userEntity, _) = userService.getOrCreateUserFrom(message.user)

        val statistic = userService.getStatistic(userEntity, chatEntity)
        if (statistic == null) {
            telegramClient.replyTextTo(
                message,
                "Ты не ${userEntity.getFormByGender("писал", "писала")} ещё ничего, алло"
            )
            return
        }

        val count = statistic.messagesCount

        telegramClient.replyTextTo(
            message,
            "Ты ${userEntity.getFormByGender("написал", "написала")} $count никому не ${count.getForm(
                "нужное сообщение",
                "нужных сообщения",
                "нужных сообщений"
            )}"
        )
    }
}


@Component
class DisplayTop10Handler(
    botInfo: ExtendedBot,
    private val chatService: ChatService,
    private val userService: UserService,
    private val telegramClient: TelegramClient
) : CommandHandler(
    botInfo,
    command = arrayOf("top10"),
    commandDescription = "кто больше всех пишет"
) {
    override suspend fun handleCommand(message: CommonMessageImpl<*>, args: String?) {
        val chat = message.chat as? PublicChat ?: return

        val (chatEntity, _) = chatService.getOrCreateChatFrom(chat)

        val top = userService.getTop(chatEntity, limit = 10)

        if (top.isEmpty()) {
            return
        }

        val text = top
            .mapIndexed { index, statistic -> "${(index + 1)}. ${statistic.user.username} - ${statistic.messagesCount}" }
            .joinToString("\n")

        telegramClient.replyTextTo(message, text, disableNotification = true)
    }
}
