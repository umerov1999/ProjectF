package dev.ragnarok.fenrir.api

import dev.ragnarok.fenrir.api.model.Captcha
import io.reactivex.rxjava3.core.Observable

interface ICaptchaProvider {
    /**
     * Запросить ввод капчи
     * После выполнения этого метода следует периодически проверять [dev.ragnarok.fenrir.api.ICaptchaProvider.lookupCode]
     *
     * @param sid     код капчи
     * @param captcha капча
     */
    fun requestCaptha(sid: String?, captcha: Captcha)

    /**
     * Отменить запрос капчи
     *
     * @param sid код капчи
     */
    fun cancel(sid: String)

    /**
     * Слушать отмену запроса капчи
     *
     * @return "паблишер" кода капчи
     */
    fun observeCanceling(): Observable<String>

    /**
     * Проверить, не появился ли введенный текст капчи
     *
     * @param sid код капчи
     * @return введенный пользователем текст с картинки
     * @throws OutOfDateException если капча больше не обрабатывается
     */
    @Throws(OutOfDateException::class)
    fun lookupCode(sid: String): String?

    /**
     * Этот "паблишер" уведомляет о том, что ожидается ввод капчи
     * Если наблюдатель получил уведомление отсюда - должен оповестить
     * с помощью метода [dev.ragnarok.fenrir.api.ICaptchaProvider.notifyThatCaptchaEntryActive]
     * о том, что активен и ожадает ввода пользователя
     *
     * @return "паблишер" кода капчи
     */
    fun observeWaiting(): Observable<String>

    /**
     * Уведомдить провайдер о том, что пользователь все еще в процессе ввода текста
     *
     * @param sid код капчи
     */
    fun notifyThatCaptchaEntryActive(sid: String)

    /**
     * Сохранение введенного пользователем текста с картинки
     *
     * @param sid  код капчи
     * @param code текст с картинки
     */
    fun enterCode(sid: String, code: String?)
}