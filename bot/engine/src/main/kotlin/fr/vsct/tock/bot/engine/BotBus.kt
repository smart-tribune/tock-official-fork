/*
 * Copyright (C) 2017 VSCT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.vsct.tock.bot.engine

import fr.vsct.tock.bot.connector.ConnectorMessage
import fr.vsct.tock.bot.definition.Intent
import fr.vsct.tock.bot.definition.Step
import fr.vsct.tock.bot.engine.action.Action
import fr.vsct.tock.bot.engine.action.ActionSignificance
import fr.vsct.tock.bot.engine.action.SendChoice
import fr.vsct.tock.bot.engine.action.SendSentence
import fr.vsct.tock.bot.engine.dialog.ContextValue
import fr.vsct.tock.bot.engine.dialog.Dialog
import fr.vsct.tock.bot.engine.dialog.EntityStateValue
import fr.vsct.tock.bot.engine.dialog.Story
import fr.vsct.tock.bot.engine.message.Message
import fr.vsct.tock.bot.engine.message.MessagesList
import fr.vsct.tock.bot.engine.user.PlayerId
import fr.vsct.tock.bot.engine.user.UserPreferences
import fr.vsct.tock.bot.engine.user.UserTimeline
import fr.vsct.tock.nlp.api.client.model.Entity
import fr.vsct.tock.nlp.entity.Value
import fr.vsct.tock.translator.I18nKeyProvider
import fr.vsct.tock.translator.I18nLabelKey
import fr.vsct.tock.translator.UserInterfaceType
import java.util.Locale

/**
 *
 */
interface BotBus {

    val userTimeline: UserTimeline
    val dialog: Dialog
    val story: Story
    val action: Action
    var i18nProvider: I18nKeyProvider

    val applicationId: String
    val botId: PlayerId
    val userId: PlayerId
    val userPreferences: UserPreferences
    val userLocale: Locale
    val userInterfaceType: UserInterfaceType

    val entities: Map<String, EntityStateValue>
    val intent: Intent?

    /**
     * Returns the step of the action, null if the user action is not a [SendChoice]
     * or if the step is not set.
     */
    fun step(): Step? =
            if (action is SendChoice) {
                (action as SendChoice).step()
                        ?.let { s -> story.definition.steps.firstOrNull { it.name == s } }
            } else {
                null
            }

    /**
     * Returns the value of the specified choice parameter, null if the user action is not a [SendChoice]
     * or if this parameter is not set.
     */
    fun paramChoice(paramName: String): String? {
        return if (action is SendChoice) {
            (action as SendChoice).parameters[paramName]
        } else {
            null
        }
    }

    /**
     * Returns true if the current action has the specified entity role.
     */
    fun hasActionEntity(role: String): Boolean {
        return action.hasEntity(role)
    }

    /**
     * Returns the current entity value for the specified role.
     */
    fun <T : Value> entityValue(role: String): T? {
        @Suppress("UNCHECKED_CAST")
        return entities[role]?.value?.value as T?
    }

    /**
     * Update the current entity value in the dialog.
     * @param role entity role
     * @param newValue the new entity value
     */
    fun changeEntityValue(role: String, newValue: ContextValue?) {
        dialog.state.changeValue(role, newValue)
    }


    /**
     * Update the current entity value in the dialog.
     * @param entity the entity definition
     * @param newValue the new entity value
     */
    fun changeEntityValue(entity: Entity, newValue: Value?) {
        dialog.state.changeValue(entity, newValue)
    }

    /**
     * Remove all current entity values.
     */
    fun removeAllEntityValues() {
        dialog.state.removeAllEntityValues()
    }

    /**
     * Remove entity value for the specified role.
     */
    fun removeEntityValue(role: String) {
        dialog.state.removeValue(role)
    }

    /**
     * Returns the persistent current context value.
     */
    fun <T : Any> contextValue(name: String): T? {
        @Suppress("UNCHECKED_CAST")
        return dialog.state.context[name] as T?
    }

    /**
     * Update persistent context value.
     */
    fun changeContextValue(name: String, value: Any?) {
        if (value == null) dialog.state.context.remove(name) else dialog.state.context[name] = value
    }

    /**
     * Returns the non persistent current context value.
     */
    fun getBusContextValue(name: String): Any?

    /**
     * Update the non persistent current context value.
     */
    fun setBusContextValue(key: String, value: Any?)

    fun end(delay: Long = 0): BotBus {
        return endPlainText(null, delay)
    }

    fun end(i18nText: String, delay: Long = 0, vararg i18nArgs: Any?): BotBus {
        return endPlainText(translate(i18nText, *i18nArgs), delay)
    }

    fun end(i18nText: String, vararg i18nArgs: Any?): BotBus {
        return endPlainText(translate(i18nText, *i18nArgs))
    }

    fun endPlainText(plainText: String?, delay: Long = 0): BotBus {
        return end(SendSentence(botId, applicationId, userId, plainText), delay)
    }

    fun end(message: Message, delay: Long = 0): BotBus {
        return end(message.toAction(this), delay)
    }

    fun end(action: Action, delay: Long = 0): BotBus

    fun end(messages: MessagesList, initialDelay: Long = 0): BotBus {
        messages.messages.forEachIndexed { i, m ->
            val wait = initialDelay + m.delay
            if (messages.messages.size - 1 == i) {
                end(m.toAction(this), wait)
            } else {
                send(m.toAction(this), wait)
            }
        }
        return this;
    }


    fun send(i18nText: String, delay: Long = 0, vararg i18nArgs: Any?): BotBus {
        return sendPlainText(translate(i18nText, *i18nArgs), delay)
    }

    fun send(i18nText: String, vararg i18nArgs: Any?): BotBus {
        return sendPlainText(translate(i18nText, *i18nArgs))
    }

    fun send(delay: Long = 0): BotBus {
        return sendPlainText(null, delay)
    }

    fun sendPlainText(plainText: String?, delay: Long = 0): BotBus

    fun send(message: Message, delay: Long = 0): BotBus {
        return send(message.toAction(this), delay)
    }

    fun send(action: Action, delay: Long = 0): BotBus


    fun with(significance: ActionSignificance): BotBus

    fun with(message: ConnectorMessage): BotBus

    fun translate(text: String?, arg: Any?): String {
        if (text.isNullOrBlank()) {
            return ""
        } else {
            return translate(i18nProvider.i18nKeyFromLabel(text!!, listOf(arg)))
        }
    }

    fun translate(text: String?, vararg args: Any?): String {
        if (text.isNullOrBlank()) {
            return ""
        } else {
            return translate(i18nProvider.i18nKeyFromLabel(text!!, args.toList()))
        }
    }

    fun translate(key: I18nLabelKey?): String
}